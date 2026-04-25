@file:Suppress("unused")

package com.github.serivesmejia.eocvsim.util.event

import io.github.deltacv.common.util.loggerOf
import kotlinx.coroutines.*
import kotlin.reflect.KClass

class Orchestrator(
    private val scope: CoroutineScope,
    val tasks: List<Orchestrable> = emptyList(),
    val name: String? = null,
) {

    enum class Phase {
        INIT,
        RUN,
        DESTROY
    }

    sealed class Dependency {
        data class Instance(val target: Orchestrable) : Dependency()
        data class Class(val target: KClass<out Orchestrable>) : Dependency()
    }

    private val logger by loggerOf("Orchestrator:${name ?: Integer.toHexString(hashCode())}")

    data class Registration(
        val instance: Orchestrable,
        val phases: Map<Phase, PhaseRegistration>
    ) {
        data class PhaseRegistration(
            val target: suspend () -> Unit,
            val dependencies: List<Dependency>
        )
    }

    class RegistrationCtx<T : Orchestrable> internal constructor(
        val instance: T
    ) {

        private data class MutablePhaseRegistration<T : Orchestrable>(
            val dependencyInstances: MutableList<Dependency> = mutableListOf(),
            var targetRunner: (suspend (T) -> Unit)? = null
        )

        private val phaseRegistrations = mutableMapOf<Phase, MutablePhaseRegistration<T>>()
        private var activePhase: Phase = Phase.INIT

        private fun currentPhaseRegistration(): MutablePhaseRegistration<T> {
            return phaseRegistrations.getOrPut(activePhase) { MutablePhaseRegistration() }
        }

        fun phase(phase: Phase, block: RegistrationCtx<T>.() -> Unit) {
            val previous = activePhase
            activePhase = phase
            try {
                block()
            } finally {
                activePhase = previous
            }
        }

        fun dependsOn(vararg targets: Orchestrable) {
            for (target in targets) {
                addDependency(Dependency.Instance(target))
            }
        }

        fun dependsOn(vararg targets: KClass<out Orchestrable>) {
            for (target in targets) {
                addDependency(Dependency.Class(target))
            }
        }

        fun dependsOn(vararg dependencies: Dependency) {
            for (dependency in dependencies) {
                addDependency(dependency)
            }
        }

        inline fun <reified D : Orchestrable> dependsOn() {
            addDependency(Dependency.Class(D::class))
        }

        @PublishedApi
        internal fun addDependency(dependency: Dependency) {
            val current = currentPhaseRegistration().dependencyInstances
            if (current.none { matchesDependency(it, dependency) }) {
                current.add(dependency)
            }
        }

        private fun matchesDependency(left: Dependency, right: Dependency): Boolean {
            return when (left) {
                is Dependency.Instance -> when (right) {
                    is Dependency.Instance -> left.target === right.target
                    is Dependency.Class -> false
                }
                is Dependency.Class -> when (right) {
                    is Dependency.Instance -> false
                    is Dependency.Class -> left.target == right.target
                }
            }
        }

        fun target(block: suspend T.() -> Unit) {
            currentPhaseRegistration().targetRunner = block
        }

        internal fun build(): Registration {
            val builtPhases = phaseRegistrations.mapNotNull { (phase, phaseRegistration) ->
                val runner = phaseRegistration.targetRunner ?: return@mapNotNull null

                phase to Registration.PhaseRegistration(
                    target = { runner(instance) },
                    dependencies = phaseRegistration.dependencyInstances.toList()
                )
            }.toMap()

            if (builtPhases.isEmpty()) {
                throw IllegalArgumentException("register { } requires at least one phase target { ... }")
            }

            return Registration(
                instance = instance,
                phases = builtPhases
            )
        }
    }

    private val registrations = mutableListOf<Registration>()
    private var tasksWired = false
    private var currentPhase: Phase = Phase.INIT
    private var runPhaseStarted = false
    private val executionGraphLoggedPhases = mutableSetOf<Phase>()
    private var runGraphLogged = false

    fun changePhase(phase: Phase) {
        if (phase == currentPhase) {
            return
        }

        if (currentPhase == Phase.RUN && runPhaseStarted) {
            logger.info("Orchestration phase RUN finished successfully")
            runPhaseStarted = false
        }

        currentPhase = phase
    }

    fun getPhase(): Phase = currentPhase

    private fun ensureTasksWired() {
        if (tasksWired) {
            return
        }

        if (tasks.isNotEmpty()) {
            logger.debug("Wiring ${tasks.size} task(s) into orchestrator")
        }

        tasks.forEach { task ->
            task.wire(this)
        }

        tasksWired = true
    }

    fun <T : Orchestrable> register(targetInstance: T, block: RegistrationCtx<T>.() -> Unit = {}) {
        val registration = RegistrationCtx(targetInstance).apply(block).build()
        register(registration)
    }

    fun register(registration: Registration) {
        if (registrations.any { it.instance === registration.instance }) {
            throw IllegalArgumentException(
                "Target instance already registered: ${registration.instance::class.qualifiedName}"
            )
        }

        registrations.add(registration)

        logger.debug(
            "Registered target ${registration.instance::class.qualifiedName} with ${registration.phases.size} phase(s)"
        )
    }

    fun orchestrate() {
        ensureTasksWired()

        val phase = currentPhase

        runBlocking(scope.coroutineContext) {
            val verboseDebug = phase != Phase.RUN
            val logLifecycle = beginLifecycleLog(phase)

            val phaseRegistrations = activeRegistrationsFor(phase)

            logExecutionGraphOnce(phase, phaseRegistrations)

            if (logLifecycle) {
                logger.info("Starting orchestration phase $phase with ${phaseRegistrations.size} registration(s)")
            }
            validateDependenciesExist(phase, phaseRegistrations, verboseDebug)

            val pending = phaseRegistrations.toMutableList()
            val executedTargets = mutableListOf<Orchestrable>()
            var wave = 0

            while (pending.isNotEmpty()) {
                wave++

                val ready = pending.filter { registration ->
                    registration.dependencies.all { dependency ->
                        executedTargets.any { executed -> matchesDependency(executed, dependency) }
                    }
                }

                if (verboseDebug) {
                    logger.debug(
                        "Wave $wave -> pending=${pending.size}, ready=${ready.size}, executed=${executedTargets.size}"
                    )
                }

                if (verboseDebug && ready.isNotEmpty()) {
                    val readyNames = ready.joinToString { it.instance::class.qualifiedName ?: "<anonymous>" }
                    logger.debug("Wave $wave ready targets: $readyNames")
                }

                if (ready.isEmpty()) {
                    val remaining = pending.joinToString { it.instance::class.qualifiedName ?: "<anonymous>" }
                    logger.error("Deadlock/cycle detected in phase $phase, remaining: $remaining")

                    throw IllegalStateException(
                        "Circular dependency detected in phase $phase among remaining targets: $remaining"
                    )
                }

                // Execute this dependency wave in parallel.
                runBatchInParallel(ready, wave, verboseDebug)

                for (registration in ready) {
                    executedTargets.add(registration.instance)
                    pending.remove(registration)
                }
            }

            if (logLifecycle && phase != Phase.RUN) {
                logger.info("Orchestration phase $phase finished successfully")
            }
        }
    }

    fun orchestrate(phase: Phase) {
        changePhase(phase)
        orchestrate()
    }

    private fun beginLifecycleLog(phase: Phase): Boolean {
        if (phase != Phase.RUN) {
            return true
        }

        if (runPhaseStarted) {
            return false
        }

        runPhaseStarted = true
        return true
    }

    private fun activeRegistrationsFor(phase: Phase): List<ActiveRegistration> {
        return registrations.mapNotNull { registration ->
            val phaseRegistration = registration.phases[phase] ?: return@mapNotNull null
            ActiveRegistration(
                instance = registration.instance,
                target = phaseRegistration.target,
                dependencies = phaseRegistration.dependencies
            )
        }
    }

    private fun logExecutionGraphOnce(phase: Phase, phaseRegistrations: List<ActiveRegistration>) {
        if (phase in executionGraphLoggedPhases) {
            return
        }

        executionGraphLoggedPhases.add(phase)

        logger.debug("Execution graph plan for phase {}:", phase)
        logger.debug("Phase {}: {} target(s)", phase, phaseRegistrations.size)

        if (phaseRegistrations.isEmpty()) {
            return
        }

        val pending = phaseRegistrations.toMutableList()
        val executedTargets = mutableListOf<Orchestrable>()
        var wave = 0

        while (pending.isNotEmpty()) {
            wave++

            val ready = pending.filter { registration ->
                registration.dependencies.all { dependency ->
                    executedTargets.any { executed -> matchesDependency(executed, dependency) }
                }
            }

            if (ready.isEmpty()) {
                val remaining = pending.joinToString { it.instance::class.qualifiedName ?: "<anonymous>" }
                logger.debug("Phase {} wave {}: <cycle/unresolved> remaining=[{}]", phase, wave, remaining)
                break
            }

            val readyNames = ready.joinToString { it.instance::class.qualifiedName ?: "<anonymous>" }
            logger.debug("Phase {} wave {} (parallel={}): [{}]", phase, wave, ready.size, readyNames)

            for (registration in ready) {
                executedTargets.add(registration.instance)
                pending.remove(registration)
            }
        }
    }

    private fun logRunGraphOnce(phaseRegistrations: List<ActiveRegistration>) {
        if (runGraphLogged) {
            return
        }

        runGraphLogged = true

        logger.debug("RUN graph has ${phaseRegistrations.size} registration(s)")
        phaseRegistrations.forEach { registration ->
            val target = registration.instance::class.qualifiedName ?: "<anonymous>"
            val deps = if (registration.dependencies.isEmpty()) {
                "<none>"
            } else {
                registration.dependencies.joinToString { dependency ->
                    when (dependency) {
                        is Dependency.Instance -> dependency.target::class.qualifiedName ?: "<anonymous>"
                        is Dependency.Class -> dependency.target.qualifiedName ?: "<anonymous>"
                    }
                }
            }

            logger.debug("RUN node: $target dependsOn [$deps]")
        }
    }

    private data class ActiveRegistration(
        val instance: Orchestrable,
        val target: suspend () -> Unit,
        val dependencies: List<Dependency>
    )

    private suspend fun runBatchInParallel(
        registrations: List<ActiveRegistration>,
        wave: Int,
        verboseDebug: Boolean
    ) = coroutineScope {
        val start = System.nanoTime()
        if (verboseDebug) {
            logger.debug("Wave $wave launching ${registrations.size} parallel target(s)")
        }

        registrations.map { registration ->
            async {
                val targetName = registration.instance::class.qualifiedName ?: "<anonymous>"
                val targetStart = System.nanoTime()

                if (verboseDebug) {
                    logger.debug("Wave $wave start target $targetName")
                }
                invokeTarget(registration)

                val targetMs = (System.nanoTime() - targetStart) / 1_000_000
                if (verboseDebug) {
                    logger.debug("Wave $wave finished target $targetName in ${targetMs}ms")
                }
            }
        }.awaitAll()

        val batchMs = (System.nanoTime() - start) / 1_000_000
        if (verboseDebug) {
            logger.debug("Wave $wave completed in ${batchMs}ms")
        }
    }

    private fun validateDependenciesExist(
        phase: Phase,
        phaseRegistrations: List<ActiveRegistration>,
        verboseDebug: Boolean
    ) {
        if (verboseDebug) {
            logger.debug("Validating dependencies for phase {}", phase)
        }

        for (registration in phaseRegistrations) {
            for (dependency in registration.dependencies) {
                val exists = phaseRegistrations.any { candidate ->
                    matchesDependency(candidate.instance, dependency)
                }

                if (!exists) {
                    val dependencyName = when (dependency) {
                        is Dependency.Instance -> dependency.target::class.qualifiedName
                        is Dependency.Class -> dependency.target.qualifiedName
                    }

                    logger.error(
                        "Unresolved dependency $dependencyName required by ${registration.instance::class.qualifiedName} in phase $phase"
                    )

                    throw IllegalStateException(
                        "Unresolved dependency $dependencyName required by ${registration.instance::class.simpleName} in phase $phase"
                    )
                }
            }
        }

        if (verboseDebug) {
            logger.debug("Dependency validation passed for phase {}", phase)
        }
    }

    private suspend fun invokeTarget(registration: ActiveRegistration) {
        try {
            registration.target()
        } catch (ex: Exception) {
            val targetName = registration.instance::class.qualifiedName ?: "<anonymous>"
            logger.error("Invocation failed for $targetName", ex)

            throw IllegalStateException(
                "Invocation failed for $targetName",
                ex
            )
        }
    }

    private fun matchesDependency(candidate: Orchestrable, dependency: Dependency): Boolean {
        return when (dependency) {
            is Dependency.Instance -> candidate === dependency.target
            is Dependency.Class -> dependency.target.isInstance(candidate)
        }
    }
}
interface Orchestrable {
    fun wire(orchestrator: Orchestrator)
}

