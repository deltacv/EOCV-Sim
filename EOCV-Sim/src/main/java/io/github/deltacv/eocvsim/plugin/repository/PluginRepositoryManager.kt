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

import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.dialog.AppendDelegate
import com.github.serivesmejia.eocvsim.gui.dialog.PluginOutput
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.hexString
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.moandjiezana.toml.Toml
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import java.io.File
import kotlin.collections.iterator

class PluginRepositoryManager(
    val appender: AppendDelegate,
    val haltLock: Object
) {

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
    private lateinit var cachePluginsToml: Toml
    private lateinit var cacheTransitiveToml: Toml

    private lateinit var resolver: ConfigurableMavenResolverSystem

    private val _resolvedFiles = mutableListOf<File>()
    val resolvedFiles get() = _resolvedFiles.toList()

    val logger by loggerForThis()

    fun init() {
        logger.info("Initializing...")

        appender // init appender

        SysUtil.copyFileIs(CACHE_TOML_RES, CACHE_FILE, false)
        cacheToml = Toml().read(CACHE_FILE)

        cachePluginsToml = cacheToml.getTable("plugins")
            ?: Toml()

        cacheTransitiveToml = cacheToml.getTable("transitive")
            ?: Toml()

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
        val newTransitiveCache = mutableMapOf<String, MutableList<String>>()

        var shouldHalt = false

        for(plugin in plugins.toMap()) {
            if(plugin.value !is String)
                throw InvalidFileException("Invalid plugin dependency in repository.toml. $UNSURE_USER_HELP")

            val pluginDep = plugin.value as String

            var pluginJar: File? = null

            try {
                var isCached = false

                mainCacheLoop@
                for(cached in cachePluginsToml.toMap()) {
                    if(cached.key == pluginDep.hexString) {
                        val cachedFile = File(cached.value as String)

                        if(cachedFile.exists()) {
                            for(transitive in cacheTransitiveToml.getList<String>(pluginDep.hexString) ?: emptyList()) {
                                val transitiveFile = File(transitive as String)

                                if (!transitiveFile.exists()) {
                                    appender.appendln(PluginOutput.SPECIAL_SILENT + "Transitive dependency $transitive for plugin $pluginDep does not exist. Resolving...")
                                    break@mainCacheLoop
                                }

                                _resolvedFiles += transitiveFile // add transitive dependency to resolved files

                                newTransitiveCache[pluginDep.hexString] = newTransitiveCache.getOrDefault(
                                    pluginDep.hexString,
                                    mutableListOf()
                                ).apply {
                                    add(transitiveFile.absolutePath)
                                }
                            }

                            appender.appendln(
                                PluginOutput.SPECIAL_SILENT +
                                        "Found cached plugin \"$pluginDep\" (${pluginDep.hexString}). All transitive dependencies OK."
                            )

                            pluginJar = cachedFile
                            _resolvedFiles += cachedFile

                            newCache[pluginDep.hexString] = cachedFile.absolutePath // add plugin to revalidated cache

                            isCached = true // skip to next plugin, this one is already resolved by cache
                        }

                        break@mainCacheLoop
                    }
                }

                if(!isCached) {
                    // if we reach this point, the plugin was not found in cache
                    appender.appendln("Resolving plugin ${plugin.key} at \"${plugin.value}\"...")

                    resolver.resolve(pluginDep)
                        .withTransitivity()
                        .asFile()
                        .forEach { file ->
                            if (pluginJar == null) {
                                // the first file returned by maven is the plugin jar we want
                                pluginJar = file
                                newCache[pluginDep.hexString] = pluginJar!!.absolutePath
                            } else {
                                newTransitiveCache[pluginDep.hexString] = newTransitiveCache.getOrDefault(
                                    pluginDep.hexString,
                                    mutableListOf()
                                ).apply {
                                    add(file.absolutePath)
                                } // add transitive dependency to cache
                            }

                            _resolvedFiles += file // add file to resolved files to later build a classpath
                        }
                }

                files += pluginJar!!
            } catch(ex: Exception) {
                logger.warn("Failed to resolve plugin dependency \"$pluginDep\"", ex)
                appender.appendln("Failed to resolve plugin ${plugin.key}: ${ex.message}")
                shouldHalt = true
            }
        }

        writeCacheFile(newCache, newTransitiveCache)

        if(shouldHalt) {
            appender.append(PluginOutput.SPECIAL_CONTINUE)
            synchronized(haltLock) {
                haltLock.wait(10000) // wait for user to read the error
            }
        } else {
            appender.append(PluginOutput.SPECIAL_CLOSE)
        }

        return files
    }

    private fun writeCacheFile(
        cache: Map<String, String>,
        transitiveCache: Map<String, List<String>>
    ) {
        val cacheBuilder = StringBuilder()

        cacheBuilder.appendLine("# Do not edit this file, it is generated by the application.")

        cacheBuilder.appendLine("[plugins]") // add plugins table

        for(cached in cache) {
            cacheBuilder.appendLine("${cached.key} = \"${cached.value.replace("\\", "/")}\"")
        }

        cacheBuilder.appendLine("[transitive]") // add transitive dependencies table

        for((plugin, deps) in transitiveCache) {
            cacheBuilder.appendLine("$plugin = [") // add plugin hash as key

            for((i, dep) in deps.withIndex()) {
                cacheBuilder.append("\t\"${dep.replace("\\", "/")}\"") // add dependency path
                if(i < deps.size - 1) cacheBuilder.append(",") // add comma if not last

                cacheBuilder.appendLine()
            }

            cacheBuilder.appendLine("]")
        }

        SysUtil.saveFileStr(CACHE_FILE, cacheBuilder.toString().trim())
    }
}


class InvalidFileException(msg: String) : RuntimeException(msg)