package io.github.deltacv.eocvsim.plugin.loader

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.EOCVSimApi

interface PluginContextHolder {
    val pluginContext: PluginContext
}

fun interface EOCVSimApiProvider {
    fun provideEOCVSimApiFor(plugin: EOCVSimPlugin): EOCVSimApi
}