interface PhaseOrchestrable : Orchestrable {
    val initDependencies: List<Orchestrator.Dependency>
        get() = emptyList()

    val runDependencies: List<Orchestrator.Dependency>
        get() = emptyList()

    val destroyDependencies: List<Orchestrator.Dependency>
        get() = emptyList()

    suspend fun init()
    suspend fun run()
    suspend fun destroy()

    override fun wire(orchestrator: Orchestrator) {
        orchestrator.register(this) {
            phase(Orchestrator.Phase.INIT) {
                target { init() }
                dependsOn(*initDependencies.toTypedArray())
            }

            phase(Orchestrator.Phase.RUN) {
                target { run() }
                dependsOn(*runDependencies.toTypedArray())
            }

            phase(Orchestrator.Phase.DESTROY) {
                target { destroy() }
                dependsOn(*destroyDependencies.toTypedArray())
            }
        }
    }
}

abstract class MagicPhaseOrchestrable : PhaseOrchestrable {

    private val _initDependencies = mutableListOf<Orchestrator.Dependency>()
    override val initDependencies = _initDependencies

    private val _runDependencies = mutableListOf<Orchestrator.Dependency>()
    override val runDependencies = _runDependencies

    private val _destroyDependencies = mutableListOf<Orchestrator.Dependency>()
    override val destroyDependencies = _destroyDependencies

