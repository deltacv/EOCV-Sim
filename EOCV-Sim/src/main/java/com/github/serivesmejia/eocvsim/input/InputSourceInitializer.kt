package com.github.serivesmejia.eocvsim.input

import io.github.deltacv.common.util.loggerForThis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext

class InputSourceInitializer : KoinComponent {

    enum class Result { SUCCESS, FAILED, CANCELED, TIMED_OUT }

    companion object {
        const val TIMEOUT = 10000L

        fun runWithTimeout(sourceName: String, manager: InputSourceManager? = null, callback: () -> Boolean): Result {

            val initializer = GlobalContext.get().get<InputSourceInitializer>()
            return initializer.runWithTimeout(sourceName, manager, callback)
        }
        
        fun initializeWithTimeout(inputSource: InputSource, manager: InputSourceManager? = null): Result {

            val initializer = GlobalContext.get().get<InputSourceInitializer>()
            return initializer.initializeWithTimeout(inputSource, manager)
        }
    }

    private val scope: CoroutineScope by inject()

    val logger by loggerForThis()

    @OptIn(DelicateCoroutinesApi::class)
    fun initializeWithTimeout(inputSource: InputSource, manager: InputSourceManager? = null): Result {
        val resultSignal = CompletableDeferred<Result>()
        val cancelSignal = Job().apply {
            invokeOnCompletion {
                if (!resultSignal.isCompleted) {
                    resultSignal.complete(Result.CANCELED)
                }
            }
        }

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

        val dialog = manager?.showLoadingDialogIfNeeded(inputSource.name, cancelSignal)

        val result = runBlocking {
            try {
                withTimeout(TIMEOUT) { resultSignal.await() }
            } catch (e: TimeoutCancellationException) {
                Result.TIMED_OUT
            } finally {
                cancelSignal.cancel()
                dialog?.dispose()
            }
        }

        return result
    }

    @OptIn(DelicateCoroutinesApi::class)
    @JvmOverloads
    fun runWithTimeout(sourceName: String, manager: InputSourceManager? = null, callback: () -> Boolean): Result {
        val resultSignal = CompletableDeferred<Result>()
        val cancelSignal = Job().apply {
            invokeOnCompletion {
                if (!resultSignal.isCompleted) {
                    resultSignal.complete(Result.CANCELED)
                }
            }
        }

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

        val dialog = manager?.showLoadingDialogIfNeeded(sourceName, cancelSignal)

        val result = runBlocking {
            try {
                withTimeout(TIMEOUT) { resultSignal.await() }
            } catch (e: TimeoutCancellationException) {
                Result.TIMED_OUT
            } finally {
                cancelSignal.cancel()
                dialog?.dispose()
            }
        }

        return result
    }

}