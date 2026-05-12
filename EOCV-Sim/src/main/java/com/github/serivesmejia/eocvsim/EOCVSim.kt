/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim

import com.github.serivesmejia.eocvsim.config.Config
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.output.RecordingManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import com.github.serivesmejia.eocvsim.util.JavaProcess
import com.github.serivesmejia.eocvsim.util.LibraryLoader
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.exception.handling.CrashReport
import com.github.serivesmejia.eocvsim.util.exception.handling.EOCVSimUncaughtExceptionHandler
import com.github.serivesmejia.eocvsim.util.fps.FpsLimiter
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import com.github.serivesmejia.eocvsim.util.orchestration.Orchestrator
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import com.qualcomm.robotcore.eventloop.opmode.OpModePipelineHandler
import io.github.deltacv.common.pipeline.util.PipelineStatisticsCalculator
import io.github.deltacv.common.util.ParsedVersion
import io.github.deltacv.common.util.loggerFor
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.opencv.core.Size
import org.openftc.easyopencv.TimestampedPipelineHandler
import java.io.File
import java.lang.Thread.sleep
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 * Main class of the EasyOpenCV Simulator
 * This class is the entry point of the program
 * and is responsible for initializing all the
 * components of the simulator.
 * @see Parameters
 */
class EOCVSim : KoinComponent {

    companion object {
        const val VERSION = Build.versionString
        const val STANDARD_VERSION = Build.standardVersionString

        val PARSED_VERSION = ParsedVersion(STANDARD_VERSION)

        const val DEFAULT_EOCV_WIDTH = 320
        const val DEFAULT_EOCV_HEIGHT = 240

        @JvmField
        val DEFAULT_EOCV_SIZE = Size(DEFAULT_EOCV_WIDTH.toDouble(), DEFAULT_EOCV_HEIGHT.toDouble())

        val logger by loggerFor(EOCVSim::class)

        init {
            EOCVSimFolder.mkdir() // mkdir needed folders
        }
    }

    val orchestrator: Orchestrator by inject()

    /**
     * Event handler for the main update loop
     * This event handler is called every frame
     * and is used to run all the pending runnables
     * posted by the different components of the simulator
     * @see EventHandler
     */
    private val onMainLoop: EventHandler by inject(named("onMainLoop"))
    private val onCrash: EventHandler by inject(named("onCrash"))

    /**
     * The visualizer instance in charge of managing the GUI
     * and the viewport where the pipeline output is shown
     * @see Visualizer
     */
    val visualizer: Visualizer by inject()

    val configManager: ConfigManager by inject()
    val inputSourceManager: InputSourceManager by inject()
    val pluginManager: PluginManager by inject()

    val recordingManager: RecordingManager by inject()
    val dialogFactory: DialogFactory by inject()

    /**
     * The pipeline statistics calculator instance in charge of
     * calculating the average FPS, pipeline time and overhead time
     * of the current pipeline
     * @see PipelineStatisticsCalculator
     */
    val pipelineStatisticsCalculator: PipelineStatisticsCalculator by inject()

    /**
     * The pipeline manager instance in charge of managing pipelines
     * and their execution, as well as making sure the pipeline
     * does not take too long to process a frame
     */
    val pipelineManager: PipelineManager by inject()

    /**
     * The tuner manager instance in charge of managing pipeline and processor
     * tunable variables and their values that can be changed in runtime
     */
    val tunerManager: TunerManager by inject()

    /**
     * The workspace manager instance in charge of managing user workspaces
     */
    val workspaceManager: WorkspaceManager by inject()

    /**
     * The current configuration of the simulator
     * Loaded from the config json file
     * @see Config
     */
    val config: Config get() = configManager.config

    val lifecycleChannel: Channel<LifecycleSignal> by inject(named("lifecycle"))

    /**
     * Utility in charge of limiting the FPS of the simulator
     * @see FpsLimiter
     */
    val fpsLimiter = FpsLimiter(30.0)

    lateinit var eocvSimThread: Thread
        private set

    @JvmField val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val hexCode = Integer.toHexString(hashCode())