    /**
     * @param phase if empty, will bind to all phases
     */
    fun <T: Orchestrable> dependency(target: KClass<T>, vararg phase: Orchestrator.Phase): KClass<T> {
        if(phase.isEmpty()) {
            initDependency(target)
            runDependency(target)
            destroyDependency(target)
        } else {
            for (p in phase) {
                when (p) {
                    Orchestrator.Phase.INIT -> initDependency(target)
                    Orchestrator.Phase.RUN -> runDependency(target)
                    Orchestrator.Phase.DESTROY -> destroyDependency(target)
                }
            }
        }

        return target
    }

    fun <T: Orchestrable> dependency(target: T, vararg phase: Orchestrator.Phase): T {
        if(phase.isEmpty()) {
            initDependency(target)
            runDependency(target)
            destroyDependency(target)
        } else {
            for (p in phase) {
                when (p) {
                    Orchestrator.Phase.INIT -> initDependency(target)
                    Orchestrator.Phase.RUN -> runDependency(target)
                    Orchestrator.Phase.DESTROY -> destroyDependency(target)
                }
            }
        }

        return target
    }

    inline fun <reified T: Orchestrable> dependency(target: Lazy<T>, vararg phase: Orchestrator.Phase): Lazy<T> {
        dependency(T::class, *phase)
        return target
    }

