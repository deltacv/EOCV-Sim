package com.github.serivesmejia.eocvsim.input

import io.github.deltacv.common.util.loggerForThis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext

class InputSourceInitializer : KoinComponent {

    companion object {
        const val TIMEOUT = 10000L

        fun runWithTimeout(sourceName: String, manager: InputSourceManager? = null, callback: () -> Boolean): Boolean {

            val initializer = GlobalContext.get().get<InputSourceInitializer>()
            return initializer.runWithTimeout(sourceName, manager, callback)
        }
        
        fun initializeWithTimeout(inputSource: InputSource): Boolean {

            val initializer = GlobalContext.get().get<InputSourceInitializer>()
            return initializer.initializeWithTimeout(inputSource)
        }
    }

    private val inputSourceManager: InputSourceManager by inject()
    private val scope: CoroutineScope by inject()

    val logger by loggerForThis()

    @OptIn(DelicateCoroutinesApi::class)
    fun initializeWithTimeout(inputSource: InputSource): Boolean {
        var result = false

        val job = scope.launch {
            try {
                result = inputSource.init()
            } catch (e: Exception) {
                logger.error("Error initializing InputSource", e)
            }
        }

        val dialog = inputSourceManager.showLoadingDialogIfNeeded(inputSource.name, job)

        runBlocking {
            try {
                withTimeout(TIMEOUT) {
                    job.join()
                }
            } catch (e: CancellationException) {
                logger.error("InputSource initialization timed out after $TIMEOUT ms", e)
            } finally {
                job.cancel()
                dialog?.dispose()
            }
        }

        return result
    }


    @OptIn(DelicateCoroutinesApi::class)
    @JvmOverloads
    fun runWithTimeout(sourceName: String, manager: InputSourceManager? = null, callback: () -> Boolean): Boolean {
        var result = false

        val job = scope.launch {
            try {
                result = callback()
            } catch (e: Exception) {
                logger.error("Error running InputSource", e)
            }
        }

        val dialog = manager?.showLoadingDialogIfNeeded(sourceName, job)

        runBlocking {
            try {
                withTimeout(TIMEOUT) {
                    job.join()
                }
            } catch (e: CancellationException) {
                logger.error("InputSource run timed out after $TIMEOUT ms", e)
            } finally {
                job.cancel()
                dialog?.dispose()
            }
        }

        return result
    }

}