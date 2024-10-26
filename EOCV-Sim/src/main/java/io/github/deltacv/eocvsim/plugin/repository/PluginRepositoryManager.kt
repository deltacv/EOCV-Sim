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

package io.github.deltacv.eocvsim.plugin.repository

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.hexString
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.moandjiezana.toml.Toml
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import java.io.File

class PluginRepositoryManager {

    companion object {
        val REPOSITORY_FILE = PluginManager.PLUGIN_FOLDER + File.separator + "repository.toml"
        val CACHE_FILE = PluginManager.PLUGIN_FOLDER + File.separator + "cache.toml"

        val REPOSITORY_TOML_RES = PluginRepositoryManager::class.java.getResourceAsStream("/repository.toml")
        val CACHE_TOML_RES = PluginRepositoryManager::class.java.getResourceAsStream("/cache.toml")

        const val UNSURE_USER_HELP = "If you're unsure on what to do, delete the repository.toml file and restart the program."
    }

    private lateinit var pluginsToml: Toml
    private lateinit var pluginsRepositories: Toml
    private lateinit var plugins: Toml

    private lateinit var cacheToml: Toml

    private lateinit var resolver: ConfigurableMavenResolverSystem

    private val _resolvedFiles = mutableListOf<File>()
    val resolvedFiles get() = _resolvedFiles.toList()

    val logger by loggerForThis()

    fun init() {
        logger.info("Initializing plugin repository manager")

        SysUtil.copyFileIs(CACHE_TOML_RES, CACHE_FILE, false)
        cacheToml = Toml().read(CACHE_FILE)

        SysUtil.copyFileIs(REPOSITORY_TOML_RES, REPOSITORY_FILE, false)
        pluginsToml = Toml().read(REPOSITORY_FILE)

        pluginsRepositories = pluginsToml.getTable("repositories")
            ?: throw InvalidFileException("No repositories found in repository.toml. $UNSURE_USER_HELP")
        plugins = pluginsToml.getTable("plugins")
            ?: Toml()

        resolver = Maven.configureResolver()
            .withClassPathResolution(false)

        for (repo in pluginsRepositories.toMap()) {
            if(repo.value !is String)
                throw InvalidFileException("Invalid repository URL in repository.toml. $UNSURE_USER_HELP")

            resolver.withRemoteRepo(repo.key, repo.value as String, "default")

            logger.info("Added repository ${repo.key} with URL ${repo.value}")
        }
    }

    fun resolveAll(): List<File> {
        val files = mutableListOf<File>()

        val newCache = mutableMapOf<String, String>()

        for(cache in cacheToml.toMap()) {
            newCache[cache.key] = cache.value as String
        }

        for(plugin in plugins.toMap()) {
            if(plugin.value !is String)
                throw InvalidFileException("Invalid plugin dependency in repository.toml. $UNSURE_USER_HELP")

            logger.info("Resolving plugin dependency ${plugin.key} with ${plugin.value}")

            val pluginDep = plugin.value as String

            var pluginJar: File? = null

            try {
                for(cached in cacheToml.toMap()) {
                    if(cached.key == pluginDep.hexString) {
                        val cachedFile = File(cached.value as String)
                        if(cachedFile.exists()) {
                            logger.info("Found cached plugin dependency $pluginDep (${pluginDep.hexString})")
                            files += cachedFile
                            _resolvedFiles += cachedFile
                            continue
                        } else {
                            newCache.remove(cached.key)
                        }
                    }
                }

                resolver.resolve(pluginDep)
                    .withTransitivity()
                    .asFile()
                    .forEach { file ->
                        if(pluginJar == null) {
                            // the first file is the plugin jar
                            pluginJar = file
                            newCache[pluginDep.hexString] = pluginJar!!.absolutePath
                        }

                        _resolvedFiles += file
                    }

                files += pluginJar!!
            } catch(ex: Exception) {
                logger.warn("Failed to resolve plugin dependency $pluginDep", ex)
            }
        }

        val cacheBuilder = StringBuilder()
        cacheBuilder.append("# Do not edit this file, it is generated by the application.\n")
        for(cached in newCache) {
            cacheBuilder.append("${cached.key} = \"${cached.value.replace("\\", "/")}\"\n")
        }

        SysUtil.saveFileStr(CACHE_FILE, cacheBuilder.toString())

        return files
    }
}


class InvalidFileException(msg: String) : RuntimeException(msg)