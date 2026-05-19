/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.repository

import com.github.serivesmejia.eocvsim.plugin.output.PluginDialogSignal
import com.github.serivesmejia.eocvsim.plugin.output.PluginOutputHandler
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.github.serivesmejia.eocvsim.util.extension.plus
import org.deltacv.common.util.serialization.Toml
import org.deltacv.common.util.ParsedVersion
import org.deltacv.common.util.loggerForThis
import org.deltacv.eocvsim.plugin.loader.PluginManager
import kotlinx.coroutines.runBlocking
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import java.io.File
import javax.swing.JOptionPane

class PluginRepositoryManager(
    val outputHandler: PluginOutputHandler,
    val onMainUpdate: EventHandler,
    val restart: () -> Unit
) {

    companion object {
        val REPOSITORY_FILE = PluginManager.PLUGIN_FOLDER + File.separator + "repository.toml"
        val CACHE_FILE = PluginManager.PLUGIN_CACHING_FOLDER + File.separator + "cache.toml"

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

    private val repositories = mutableListOf<String>()

    private val _resolvedFiles = mutableListOf<File>()
    val resolvedFiles get() = _resolvedFiles.toList()

    val logger by loggerForThis()

    fun init() {
        logger.info("Initializing...")


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

            var repoUrl = repo.value as String
            if(!repoUrl.endsWith("/")) {
                repoUrl += "/"
            }

            resolver.withRemoteRepo(repo.key, repoUrl, "default")
            repositories += repoUrl

            logger.info("Added repository ${repo.key} with URL $repoUrl")
        }
    }

    fun resolveAll(): List<File> {
        val files = mutableListOf<File>()
        val newCache = mutableMapOf<String, String>()
        val newTransitiveCache = mutableMapOf<String, MutableList<String>>()
        var shouldHalt = false

        for (plugin in plugins.toMap()) {
            if (plugin.value !is String) {
                throw InvalidFileException("Invalid plugin dependency in repository.toml. $UNSURE_USER_HELP")
            }

            val pluginDep = plugin.value as String
            var pluginJar: File?

            try {
                // Attempt to resolve from cache
                pluginJar = if (resolveFromCache(pluginDep, newCache, newTransitiveCache)) {
                    File(newCache[pluginDep.hashString]!!)
                } else {
                    // Resolve from the resolver if not found in cache
                    resolveFromResolver(pluginDep, newCache, newTransitiveCache)
                }

                files += pluginJar
            } catch (ex: Exception) {
                handleResolutionError(pluginDep, ex)
                shouldHalt = true
            }

            val latest = checkForUpdates(pluginDep, repositories.toTypedArray())

            if(latest != null) {
                outputHandler.sendOutputLine(
                    "Plugin \"${plugin.key}\" is outdated. Latest version is ${latest.version}."
                )

                onMainUpdate.once {
                    promptUpdateAndRestart(plugin.key, pluginDep, latest)
                }
            } else {
                outputHandler.sendOutputLine(
                    "Plugin \"${plugin.key}\" is up to date."
                )
            }
        }

        writeCacheFile(newCache, newTransitiveCache)

        handleResolutionOutcome(shouldHalt)

        return files
    }

    private fun promptUpdateAndRestart(pluginName: String, pluginDep: String, latest: ParsedVersion) {
        if (promptUpdate(pluginName, latest)) {
            outputHandler.sendOutputLine("Updating plugin \"$pluginName\" to version ${latest.version}...")

            val artifact = parseArtifact(pluginDep)

            // Read the current TOML file content
            val tomlFile = REPOSITORY_FILE
            val tomlString = tomlFile.readText()
            val tomlLines = tomlString.lines().toMutableList()

            // Locate the `[plugins]` section
            val indexOfPlugins = tomlLines.indexOfFirst { it.trim() == "[plugins]" }
            if (indexOfPlugins == -1) {
                outputHandler.sendOutputLine("Failed to find [plugins] section in the TOML file.")
                return
            }

            // Find the line for `pluginDep` under `[plugins]`
            val pluginLineIndex = tomlLines
                .subList(indexOfPlugins + 1, tomlLines.size) // Only consider lines after `[plugins]`
                .indexOfFirst { it.trim().matches(Regex("^$pluginName\\s*=\\s*.*")) } // Find the line
                .takeIf { it != -1 } // Check if the plugin was found
                ?.let { it + indexOfPlugins + 1 } // Adjust the index relative to the full list

            if (pluginLineIndex == -1 || pluginLineIndex == null) {
                outputHandler.sendOutputLine("Failed to find plugin \"$pluginName\" in the TOML file.")
                return
            }

            // Update the version
            val oldLine = tomlLines[pluginLineIndex]
            tomlLines[pluginLineIndex] = "$pluginName = \"${artifact.groupId}:${artifact.artifactId}:${latest.version}\""

            // Write updated content back to the TOML file
            tomlFile.writeText(tomlLines.joinToString("\n"))

            outputHandler.sendOutputLine("Successfully updated \"$pluginName\" to version ${latest.version}. Restarting...")
            restart()
        }
    }

    private fun promptUpdate(
        pluginName: String,
        latest: ParsedVersion
    ): Boolean {
        val result = JOptionPane.showOptionDialog(
            null,
            "Plugin \"$pluginName\" is outdated. Latest version is ${latest.version}. Do you want to update? This will restart EOCV-Sim.",
            "Update Plugin",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            arrayOf("Update", "Ignore"),
            null
        )

        return result == JOptionPane.YES_OPTION
    }

    // Function to handle resolution from cache
    private fun resolveFromCache(
        pluginDep: String,
        newCache: MutableMap<String, String>,
        newTransitiveCache: MutableMap<String, MutableList<String>>
    ): Boolean {
        for (cached in cachePluginsToml.toMap()) {
            if (cached.key == pluginDep.hashString) {
                val cachedFile = File(cached.value as String)

                if (cachedFile.exists() && areAllTransitivesCached(pluginDep)) {
                    addToResolvedFiles(cachedFile, pluginDep, newCache, newTransitiveCache)
                    outputHandler.sendOutputLine(
                        "Found cached plugin \"$pluginDep\" (${pluginDep.hashString}). All transitive dependencies OK."
                    )
                    return true
                } else {
                    outputHandler.sendOutputLine(
                        "Dependency missing for plugin $pluginDep. Resolving..."
                    )
                }
            }
        }
        return false
    }

    // Function to check if all transitive dependencies are cached
    private fun areAllTransitivesCached(pluginDep: String): Boolean {
        val deps = cacheTransitiveToml.getList<String>(pluginDep.hashString)

        val depsHash = depsToHash(deps)
        val tomlHash = cacheTransitiveToml.getString("${pluginDep.hashString}_hash", "")

        val matchesDepsHash = depsHash == tomlHash

        if(!matchesDepsHash) {
            outputHandler.sendOutputLine(
                "Mismatch, $depsHash != $tomlHash"
            )

            outputHandler.sendOutputLine(
                "Transitive dependencies hash mismatch for plugin $pluginDep. Resolving..."
            )
        }

        return matchesDepsHash &&
            deps != null &&
            deps.isNotEmpty() &&
            deps.all {
                val exists = File(it).exists()

                if(!exists) {
                    outputHandler.sendOutputLine(
                        "Couldn't find file specified in cache for plugin $pluginDep, expected at \"$it\"."
                    )
                }

                exists
            }
    }

    // Function to resolve plugin using the resolver
    private fun resolveFromResolver(
        pluginDep: String,
        newCache: MutableMap<String, String>,
        newTransitiveCache: MutableMap<String, MutableList<String>>
    ): File {
        outputHandler.sendOutputLine("Resolving plugin \"$pluginDep\"...")

        var pluginJar: File? = null

        resolver.resolve(pluginDep)
            .withTransitivity()
            .asFile()
            .forEach { file ->
                if (pluginJar == null) {
                    pluginJar = file
                    newCache[pluginDep.hashString] = file.absolutePath
                } else {
                    newTransitiveCache.getOrPut(pluginDep.hashString) { mutableListOf() }
                        .add(file.absolutePath)
                }

                _resolvedFiles += file
            }

        return pluginJar!!
    }

    // Function to add resolved files to the cache and resolved list
    private fun addToResolvedFiles(
        cachedFile: File,
        pluginDep: String,
        newCache: MutableMap<String, String>,
        newTransitiveCache: MutableMap<String, MutableList<String>>
    ) {
        _resolvedFiles += cachedFile
        newCache[pluginDep.hashString] = cachedFile.absolutePath

        cacheTransitiveToml.getList<String>(pluginDep.hashString)?.forEach { transitive ->
            _resolvedFiles += File(transitive)
            newTransitiveCache.getOrPut(pluginDep.hashString) { mutableListOf() }
                .add(transitive)
        }
    }

    // Function to handle resolution errors
    private fun handleResolutionError(pluginDep: String, ex: Exception) {
        logger.warn("Failed to resolve plugin dependency \"$pluginDep\"", ex)
        outputHandler.sendOutputLine("Failed to resolve plugin \"$pluginDep\": ${ex.message}")
    }

    // Function to handle the outcome of the resolution process
    private fun handleResolutionOutcome(shouldHalt: Boolean) {
        if (shouldHalt) {
            // Show dialog and enable Continue button if errors occurred
            outputHandler.sendDialogSignal(PluginDialogSignal.ShowOutput)
            outputHandler.sendDialogSignal(PluginDialogSignal.EnableContinue)

            // Block for up to 15 seconds waiting for user to read the error
            runBlocking {
                outputHandler.waitForContinuation(15000L)
            }
        } else {
            // Close dialog if everything resolved successfully
            outputHandler.sendDialogSignal(PluginDialogSignal.Hide)
        }
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

            cacheBuilder.appendLine("${plugin}_hash=\"${depsToHash(deps)}\"")
        }

        SysUtil.saveFileStr(CACHE_FILE, cacheBuilder.toString().trim())
    }

    private fun depsToHash(deps: List<String>) =
        deps.joinToString(File.pathSeparator).replace("\\", "/").trimEnd(File.pathSeparatorChar).hashString
}

class InvalidFileException(msg: String) : RuntimeException(msg)
