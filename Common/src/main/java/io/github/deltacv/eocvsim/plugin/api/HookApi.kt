/*
 * Copyright (c) 2026 Sebastian Erives
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

package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin

/**
 * API for registering and executing lifecycle hooks.
 *
 * Hooks are lightweight callbacks that can be registered either as one-shot
 * listeners or as persistent listeners that can later detach themselves.
 *
 * This API is typically used to react to events such as frame updates,
 * lifecycle transitions, or other loader-defined execution points.
 *
 * @param owner the plugin that owns this API instance
 */
abstract class HookApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * A hook that is executed exactly once and then discarded.
     */
    typealias OnceHook = () -> Unit

    /**
     * A hook that is executed repeatedly until it is explicitly detached.
     *
     * The provided [Detacher] can be used by the hook implementation to
     * unregister itself.
     */
    typealias PersistentHook = (Detacher) -> Unit

    /**
     * Registers a hook that will be executed once on the next run cycle.
     *
     * @param hook the callback to execute
     */
    abstract fun once(hook: OnceHook)

    /**
     * Registers a persistent hook.
     *
     * The hook will be invoked on every run cycle until it calls
     * [Detacher.detach].
     *
     * @param hook the callback to register
     */
    abstract fun attach(hook: PersistentHook)

    /**
     * Shorthand for [attach].
     */
    operator fun invoke(hook: PersistentHook) = attach(hook)

    /**
     * Executes all registered hooks.
     *
     * This method is intended to be called by the owner of the hook source
     * (for example, the main loop or an event dispatcher), not by plugins.
     */
    abstract fun runListeners()

    /**
     * Handle passed to persistent hooks that allows them to unregister
     * themselves.
     */
    fun interface Detacher {
        /**
         * Detaches the associated hook so it will no longer be executed.
         */
        fun detach()
    }
}
