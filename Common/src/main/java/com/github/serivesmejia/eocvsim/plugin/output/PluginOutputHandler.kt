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

/**
 * Dialog control signal - structured events replacing magic string codes.
 */
sealed class PluginDialogSignal {
    /** Show the plugin dialog (focus on output tab for messages) */
    object ShowOutput : PluginDialogSignal()

    /** Show the plugin dialog (focus on plugins manager tab) */
    object ShowPlugins : PluginDialogSignal()

    /** Hide the plugin dialog */
    object Hide : PluginDialogSignal()

    /** Enable the "Continue" button (user can proceed) */
    object EnableContinue : PluginDialogSignal()

    /** Disable the "Continue" button (user must wait) */
    object DisableContinue : PluginDialogSignal()
}

/**
 * Abstraction for plugin output and UI control.
 *
 * PluginManager is completely UI-agnostic and uses this handler to:
 * - Send output messages to the UI
 * - Signal dialog visibility and button state changes
 * - Wait for user continuation (e.g., when user clicks "Continue" button)
 *
 * The actual UI dialog is owned by Visualizer, which subscribes to these
 * event handlers and manages the dialog lifecycle.
 *
 * Design: No magic string codes, fully structured events.
 */
interface PluginOutputHandler {

    /**
     * Event handler for all output messages.
     * Subscribers (e.g., Visualizer) display these in the output area.
     */
    val onOutput: ParamEventHandler<String>

    /**
     * Event handler for structured dialog control signals.
     * Subscribers get explicit instructions for UI state (show/hide/button state).
     */
    val onDialogSignal: ParamEventHandler<PluginDialogSignal>

    /**
     * Sends an output message (logged and emitted).
     *
     * @param message the message to send
     */
    fun sendOutput(message: String)

    /**
     * Convenience method to send a message with a newline.
     *
     * @param message the message to send
     */
    fun sendOutputLine(message: String) = sendOutput(message + "\n")

    /**
     * Sends a dialog control signal (show/hide/button state).
     *
     * @param signal the control signal
     */
    fun sendDialogSignal(signal: PluginDialogSignal)

    /**
     * Waits for continuation signal from the UI (e.g., when user clicks "Continue").
     *
     * This is a coroutine-suspending function that awaits until the UI signals
     * completion or the timeout expires.
     *
     * Must be called from a coroutine context. Blocks via suspension, not threads.
     *
     * @param timeoutMillis timeout in milliseconds (0 = wait indefinitely)
     * @return true if completed by UI, false if timeout expired
     * @throws CancellationException if the coroutine is cancelled
     */
    suspend fun waitForContinuation(timeoutMillis: Long = 0L): Boolean

    /**
     * Programmatically signal that continuation is complete.
     */
    fun signalContinuation()
}