    // -- INIT DEPENDENCY SUGAR --

    fun <T: Orchestrable> initDependency(target: KClass<T>): KClass<T> {
        initDependencies.add(Orchestrator.Dependency.Class(target))
        return target
    }

    fun <T: Orchestrable> initDependency(target: T): T {
        initDependencies.add(Orchestrator.Dependency.Instance(target))
        return target
    }

    inline fun <reified T: Orchestrable> initDependency(target: Lazy<T>): Lazy<T> {
        initDependency(T::class)
        return target
    }

    // -- RUN DEPENDENCY SUGAR --

    fun <T: Orchestrable> runDependency(target: KClass<T>): KClass<T> {
        runDependencies.add(Orchestrator.Dependency.Class(target))
        return target
    }

    fun <T: Orchestrable> runDependency(target: T): T {
        runDependencies.add(Orchestrator.Dependency.Instance(target))
        return target
    }

    inline fun <reified T: Orchestrable> runDependency(target: Lazy<T>): Lazy<T> {
        runDependency(T::class)
        return target
    }

    // -- DESTROY DEPENDENCY SUGAR --

    fun <T: Orchestrable> destroyDependency(target: KClass<T>): KClass<T> {
        destroyDependencies.add(Orchestrator.Dependency.Class(target))
        return target
    }

    fun <T: Orchestrable> destroyDependency(target: T): T {
        destroyDependencies.add(Orchestrator.Dependency.Instance(target))
        return target
    }

    inline fun <reified T: Orchestrable> destroyDependency(target: Lazy<T>): Lazy<T> {
        destroyDependency(T::class)
        return target
    }
}