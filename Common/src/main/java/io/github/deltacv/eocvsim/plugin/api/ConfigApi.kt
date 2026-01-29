package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin

abstract class ConfigApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract fun putFlag(flag: String)
    abstract fun clearFlag(flag: String)

    abstract fun hasFlag(flag: String): Boolean
}