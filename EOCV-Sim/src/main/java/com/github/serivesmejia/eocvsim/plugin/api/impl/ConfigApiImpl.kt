package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.config.ConfigManager
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.ConfigApi

class ConfigApiImpl(owner: EOCVSimPlugin, val internalConfigManager: ConfigManager) : ConfigApi(owner) {
    override fun putFlag(flag: String) = apiImpl {
        internalConfigManager.config.flags[flag] = true
    }

    override fun clearFlag(flag: String) = apiImpl {
        internalConfigManager.config.flags[flag] = false
    }

    override fun hasFlag(flag: String) = apiImpl {
        internalConfigManager.config.hasFlag(flag)
    }

    override fun disableApi() {
        internalConfigManager.saveToFile()
    }
}