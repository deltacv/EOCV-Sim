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

package io.github.deltacv.eocvsim.plugin.loader

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.io.File

class PluginManager(val eocvSim: EOCVSim) {

    companion object {
        val PLUGIN_FOLDER = (EOCVSimFolder + File.separator + "plugins").apply { mkdir() }
        val FILESYSTEMS_FOLDER = (PLUGIN_FOLDER + File.separator + "filesystems").apply { mkdir() }
    }

    val logger by loggerForThis()

    private val _pluginFiles = mutableListOf<File>()
    val pluginFiles get() = _pluginFiles.toList()

    private val loaders = mutableMapOf<File, PluginLoader>()

    fun init() {
        val filesInPluginFolder = PLUGIN_FOLDER.listFiles() ?: arrayOf()

        for (file in filesInPluginFolder) {
            if (file.extension == "jar") _pluginFiles.add(file)
        }

        if(pluginFiles.isEmpty()) {
            logger.info("No plugins to load")
            return
        }

        for (pluginFile in pluginFiles) {
            loaders[pluginFile] = PluginLoader(pluginFile, eocvSim)
        }
    }

    fun loadPlugins() {
        for ((file, loader) in loaders) {
            try {
                loader.load()
            } catch (e: Throwable) {
                logger.error("Failure loading ${file.name}", e)
                loaders.remove(file)

                EventHandler.banClassLoader(loader.pluginClassLoader)
            }
        }
    }

    fun enablePlugins() {
        for (loader in loaders.values) {
            try {
                loader.enable()
            } catch (e: Throwable) {
                logger.error("Failure enabling ${loader.pluginName} v${loader.pluginVersion}", e)

                EventHandler.banClassLoader(loader.pluginClassLoader)
            }
        }
    }


    fun disablePlugins() {
        for (loader in loaders.values) {
            try {
                loader.disable()
                EventHandler.banClassLoader(loader.pluginClassLoader)
            } catch (e: Throwable) {
                logger.error("Failure disabling ${loader.pluginName} v${loader.pluginVersion}", e)
            }
        }
    }
}