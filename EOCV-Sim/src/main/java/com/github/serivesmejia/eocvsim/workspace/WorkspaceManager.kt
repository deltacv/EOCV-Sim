/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.workspace

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.pipeline.compiler.CompiledPipelineManager
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.io.FileWatcher
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.github.serivesmejia.eocvsim.workspace.config.WorkspaceConfig
import com.github.serivesmejia.eocvsim.workspace.config.WorkspaceConfigLoader
import com.github.serivesmejia.eocvsim.workspace.util.WorkspaceTemplate
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Paths

/**
 * WorkspaceManager class to manage the user-defined workspace
 * and to handle the workspace configuration file
 * Workspaces are a way to organize the user's pipelines and resources
 * to dynamically load them into the simulator on runtime
 * and to compile them into OpenCV pipelines on the fly as well.
 * New user code can be added to the workspace and the simulator
 * without the need to restart it. The workspace is defined by a
 * workspace configuration file, which is a JSON file that contains
 * the paths to the sources and resources folders, as well as the
 * excluded paths and file extensions, and the source files themselves
 * which are built using an embedded compiler and loaded with a custom
 * classloader.
 *
 * @param eocvSim the EOCVSim instance to manage the workspace for
 */
@OptIn(DelicateCoroutinesApi::class)
class WorkspaceManager(val eocvSim: EOCVSim) {

    val logger by loggerForThis()

    /**
     * Workspace configuration loader to load and save the workspace configuration file
     */
    val workspaceConfigLoader by lazy { WorkspaceConfigLoader(workspaceFile) }

    /**
     * The current workspace file
     */
    var workspaceFile = File(".")
        set(value) {
            if(value != workspaceFile) {
                workspaceConfigLoader.workspaceFile = value

                eocvSim.config.workspacePath = value.absolutePath
                eocvSim.configManager.saveToFile()

                field = value

                logger.info("Set current workspace to ${value.absolutePath}")

                if(::fileWatcher.isInitialized)
                    fileWatcher.stop()

                fileWatcher = FileWatcher(
                    arrayListOf(
                        sourcesAbsolutePath.toFile(),
                        resourcesAbsolutePath.toFile()
                    ), null, "Workspace"
                )
                fileWatcher.init()

                onWorkspaceChange.run()
            }

            cachedWorkspConfig = workspaceConfigLoader.loadWorkspaceConfig()

            if(cachedWorkspConfig == null) {
                cachedWorkspConfig = WorkspaceConfig()

                if(workspaceConfigLoader.workspaceConfigFile.exists())
                    logger.warn("Recreating workspace config file, old one failed to parse")
                else
                    logger.info("Creating workspace config file...")

                workspaceConfigLoader.saveWorkspaceConfig(workspaceConfig)
            } else {
                logger.info("Loaded workspace config successfully")
            }
        }

    private var cachedWorkspConfig: WorkspaceConfig? = null

    /**
     * The current workspace configuration, automagically saved when set
     */
    var workspaceConfig: WorkspaceConfig
        set(value) {
            logger.info("Saving workspace config file of ${workspaceFile.absolutePath}")
            workspaceConfigLoader.saveWorkspaceConfig(value)
            cachedWorkspConfig = value
        }
        get() {
            if(cachedWorkspConfig == null)
                ::workspaceFile.set(workspaceFile)

            return cachedWorkspConfig!!
        }

    /**
     * The relative path to the sources folder specified in the workspace configuration
     */
    val sourcesRelativePath get() = workspaceConfig.sourcesPath!!
    /**
     * The absolute path to the sources folder specified in the workspace configuration
     */
    val sourcesAbsolutePath get() = Paths.get(workspaceFile.absolutePath, sourcesRelativePath).normalize()!!

    /**
     * The relative path to the resources folder specified in the workspace configuration
     */
    val resourcesRelativePath get() = workspaceConfig.resourcesPath!!
    /**
     * The absolute path to the resources folder specified in the workspace configuration
     */
    val resourcesAbsolutePath get() = Paths.get(workspaceFile.absolutePath, resourcesRelativePath).normalize()!!

