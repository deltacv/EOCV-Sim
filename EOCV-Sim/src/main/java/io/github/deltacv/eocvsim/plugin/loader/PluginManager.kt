/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.eocvsim.plugin.loader

import com.github.serivesmejia.eocvsim.Build
import com.github.serivesmejia.eocvsim.LifecycleSignal
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.plugin.api.impl.EOCVSimApiImpl
import com.github.serivesmejia.eocvsim.plugin.output.PluginDialogSignal
import com.github.serivesmejia.eocvsim.plugin.output.PluginOutputHandler
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.orchestration.initDependency
import com.github.serivesmejia.eocvsim.util.orchestration.PhaseOrchestrableBase
import io.github.deltacv.common.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.repository.PluginRepositoryManager
import io.github.deltacv.eocvsim.plugin.security.superaccess.SuperAccessDaemon
import io.github.deltacv.eocvsim.plugin.security.superaccess.SuperAccessDaemonClient
import io.github.deltacv.eocvsim.plugin.security.toMutable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import kotlin.properties.Delegates

/**
 * Manages the loading, enabling and disabling of plugins
 */
class PluginManager : PhaseOrchestrableBase(), KoinComponent {

    private val configManager: ConfigManager by initDependency(inject())
    private val visualizer: Visualizer by inject()
    private val outputHandler: PluginOutputHandler by inject()

    private val onMainUpdate: EventHandler by inject(named("onMainLoop"))
    private val lifecycleChannel: Channel<LifecycleSignal> by inject(named("lifecycle"))

    companion object {
        val PLUGIN_FOLDER = io.github.deltacv.eocvsim.plugin.PLUGIN_FOLDER
        val PLUGIN_CACHING_FOLDER = io.github.deltacv.eocvsim.plugin.PLUGIN_CACHING_FOLDER
        val FILESYSTEMS_FOLDER = io.github.deltacv.eocvsim.plugin.FILESYSTEMS_FOLDER

        const val GENERIC_SUPERACCESS_WARN =
            "Plugins run in a restricted environment by default. <b>SuperAccess will grant full system access. Ensure you trust the authors before accepting.</b>"
        const val GENERIC_LAWYER_YEET =
            "<br><br>By accepting, you acknowledge that deltacv is not responsible for damages caused by third-party plugins."
    }

    val logger by loggerForThis()

    val superAccessDaemonClient by lazy {
        SuperAccessDaemonClient(
            autoacceptOnTrusted = configManager.config.autoAcceptSuperAccessOnTrusted
        )
    }

    private val loadedPluginHashes = mutableListOf<String>()

    val repositoryManager by lazy {
        PluginRepositoryManager(outputHandler, onMainUpdate) { lifecycleChannel.trySend(LifecycleSignal.Restart) }
    }

    private val _pluginFiles = mutableListOf<File>()

    /**
     * List of plugin files in the plugins folder
     */
    val pluginFiles get() = _pluginFiles.toList()

    private val _loaders = mutableListOf<PluginLoader>()
    val loaders get() = _loaders.toList()

    private var enableTimestamp by Delegates.notNull<Long>()
    private var isEnabled = false

    /**
     * Provides EOCV-Sim API instances for plugins
     */
    val eocvSimApiProvider = EOCVSimApiProvider { plugin -> EOCVSimApiImpl(plugin) }

