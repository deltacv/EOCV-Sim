/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.plugin.output

import com.github.serivesmejia.eocvsim.util.event.ParamEventHandler
import io.github.deltacv.common.util.loggerOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

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
        if (timeoutMillis > 0L) {
            sendOutputLine("Waiting for confirmation for ${timeoutMillis / 1000} seconds...")
        } else {
            sendOutputLine("Waiting for confirmation...")
        }

        val deferred = continuationDeferred

        return if (timeoutMillis > 0L) {
            // Use withTimeoutOrNull for coroutine-native timeout
            val result = withTimeoutOrNull(timeoutMillis.milliseconds) {
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
    override fun signalContinuation() {
        if (!continuationDeferred.isCompleted) {
            continuationDeferred.complete(Unit)
        }
    }
}
