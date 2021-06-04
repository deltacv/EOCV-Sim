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

package com.github.serivesmejia.eocvsim

import com.github.serivesmejia.eocvsim.config.Config
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.dialog.FileAlreadyExists
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.output.VideoRecordingSession
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import com.github.serivesmejia.eocvsim.util.FileFilters
import com.github.serivesmejia.eocvsim.util.Log
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.exception.MaxActiveContextsException
import com.github.serivesmejia.eocvsim.util.exception.handling.EOCVSimUncaughtExceptionHandler
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.fps.FpsLimiter
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import com.github.serivesmejia.eocvsim.workspace.util.VSCodeLauncher
import nu.pattern.OpenCV
import org.opencv.core.Size
import java.awt.Dimension
import java.io.File
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess

class EOCVSim(val params: Parameters = Parameters()) {

    companion object {
        const val VERSION = Build.versionString
        const val DEFAULT_EOCV_WIDTH = 320
        const val DEFAULT_EOCV_HEIGHT = 240
        @JvmField
        val DEFAULT_EOCV_SIZE = Size(DEFAULT_EOCV_WIDTH.toDouble(), DEFAULT_EOCV_HEIGHT.toDouble())

        private const val TAG = "EOCVSim"

        private var isNativeLibLoaded = false

        fun loadOpenCvLib() {
            if (isNativeLibLoaded) return

            Log.info(TAG, "Loading native lib...")

            try {
                OpenCV.loadLocally()
                Log.info(TAG, "Successfully loaded native lib")
            } catch (ex: Throwable) {
                Log.error(TAG, "Failure loading native lib", ex)
                Log.info(TAG, "Retrying with old method...")

                if (!SysUtil.loadCvNativeLib()) exitProcess(-1)
            }

            isNativeLibLoaded = true
        }
    }

    @JvmField
    val onMainUpdate = EventHandler("OnMainUpdate")

    @JvmField
    val visualizer = Visualizer(this)

    @JvmField
    val configManager = ConfigManager()
    @JvmField
    val inputSourceManager = InputSourceManager(this)
    @JvmField
    val pipelineManager = PipelineManager(this)
    @JvmField
    val tunerManager = TunerManager(this)

    @JvmField
    val workspaceManager = WorkspaceManager(this)

    val config: Config
        get() = configManager.config

    var currentRecordingSession: VideoRecordingSession? = null

    val fpsLimiter = FpsLimiter(30.0)

    lateinit var eocvSimThread: Thread
        private set

    private val hexCode = Integer.toHexString(hashCode())

    enum class DestroyReason {
        USER_REQUESTED, THEME_CHANGING, RESTART, CRASH
    }

    fun init() {
        eocvSimThread = Thread.currentThread()

        Log.info(TAG, "Initializing EasyOpenCV Simulator v$VERSION ($hexCode)")
        Log.blank()

        EOCVSimUncaughtExceptionHandler.register()

        //loading native lib only once in the app runtime
        loadOpenCvLib()
        Log.blank()

        configManager.init() //load config

        workspaceManager.init()

        visualizer.initAsync(configManager.config.simTheme) //create gui in the EDT

        inputSourceManager.init() //loading user created input sources
        pipelineManager.init() //init pipeline manager (scan for pipelines)
        tunerManager.init() //init tunable variables manager

        //shows a warning when a pipeline gets "stuck"
        pipelineManager.onPipelineTimeout {
            visualizer.asyncPleaseWaitDialog(
                "Current pipeline took too long to ${pipelineManager.lastPipelineAction}",
                "Falling back to DefaultPipeline",
                "Close", Dimension(310, 150), true, true
            )
        }

        inputSourceManager.inputSourceLoader.saveInputSourcesToFile()

        visualizer.waitForFinishingInit()

        visualizer.sourceSelectorPanel.updateSourcesList() //update sources and pick first one
        visualizer.sourceSelectorPanel.sourceSelector.selectedIndex = 0
        visualizer.pipelineSelectorPanel.updatePipelinesList() //update pipelines and pick first one (DefaultPipeline)
        visualizer.pipelineSelectorPanel.selectedIndex = 0

        //post output mats from the pipeline to the visualizer viewport
        pipelineManager.pipelineOutputPosters.add(visualizer.viewport.matPoster)

        start()
    }

    private fun start() {
        Log.info(TAG, "Begin EOCVSim loop")
        Log.blank()

        while (!eocvSimThread.isInterrupted) {
            //run all pending requested runnables
            onMainUpdate.run()

            updateVisualizerTitle()

            inputSourceManager.update(pipelineManager.paused)
            tunerManager.update()

            try {
                pipelineManager.update(inputSourceManager.lastMatFromSource)
            } catch (ex: MaxActiveContextsException) { //handles when a lot of pipelines are stuck in the background
                visualizer.asyncPleaseWaitDialog(
                    "There are many pipelines stuck in processFrame running in the background",
                    "To avoid further issues, EOCV-Sim will exit now.",
                    "Ok",
                    Dimension(450, 150),
                    true, true
                ).onCancel {
                    destroy(DestroyReason.CRASH) //destroy eocv sim when pressing "exit"
                }

                //print exception
                Log.error(
                    TAG,
                    "Please note that the following exception is likely to be caused by one or more of the user pipelines",
                    ex
                )

                //block the current thread until the user closes the dialog
                try {
                    //using sleep for avoiding burning cpu cycles
                    Thread.sleep(Long.MAX_VALUE)
                } catch (ignored: InterruptedException) {
                    //reinterrupt once user closes the dialog
                    Thread.currentThread().interrupt()
                }

                break //bye bye
            }

            //updating displayed telemetry
            visualizer.telemetryPanel.updateTelemetry(pipelineManager.currentTelemetry)

            //limit FPS
            fpsLimiter.maxFPS = configManager.config.maxFps.toDouble()
            fpsLimiter.sync()
        }

        Log.warn(TAG, "Main thread interrupted (" + Integer.toHexString(hashCode()) + ")")
    }

