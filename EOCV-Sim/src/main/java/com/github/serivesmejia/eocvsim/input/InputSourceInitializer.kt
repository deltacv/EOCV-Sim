package com.github.serivesmejia.eocvsim.input

import com.github.serivesmejia.eocvsim.util.loggerForThis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object InputSourceInitializer {

    const val TIMEOUT = 10000L

    val logger by loggerForThis()

    @OptIn(DelicateCoroutinesApi::class)
    fun initializeWithTimeout(inputSource: InputSource, manager: InputSourceManager? = null): Boolean {
        var result = false

        val job = GlobalScope.launch {
            try {
                result = inputSource.init()
            } catch (e: Exception) {
                logger.error("Error initializing InputSource", e)
            }
        }

        val dialog = manager?.showApwdIfNeeded(inputSource.name, job)

        runBlocking {
            try {
                withTimeout(TIMEOUT) {
                    job.join()
                }
            } catch (e: CancellationException) {
                logger.error("InputSource initialization timed out after $TIMEOUT ms", e)
            } finally {
                job.cancel()
                dialog?.destroyDialog()
            }
        }

        return result
    }


    @OptIn(DelicateCoroutinesApi::class)
    @JvmOverloads
    fun runWithTimeout(sourceName: String, manager: InputSourceManager? = null, callback: () -> Boolean): Boolean {
        var result = false

        val job = GlobalScope.launch {
            try {
                result = callback()
            } catch (e: Exception) {
                logger.error("Error running InputSource", e)
            }
        }

        val dialog = manager?.showApwdIfNeeded(sourceName, job)

        runBlocking {
            try {
                withTimeout(TIMEOUT) {
                    job.join()
                }
            } catch (e: CancellationException) {
                logger.error("InputSource run timed out after $TIMEOUT ms", e)
            } finally {
                job.cancel()
                dialog?.destroyDialog()
            }
        }

        return result
    }

}