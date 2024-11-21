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

import com.github.serivesmejia.eocvsim.EOCVSim.Parameters
import com.github.serivesmejia.eocvsim.config.Config
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.dialog.FileAlreadyExists
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.output.VideoRecordingSession
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import com.github.serivesmejia.eocvsim.util.ClasspathScan
import com.github.serivesmejia.eocvsim.util.FileFilters
import com.github.serivesmejia.eocvsim.util.JavaProcess
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.exception.handling.CrashReport
import com.github.serivesmejia.eocvsim.util.exception.handling.EOCVSimUncaughtExceptionHandler
import com.github.serivesmejia.eocvsim.util.fps.FpsLimiter
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import com.github.serivesmejia.eocvsim.util.loggerFor
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModePipelineHandler
import io.github.deltacv.common.pipeline.util.PipelineStatisticsCalculator
import io.github.deltacv.common.util.ParsedVersion
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import io.github.deltacv.vision.external.PipelineRenderHook
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.core.Size
import org.openftc.easyopencv.TimestampedPipelineHandler
import java.awt.Dimension
import java.io.File
import java.lang.Thread.sleep
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.jvm.Throws
import kotlin.system.exitProcess

/**
 * Main class of the EasyOpenCV Simulator
 * This class is the entry point of the program
 * and is responsible for initializing all the
 * components of the simulator.
 * @param params the parameters to initialize the simulator with
 * @see Parameters
 */
class EOCVSim(val params: Parameters = Parameters()) {

    companion object {
        const val VERSION = Build.versionString
        const val STANDARD_VERSION = Build.standardVersionString

        val PARSED_VERSION = ParsedVersion(STANDARD_VERSION)

        const val DEFAULT_EOCV_WIDTH = 320
        const val DEFAULT_EOCV_HEIGHT = 240

        @JvmField
        val DEFAULT_EOCV_SIZE = Size(DEFAULT_EOCV_WIDTH.toDouble(), DEFAULT_EOCV_HEIGHT.toDouble())

        private var hasScanned = false
        private val classpathScan = ClasspathScan()

        val logger by loggerFor(EOCVSim::class)

        init {
            EOCVSimFolder // mkdir needed folders
        }

        private var isNativeLibLoaded = false

        /**
         * Load the OpenCV native library
         * @param alternativeNative the alternative native library file to load instead of the packaged one
         */
        fun loadOpenCvLib(alternativeNative: File? = null) {
            if (isNativeLibLoaded) return

            if (alternativeNative != null) {
                logger.info("Loading native lib from ${alternativeNative.absolutePath}...")

                try {
                    System.load(alternativeNative.absolutePath)

                    Mat().release() //test if native lib is loaded correctly

                    isNativeLibLoaded = true
                    logger.info("Successfully loaded the OpenCV native lib from specified path")

                    return
                } catch (ex: Throwable) {
                    logger.error("Failure loading the OpenCV native lib from specified path", ex)
                    logger.info("Retrying with loadLocally...")
                }
            }

            try {
                OpenCV.loadLocally()
                logger.info("Successfully loaded the OpenCV native lib")
            } catch (ex: Throwable) {
                logger.error("Failure loading the OpenCV native lib", ex)
                logger.error("The sim will exit now as it's impossible to continue execution without OpenCV")

                CrashReport(ex).saveCrashReport()

                exitProcess(-1)
            }

            isNativeLibLoaded = true
        }
    }

    /**
     * Event handler for the main update loop
     * This event handler is called every frame
     * and is used to run all the pending runnables
     * posted by the different components of the simulator
     * @see EventHandler
     */
    @JvmField val onMainUpdate = EventHandler("OnMainUpdate")

    /**
     * The visualizer instance in charge of managing the GUI
     * and the viewport where the pipeline output is shown
     * @see Visualizer
     */
    @JvmField val visualizer = Visualizer(this)

    /**
     * The configuration manager instance in charge of managing
     * the configuration of the simulator from the config json file
     */
    @JvmField val configManager = ConfigManager()

    /**
     * The input source manager instance in charge of managing input sources
     * and their loading, as well as the current input source.
     * Input Sources are the sources of the frames that the pipeline processes
     * they can be from a webcam, a video or image file, etc.
     * @see InputSourceManager
     */
    @JvmField val inputSourceManager = InputSourceManager(this)

