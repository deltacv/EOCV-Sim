/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.config

import com.github.serivesmejia.eocvsim.util.orchestration.PhaseOrchestrableBase
import org.deltacv.common.util.loggerForThis
import org.koin.core.component.KoinComponent

class ConfigManager : PhaseOrchestrableBase(), KoinComponent {
    val configLoader = ConfigLoader()

    var config: Config = Config()
        private set

    private val logger by loggerForThis()

    override suspend fun init() {
        logger.info("Initializing...")

        try {
            val loadedConfig = configLoader.loadFromFile()
            if (loadedConfig == null) {
                logger.error("Error while parsing config file, it will be replaced and fixed, but the user configurations will be reset")
                throw NullPointerException() // for it to be caught later and handle the creation of a new config
            } else {
                config = loadedConfig
                logger.info("Loaded config from file successfully")
            }
        } catch (ex: Exception) { // handles FileNotFoundException & a NullPointerException thrown above
            config = Config()
            logger.info("Creating config file...")
            configLoader.saveToFile(config)
        }
    }

    override suspend fun run() { }

    override suspend fun destroy() {
        saveToFile()
    }

    fun saveToFile() {
        configLoader.saveToFile(config)
    }
}