    /**
     * Initializes the plugin manager
     * Loads all plugin files in the plugins folder
     * Creates a PluginLoader for each plugin file
     * and stores them in the loaders map
     * @see PluginLoader
     */
    override suspend fun init() {
        visualizer.onInitFinished {
            outputHandler.sendDialogSignal(PluginDialogSignal.Hide)
        }

        outputHandler.sendOutputLine("Initializing PluginManager")

        superAccessDaemonClient.init()

        // replace papervision line

        if (!configManager.config.flags.getOrDefault("hasDiscardedPaperVisionRepository", false)) {
            try {
                val repositoriesStr = PluginRepositoryManager.REPOSITORY_FILE.readText()
                for (line in repositoriesStr.lines()) {
                    // retrofit to now instead use embedded papervision
                    if (line.contains("papervision", ignoreCase = true) && !line.trim().startsWith("#")) {
                        // add a # to the start of the line to comment it out
                        PluginRepositoryManager.REPOSITORY_FILE.writeText(
                            repositoriesStr.replaceFirst(
                                line,
                                "\n# PaperVision is now embedded inside EOCV-Sim, there's no need to declare it here\n# $line"
                            )
                        )

                        logger.info("Commented out PaperVision repository line in ${PluginRepositoryManager.REPOSITORY_FILE.absolutePath}")
                        break
                    }
                }
            } catch (_: Exception) {
            }

            configManager.config.flags["hasDiscardedPaperVisionRepository"] = true
        }

        repositoryManager.init()

        val pluginFilesInFolder = PLUGIN_FOLDER.listFiles()?.let {
            it.filter { file -> file.extension == "jar" }
        } ?: emptyList()

        _pluginFiles.addAll(repositoryManager.resolveAll())

        if (configManager.config.flags.getOrDefault("startFresh", false)) {
            logger.warn("startFresh = true, deleting all plugins in the plugins folder")

            for (file in pluginFilesInFolder) {
                file.delete()
            }

            configManager.config.flags["startFresh"] = false
            configManager.saveToFile()
        } else {
            _pluginFiles.addAll(pluginFilesInFolder)
        }

        if (pluginFiles.isEmpty()) {
            outputHandler.sendOutputLine("No plugin files to load")
        }

        for (pluginFile in pluginFiles) {
            try {
                val loader = FilePluginLoaderImpl(
                    pluginFile,
                    repositoryManager.resolvedFiles,
                    if (pluginFile in repositoryManager.resolvedFiles)
                        PluginSource.REPOSITORY else PluginSource.FILE,
                    this,
                    outputHandler
                )

                _loaders.add(loader)
                loader.fetchInfoFromToml()
            } catch (e: Throwable) {
                outputHandler.sendOutputLine("Failure creating PluginLoader for ${pluginFile.name}: ${e.message}")
                logger.error("Failure creating PluginLoader for ${pluginFile.name}", e)
            }
        }

        enableTimestamp = System.currentTimeMillis()
        isEnabled = true

        if (_loaders.find { it.pluginInfo.name == "PaperVision" && it.pluginInfo.author == "deltacv" } == null) {
            if (PluginManager::class.java.getResourceAsStream("/embedded_plugins/PaperVisionPlugin.jar") != null) {
                _loaders.add(
                    EmbeddedFilePluginLoader(
                        "/embedded_plugins/PaperVisionPlugin.jar",
                        listOf(),
                        this,
                        outputHandler
                    )
                )

                logger.info("Loaded embedded PaperVision from resources")
            } else {
                try {
                    val pluginInfo = PluginInfo(
                        "PaperVision",
                        Build.paperVisionVersion,
                        "deltacv",
                        "dev@deltacv.org",
                        "io.github.deltacv.papervision.plugin.PaperVisionEOCVSimPlugin",
                        "Create your custom OpenCV algorithms using a user-friendly node editor interface",
                        true
                    )

                    @Suppress("UNCHECKED_CAST")
                    addEmbeddedPlugin(
                        pluginInfo,
                        Class.forName("io.github.deltacv.papervision.plugin.PaperVisionEOCVSimPlugin") as Class<out EOCVSimPlugin>,
                    )

                    logger.info("Loaded embedded PaperVision from built-in class")
                } catch(_: ClassNotFoundException) {
                    logger.info("Embedded PaperVision was not found")
                }
            }
        } else {
            outputHandler.sendOutputLine("PaperVision plugin is already loaded, skipping embedded plugin.")
        }

        loadPlugins()
    }

    override suspend fun run() { }

    override suspend fun destroy() {
        disablePlugins()
    }

    private fun <T : EOCVSimPlugin> addEmbeddedPlugin(
        pluginInfo: PluginInfo,
        pluginClass: Class<T>
    ) {
        val tempLoader = EmbeddedPluginLoader(pluginInfo, pluginClass, eocvSimApiProvider)

        logger.info("Adding embedded plugin: ${pluginInfo.nameWithVersionAndAuthor}")
        _loaders.add(tempLoader)
    }

