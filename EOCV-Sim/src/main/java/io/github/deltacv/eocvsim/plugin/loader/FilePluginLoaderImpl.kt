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
import com.github.serivesmejia.eocvsim.config.ConfigLoader
import com.github.serivesmejia.eocvsim.gui.dialog.AppendDelegate
import com.github.serivesmejia.eocvsim.gui.dialog.PluginOutput
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.github.serivesmejia.eocvsim.util.extension.plus
import io.github.deltacv.common.util.loggerForThis
import com.moandjiezana.toml.Toml
import io.github.deltacv.common.util.ParsedVersion
import io.github.deltacv.eocvsim.plugin.EMBEDDED_PLUGIN_FOLDER
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.ApiDisabler
import io.github.deltacv.eocvsim.plugin.api.EOCVSimApi
import io.github.deltacv.eocvsim.plugin.security.PluginSignatureVerifier
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import net.lingala.zip4j.ZipFile
import java.io.File
import java.nio.file.Path

/**
 * Loads a plugin from a jar file
 * @param pluginFile the jar file of the plugin
 * @param classpath the classpath of the plugin
 * @param pluginSource the source of the plugin (file or repository)
 * @param pluginManager the plugin manager
 * @param appender the appender to use for logging
 */
open class FilePluginLoaderImpl(
    override val pluginFile: File,
    override val classpath: List<File>,
    override val pluginSource: PluginSource,
    val pluginManager: PluginManager,
    val appender: AppendDelegate
) : FilePluginLoader() {

    val logger by loggerForThis()

    override var loaded = false
        protected set

    override var enabled = false
        protected set

    val pluginClassLoader = PluginClassLoader(
        pluginFile, classpath, this
    )

    override var shouldEnable: Boolean
        get() = pluginManager.isPluginEnabledInConfig(this)
        set(value) = pluginManager.setPluginEnabledInConfig(this, value)

    lateinit var pluginToml: Toml
        private set

    override lateinit var pluginInfo: PluginInfo
        protected set
    override lateinit var pluginClass: Class<*>
        protected set

    override lateinit var plugin: EOCVSimPlugin
        protected set

    /**
     * The file system for the plugin
     */
    override lateinit var fileSystem: SandboxFileSystem
        protected set

    /**
     * The signature of the plugin, issued by a verified authority
     */
    override val signature by lazy { PluginSignatureVerifier.verify(pluginFile) }

    val fileSystemZip by lazy { PluginManager.FILESYSTEMS_FOLDER + File.separator + "${hash()}-fs" }
    val fileSystemZipPath: Path by lazy { fileSystemZip.toPath() }

    /**
     * Whether the plugin has super access (full system access)
     */
    override val hasSuperAccess get() = pluginManager.hasSuperAccess(pluginFile)

    override var eocvSimApi: EOCVSimApi? = null
        protected set

    /**
     * Fetch the plugin info from the plugin.toml file
     * Fills the pluginName, pluginVersion, pluginAuthor and pluginAuthorEmail fields
     */
    fun fetchInfoFromToml() {
        if(::pluginToml.isInitialized) return

        pluginToml = Toml().read(pluginClassLoader.getResourceAsStream("plugin.toml")
            ?: throw InvalidPluginException("No plugin.toml in the jar file")
        )

        pluginInfo = PluginInfo.fromToml(pluginToml)
    }

    /**
     * Load the plugin from the jar file
     * @throws InvalidPluginException if the plugin.toml file is not found
     * @throws UnsupportedPluginException if the plugin requests an api version higher than the current one
     */
    override fun load() {
        if(loaded) return

        fetchInfoFromToml()

        if(!shouldEnable) {
            appender.appendln("${PluginOutput.SPECIAL_SILENT}Plugin ${pluginInfo.name} v${pluginInfo.version} is disabled")
            return
        }

        appender.appendln("${PluginOutput.SPECIAL_SILENT}Loading plugin ${pluginInfo.name} v${pluginInfo.version} by ${pluginInfo.version} from ${pluginSource.name}")

        signature

        setupFs()

        if(pluginToml.contains("api-version") || pluginToml.contains("min-api-version")) {
            // default to api-version if min-api-version is not present
            val apiVersionKey = if(pluginToml.contains("api-version")) "api-version" else "min-api-version"
            val parsedVersion = ParsedVersion(pluginToml.getString(apiVersionKey))

            if(parsedVersion > EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires a minimum api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin ${pluginInfo.name} requests min api version of v${parsedVersion}")
        }

        if(pluginToml.contains("max-api-version")) {
            val parsedVersion = ParsedVersion(pluginToml.getString("max-api-version"))

            if(parsedVersion < EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires a max api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin ${pluginInfo.name} requests max api version of v${parsedVersion}")
        }

        if(pluginToml.contains("exact-api-version")) {
            val parsedVersion = ParsedVersion(pluginToml.getString("exact-api-version"))

            if(parsedVersion != EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires an exact api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin ${pluginInfo.name} requests exact api version of v${parsedVersion}")
        }

        if(pluginToml.getBoolean("super-access", false)) {
            requestSuperAccess(pluginToml.getString("super-access-reason", ""))
        }

        pluginClass = pluginClassLoader.loadClassStrict(pluginInfo.main)
        plugin = try {
            pluginClass.getConstructor().newInstance() as EOCVSimPlugin
        } catch(e: NoSuchMethodException) {
            throw InvalidPluginException("Plugin main class must have a no-argument constructor", e)
        } catch(e: ClassCastException) {
            throw InvalidPluginException("Plugin main class must extend EOCVSimPlugin", e)
        } catch(e: Throwable) {
            throw InvalidPluginException("Failed to instantiate plugin main class", e)
        }

        eocvSimApi = pluginManager.eocvSimApiProvider.provideEOCVSimApiFor(plugin)

        plugin.onLoad()

        loaded = true
    }

    private fun setupFs() {
        if(!fileSystemZip.exists()) {
            val zip = ZipFile(fileSystemZip) // kinda wack but uh, yeah...
            zip.addFile(ConfigLoader.CONFIG_SAVEFILE)
            zip.removeFile(ConfigLoader.CONFIG_SAVEFILE.name)
            zip.close()
        }

        fileSystem = SandboxFileSystem(fileSystemZipPath, hash())
    }

    /**
     * Enable the plugin
     */
    override fun enable() {
        if(enabled || !loaded) return

        if(!shouldEnable) return

        appender.appendln("${PluginOutput.SPECIAL_SILENT}Enabling plugin ${pluginInfo.name} v${pluginInfo.version}")

        plugin.onEnable()

        enabled = true
    }

    /**
     * Disable the plugin
     */
    override fun disable() {
        if(!enabled || !loaded) return

        appender.appendln("${PluginOutput.SPECIAL_SILENT}Disabling plugin ${pluginInfo.name} v${pluginInfo.version}")

        plugin.onDisable()

        eocvSimApi?.let { ApiDisabler.disableApis(it) }

        kill()
    }

    /**
     * Kill the plugin
     * This will close the file system and ban the class loader
     * @see EventHandler.banClassLoader
     */
    override fun kill() {
        if(!loaded) return
        fileSystem.close()
        enabled = false

        pluginClassLoader.close()
    }

    /**
     * Request super access for the plugin
     * @param reason the reason for requesting super access
     */
    override fun requestSuperAccess(reason: String): Boolean {
        return pluginManager.requestSuperAccessFor(this, reason)
    }
}

class EmbeddedFilePluginLoader(
    resourcePath: String,
    classpath: List<File>,
    pluginManager: PluginManager,
    appender: AppendDelegate
) : FilePluginLoaderImpl(
    pluginFile = resourcePath.let {
        // extract to EMBEDDED_PLUGIN_FOLDER
        val hash = it.hashString
        val file = EMBEDDED_PLUGIN_FOLDER + File.separator + "$hash.jar"

        try {
            SysUtil.copyFileIs(EmbeddedFilePluginLoader::class.java.getResourceAsStream(it), file, true)
        } catch (e: Exception) {
            throw InvalidPluginException("Failed to extract embedded plugin from resource path $resourcePath", e)
        }

        file
    },
    classpath = classpath.let {
        // add pluginFile
        val hash = it.hashString
        val file = EMBEDDED_PLUGIN_FOLDER + File.separator + "$hash.jar"

        classpath + file
    },
    pluginSource = PluginSource.FILE,
    pluginManager = pluginManager,
    appender = appender
) {
    override val hasSuperAccess = true // Embedded plugins always have super access

    init {
        fetchInfoFromToml()
    }
}