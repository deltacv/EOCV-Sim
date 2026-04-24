@file:Suppress("unused")

package com.github.serivesmejia.eocvsim.util.event

import io.github.deltacv.common.util.loggerOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class Orchestrator(
    val name: String? = null,
    private val scope: CoroutineScope
) {

    private val logger by loggerOf("Orchestrator:${name ?: Integer.toHexString(hashCode())}")

    data class Registration(
        val instance: Orchestrable,
        val target: suspend () -> Unit,
        val dependencies: List<Orchestrable>
    )

    class RegistrationCtx<T : Orchestrable> internal constructor(
        val instance: T
    ) {

        private val dependencyInstances = mutableListOf<Orchestrable>()
        private var targetRunner: (suspend (T) -> Unit)? = null

        fun dependsOn(vararg targets: Orchestrable) {
            for (target in targets) {
                if (dependencyInstances.none { it === target }) {
                    dependencyInstances.add(target)
                }
            }
        }

        fun target(block: suspend (T) -> Unit) {
            targetRunner = block
        }

        internal fun build(): Registration {
            val runner = targetRunner
                ?: throw IllegalArgumentException("register { } requires targetMethod { instance -> ... }")

            return Registration(
                instance = instance,
                target = { runner(instance) },
                dependencies = dependencyInstances.toList()
            )
        }
    }

    private val registrations = mutableListOf<Registration>()

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
            "Registered target ${registration.instance::class.qualifiedName} with ${registration.dependencies.size} dependency(ies)"
        )
    }

    fun orchestrate() {
        runBlocking(scope.coroutineContext) {
            logger.info("Starting orchestration with ${registrations.size} registration(s)")
            validateDependenciesExist()

            val pending = registrations.toMutableList()
            val executedTargets = mutableListOf<Any>()
            var wave = 0

            while (pending.isNotEmpty()) {
                wave++

                val ready = pending.filter { registration ->
                    registration.dependencies.all { dep -> executedTargets.any { it === dep } }
                }

                logger.debug(
                    "Wave $wave -> pending=${pending.size}, ready=${ready.size}, executed=${executedTargets.size}"
                )

                if (ready.isNotEmpty()) {
                    val readyNames = ready.joinToString { it.instance::class.qualifiedName ?: "<anonymous>" }
                    logger.debug("Wave $wave ready targets: $readyNames")
                }

                if (ready.isEmpty()) {
                    val remaining = pending.joinToString { it.instance::class.qualifiedName ?: "<anonymous>" }
                    logger.error("Deadlock/cycle detected, remaining: $remaining")

                    throw IllegalStateException(
                        "Circular dependency detected among remaining targets: $remaining"
                    )
                }

                // Execute this dependency wave in parallel.
                runBatchInParallel(ready, wave)

                for (registration in ready) {
                    executedTargets.add(registration.instance)
                    pending.remove(registration)
                }
            }

            logger.info("Orchestration finished successfully")
        }
    }

    private suspend fun runBatchInParallel(registrations: List<Registration>, wave: Int) = coroutineScope {
        val start = System.nanoTime()
        logger.debug("Wave $wave launching ${registrations.size} parallel target(s)")

        registrations.map { registration ->
            async {
                val targetName = registration.instance::class.qualifiedName ?: "<anonymous>"
                val targetStart = System.nanoTime()

                logger.debug("Wave $wave start target $targetName")
                invokeTarget(registration)

                val targetMs = (System.nanoTime() - targetStart) / 1_000_000
                logger.debug("Wave $wave finished target $targetName in ${targetMs}ms")
            }
        }.awaitAll()

        val batchMs = (System.nanoTime() - start) / 1_000_000
        logger.debug("Wave $wave completed in ${batchMs}ms")
    }

    private fun validateDependenciesExist() {
        logger.debug("Validating dependencies")

        for (registration in registrations) {
            for (dependency in registration.dependencies) {
                val exists = registrations.any { it.instance === dependency }

                if (!exists) {
                    logger.error(
                        "Unresolved dependency ${dependency::class.qualifiedName} required by ${registration.instance::class.qualifiedName}"
                    )

                    throw IllegalStateException(
                        "Unresolved dependency ${dependency::class.simpleName} required by ${registration.instance::class.simpleName}"
                    )
                }
            }
        }

        logger.debug("Dependency validation passed")
    }

    private suspend fun invokeTarget(registration: Registration) {
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
}
interface Orchestrable {

}