    private var isRestarting = false
    private var destroying = false

    /**
     * Initializes the simulator
     * This method is called to initialize all the components
     * of the simulator and start the main loop
     * @see mainLoop
     */
    fun start() {
        eocvSimThread = Thread.currentThread()

        if (!EOCVSimFolder.couldLock) {
            logger.error(
                "Couldn't finally claim lock file in \"${EOCVSimFolder.absolutePath}\"! " + "Is the folder opened by another EOCV-Sim instance?"
            )

            JOptionPane.showMessageDialog(
                null,
                "Another instance of EOCV-Sim is already running in this machine, please close it and try again.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )

            logger.error("Unable to continue with the execution, the sim will exit now.")
            exitProcess(-1)
        } else {
            logger.info("Confirmed claiming of the lock file in ${EOCVSimFolder.absolutePath}")
        }

        dialogFactory.createSplashScreen(visualizer.onInitFinished, onCrash)

        logger.info("-- Initializing EasyOpenCV Simulator v$VERSION ($hexCode) --")

        EOCVSimUncaughtExceptionHandler.register()

        //loading native lib only once in the app runtime
        val loadLibrariesResult = LibraryLoader.loadLibraries()
        if(!loadLibrariesResult.success) {
            logger.error("Exception in loadLibraries():", loadLibrariesResult.error)
            logger.error("The sim will exit now as it's impossible to continue without the required libraries")

            onCrash.run()

            runBlocking {
                launch(Dispatchers.Swing) {
                    val crashHeader = """
                        One or more of WPILib's native libraries failed to load at the earliest stage.
                        Ensure you're running EOCV-Sim on a supported platform, Java 25 is required.
                        Read the crash report below, this is not likely to be a bug on the program,
                        If it seems like a bug, please open an issue in GitHub with this report. 
                    """.trimIndent()

                    dialogFactory.instantiateCrashReport(
                        crash = "$crashHeader\n\n${CrashReport(loadLibrariesResult.error())}",
                        headerText = "An error occurred while loading the required native libraries for your platform"
                    )
                }
            }

            exitProcess(-1)
        }

        orchestrator.changePhase(Orchestrator.Phase.INIT)
        orchestrator.orchestrate()

        visualizer.onInitFinished {
            // SHOW WELCOME DIALOGS TO NEW USERS

            if(!config.flags.contains("hasShownIamA") || config.flags["hasShownIamA"] == false) {
                // Initial dialog to introduce our cool stuff to the user
                dialogFactory.createIAmA()
            } else if(!config.flags.contains("hasShownIamPaperVision") || config.flags["hasShownIamPaperVision"] == false) {
                // sometimes the users might miss the PaperVision dialog after the IAmA dialog
                // so we show it here if the user hasn't seen it yet
                dialogFactory.createIAmAPaperVision(false)
            } else if(config.flags["prefersPaperVision"] == true) {
                // if the user prefers PaperVision, switch to it upon start up
                val indexOfTab = visualizer.sidebarPanel.indexOfTab("PaperVision")
                if(indexOfTab >= 0) {
                    visualizer.sidebarPanel.selectedIndex = indexOfTab
                }
            }

            // END OF WELCOME DIALOGS
        }

        inputSourceManager.inputSourceLoader.saveInputSourcesToFile()

        visualizer.joinInit()

        pipelineManager.subscribePipelineHandler(TimestampedPipelineHandler())
        pipelineManager.subscribePipelineHandler(OpModePipelineHandler(inputSourceManager, visualizer.viewport))

        visualizer.sourceSelectorPanel.updateSourcesList() //update sources and pick first one
        visualizer.sourceSelectorPanel.sourceSelector.selectedIndex = 0
        visualizer.sourceSelectorPanel.allowSourceSwitching = true

        visualizer.opModeSelectorPanel.updateOpModesList()
        visualizer.pipelineSelectorPanel.updatePipelinesList()

        visualizer.pipelineSelectorPanel.selectedIndex = 0 //update pipelines and pick first one (DefaultPipeline)
        visualizer.opModeSelectorPanel.selectedIndex = 0 //update opmodes and pick first one (DefaultPipeline)

        visualizer.pipelineSelectorPanel.allowPipelineSwitching = true
        visualizer.opModeSelectorPanel.allowOpModeSwitching = true

        //post output mats from the pipeline to the visualizer viewport
        pipelineManager.pipelineOutputPosters.add(visualizer.viewport)

        pluginManager.enablePlugins()

         // BEGIN

        try {
            mainLoop()
        } catch (e: InterruptedException) {
            logger.warn("Main thread interrupted ($hexCode)", e)
        }

        // HANDLE MAIN LOOP EXITS

        if(!destroying) {
            destroy(LifecycleSignal.Destroy.Reason.THREAD_EXIT)
        } else if (isRestarting) {
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
     * @see start
     */
    @Throws(InterruptedException::class)
    private fun mainLoop() {
        if(Thread.currentThread() != eocvSimThread) {
            throw IllegalStateException("start() must be called from the EOCVSim thread")
        }

        logger.info("-- Begin EOCVSim loop ($hexCode) --")

        orchestrator.changePhase(Orchestrator.Phase.RUN)

        while (!eocvSimThread.isInterrupted && !destroying) {
            //run all pending requested runnables
            onMainLoop.run()

            pipelineStatisticsCalculator.newInputFrameStart()

            orchestrator.orchestrate()

            //limit FPS
            fpsLimiter.maxFPS = config.pipelineMaxFps.fps.toDouble()
            try {
                fpsLimiter.sync()
            } catch (_: InterruptedException) {
                break
            }

            when(val signal = lifecycleChannel.tryReceive().getOrNull()) {
                is LifecycleSignal.Restart -> {
                    logger.info("Restart signal received")
                    restart()
                }
                is LifecycleSignal.Destroy -> {
                    logger.info("Destroy signal received")
                    destroy(signal.reason)
                }
                null -> {}
            }
        }

        logger.warn("Main thread interrupted ($hexCode)")
    }

    /**
     * Destroys the simulator
     * @param reason the reason why the simulator is being destroyed, it mainly allows to restart the simulator if requested
     */
    fun destroy(reason: LifecycleSignal.Destroy.Reason) {
        logger.warn("-- Destroying current EOCVSim ($hexCode) due to $reason, it is normal to see InterruptedExceptions and other kinds of stack traces below --")

        logger.info("Trying to save config file...")

        orchestrator.changePhase(Orchestrator.Phase.DESTROY)
        orchestrator.orchestrate()

        destroying = true
        scope.cancel()

        if(reason == LifecycleSignal.Destroy.Reason.THREAD_EXIT) {
            exitProcess(0)
        } else {
            eocvSimThread.interrupt()
        }

        if (reason == LifecycleSignal.Destroy.Reason.USER_REQUESTED || reason == LifecycleSignal.Destroy.Reason.CRASH)
            jvmMainThread.interrupt()
    }

    /**
     * Destroys the simulator with the reason being USER_REQUESTED
     */
    fun destroy() {
        destroy(LifecycleSignal.Destroy.Reason.USER_REQUESTED)
    }

    /**
     * Destroys the current simulator with the reason being RESTART.
     * This method is used to restart the simulator.
     */
    fun restart() {
        logger.info("Restarting...")

        pipelineManager.captureStaticSnapshot()

        isRestarting = true
        destroy(LifecycleSignal.Destroy.Reason.USER_REQUESTED)
    }

    /**
     * Checks if the simulator is currently recording
     * @return true if the simulator is currently recording, false otherwise
     */
    fun isCurrentlyRecording() = recordingManager.isCurrentlyRecording()

    /**
     * Parameters class to initialize the simulator with
     * @see EOCVSim
     */
    class Parameters {
        /**
         * The initial user workspace path to load
         * Overrides the workspace path in the config
         */
        var initialWorkspace: File? = null

        /**
         * The initial pipeline to load
         */
        var initialPipelineName: String? = null

        /**
         * Whether the specified pipeline must be searched in the CLASSPATH or from the workspace
         */
        var initialPipelineSource: PipelineSource? = null
    }

}