    /**
     * Loads all plugins
     * @see PluginLoader.load
     */
    private fun loadPlugins() {
        for (loader in _loaders.toTypedArray()) {
            try {
                val hash = loader.hash()

                if (hash in loadedPluginHashes) {
                    val source = when (loader.pluginSource) {
                        PluginSource.FILE -> "plugins folder"
                        PluginSource.REPOSITORY -> "repository"
                        PluginSource.EMBEDDED -> "embedded plugin"
                    }

                    outputHandler.sendDialogSignal(PluginDialogSignal.ShowOutput)
                    outputHandler.sendOutputLine("Plugin ${loader.pluginInfo.nameWithVersion} is already loaded. Please delete the duplicate from the $source !")
                    return
                }

                loader.load()
                loadedPluginHashes.add(hash)
            } catch (e: Throwable) {
                outputHandler.sendDialogSignal(PluginDialogSignal.ShowOutput)
                outputHandler.sendOutputLine("-- Failure loading ${loader.pluginInfo.nameWithVersion} --")
                outputHandler.sendOutputLine("'${e.toString()}'")

                logger.error("Failure loading ${loader.pluginInfo.nameWithVersion}", e)

                outputHandler.sendDialogSignal(PluginDialogSignal.EnableContinue)
                runBlocking {
                    outputHandler.waitForContinuation(15000L)
                }
                outputHandler.sendDialogSignal(PluginDialogSignal.DisableContinue)

                _loaders.remove(loader)
                loader.kill()
            }
        }
    }

    /**
     * Enables all plugins
     * @see PluginLoader.enable
     */
    fun enablePlugins() {
        for (loader in _loaders) {
            try {
                loader.enable()
            } catch (e: Throwable) {
                outputHandler.sendOutputLine("Failure enabling ${loader.pluginInfo.nameWithVersion}: ${e.message}")
                logger.error("Failure enabling ${loader.pluginInfo.nameWithVersion}", e)
                loader.kill()
            }
        }
    }

    /**
     * Disables all plugins
     * @see PluginLoader.disable
     */
    @Synchronized
    fun disablePlugins() {
        if (!isEnabled) return

        for (loader in _loaders) {
            try {
                loader.disable()
            } catch (e: Throwable) {
                outputHandler.sendOutputLine("Failure disabling ${loader.pluginInfo.nameWithVersion}: ${e.message}")
                logger.error("Failure disabling ${loader.pluginInfo.nameWithVersion}", e)
                loader.kill()
            }
        }

        isEnabled = false
    }

    /**
     * Requests super access for a plugin loader
     *
     * @param loader the plugin loader to request super access for
     * @param reason the reason for requesting super access
     * @return true if super access was granted, false otherwise
     */
    fun requestSuperAccessFor(loader: PluginLoader, reason: String): Boolean {
        if (loader.hasSuperAccess) {
            outputHandler.sendOutputLine("Plugin ${loader.pluginInfo.name} v${loader.pluginInfo.version} already has super access")
            return true
        }

        val signature = loader.signature

        outputHandler.sendOutputLine("Requesting super access for ${loader.pluginInfo.name} v${loader.pluginInfo.version}")

        if (loader is FilePluginLoaderImpl) {
            var access = false

            superAccessDaemonClient.sendRequest(
                SuperAccessDaemon.SuperAccessMessage.Request(
                    loader.pluginFile.absolutePath,
                    signature.toMutable(),
                    reason
                )
            ) {
                access = it
                outputHandler.signalContinuation()
            }

            runBlocking {
                outputHandler.waitForContinuation(0L) // Wait indefinitely.
            }

            outputHandler.sendOutputLine("Super access for ${loader.pluginInfo.nameWithVersion} was ${if (access) "granted" else "denied"}")

            return access
        } else {
            outputHandler.sendOutputLine("Super access for ${loader.pluginInfo.nameWithVersion} is automatically determined, it was ${if (loader.hasSuperAccess) "granted" else "denied"}")
            return loader.hasSuperAccess
        }
    }

    fun hasSuperAccess(pluginFile: File) = superAccessDaemonClient.checkAccess(pluginFile)

    fun isPluginEnabledInConfig(loader: PluginLoader) = configManager.config.flags.getOrDefault(loader.hash(), true)!!

    fun setPluginEnabledInConfig(loader: PluginLoader, enabled: Boolean) {
        configManager.config.flags[loader.hash()] = enabled
        configManager.saveToFile()
    }
}
