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

import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.moandjiezana.toml.Toml
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.EOCVSimApi
import io.github.deltacv.eocvsim.plugin.security.PluginSignature
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import java.io.File

enum class PluginSource {
    REPOSITORY,
    FILE,
    EMBEDDED
}

data class PluginInfo(
    val name: String,
    val version: String,
    val author: String,
    val authorEmail: String,
    val main: String,
    val description: String,
    val superAccess: Boolean,
) {
    val nameWithVersion = "$name v$version"

    val nameWithAuthorVersion = "$name v$version by $author"

    companion object {
        // should throw corresponding exceptions if invalid
        fun fromToml(pluginToml: Toml): PluginInfo {
            val name = pluginToml.getString("name")?.trim() ?: throw InvalidPluginException("No name in plugin.toml")
            val version = pluginToml.getString("version")?.trim() ?: throw InvalidPluginException("No version in plugin.toml")
            val author = pluginToml.getString("author")?.trim() ?: throw InvalidPluginException("No author in plugin.toml")
            val authorEmail = pluginToml.getString("author-email")?.trim() ?: ""
            val main = pluginToml.getString("main")?.trim() ?: throw InvalidPluginException("No main in plugin.toml")
            val description = pluginToml.getString("description")?.trim() ?: ""
            val superAccess = pluginToml.getBoolean("super-access") ?: false

            return PluginInfo(
                name,
                version,
                author,
                authorEmail,
                main,
                description,
                superAccess
            )
        }
    }

    fun hash() = "$name[13mck]$author".hashString
}

abstract class PluginLoader {
    abstract val pluginSource: PluginSource

    abstract val loaded: Boolean
    abstract val enabled: Boolean
    abstract var shouldEnable: Boolean

    abstract val pluginInfo: PluginInfo

    abstract val classpath: List<File>

    abstract val pluginClass: Class<*>
    abstract val plugin: EOCVSimPlugin?

    abstract val fileSystem: SandboxFileSystem
    abstract val signature: PluginSignature
    abstract val hasSuperAccess: Boolean

    abstract val eocvSimApi: EOCVSimApi?

    /**
     * Loads the plugin into memory without enabling it.
     */
    abstract fun load()

    /**
     * Enables the plugin if loaded and allowed.
     */
    abstract fun enable()

    /**
     * Disables the plugin if enabled.
     */
    abstract fun disable()

    /**
     * Fully unloads the plugin and closes resources.
     */
    abstract fun kill()

    /**
     * Requests elevated permissions for the plugin.
     */
    abstract fun requestSuperAccess(reason: String): Boolean

    /**
     * Hash of the plugin based on name and author.
     */
    fun hash(): String = pluginInfo.hash()
}