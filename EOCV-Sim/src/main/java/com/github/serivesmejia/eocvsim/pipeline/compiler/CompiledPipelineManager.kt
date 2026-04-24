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

package com.github.serivesmejia.eocvsim.pipeline.compiler

import com.github.serivesmejia.eocvsim.Build
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.dialog.Output
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.StrUtil
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.common.util.loggerForThis
import com.github.serivesmejia.eocvsim.workspace.config.WorkspaceConfigLoader
import com.github.serivesmejia.eocvsim.workspace.util.template.DefaultWorkspaceTemplate
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.*
import java.io.File

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.util.event.Orchestrable
import com.github.serivesmejia.eocvsim.util.event.Orchestrator
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import org.koin.core.qualifier.named

class CompiledPipelineManager : Orchestrable, KoinComponent {

    private val initOrchestrator: Orchestrator by inject(named("init"))
    private val onMainLoop: EventHandler by inject(named("onMainLoop"))
    private val scope: CoroutineScope by inject()

    private val pipelineManager: PipelineManager by inject()
    val workspaceManager: WorkspaceManager by inject()
    private val visualizer: Visualizer by inject()
    private val dialogFactory: DialogFactory by inject()

    companion object {
        val logger by loggerForThis()

        val DEF_WORKSPACE_FOLDER  = File(SysUtil.getEOCVSimFolder(), File.separator + "default_workspace").apply {
            if(!exists()) {
                mkdir()
                DefaultWorkspaceTemplate.extractToIfEmpty(this)
            } else {
                val loader = WorkspaceConfigLoader(this)
                val config = loader.loadWorkspaceConfig()

                if(config?.eocvSimVersion != Build.standardVersionString) {
                    logger.info("Replacing old default workspace with latest one (version mismatch)")
                    SysUtil.deleteFilesUnder(this)
                    DefaultWorkspaceTemplate.extractTo(this)
                }
            }
        }

        val COMPILER_FOLDER       = File(SysUtil.getEOCVSimFolder(), File.separator + "compiler").mkdirLazy()

        val SOURCES_OUTPUT_FOLDER = File(COMPILER_FOLDER, File.separator + "gen_src").mkdirLazy()
        val CLASSES_OUTPUT_FOLDER = File(COMPILER_FOLDER, File.separator + "out_classes").mkdirLazy()
        val JARS_OUTPUT_FOLDER    = File(COMPILER_FOLDER, File.separator + "out_jars").mkdirLazy()

        val PIPELINES_OUTPUT_JAR  = File(JARS_OUTPUT_FOLDER, File.separator + "pipelines.jar")
    }

    val logger by loggerForThis()

    var currentPipelineClassLoader: PipelineClassLoader? = null
        private set

    val onBuildStart = EventHandler("CompiledPipelineManager-OnBuildStart")
    val onBuildEnd   = EventHandler("CompiledPipelineManager-OnBuildEnd")

    var lastBuildResult: PipelineCompileResult? = null
        private set
    var lastBuildOutputMessage: String? = null
        private set

    var isBuildRunning = false
        private set

    init {
        initOrchestrator.register(this) {
            target { it.init() }
            dependsOn(workspaceManager)
        }
    }

