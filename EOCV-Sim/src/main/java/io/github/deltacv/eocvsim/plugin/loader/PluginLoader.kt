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
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.moandjiezana.toml.Toml
import io.github.deltacv.common.util.ParsedVersion
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.security.PluginSignature
import io.github.deltacv.eocvsim.plugin.security.PluginSignatureVerifier
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import net.lingala.zip4j.ZipFile
import java.io.File

enum class PluginSource {
    REPOSITORY,
    FILE,
    EMBEDDED
}

class PluginParser(pluginToml: Toml) {
    val pluginName = pluginToml.getString("name")?.trim() ?: throw InvalidPluginException("No name in plugin.toml")
    val pluginVersion = pluginToml.getString("version")?.trim() ?: throw InvalidPluginException("No version in plugin.toml")

    val pluginAuthor = pluginToml.getString("author")?.trim() ?: throw InvalidPluginException("No author in plugin.toml")
    val pluginAuthorEmail = pluginToml.getString("author-email", "")?.trim()

    val pluginMain = pluginToml.getString("main")?.trim() ?: throw InvalidPluginException("No main in plugin.toml")

    val pluginDescription = pluginToml.getString("description", "")?.trim()

    /**
     * Get the hash of the plugin based off the plugin name and author
     * @return the hash
     */
    fun hash() = "${pluginName}${PluginOutput.SPECIAL}${pluginAuthor}".hashString
}

interface PluginLoader {

    val pluginSource: PluginSource

    val loaded: Boolean
    val enabled: Boolean
    var shouldEnable: Boolean

    val pluginName: String
    val pluginVersion: String
    val pluginDescription: String
    val pluginAuthor: String
    val pluginAuthorEmail: String

    val classpath: List<File>

    val pluginClass: Class<*>
    val plugin: EOCVSimPlugin

    val fileSystem: SandboxFileSystem
    val signature: PluginSignature
    val hasSuperAccess: Boolean

    /**
     * Loads the plugin into memory without enabling it.
     */
    fun load()

    /**
     * Enables the plugin if loaded and allowed.
     */
    fun enable()

    /**
     * Disables the plugin if enabled.
     */
    fun disable()

    /**
     * Fully unloads the plugin and closes resources.
     */
    fun kill()

    /**
     * Requests elevated permissions for the plugin.
     */
    fun requestSuperAccess(reason: String): Boolean

    /**
     * Hash of the plugin based on name and author.
     */
    fun hash(): String
}