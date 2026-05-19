/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.loader

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.config.ConfigLoader
import com.github.serivesmejia.eocvsim.plugin.output.PluginOutputHandler
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.github.serivesmejia.eocvsim.util.extension.plus
import org.deltacv.common.util.loggerForThis
import org.deltacv.common.util.Toml
import org.deltacv.common.util.ParsedVersion
import org.deltacv.eocvsim.plugin.EMBEDDED_PLUGIN_FOLDER
import org.deltacv.eocvsim.plugin.EOCVSimPlugin
import org.deltacv.eocvsim.plugin.api.ApiDisabler
import org.deltacv.eocvsim.plugin.api.EOCVSimApi
import org.deltacv.eocvsim.plugin.security.PluginSignatureVerifier
import org.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import net.lingala.zip4j.ZipFile
import java.io.File
import java.nio.file.Path

/**
 * Loads a plugin from a jar file
 * @param pluginFile the jar file of the plugin
 * @param classpath the classpath of the plugin
 * @param pluginSource the source of the plugin (file or repository)
 * @param pluginManager the plugin manager
 * @param outputHandler the output handler for plugin logging
 */
open class FilePluginLoaderImpl(
    override val pluginFile: File,
    override val classpath: List<File>,
    override val pluginSource: PluginSource,
    val pluginManager: PluginManager,
    val outputHandler: PluginOutputHandler
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
     *
     * Fills the pluginName, pluginVersion, pluginAuthor and pluginAuthorEmail fields
     */
    fun fetchInfoFromToml() {
        if(::pluginToml.isInitialized) return

        pluginToml = Toml().read(pluginClassLoader.getResourceAsStream("plugin.toml")
            ?: throw InvalidPluginException("No plugin.toml in the jar file")
        )

        // Parse plugin info directly to avoid cross-module Toml type mismatch
        val name = pluginToml.getString("name")?.trim()
            ?: throw InvalidPluginException("No name in plugin.toml")

        val version = pluginToml.getString("version")?.trim()
            ?: throw InvalidPluginException("No version in plugin.toml")

        val author = pluginToml.getString("author")?.trim()
            ?: throw InvalidPluginException("No author in plugin.toml")

        val authorEmail = pluginToml.getString("author-email")?.trim() ?: ""

        val main = pluginToml.getString("main")?.trim()
            ?: throw InvalidPluginException("No main in plugin.toml")

        val description = pluginToml.getString("description")?.trim() ?: ""
        val superAccess = pluginToml.getBoolean("super-access") ?: false

        pluginInfo = PluginInfo(name, version, author, authorEmail, main, description, superAccess)
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
            outputHandler.sendOutputLine("Plugin ${pluginInfo.nameWithVersion} is disabled")
            return
        }

        outputHandler.sendOutputLine("Loading plugin ${pluginInfo.nameWithVersionAndAuthor} from ${pluginSource.name}")

        signature

        setupFs()

        if(pluginToml.getString("api-version") != null || pluginToml.getString("min-api-version") != null) {
            // default to api-version if min-api-version is not present
            val apiVersionKey = if(pluginToml.getString("api-version") != null) "api-version" else "min-api-version"
            val parsedVersion = ParsedVersion(pluginToml.getString(apiVersionKey))

            if(parsedVersion > EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires a minimum api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin ${pluginInfo.name} requests min api version of v${parsedVersion}")
        }

        if(pluginToml.getString("max-api-version") != null) {
            val parsedVersion = ParsedVersion(pluginToml.getString("max-api-version"))

            if(parsedVersion < EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires a max api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin ${pluginInfo.name} requests max api version of v${parsedVersion}")
        }

        if(pluginToml.getString("exact-api-version") != null) {
            val parsedVersion = ParsedVersion(pluginToml.getString("exact-api-version"))

            if(parsedVersion != EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires an exact api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin ${pluginInfo.name} requests exact api version of v${parsedVersion}")
        }

        if(pluginToml.getBoolean("super-access") != null && pluginToml.getBoolean("super-access")) {
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

        outputHandler.sendOutputLine("Enabling plugin ${pluginInfo.nameWithVersionAndAuthor}")

        plugin.onEnable()

        enabled = true
    }

    /**
     * Disable the plugin
     */
    override fun disable() {
        if(!enabled || !loaded) return

        outputHandler.sendOutputLine("Disabling plugin ${pluginInfo.name} v${pluginInfo.version}")

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
    outputHandler: PluginOutputHandler
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
    outputHandler = outputHandler
) {
    override val hasSuperAccess = true // Embedded plugins always have super access

    init {
        fetchInfoFromToml()
    }
}
