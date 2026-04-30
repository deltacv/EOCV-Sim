/*
 * Copyright (c) 2024 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.plugin.output

import com.github.serivesmejia.eocvsim.util.event.ParamEventHandler
import io.github.deltacv.common.util.loggerOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Concrete implementation of PluginOutputHandler.
 *
 * Proper coroutine-based design:
 * - Uses [withTimeoutOrNull] for timeout handling (not thread hacks)
 * - Emits structured events, not magic strings
 * - Manages continuation via [CompletableDeferred] suspension
 */
class VisualPluginOutputHandler : PluginOutputHandler {

    private val logger by loggerOf("PluginOutputHandler")

    // Continuation signal: plugin code awaits this, UI completes it
    private var continuationDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    override val onOutput = ParamEventHandler<String>("PluginOutput")
    override val onDialogSignal = ParamEventHandler<PluginDialogSignal>("PluginDialogSignal")

    /**
     * Sends an output message. Emitted via [onOutput] event and logged.
     */
    override fun sendOutput(message: String) {
        // Log (trim leading newlines/spaces)
        val trimmed = message.trim()
        if (trimmed.isNotEmpty()) {
            logger.info(trimmed)
        }

        // Emit event
        onOutput.run(message)
    }

    /**
     * Sends a dialog control signal (structured, not magic codes).
     */
    override fun sendDialogSignal(signal: PluginDialogSignal) {
        onDialogSignal.run(signal)
    }

    /**
     * Waits for continuation from the UI.
     *
     * Uses [withTimeoutOrNull] for coroutine-native timeout handling:
     * - No daemon threads
     * - Proper suspension (not sleep)
     * - Clean timeout semantics
     *
     * @param timeoutMillis timeout in milliseconds (0 = wait indefinitely)
     * @return true if completed by UI, false if timeout expired
     */
    override suspend fun waitForContinuation(timeoutMillis: Long): Boolean {
        val deferred = continuationDeferred

        return if (timeoutMillis > 0L) {
            // Use withTimeoutOrNull for coroutine-native timeout
            val result = withTimeoutOrNull(timeoutMillis) {
                deferred.await()
                true
            }

            if (result == null) {
                // Timeout expired
                logger.warn("Plugin output continuation timed out after ${timeoutMillis}ms")
                false
            } else {
                // Completed by UI
                true
            }
        } else {
            // No timeout, wait indefinitely
            deferred.await()
            true
        }.also {
            // Reset for next cycle
            continuationDeferred = CompletableDeferred()
        }
    }

    /**
     * UI calls this to signal that continuation is complete (e.g., user clicked "Continue").
     * This allows plugin code waiting in [waitForContinuation] to resume.
     */
    fun signalContinuation() {
        if (!continuationDeferred.isCompleted) {
            continuationDeferred.complete(Unit)
        }
    }
}