    /**
     * The pipeline statistics calculator instance in charge of
     * calculating the average FPS, pipeline time and overhead time
     * of the current pipeline
     * @see PipelineStatisticsCalculator
     */
    @JvmField val pipelineStatisticsCalculator = PipelineStatisticsCalculator()

    /**
     * The pipeline manager instance in charge of managing pipelines
     * and their execution, as well as making sure the pipeline
     * does not take too long to process a frame
     */
    @JvmField val pipelineManager = PipelineManager(this, pipelineStatisticsCalculator)

    /**
     * The tuner manager instance in charge of managing pipeline and processor
     * tunable variables and their values that can be changed in runtime
     */
    @JvmField val tunerManager = TunerManager(this)

    /**
     * The workspace manager instance in charge of managing user workspaces
     */
    @JvmField val workspaceManager = WorkspaceManager(this)

    /**
     * The current configuration of the simulator
     * Loaded from the config json file
     * @see Config
     */
    val config: Config get() = configManager.config

    /**
     * The classpath scanner instance containing all the scanned classes
     * From OpenCvPipeline, VisionProcessor, TunableField and OpMode classes
     * @see ClasspathScan
     */
    val classpathScan get() = Companion.classpathScan

    /**
     * The current recording session of the simulator
     * This allows the user to record the output of the pipeline
     * and save it to a file
     * @see VideoRecordingSession
     */
    var currentRecordingSession: VideoRecordingSession? = null

    /**
     * Utility in charge of limiting the FPS of the simulator
     * @see FpsLimiter
     */
    val fpsLimiter = FpsLimiter(30.0)

    /**
     * Manager in charge of loading and managing plugins
     * @see PluginManager
     */
    val pluginManager = PluginManager(this)

    lateinit var eocvSimThread: Thread
        private set

    private val hexCode = Integer.toHexString(hashCode())

    private var isRestarting = false
    private var destroying = false

    /**
     * The reason why the simulator was destroyed
     * to handle different actions when flushing
     * the simulator away.
     * @see destroy
     */
    enum class DestroyReason {
        USER_REQUESTED, THREAD_EXIT, RESTART, CRASH
    }

