@file:Suppress("unused")

package com.github.serivesmejia.eocvsim.util.orchestration

import kotlin.reflect.KClass

interface Orchestrable {
    fun wire(orchestrator: Orchestrator)
}

class PhaseDependencies {
    private val _initDependencies = mutableListOf<Orchestrator.Dependency>()
    private val _runDependencies = mutableListOf<Orchestrator.Dependency>()
    private val _destroyDependencies = mutableListOf<Orchestrator.Dependency>()

    val initDependencies: List<Orchestrator.Dependency> get() = _initDependencies
    val runDependencies: List<Orchestrator.Dependency> get() = _runDependencies
    val destroyDependencies: List<Orchestrator.Dependency> get() = _destroyDependencies

    /**
     * @param phase if empty, will bind to all phases
     */
    fun <T : Orchestrable> dependency(target: KClass<T>, vararg phase: Orchestrator.Phase): KClass<T> {
        if (phase.isEmpty()) {
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

    fun <T : Orchestrable> dependency(target: T, vararg phase: Orchestrator.Phase): T {
        if (phase.isEmpty()) {
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

    inline fun <reified T : Orchestrable> dependency(target: Lazy<T>, vararg phase: Orchestrator.Phase): Lazy<T> {
        dependency(T::class, *phase)
        return target
    }

    // -- INIT DEPENDENCY SUGAR --

    fun <T : Orchestrable> initDependency(target: KClass<T>): KClass<T> {
        _initDependencies.add(Orchestrator.Dependency.Class(target))
        return target
    }

    fun <T : Orchestrable> initDependency(target: T): T {
        _initDependencies.add(Orchestrator.Dependency.Instance(target))
        return target
    }

    inline fun <reified T : Orchestrable> initDependency(target: Lazy<T>): Lazy<T> {
        initDependency(T::class)
        return target
    }

    // -- RUN DEPENDENCY SUGAR --

    fun <T : Orchestrable> runDependency(target: KClass<T>): KClass<T> {
        _runDependencies.add(Orchestrator.Dependency.Class(target))
        return target
    }

    fun <T : Orchestrable> runDependency(target: T): T {
        _runDependencies.add(Orchestrator.Dependency.Instance(target))
        return target
    }

    inline fun <reified T : Orchestrable> runDependency(target: Lazy<T>): Lazy<T> {
        runDependency(T::class)
        return target
    }

    // -- DESTROY DEPENDENCY SUGAR --

    fun <T : Orchestrable> destroyDependency(target: KClass<T>): KClass<T> {
        _destroyDependencies.add(Orchestrator.Dependency.Class(target))
        return target
    }

    fun <T : Orchestrable> destroyDependency(target: T): T {
        _destroyDependencies.add(Orchestrator.Dependency.Instance(target))
        return target
    }

    inline fun <reified T : Orchestrable> destroyDependency(target: Lazy<T>): Lazy<T> {
        destroyDependency(T::class)
        return target
    }
}

interface PhaseOrchestrable : Orchestrable {
    val phaseDependencies: PhaseDependencies

    suspend fun init()
    suspend fun run()
    suspend fun destroy()

    override fun wire(orchestrator: Orchestrator) {
        orchestrator.register(this) {
            phase(Orchestrator.Phase.INIT) {
                target { init() }
                dependsOn(*phaseDependencies.initDependencies.toTypedArray())
            }

            phase(Orchestrator.Phase.RUN) {
                target { run() }
                dependsOn(*phaseDependencies.runDependencies.toTypedArray())
            }

            phase(Orchestrator.Phase.DESTROY) {
                target { destroy() }
                dependsOn(*phaseDependencies.destroyDependencies.toTypedArray())
            }
        }
    }
}

/**
 * Dependency helper extensions for any [PhaseOrchestrable].
 */

/**
 * @param phase if empty, will bind to all phases
 */
fun <T : Orchestrable> PhaseOrchestrable.dependency(target: KClass<T>, vararg phase: Orchestrator.Phase): KClass<T> {
    phaseDependencies.dependency(target, *phase)
    return target
}

fun <T : Orchestrable> PhaseOrchestrable.dependency(target: T, vararg phase: Orchestrator.Phase): T {
    phaseDependencies.dependency(target, *phase)
    return target
}

inline fun <reified T : Orchestrable> PhaseOrchestrable.dependency(target: Lazy<T>, vararg phase: Orchestrator.Phase): Lazy<T> {
    phaseDependencies.dependency(T::class, *phase)
    return target
}

// -- INIT DEPENDENCY SUGAR --

fun <T : Orchestrable> PhaseOrchestrable.initDependency(target: KClass<T>): KClass<T> = phaseDependencies.initDependency(target)

fun <T : Orchestrable> PhaseOrchestrable.initDependency(target: T): T = phaseDependencies.initDependency(target)

inline fun <reified T : Orchestrable> PhaseOrchestrable.initDependency(target: Lazy<T>): Lazy<T> {
    phaseDependencies.initDependency(T::class)
    return target
}

// -- RUN DEPENDENCY SUGAR --

fun <T : Orchestrable> PhaseOrchestrable.runDependency(target: KClass<T>): KClass<T> = phaseDependencies.runDependency(target)

fun <T : Orchestrable> PhaseOrchestrable.runDependency(target: T): T = phaseDependencies.runDependency(target)

inline fun <reified T : Orchestrable> PhaseOrchestrable.runDependency(target: Lazy<T>): Lazy<T> {
    phaseDependencies.runDependency(T::class)
    return target
}

// -- DESTROY DEPENDENCY SUGAR --

fun <T : Orchestrable> PhaseOrchestrable.destroyDependency(target: KClass<T>): KClass<T> = phaseDependencies.destroyDependency(target)

fun <T : Orchestrable> PhaseOrchestrable.destroyDependency(target: T): T = phaseDependencies.destroyDependency(target)

inline fun <reified T : Orchestrable> PhaseOrchestrable.destroyDependency(target: Lazy<T>): Lazy<T> {
    phaseDependencies.destroyDependency(T::class)
    return target
}

abstract class PhaseOrchestrableBase : PhaseOrchestrable {
    final override val phaseDependencies: PhaseDependencies = PhaseDependencies()
}