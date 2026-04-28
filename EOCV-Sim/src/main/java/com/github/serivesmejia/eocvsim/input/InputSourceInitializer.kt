package com.github.serivesmejia.eocvsim.input

import com.github.serivesmejia.eocvsim.util.event.ParamEventHandler
import io.github.deltacv.common.util.loggerForThis
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import java.util.concurrent.atomic.AtomicInteger

class InputSourceInitializer : KoinComponent {

    enum class Result { SUCCESS, FAILED, CANCELED, TIMED_OUT }

    companion object {
        const val TIMEOUT = 10000L

        fun runWithTimeout(sourceName: String, callback: () -> Boolean): Result {
            val initializer = GlobalContext.get().get<InputSourceInitializer>()
            return initializer.runWithTimeout(sourceName, false, callback)
        }

        fun runWithTimeout(inputSource: InputSource, callback: () -> Boolean): Result {
            val initializer = GlobalContext.get().get<InputSourceInitializer>()
            return initializer.runWithTimeout(inputSource.name, inputSource.hasSlowInitialization, callback)
        }
        
        fun initializeWithTimeout(inputSource: InputSource): Result {
            val initializer = GlobalContext.get().get<InputSourceInitializer>()
            return initializer.initializeWithTimeout(inputSource)
        }
    }

    private val scope: CoroutineScope by inject()

    val logger by loggerForThis()

    // Event fired when an initialization that may need UI interaction starts.
    // Listeners will receive the InitSession payload directly.
    val onInitBegin = ParamEventHandler<InitSession>("InputSourceInitBegin")

    private val sessionIdCounter = AtomicInteger(0)
    data class InitSession(
        val id: Int,
        val inputSource: InputSource?,
        val sourceName: String?,
        val cancelJob: Job,
        val resultSignal: CompletableDeferred<Result>,
        val hasSlowInitialization: Boolean = false
    )

    fun initializeWithTimeout(inputSource: InputSource): Result {
        val resultSignal = CompletableDeferred<Result>()
        val cancelSignal = Job().apply {
            invokeOnCompletion {
                if (!resultSignal.isCompleted) {
                    resultSignal.complete(Result.CANCELED)
                }
            }
        }

        val sessionId = sessionIdCounter.getAndIncrement()
        val session = InitSession(sessionId, inputSource, inputSource.name, cancelSignal, resultSignal, inputSource.hasSlowInitialization)

        scope.launch {
            try {
                val initialized = inputSource.init()

                if (cancelSignal.isCancelled) {
                    runCatching { inputSource.close() }
                        .onFailure { logger.error("Error while closing canceled InputSource", it) }

                    if (!resultSignal.isCompleted) {
                        resultSignal.complete(Result.CANCELED)
                    }
                    return@launch
                }

                resultSignal.complete(if (initialized) Result.SUCCESS else Result.FAILED)
            } catch (e: Exception) {
                logger.error("Error initializing InputSource", e)

                if (cancelSignal.isCancelled) {
                    runCatching { inputSource.close() }
                        .onFailure { logger.error("Error while closing canceled InputSource", it) }

                    if (!resultSignal.isCompleted) {
                        resultSignal.complete(Result.CANCELED)
                    }
                } else {
                    resultSignal.complete(Result.FAILED)
                }
            }
        }

        onInitBegin.run(session)

        val result = runBlocking {
            try {
                withTimeout(TIMEOUT) { resultSignal.await() }
            } catch (_: TimeoutCancellationException) {
                Result.TIMED_OUT
            } finally {
                cancelSignal.cancel()
            }
        }

        return result
    }

    @OptIn(DelicateCoroutinesApi::class)
    @JvmOverloads
    fun runWithTimeout(sourceName: String, showDialog: Boolean = false, callback: () -> Boolean): Result {
        val resultSignal = CompletableDeferred<Result>()
        val cancelSignal = Job().apply {
            invokeOnCompletion {
                if (!resultSignal.isCompleted) {
                    resultSignal.complete(Result.CANCELED)
                }
            }
        }

        val sessionId = sessionIdCounter.getAndIncrement()
        val session = InitSession(sessionId, null, sourceName, cancelSignal, resultSignal, showDialog)

        scope.launch {
            try {
                val ran = callback()

                if (cancelSignal.isCancelled) {
                    if (!resultSignal.isCompleted) {
                        resultSignal.complete(Result.CANCELED)
                    }
                    return@launch
                }

                resultSignal.complete(if (ran) Result.SUCCESS else Result.FAILED)
            } catch (e: Exception) {
                logger.error("Error running InputSource", e)

                if (cancelSignal.isCancelled) {
                    if (!resultSignal.isCompleted) {
                        resultSignal.complete(Result.CANCELED)
                    }
                } else {
                    resultSignal.complete(Result.FAILED)
                }
            }
        }

        // always notify, listeners decide if they want to show UI
        onInitBegin.run(session)

        val result = runBlocking {
            try {
                withTimeout(TIMEOUT) { resultSignal.await() }
            } catch (_: TimeoutCancellationException) {
                Result.TIMED_OUT
            } finally {
                cancelSignal.cancel()
            }
        }

        return result
    }


}