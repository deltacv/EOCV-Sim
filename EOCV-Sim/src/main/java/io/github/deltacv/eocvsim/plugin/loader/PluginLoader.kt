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
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.moandjiezana.toml.Toml
import org.apache.logging.log4j.LogManager
import io.github.deltacv.common.util.ParsedVersion
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import net.lingala.zip4j.ZipFile
import org.apache.logging.log4j.core.LoggerContext
import java.io.File
import java.security.MessageDigest

enum class PluginSource {
    REPOSITORY,
    FILE
}

/**
 * Loads a plugin from a jar file
 * @param pluginFile the jar file of the plugin
 * @param eocvSim the EOCV-Sim instance
 */
class PluginLoader(
    val pluginFile: File,
    val classpath: List<File>,
    val pluginSource: PluginSource,
    val eocvSim: EOCVSim,
    val appender: AppendDelegate
) {

    val logger by loggerForThis()

    var loaded = false
        private set

    var enabled = false
        private set

    val pluginClassLoader: PluginClassLoader

    lateinit var pluginToml: Toml
        private set

    lateinit var pluginName: String
        private set
    lateinit var pluginVersion: String
        private set

    lateinit var pluginAuthor: String
        private set
    lateinit var pluginAuthorEmail: String
        private set

    lateinit var pluginClass: Class<*>
        private set
    lateinit var plugin: EOCVSimPlugin
        private set

    /**
     * The file system for the plugin
     */
    lateinit var fileSystem: SandboxFileSystem
        private set

    val fileSystemZip by lazy { PluginManager.FILESYSTEMS_FOLDER + File.separator + "${hash()}-fs" }
    val fileSystemZipPath by lazy { fileSystemZip.toPath() }

    /**
     * Whether the plugin has super access (full system access)
     */
    val hasSuperAccess get() = eocvSim.config.superAccessPluginHashes.contains(pluginFileHash)

    init {
        pluginClassLoader = PluginClassLoader(
            pluginFile,
            classpath
        ) {
            PluginContext(eocvSim, fileSystem, this)
        }
    }

    /**
     * Fetch the plugin info from the plugin.toml file
     * Fills the pluginName, pluginVersion, pluginAuthor and pluginAuthorEmail fields
     */
    fun fetchInfoFromToml() {
        if(::pluginToml.isInitialized) return

        pluginToml = Toml().read(pluginClassLoader.getResourceAsStream("plugin.toml")
            ?: throw InvalidPluginException("No plugin.toml in the jar file")
        )

        pluginName = pluginToml.getString("name") ?: throw InvalidPluginException("No name in plugin.toml")
        pluginVersion = pluginToml.getString("version") ?: throw InvalidPluginException("No version in plugin.toml")

        pluginAuthor = pluginToml.getString("author") ?: throw InvalidPluginException("No author in plugin.toml")
        pluginAuthorEmail = pluginToml.getString("author-email", "")
    }

    /**
     * Load the plugin from the jar file
     * @throws InvalidPluginException if the plugin.toml file is not found
     * @throws UnsupportedPluginException if the plugin requests an api version higher than the current one
     */
    fun load() {
        if(loaded) return

        fetchInfoFromToml()

        appender.appendln("${PluginOutput.SPECIAL_SILENT}Loading plugin $pluginName v$pluginVersion by $pluginAuthor from ${pluginSource.name}")

        setupFs()

        if(pluginToml.contains("api-version")) {
            val parsedVersion = ParsedVersion(pluginToml.getString("api-version"))

            if(parsedVersion > EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires a minimum api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin $pluginName requests min api version of v${parsedVersion}")
        }

        if(pluginToml.contains("max-api-version")) {
            val parsedVersion = ParsedVersion(pluginToml.getString("max-api-version"))

            if(parsedVersion < EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires a maximum api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin $pluginName requests max api version of v${parsedVersion}")
        }

        if(pluginToml.contains("exact-api-version")) {
            val parsedVersion = ParsedVersion(pluginToml.getString("exact-api-version"))

            if(parsedVersion != EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires an exact api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin $pluginName requests exact api version of v${parsedVersion}")
        }

        if(pluginToml.getBoolean("super-access", false)) {
            requestSuperAccess(pluginToml.getString("super-access-reason", ""))
        }

        pluginClass = pluginClassLoader.loadClassStrict(pluginToml.getString("main"))
        plugin = pluginClass.getConstructor().newInstance() as EOCVSimPlugin

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

        fileSystem = SandboxFileSystem(this)
    }

    /**
     * Enable the plugin
     */
    fun enable() {
        if(enabled || !loaded) return

        logger.info("Enabling plugin $pluginName v$pluginVersion")

        plugin.enabled = true
        plugin.onEnable()

        enabled = true
    }

    /**
     * Disable the plugin
     */
    fun disable() {
        if(!enabled || !loaded) return

        logger.info("Disabling plugin $pluginName v$pluginVersion")

        plugin.enabled = false
        plugin.onDisable()

        kill()
    }

    /**
     * Kill the plugin
     * This will close the file system and ban the class loader
     * @see EventHandler.banClassLoader
     */
    fun kill() {
        fileSystem.close()
        enabled = false
        EventHandler.banClassLoader(pluginClassLoader)
    }

    /**
     * Request super access for the plugin
     * @param reason the reason for requesting super access
     */
    fun requestSuperAccess(reason: String): Boolean {
        if(hasSuperAccess) return true
        return eocvSim.pluginManager.requestSuperAccessFor(this, reason)
    }

    /**
     * Get the hash of the plugin file based off the plugin name and author
     * @return the hash
     */
    fun hash(): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update("${pluginName} by ${pluginAuthor}".toByteArray())
        return SysUtil.byteArray2Hex(messageDigest.digest())
    }

    /**
     * Get the hash of the plugin file based off the file contents
     * @return the hash
     */
    val pluginFileHash by lazy {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(pluginFile.readBytes())
        SysUtil.byteArray2Hex(messageDigest.digest())
    }

}