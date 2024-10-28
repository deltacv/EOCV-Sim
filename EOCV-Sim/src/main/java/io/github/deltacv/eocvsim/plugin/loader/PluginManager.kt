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
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.dialog.PluginOutput
import com.github.serivesmejia.eocvsim.gui.dialog.PluginOutput.Companion.trimSpecials
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.github.serivesmejia.eocvsim.util.loggerOf
import io.github.deltacv.eocvsim.plugin.repository.PluginRepositoryManager
import io.github.deltacv.eocvsim.plugin.security.superaccess.SuperAccessDaemon
import io.github.deltacv.eocvsim.plugin.security.superaccess.SuperAccessDaemonClient
import io.github.deltacv.eocvsim.plugin.security.toMutable
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

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

    val superAccessDaemonClient = SuperAccessDaemonClient()

    private val _loadedPluginHashes = mutableListOf<String>()
    val loadedPluginHashes get() = _loadedPluginHashes.toList()

    private val haltLock = ReentrantLock()
    private val haltCondition = haltLock.newCondition()

    val appender by lazy {
        val appender = DialogFactory.createMavenOutput(this) {
            haltLock.withLock {
                haltCondition.signalAll()
            }
        }

        val logger by loggerOf("PluginOutput")

        appender.subscribe {
            if(!it.isBlank()) {
                val message = it.trimSpecials()

                if(message.isNotBlank()) {
                    logger.info(message)
                }
            }
        }

        appender
    }

    val repositoryManager by lazy {
        PluginRepositoryManager(appender, haltLock, haltCondition)
    }

    private val _pluginFiles = mutableListOf<File>()

    /**
     * List of plugin files in the plugins folder
     */
    val pluginFiles get() = _pluginFiles.toList()

    private val _loaders = mutableMapOf<File, PluginLoader>()
    val loaders get() = _loaders.toMap()

    private var enableTimestamp by Delegates.notNull<Long>()
    private var isEnabled = false

    /**
     * Initializes the plugin manager
     * Loads all plugin files in the plugins folder
     * Creates a PluginLoader for each plugin file
     * and stores them in the loaders map
     * @see PluginLoader
     */
    fun init() {
        eocvSim.visualizer.onInitFinished {
            appender.append(PluginOutput.SPECIAL_FREE)
        }

        superAccessDaemonClient.init()

        repositoryManager.init()

        val pluginFiles = mutableListOf<File>()
        pluginFiles.addAll(repositoryManager.resolveAll())

        PLUGIN_FOLDER.listFiles()?.let {
            pluginFiles.addAll(it.filter { it.extension == "jar" })
        }

        for (file in pluginFiles) {
            if (file.extension == "jar") _pluginFiles.add(file)
        }

        if(pluginFiles.isEmpty()) {
            appender.appendln(PluginOutput.SPECIAL_SILENT + "No plugins to load")
            return
        }

        for (pluginFile in pluginFiles) {
            _loaders[pluginFile] = PluginLoader(
                pluginFile,
                repositoryManager.resolvedFiles,
                if(pluginFile in repositoryManager.resolvedFiles)
                    PluginSource.REPOSITORY else PluginSource.FILE,
                eocvSim,
                appender
            )
        }

        enableTimestamp = System.currentTimeMillis()
        isEnabled = true
    }

    /**
     * Loads all plugins
     * @see PluginLoader.load
     */
    fun loadPlugins() {
        for ((file, loader) in _loaders) {
            try {
                loader.fetchInfoFromToml()

                val hash = loader.hash()

                if(hash in _loadedPluginHashes) {
                    val source = if(loader.pluginSource == PluginSource.REPOSITORY) "repository.toml file" else "plugins folder"

                    appender.appendln("Plugin ${loader.pluginName} by ${loader.pluginAuthor} is already loaded. Please delete the duplicate from the $source !")
                    return
                }

                loader.load()
                _loadedPluginHashes.add(hash)
            } catch (e: Throwable) {
                appender.appendln("Failure loading ${loader.pluginName} v${loader.pluginVersion}:")
                appender.appendln(e.message ?: "Unknown error")
                logger.error("Failure loading ${loader.pluginName} v${loader.pluginVersion}", e)

                _loaders.remove(file)
                loader.kill()
            }
        }
    }

    /**
     * Enables all plugins
     * @see PluginLoader.enable
     */
    fun enablePlugins() {
        for (loader in _loaders.values) {
            try {
                loader.enable()
            } catch (e: Throwable) {
                appender.appendln("Failure enabling ${loader.pluginName} v${loader.pluginVersion}: ${e.message}")
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

        for (loader in _loaders.values) {
            try {
                loader.disable()
            } catch (e: Throwable) {
                appender.appendln("Failure disabling ${loader.pluginName} v${loader.pluginVersion}: ${e.message}")
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
        if(loader.hasSuperAccess) {
            appender.appendln(PluginOutput.SPECIAL_SILENT + "Plugin ${loader.pluginName} v${loader.pluginVersion} already has super access")
            return true
        }

        val signature = loader.signature

        appender.appendln(PluginOutput.SPECIAL_SILENT + "Requesting super access for ${loader.pluginName} v${loader.pluginVersion}")

        var access = false

        superAccessDaemonClient.sendRequest(SuperAccessDaemon.SuperAccessMessage.Request(
            loader.pluginFile.absolutePath,
            signature.toMutable(),
            reason
        )) {
            if(it) {
                access = true
            }

            haltLock.withLock {
                haltCondition.signalAll()
            }
        }

        haltLock.withLock {
            haltCondition.await()
        }

        return access
    }
}