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

package io.github.deltacv.eocvsim.plugin.loader

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.EOCVSimApi

/**
 * Marks an object as carrying a [PluginContext].
 *
 * This is used in cases where code is executed outside of the plugin's
 * classloader or without an active thread-local context, but still needs
 * access to the plugin context.
 */
interface PluginContextHolder {
    /**
     * The associated [PluginContext].
     */
    val pluginContext: PluginContext
}

/**
 * Provides an [EOCVSimApi] instance for a given plugin.
 *
 * This abstraction exists to decouple plugins from the concrete source
 * of the API instance, allowing it to be resolved dynamically based
 * on the plugin being loaded.
 */
fun interface EOCVSimApiProvider {
    /**
     * Returns the [EOCVSimApi] instance associated with the given plugin.
     *
     * @param plugin the plugin requesting access to the API
     */
    fun provideEOCVSimApiFor(plugin: EOCVSimPlugin): EOCVSimApi
}