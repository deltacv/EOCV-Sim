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
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.moandjiezana.toml.Toml
import io.github.deltacv.common.util.ParsedVersion
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.sandbox.nio.SandboxFileSystem
import net.lingala.zip4j.ZipFile
import java.io.File
import java.security.MessageDigest

class PluginLoader(private val pluginFile: File, val eocvSim: EOCVSim) {

    val logger by loggerForThis()

    var loaded = false
        private set

    var enabled = false
        private set

    val pluginClassLoader: PluginClassLoader

    lateinit var pluginToml: Toml

    lateinit var pluginName: String
    lateinit var pluginVersion: String

    lateinit var pluginClass: Class<*>
        private set
    lateinit var plugin: EOCVSimPlugin

    lateinit var fileSystem: SandboxFileSystem
        private set

    val fileSystemZip = PluginManager.FILESYSTEMS_FOLDER + File.separator + "${hash()}-fs"
    val fileSystemZipPath = fileSystemZip.toPath()

    init {
        setupFs()
        pluginClassLoader = PluginClassLoader(pluginFile, PluginContext(eocvSim, fileSystem))
    }

    fun load() {
        if(loaded) return

        pluginToml = Toml().read(pluginClassLoader.getResourceAsStream("plugin.toml")
            ?: throw InvalidPluginException("No plugin.toml in the jar file")
        )

        pluginName = pluginToml.getString("name")
        pluginVersion = pluginToml.getString("version")

        logger.info("Loading plugin $pluginName v$pluginVersion")

        if(pluginToml.contains("api-version")) {
            val parsedVersion = ParsedVersion(pluginToml.getString("api-version"))

            if(parsedVersion > EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin request api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")
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

    fun enable() {
        if(enabled || !loaded) return

        logger.info("Enabling plugin $pluginName v$pluginVersion")

        plugin.enabled = true
        plugin.onEnable()

        enabled = true
    }

    fun disable() {
        if(!enabled || !loaded) return

        logger.info("Disabling plugin $pluginName v$pluginVersion")

        plugin.enabled = false
        plugin.onDisable()

        fileSystem.close()

        enabled = false
    }

    fun hash(): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(pluginFile.absolutePath.encodeToByteArray())
        return SysUtil.byteArray2Hex(messageDigest.digest())
    }

}