    /**
     * The relative paths to the excluded paths specified in the workspace configuration
     */
    val excludedRelativePaths get() = workspaceConfig.excludedPaths
    /**
     * The absolute paths to the excluded paths specified in the workspace configuration
     */
    val excludedAbsolutePaths get() = excludedRelativePaths.map {
        Paths.get(workspaceFile.absolutePath, it).normalize()!!
    }

    /**
     * The file extensions to exclude from the workspace
     */
    val excludedFileExtensions get() = workspaceConfig.excludedFileExtensions

    /**
     * The source files in the workspace, excluding the excluded paths and file extensions
     */
    val sourceFiles get() = SysUtil.filesUnder(sourcesAbsolutePath.toFile()) { file ->
        file.name.endsWith(".java") && excludedAbsolutePaths.stream().noneMatch {
            file.startsWith(it.toFile().absolutePath)
        }
    }

    /**
     * The resource files in the workspace, excluding the excluded paths and file extensions
     */
    val resourceFiles get() = SysUtil.filesUnder(resourcesAbsolutePath.toFile()) { file ->
        file.name.run {
            !endsWith(".java") && !endsWith(".class") && this != "eocvsim_workspace.json"
        } && excludedAbsolutePaths.stream().noneMatch {
            file.startsWith(it.toFile().absolutePath)
        } && excludedFileExtensions.stream().noneMatch {
            file.name.endsWith(".$it")
        }
    }

    /**
     * Event handler to run code when the workspace changes
     */
    val onWorkspaceChange = EventHandler("WorkspaceManager-OnChange")

    /**
     * File watcher to watch for changes in the workspace
     */
    lateinit var fileWatcher: FileWatcher
        private set

    /**
     * Stops the current file watcher, if initialized
     */
    fun stopFileWatcher() {
        if(::fileWatcher.isInitialized) {
            fileWatcher.stop()
        }
    }

    /**
     * Creates a new workspace with the specified folder
     * from a template file bundled with the simulator
     * @param folder the folder to create the workspace in
     * @param template the workspace template to use
     * @param workspaceConfig the workspace configuration to use
     * @return true if the workspace was created successfully, false otherwise
     */
    fun createWorkspaceWithTemplate(folder: File, template: WorkspaceTemplate): Boolean {
        if(!folder.isDirectory) return false
        if(!template.extractToIfEmpty(folder)) return false

        workspaceFile = folder
        return true
    }

    /**
     * Creates a new workspace with the specified folder
     * from a template file bundled with the simulator
     * Runs asynchronously on a coroutine
     * @param folder the folder to create the workspace in
     */
    @JvmOverloads fun createWorkspaceWithTemplateAsync(
        folder: File,
        template: WorkspaceTemplate,
        finishCallback: (() -> Unit)? = null
    ) = GlobalScope.launch(Dispatchers.IO) {
        if(!folder.isDirectory) return@launch
        if(!template.extractToIfEmpty(folder)) return@launch

        eocvSim.onMainUpdate.doOnce {
            workspaceFile = folder
            if(finishCallback != null) finishCallback()

            eocvSim.visualizer.asyncCompilePipelines()
        }
    }

    /**
     * Initializes the workspace manager
     * To be called by the EOCVSim instance
     * Initializes the file watcher and the workspace configuration loader
     * and sets the workspace file to the initial workspace or the default one
     */
    fun init() {
        onWorkspaceChange {
            fileWatcher.onChange {
                eocvSim.pipelineManager.compiledPipelineManager.asyncBuild()
            }
        }

        val file = eocvSim.params.initialWorkspace ?: File(eocvSim.config.workspacePath)

        workspaceFile = if(file.exists())
            file
        else
            CompiledPipelineManager.DEF_WORKSPACE_FOLDER
    }

    /**
     * Saves the current workspace configuration
     */
    fun saveCurrentConfig() {
        ::workspaceConfig.set(workspaceConfig)
    }

    /**
     * Reloads the workspace configuration
     * @return the reloaded workspace configuration
     */
    fun reloadConfig(): WorkspaceConfig {
        cachedWorkspConfig = null
        return workspaceConfig
    }

}