    fun destroy(reason: DestroyReason) {
        Log.warn(TAG, "Destroying current EOCVSim ($hexCode) due to $reason")

        //stop recording session if there's currently an ongoing one
        currentRecordingSession?.stopRecordingSession()
        currentRecordingSession?.discardVideo()

        Log.info(TAG, "Trying to save config file...")

        inputSourceManager.currentInputSource?.close()
        workspaceManager.stopFileWatcher()
        configManager.saveToFile()
        visualizer.close()

        eocvSimThread.interrupt()
    }

    fun destroy() {
        destroy(DestroyReason.USER_REQUESTED)
    }

    fun restart() {
        Log.info(TAG, "Restarting...")

        pipelineManager.captureStaticSnapshot()

        Log.blank()
        destroy(DestroyReason.RESTART)
        Log.blank()

        Thread(
            { EOCVSim().init() },
            "main"
        ).start() //run next instance on a separate thread for the old one to get interrupted and ended
    }

    fun startRecordingSession() {
        if (currentRecordingSession == null) {
            currentRecordingSession = VideoRecordingSession(fpsLimiter.maxFPS, configManager.config.videoRecordingSize)
            currentRecordingSession!!.startRecordingSession()

            Log.info(TAG, "Recording session started")

            pipelineManager.pipelineOutputPosters.add(currentRecordingSession!!.matPoster)
        }
    }

    //stopping recording session and saving file
    fun stopRecordingSession() {
        currentRecordingSession?.let { itVideo ->

            visualizer.pipelineSelectorPanel.buttonsPanel.pipelineRecordBtt.isEnabled = false

            itVideo.stopRecordingSession()
            pipelineManager.pipelineOutputPosters.remove(itVideo.matPoster)

            Log.info(TAG, "Recording session stopped")

            DialogFactory.createFileChooser(
                visualizer.frame,
                DialogFactory.FileChooser.Mode.SAVE_FILE_SELECT, FileFilters.recordedVideoFilter
            ).addCloseListener { _: Int, file: File?, selectedFileFilter: FileFilter? ->
                onMainUpdate.doOnce {
                    if (file != null) {

                        var correctedFile = File(file.absolutePath)
                        val extension = SysUtil.getExtensionByStringHandling(file.name)

                        if (selectedFileFilter is FileNameExtensionFilter) { //if user selected an extension
                            //get selected extension
                            correctedFile = file + "." + selectedFileFilter.extensions[0]
                        } else if (extension.isPresent) {
                            if (!extension.get().equals("avi", true)) {
                                correctedFile = file + ".avi"
                            }
                        } else {
                            correctedFile = file + ".avi"
                        }

                        if (correctedFile.exists()) {
                            SwingUtilities.invokeLater {
                                if (DialogFactory.createFileAlreadyExistsDialog(this) == FileAlreadyExists.UserChoice.REPLACE) {
                                    onMainUpdate.doOnce { itVideo.saveTo(correctedFile) }
                                }
                            }
                        } else {
                            itVideo.saveTo(correctedFile)
                        }
                    } else {
                        itVideo.discardVideo()
                    }

                    currentRecordingSession = null
                    visualizer.pipelineSelectorPanel.buttonsPanel.pipelineRecordBtt.isEnabled = true
                }
            }
        }
    }

    fun isCurrentlyRecording() = currentRecordingSession?.isRecording ?: false

    private fun updateVisualizerTitle() {
        val isBuildRunning = if (pipelineManager.compiledPipelineManager.isBuildRunning) "(Building)" else ""

        val workspaceMsg = " - ${config.workspacePath} $isBuildRunning"

        val pipelineFpsMsg = " (${pipelineManager.pipelineFpsCounter.fps} Pipeline FPS)"
        val posterFpsMsg = " (${visualizer.viewport.matPoster.fpsCounter.fps} Poster FPS)"
        val isPaused = if (pipelineManager.paused) " (Paused)" else ""
        val isRecording = if (isCurrentlyRecording()) " RECORDING" else ""

        val msg = isRecording + pipelineFpsMsg + posterFpsMsg + isPaused

        if (pipelineManager.currentPipeline == null) {
            visualizer.setTitleMessage("No pipeline$msg${workspaceMsg}")
        } else {
            visualizer.setTitleMessage("${pipelineManager.currentPipelineName}$msg${workspaceMsg}")
        }
    }

    class Parameters {
        var scanForPipelinesIn = "org.firstinspires"
        var scanForTunableFieldsIn = "com.github.serivesmejia"
    }

}
