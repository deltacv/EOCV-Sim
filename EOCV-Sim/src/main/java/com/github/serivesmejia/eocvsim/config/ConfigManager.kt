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

package com.github.serivesmejia.eocvsim.config

import com.github.serivesmejia.eocvsim.util.orchestration.PhaseOrchestrableBase
import io.github.deltacv.common.util.loggerForThis
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
