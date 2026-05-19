/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.loader

import org.deltacv.eocvsim.plugin.EOCVSimPlugin
import org.deltacv.eocvsim.plugin.api.EOCVSimApi

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
