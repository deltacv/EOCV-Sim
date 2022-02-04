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

@OptIn(DelicateCoroutinesApi::class)
class WorkspaceManager(val eocvSim: EOCVSim) {

    val logger by loggerForThis()

    val workspaceConfigLoader by lazy { WorkspaceConfigLoader(workspaceFile) }

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

    val sourcesRelativePath get() = workspaceConfig.sourcesPath!!
    val sourcesAbsolutePath get() = Paths.get(workspaceFile.absolutePath, sourcesRelativePath).normalize()!!

    val resourcesRelativePath get() = workspaceConfig.resourcesPath!!
    val resourcesAbsolutePath get() = Paths.get(workspaceFile.absolutePath, resourcesRelativePath).normalize()!!

    val excludedRelativePaths get() = workspaceConfig.excludedPaths
    val excludedAbsolutePaths get() = excludedRelativePaths.map {
        Paths.get(workspaceFile.absolutePath, it).normalize()!!
    }

    val excludedFileExtensions get() = workspaceConfig.excludedFileExtensions

    // TODO: Excluding ignored paths
    val sourceFiles get() = SysUtil.filesUnder(sourcesAbsolutePath.toFile()) { file ->
        file.name.endsWith(".java") && excludedAbsolutePaths.stream().noneMatch {
            file.startsWith(it.toFile().absolutePath)
        }
    }

    val resourceFiles get() = SysUtil.filesUnder(resourcesAbsolutePath.toFile()) { file ->
        file.name.run {
            !endsWith(".java") && !endsWith(".class") && this != "eocvsim_workspace.json"
        } && excludedAbsolutePaths.stream().noneMatch {
            file.startsWith(it.toFile().absolutePath)
        } && excludedFileExtensions.stream().noneMatch {
            file.name.endsWith(".$it")
        }
    }

    val onWorkspaceChange = EventHandler("WorkspaceManager-OnChange")

    lateinit var fileWatcher: FileWatcher
        private set

    fun stopFileWatcher() {
        if(::fileWatcher.isInitialized) {
            fileWatcher.stop()
        }
    }

    fun createWorkspaceWithTemplate(folder: File, template: WorkspaceTemplate): Boolean {
        if(!folder.isDirectory) return false
        if(!template.extractToIfEmpty(folder)) return false

        workspaceFile = folder
        return true
    }

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

    fun init() {
        onWorkspaceChange {
            fileWatcher.onChange {
                eocvSim.pipelineManager.compiledPipelineManager.asyncCompile()
            }
        }

        val file = eocvSim.params.initialWorkspace ?: File(eocvSim.config.workspacePath)

        workspaceFile = if(file.exists())
            file
        else
            CompiledPipelineManager.DEF_WORKSPACE_FOLDER
    }

    fun saveCurrentConfig() {
        ::workspaceConfig.set(workspaceConfig)
    }

    fun reloadConfig(): WorkspaceConfig {
        cachedWorkspConfig = null
        return workspaceConfig
    }

}