    /**
     * Initializes the simulator
     * This method is called to initialize all the components
     * of the simulator and start the main loop
     * @see start
     */
    fun init() {
        eocvSimThread = Thread.currentThread()

        if (!EOCVSimFolder.couldLock) {
            logger.error(
                "Couldn't finally claim lock file in \"${EOCVSimFolder.absolutePath}\"! " + "Is the folder opened by another EOCV-Sim instance?"
            )

            logger.error("Unable to continue with the execution, the sim will exit now.")
            exitProcess(-1)
        } else {
            logger.info("Confirmed claiming of the lock file in ${EOCVSimFolder.absolutePath}")
        }

        DialogFactory.createSplashScreen(visualizer.onInitFinished)

        logger.info("-- Initializing EasyOpenCV Simulator v$VERSION ($hexCode) --")

        EOCVSimUncaughtExceptionHandler.register()

        //loading native lib only once in the app runtime
        loadOpenCvLib(params.opencvNativeLibrary)

        if (!hasScanned) {
            classpathScan.asyncScan()
            hasScanned = true
        }

        configManager.init()

        configManager.config.simTheme.install()

        pluginManager.init() // woah
        pluginManager.loadPlugins()

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
                "Close",
                Dimension(310, 150),
                true,
                true
            )
        }

        inputSourceManager.inputSourceLoader.saveInputSourcesToFile()

        visualizer.joinInit()

        pipelineManager.subscribePipelineHandler(TimestampedPipelineHandler())
        pipelineManager.subscribePipelineHandler(OpModePipelineHandler(inputSourceManager, visualizer.viewport))

        visualizer.sourceSelectorPanel.updateSourcesList() //update sources and pick first one
        visualizer.sourceSelectorPanel.sourceSelector.selectedIndex = 0
        visualizer.sourceSelectorPanel.allowSourceSwitching = true

        visualizer.pipelineOpModeSwitchablePanel.updateSelectorListsBlocking()

        visualizer.pipelineSelectorPanel.selectedIndex = 0 //update pipelines and pick first one (DefaultPipeline)
        visualizer.opModeSelectorPanel.selectedIndex = 0 //update opmodes and pick first one (DefaultPipeline)

        visualizer.pipelineOpModeSwitchablePanel.enableSwitchingBlocking()

        //post output mats from the pipeline to the visualizer viewport
        pipelineManager.pipelineOutputPosters.add(visualizer.viewport)

        // now that we have two different runnable units (OpenCvPipeline and OpMode)
        // we have to give a more special treatment to the OpenCvPipeline
        // OpModes can take care of themselves, setting up their own stuff
        // but we need to do some hand holding for OpenCvPipelines...
        pipelineManager.onPipelineChange {
            pipelineStatisticsCalculator.init()

            if(pipelineManager.currentPipeline !is OpMode && pipelineManager.currentPipeline != null) {
                visualizer.viewport.activate()
                visualizer.viewport.setRenderHook(PipelineRenderHook) // calls OpenCvPipeline#onDrawFrame on the viewport (UI) thread
            } else {
                // opmodes are on their own, lol
                visualizer.viewport.deactivate()
                visualizer.viewport.clearViewport()
            }
        }

        pipelineManager.onUpdate {
            if(pipelineManager.currentPipeline !is OpMode && pipelineManager.currentPipeline != null) {
                visualizer.viewport.notifyStatistics(
                        pipelineStatisticsCalculator.avgFps,
                        pipelineStatisticsCalculator.avgPipelineTime,
                        pipelineStatisticsCalculator.avgOverheadTime
                )
            }

            updateVisualizerTitle() // update current pipeline in title
        }

        pluginManager.enablePlugins()

        try {
            start()
        } catch (e: InterruptedException) {
            logger.warn("Main thread interrupted ($hexCode)", e)
        }

        if(!destroying) {
            destroy(DestroyReason.THREAD_EXIT)
        }

        if (isRestarting) {
            Thread.interrupted() //clear interrupted flag
            EOCVSimFolder.lock?.lock?.close()

            JavaProcess.killSubprocessesOnExit = false
            Thread {
                JavaProcess.exec(Main::class.java, null, null)
            }.start()

            sleep(1000)
        }

        logger.info("-- End of EasyOpenCV Simulator v$VERSION ($hexCode) --")
        exitProcess(0)
    }

    /**
     * Starts the main loop of the simulator
     * This method is called after all the components
     * of the simulator have been initialized
     * and is responsible for running the main loop
     * of the simulator where the static components
     * such as the GUI, the pipelines, the input sources,
     * and the different manages declared at the top level
     * are explicitly updated within this method
     * @see init
     */
    @Throws(InterruptedException::class)
    private fun start() {
        if(Thread.currentThread() != eocvSimThread) {
            throw IllegalStateException("start() must be called from the EOCVSim thread")
        }

        logger.info("-- Begin EOCVSim loop ($hexCode) --")

        if(!config.flags.contains("showIAmA") || config.flags["showIAmA"] == true) {
            DialogFactory.createIAmA(visualizer)
        }

        while (!eocvSimThread.isInterrupted && !destroying) {
            //run all pending requested runnables
            onMainUpdate.run()

            pipelineStatisticsCalculator.newInputFrameStart()

            inputSourceManager.update(pipelineManager.paused)
            tunerManager.update()

            pipelineManager.update(
                if (inputSourceManager.lastMatFromSource != null && !inputSourceManager.lastMatFromSource.empty()) {
                    inputSourceManager.lastMatFromSource
                } else null
            )

            //limit FPS
            fpsLimiter.maxFPS = config.pipelineMaxFps.fps.toDouble()
            try {
                fpsLimiter.sync()
            } catch (e: InterruptedException) {
                break
            }
        }

        logger.warn("Main thread interrupted ($hexCode)")
    }

    /**
     * Destroys the simulator
     * @param reason the reason why the simulator is being destroyed, it mainly allows to restart the simulator if requested
     */
    fun destroy(reason: DestroyReason) {
        logger.warn("-- Destroying current EOCVSim ($hexCode) due to $reason, it is normal to see InterruptedExceptions and other kinds of stack traces below --")

        pluginManager.disablePlugins()

        //stop recording session if there's currently an ongoing one
        currentRecordingSession?.stopRecordingSession()
        currentRecordingSession?.discardVideo()

        logger.info("Trying to save config file...")

        inputSourceManager.currentInputSource?.close()
        workspaceManager.stopFileWatcher()
        configManager.saveToFile()
        visualizer.close()

        destroying = true

        if(reason == DestroyReason.THREAD_EXIT) {
            exitProcess(0)
        } else {
            eocvSimThread.interrupt()
        }

        if (reason == DestroyReason.USER_REQUESTED || reason == DestroyReason.CRASH) jvmMainThread.interrupt()
    }

    /**
     * Destroys the simulator with the reason being USER_REQUESTED
     */
    fun destroy() {
        destroy(DestroyReason.USER_REQUESTED)
    }

    /**
     * Destroys the current simulator with the reason being RESTART.
     * This method is used to restart the simulator.
     */
    fun restart() {
        logger.info("Restarting...")

        pipelineManager.captureStaticSnapshot()

        isRestarting = true
        destroy(DestroyReason.RESTART)
    }

    /**
     * Starts a recording session to record the output of the pipeline
     * to a video file in real-time which will be prompted to the user
     * to save it to a file after stopping the recording session.
     */
    fun startRecordingSession() {
        if (currentRecordingSession == null) {
            currentRecordingSession = VideoRecordingSession(
                config.videoRecordingFps.fps.toDouble(), config.videoRecordingSize
            )

            currentRecordingSession!!.startRecordingSession()

            logger.info("Recording session started")

            pipelineManager.pipelineOutputPosters.add(currentRecordingSession!!.matPoster)
        }
    }

    /**
     * Stops the current recording session and prompts the user to save the recorded video
     */
    fun stopRecordingSession() {
        currentRecordingSession?.let { itVideo ->

            visualizer.pipelineSelectorPanel.buttonsPanel.pipelineRecordBtt.isEnabled = false

            itVideo.stopRecordingSession()
            pipelineManager.pipelineOutputPosters.remove(itVideo.matPoster)

            logger.info("Recording session stopped")

            DialogFactory.createFileChooser(
                visualizer.frame, DialogFactory.FileChooser.Mode.SAVE_FILE_SELECT, FileFilters.recordedVideoFilter
            ).addCloseListener { _: Int, file: File?, selectedFileFilter: FileFilter? ->
                onMainUpdate.doOnce {
                    if (file != null) {
                        var correctedFile = file
                        val extension = SysUtil.getExtensionByStringHandling(file.name)

                        if (selectedFileFilter is FileNameExtensionFilter) { //if user selected an extension
                            //get selected extension
                            correctedFile = File(file.absolutePath + "." + selectedFileFilter.extensions[0])
                        } else if (extension.isPresent) {
                            if (!extension.get().equals("avi", true)) {
                                correctedFile = File(file.absolutePath + ".avi")
                            }
                        } else {
                            correctedFile = File(file.absolutePath + ".avi")
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

    /**
     * Checks if the simulator is currently recording
     * @return true if the simulator is currently recording, false otherwise
     */
    fun isCurrentlyRecording() = currentRecordingSession?.isRecording == true

    /**
     * Updates the visualizer title message
     * with different information such as the current pipeline
     * the workspace file, if the pipeline is paused, if the pipeline
     * is currently building, and if the simulator is currently recording
     */
    private fun updateVisualizerTitle() {
        val isBuildRunning = if (pipelineManager.compiledPipelineManager.isBuildRunning) "(Building)" else ""

        val workspaceMsg = " - ${workspaceManager.workspaceFile.absolutePath} $isBuildRunning"

        val isPaused = if (pipelineManager.paused) " (Paused)" else ""
        val isRecording = if (isCurrentlyRecording()) " RECORDING" else ""

        val msg = isRecording + isPaused

        if (pipelineManager.currentPipeline == null) {
            visualizer.setTitleMessage("No pipeline$msg${workspaceMsg}")
        } else {
            visualizer.setTitleMessage("${pipelineManager.currentPipelineName}$msg${workspaceMsg}")
        }
    }

    /**
     * Parameters class to initialize the simulator with
     * @see EOCVSim
     */
    class Parameters {
        /**
         * The initial user workspace file to load
         * Overrides the workspace file in the config file
         */
        var initialWorkspace: File? = null

        /**
         * The initial pipeline name to load
         * specified by class name
         */
        var initialPipelineName: String? = null

        /**
         * Whether the specified pipeline must be searched in the CLASSPATH or from the workspace
         */
        var initialPipelineSource: PipelineSource? = null

        /**
         * An alternative path for the OpenCV native library to be loaded at runtime
         */
        var opencvNativeLibrary: File? = null
    }

}