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
    /** Plugin loaded from a remote repository */
    REPOSITORY,

    /** Plugin loaded from a local file */
    FILE,

    /** Plugin bundled with the application */
    EMBEDDED
}

/**
 * Immutable metadata describing a plugin.
 *
 * This information is loaded from `plugin.toml` and is used by the loader
 * to identify, validate, and initialize the plugin.
 */
data class PluginInfo(
    val name: String,
    val version: String,
    val author: String,
    val authorEmail: String,
    val main: String,
    val description: String,
    val superAccess: Boolean,
) {

    /** Human-readable name including version */
    val nameWithVersion = "$name v$version"

    /** Human-readable name including version and author */
    val nameWithAuthorVersion = "$name v$version by $author"

    companion object {

        /**
         * Parses and validates plugin metadata from a TOML file.
         *
         * Required fields must be present and non-blank, otherwise an
         * [InvalidPluginException] is thrown.
         */
        fun fromToml(pluginToml: Toml): PluginInfo {
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

    /**
     * Stable hash derived from the plugin name and author.
     *
     * Used to uniquely identify a plugin across reloads.
     */
    fun hash() = "$name[13mck]$author".hashString
}

/**
 * Base class responsible for loading, enabling, and managing a plugin.
 *
 * Implementations define how plugins are sourced, isolated, and wired
 * into the runtime.
 */
abstract class PluginLoader {

    /** Source from which this plugin was loaded */
    abstract val pluginSource: PluginSource

    /** Whether the plugin has been loaded into memory */
    abstract val loaded: Boolean

    /** Whether the plugin is currently enabled */
    abstract val enabled: Boolean

    /** Whether the plugin should be enabled after loading */
    abstract var shouldEnable: Boolean

    /** Parsed plugin metadata */
    abstract val pluginInfo: PluginInfo

    /** Additional classpath entries used to load the plugin */
    abstract val classpath: List<File>

    /** Main plugin class */
    abstract val pluginClass: Class<*>

    /** Plugin instance, if loaded */
    abstract val plugin: EOCVSimPlugin?

    /** Sandbox filesystem assigned to this plugin */
    abstract val fileSystem: SandboxFileSystem

    /** Cryptographic signature of the plugin */
    abstract val signature: PluginSignature

    /** Whether the plugin currently has super access */
    abstract val hasSuperAccess: Boolean

    /** API instance exposed to the plugin */
    abstract val eocvSimApi: EOCVSimApi?

    /** Loads the plugin into memory without enabling it */
    abstract fun load()

    /** Enables the plugin if it is loaded and permitted */
    abstract fun enable()

    /** Disables the plugin if it is enabled */
    abstract fun disable()

    /** Fully unloads the plugin and releases all resources */
    abstract fun kill()

    /**
     * Requests elevated permissions for the plugin.
     *
     * @param reason explanation shown to the user
     * @return `true` if access was granted
     */
    abstract fun requestSuperAccess(reason: String): Boolean

    /**
     * Returns the stable hash of this plugin.
     */
    fun hash(): String = pluginInfo.hash()
}