    private fun init() {
        logger.info("Initializing...")

        onBuildStart {
            onMainLoop.once {
                visualizer.menuBar.workspCompile.isEnabled = false
                visualizer.pipelineSelectorPanel.buttonsPanel.pipelineCompileBtt.isEnabled = false
            }
        }

        onBuildEnd {
            onMainLoop.once {
                visualizer.menuBar.workspCompile.isEnabled = true
                visualizer.pipelineSelectorPanel.buttonsPanel.pipelineCompileBtt.isEnabled = true
            }
        }

        asyncBuild()

        workspaceManager.onWorkspaceChange {
            asyncBuild()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun uncheckedBuild(): PipelineCompileResult {
        if(isBuildRunning) return PipelineCompileResult(
            PipelineCompileStatus.FAILED, "A build is already running"
        )

        isBuildRunning = true
        onBuildStart.run()

        if(!PipelineCompiler.IS_USABLE) {
            lastBuildResult = PipelineCompileResult(
                PipelineCompileStatus.FAILED,
                "Current JVM does not have a javac executable (a JDK is needed)"
            )
            lastBuildOutputMessage = null

            onBuildEnd.run()
            isBuildRunning = false

            return lastBuildResult!!
        }

        workspaceManager.reloadConfig()

        val absoluteSourcesPath = workspaceManager.sourcesAbsolutePath.toFile()
        logger.info("Building java files in workspace, at ${absoluteSourcesPath.absolutePath}")

        val runtime = ElapsedTime()

        val compiler = PipelineCompiler(
            absoluteSourcesPath, workspaceManager.sourceFiles,
            workspaceManager.resourcesAbsolutePath.toFile(), workspaceManager.resourceFiles
        )
        
        val result = compiler.compile(PIPELINES_OUTPUT_JAR)
        lastBuildResult = result

        val timeElapsed = String.format("%.2f", runtime.seconds())

        currentPipelineClassLoader = null
        val messageEnd = "(took $timeElapsed seconds)\n\n${result.message}".trim()

        pipelineManager.requestRemoveAllPipelinesFrom(
            PipelineSource.COMPILED_ON_RUNTIME,
            refreshGuiPipelineList = false,
            changeToDefaultIfRemoved = false
        )

        lastBuildOutputMessage = when(result.status) {
            PipelineCompileStatus.SUCCESS -> {
                loadFromPipelinesJar()
                "Build successful $messageEnd"
            }
            PipelineCompileStatus.NO_SOURCE -> {
                //delete jar if we had no sources, the most logical outcome in this case
                deleteJarFile()
                pipelineManager.onPipelineListRefresh.run()

                "Build cancelled, no source files to compile $messageEnd"
            }
            else -> {
                deleteJarFile()
                "Build failed $messageEnd"
            }
        }

        pipelineManager.onUpdate.once {
            pipelineManager.onPipelineListRefresh.run()
            pipelineManager.reloadPipelineByName()
        }

        if(result.status == PipelineCompileStatus.SUCCESS) {
            logger.info("$lastBuildOutputMessage\n")
        } else {
            logger.warn("$lastBuildOutputMessage\n")

            if(result.status == PipelineCompileStatus.FAILED && !Output.isAlreadyOpened)
                withContext(Dispatchers.Main) {
                    dialogFactory.createBuildOutput()
                }
        }

        onBuildEnd.callRightAway = EventHandler.CallRightAway.InPlace
        onBuildEnd.run()

        scope.launch {
            delay(1000)
            onBuildEnd.callRightAway = EventHandler.CallRightAway.Disabled
        }

        isBuildRunning = false

        return result
    }

    fun build() = try {
        runBlocking { uncheckedBuild() }
    } catch(e: Throwable) {
        isBuildRunning = false
        onBuildEnd.run()

        val stacktrace = StrUtil.fromException(e)
        lastBuildOutputMessage = """
            |Unexpected exception thrown while the build was running
            |
            |$stacktrace
            |   
            |If this seems like a bug, please open an issue in the EOCV-Sim github repo
        """.trimMargin()

        logger.error(lastBuildOutputMessage)

        lastBuildResult = PipelineCompileResult(PipelineCompileStatus.FAILED, lastBuildOutputMessage!!)

        if(!Output.isAlreadyOpened)
            dialogFactory.createBuildOutput()

        lastBuildResult!!
    }

    @JvmOverloads
    @OptIn(DelicateCoroutinesApi::class)
    fun asyncBuild(
        endCallback: (PipelineCompileResult) -> Unit = {}
    ) = scope.launch(Dispatchers.IO) {
        if(PipelineCompiler.IS_USABLE) {
            endCallback(build())
        } else {
            onMainLoop.once {
                visualizer.compilerUnsupported()
            }
        }
    }

    val isCompilerSupported get() = PipelineCompiler.IS_USABLE


    private fun deleteJarFile() {
        if(PIPELINES_OUTPUT_JAR.exists()) PIPELINES_OUTPUT_JAR.delete()
        currentPipelineClassLoader = null
    }

    fun loadFromPipelinesJar() {
        if(!PIPELINES_OUTPUT_JAR.exists()) return

        logger.trace("Looking for pipelines in jar file {}", PIPELINES_OUTPUT_JAR)

        try {
            currentPipelineClassLoader = PipelineClassLoader(PIPELINES_OUTPUT_JAR)

            val pipelines = mutableListOf<Class<*>>()

            for(pipelineClass in currentPipelineClassLoader!!.pipelineClasses) {
                pipelines.add(pipelineClass)
                logger.trace("Added ${pipelineClass.simpleName} from jar")
            }

            pipelineManager.requestAddPipelineClasses(pipelines, PipelineSource.COMPILED_ON_RUNTIME, refreshGui = false)
        } catch(e: Exception) {
            logger.error("Uncaught exception thrown while loading jar $PIPELINES_OUTPUT_JAR", e)
        }
    }

}

private fun File.mkdirLazy() = apply { mkdir() }
