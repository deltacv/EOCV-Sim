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
import com.github.serivesmejia.eocvsim.util.JavaProcess
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.gui.dialog.SuperAccessRequestMain
import java.io.File
import java.util.*

/**
 * Manages the loading, enabling and disabling of plugins
 * @param eocvSim the EOCV-Sim instance
 */
class PluginManager(val eocvSim: EOCVSim) {

    companion object {
        val PLUGIN_FOLDER = (EOCVSimFolder + File.separator + "plugins").apply { mkdir() }
        val FILESYSTEMS_FOLDER = (PLUGIN_FOLDER + File.separator + "filesystems").apply { mkdir() }

        const val GENERIC_SUPERACCESS_WARN = "Plugins run in a restricted environment by default. <b>SuperAccess will grant full system access. Ensure you trust the authors before accepting.</b>"
        const val GENERIC_LAWYER_YEET = "<br><br>By accepting, you acknowledge that deltacv is not responsible for damages caused by third-party plugins."
    }

    val logger by loggerForThis()

    private val _pluginFiles = mutableListOf<File>()

    /**
     * List of plugin files in the plugins folder
     */
    val pluginFiles get() = _pluginFiles.toList()

    private val loaders = mutableMapOf<File, PluginLoader>()

    private var isEnabled = false

    /**
     * Initializes the plugin manager
     * Loads all plugin files in the plugins folder
     * Creates a PluginLoader for each plugin file
     * and stores them in the loaders map
     * @see PluginLoader
     */
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

        isEnabled = true
    }

    /**
     * Loads all plugins
     * @see PluginLoader.load
     */
    fun loadPlugins() {
        for ((file, loader) in loaders) {
            try {
                loader.load()
            } catch (e: Throwable) {
                logger.error("Failure loading ${file.name}", e)
                loaders.remove(file)
                loader.kill()
            }
        }
    }

    /**
     * Enables all plugins
     * @see PluginLoader.enable
     */
    fun enablePlugins() {
        for (loader in loaders.values) {
            try {
                loader.enable()
            } catch (e: Throwable) {
                logger.error("Failure enabling ${loader.pluginName} v${loader.pluginVersion}", e)
                loader.kill()
            }
        }
    }

    /**
     * Disables all plugins
     * @see PluginLoader.disable
     */
    @Synchronized
    fun disablePlugins() {
        if(!isEnabled) return

        for (loader in loaders.values) {
            try {
                loader.disable()
            } catch (e: Throwable) {
                logger.error("Failure disabling ${loader.pluginName} v${loader.pluginVersion}", e)
                loader.kill()
            }
        }

        isEnabled = false
    }

    /**
     * Requests super access for a plugin loader
     *
     * @param loader the plugin loader to request super access for
     * @param reason the reason for requesting super access
     * @return true if super access was granted, false otherwise
     */
    fun requestSuperAccessFor(loader: PluginLoader, reason: String): Boolean {
        if(loader.hasSuperAccess) return true

        logger.info("Requesting super access for ${loader.pluginName} v${loader.pluginVersion}")

        var warning = "<html>$GENERIC_SUPERACCESS_WARN"
        if(reason.trim().isNotBlank()) {
            warning += "<br><br><i>$reason</i>"
        }

        warning += GENERIC_LAWYER_YEET

        warning += "</html>"

        val name = "${loader.pluginName} by ${loader.pluginAuthor}".replace(" ", "-")

        if(JavaProcess.exec(SuperAccessRequestMain::class.java, null, Arrays.asList(name, warning)) == 171) {
            eocvSim.config.superAccessPluginHashes.add(loader.pluginHash)
            eocvSim.configManager.saveToFile()
            return true
        }

        return false
    }
}