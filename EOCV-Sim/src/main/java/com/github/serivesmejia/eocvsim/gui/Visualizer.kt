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

package com.github.serivesmejia.eocvsim.gui

import com.formdev.flatlaf.FlatLaf
import com.github.serivesmejia.eocvsim.Build
import com.github.serivesmejia.eocvsim.LifecycleSignal
import com.github.serivesmejia.eocvsim.LifecycleSignal.Destroy.Reason
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.component.CollapsiblePanelX
import com.github.serivesmejia.eocvsim.gui.component.tuner.ColorPicker
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.InputSourceDropTarget
import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TopMenuBar
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeSelectorPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.SidebarOpModeTabPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.PipelineSelectorPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.SidebarPipelineTabPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.SourceSelectorPanel
import com.github.serivesmejia.eocvsim.gui.theme.Theme
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.pipeline.compiler.PipelineCompiler
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.workspace.util.VSCodeLauncher
import com.github.serivesmejia.eocvsim.workspace.util.template.GradleWorkspaceTemplate
import io.github.deltacv.common.util.loggerForThis
import io.github.deltacv.vision.external.gui.SwingOpenCvViewport
import org.opencv.core.Size
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Taskbar
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.github.serivesmejia.eocvsim.output.RecordingManager
import com.github.serivesmejia.eocvsim.util.event.Orchestrable
import com.github.serivesmejia.eocvsim.util.event.Orchestrator
import io.github.deltacv.common.pipeline.util.PipelineStatisticsCalculator
import org.openftc.easyopencv.OpenCvViewport
import org.koin.core.qualifier.named
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class Visualizer : Orchestrable, KoinComponent {

    val onMainUpdate: EventHandler by inject(named("onMainLoop"))

    val lifecycleChannel: Channel<LifecycleSignal> by inject(named("lifecycle"))

    val pipelineManager: PipelineManager by inject()
    val inputSourceManager: InputSourceManager by inject()
    val configManager: ConfigManager by inject()
    val workspaceManager: WorkspaceManager by inject()
    val dialogFactory: DialogFactory by inject()
    val recordingManager: RecordingManager by inject()
    val pipelineStatisticsCalculator: PipelineStatisticsCalculator by inject()
    val scope: CoroutineScope by inject()

    private val initOrchestrator: Orchestrator by inject(named("init"))

    val onInitFinished = EventHandler("OnVisualizerInitFinish")
    val onPluginGuiAttachment = EventHandler("OnPluginGuiAttachment")

    lateinit var frame: JFrame
        private set

    private val fpsMeterDescriptor: String
        get() = "deltacv EOCV-Sim v" + Build.standardVersionString + if (Build.isDev) "-dev" else ""

    @JvmField
    val viewport = SwingOpenCvViewport(Size(1080.0, 720.0), fpsMeterDescriptor)

    lateinit var menuBar: TopMenuBar
        private set

    lateinit var tunerMenuPanel: JPanel
        private set

    lateinit var sidebarContainer: JPanel
        private set

    lateinit var sidebarPanel: SidebarPanel
        private set

    lateinit var sidebarPipelineTabPanel: SidebarPipelineTabPanel
        private set
    lateinit var sidebarOpModeTabPanel: SidebarOpModeTabPanel
        private set

    lateinit var pipelineSelectorPanel: PipelineSelectorPanel
        private set
    lateinit var sourceSelectorPanel: SourceSelectorPanel
        private set

    lateinit var opModeSelectorPanel: OpModeSelectorPanel
        private set

    lateinit var tunerCollapsible: CollapsiblePanelX
        private set

    private var title = "EasyOpenCV Simulator v" + Build.standardVersionString
    private var titleMsg = "No pipeline"
    private var beforeTitle = ""
    private var beforeTitleMsg = ""

    lateinit var colorPicker: ColorPicker
        private set

    @Volatile
    var hasFinishedInitializing = false
        private set

    private val logger by loggerForThis()

    private val pipelineRenderHook =
        OpenCvViewport.RenderHook {
            canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, scaleCanvasDensity, userContext ->
            if (pipelineManager.hasInitCurrentPipeline) {
                pipelineManager.currentPipeline?.onDrawFrame(canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, scaleCanvasDensity, userContext)
            }
        }

    init {
        initOrchestrator.register(this) {
            target {
                withContext(Dispatchers.Swing) {
                    it.init(configManager.config.simTheme)
                }
            }
            dependsOn(configManager)
        }
    }

    private fun init(theme: Theme) {
        try {
            theme.install()
        } catch (e: Exception) {
            logger.error("Failed to install theme ${theme.name}", e)
        }

        Icons.setDark(FlatLaf.isLafDark())

        if (Build.isDev) {
            title += "-dev "
        }

        frame = JFrame()

        viewport.init()
        viewport.dark = FlatLaf.isLafDark()

        colorPicker = ColorPicker(viewport)

        val skiaPanel = viewport.skiaPanel()
        skiaPanel.layout = BorderLayout()

        frame.add(skiaPanel)

        menuBar = TopMenuBar()
        tunerMenuPanel = JPanel()

        sidebarPanel = SidebarPanel()

        sidebarPipelineTabPanel = SidebarPipelineTabPanel()
        pipelineSelectorPanel = sidebarPipelineTabPanel.pipelineSelectorPanel
        sourceSelectorPanel = sidebarPipelineTabPanel.sourceSelectorPanel

        sidebarOpModeTabPanel = SidebarOpModeTabPanel()
        opModeSelectorPanel = sidebarOpModeTabPanel.opModeSelectorPanel

        sidebarPanel.add("Pipeline", sidebarPipelineTabPanel)
        sidebarPanel.add("OpMode", sidebarOpModeTabPanel)

        sidebarContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Separator.foreground"))
            add(sidebarPanel)
        }

        frame.jMenuBar = menuBar

        frame.contentPane.dropTarget = InputSourceDropTarget()

        tunerCollapsible = CollapsiblePanelX("Variable Tuner", null, null).apply {
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.LINE_AXIS)
            isVisible = false
        }

        val tunerScrollPane = JScrollPane(tunerMenuPanel).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
        }

        tunerCollapsible.contentPanel.add(tunerScrollPane)

        onPluginGuiAttachment.run()
        onPluginGuiAttachment.callRightAway = EventHandler.CallRightAway.InPlace

        frame.add(tunerCollapsible, BorderLayout.SOUTH)
        frame.add(sidebarContainer, BorderLayout.EAST)

        frame.size = Dimension(780, 645)
        frame.minimumSize = frame.size
        frame.title = "EasyOpenCV Simulator - No Pipeline"

        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE

        frame.iconImages = listOf(
            EOCVSimIconLibrary.icoEOCVSim128.image,
            EOCVSimIconLibrary.icoEOCVSim64.image,
            EOCVSimIconLibrary.icoEOCVSim32.image,
            EOCVSimIconLibrary.icoEOCVSim16.image
        )

        if (Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            try {
                taskbar.iconImage = EOCVSimIconLibrary.icoEOCVSim128.image
            } catch (e: UnsupportedOperationException) {
                logger.warn("Setting the Taskbar icon image is not supported on this platform")
            } catch (e: SecurityException) {
                logger.warn("Setting the Taskbar icon image was not allowed by the security manager")
            }
        }

        frame.setLocationRelativeTo(null)
        frame.extendedState = JFrame.MAXIMIZED_BOTH

        frame.isVisible = true

        onInitFinished.run()
        onInitFinished.callRightAway = EventHandler.CallRightAway.InPlace

        registerListeners()

        inputSourceManager.onInputSourceInitError {
            dialogFactory.createInformation(
                frame,
                "Error while loading requested source", "Falling back to previous source",
                "Operation failed"
            )
        }

        hasFinishedInitializing = true

        if (!PipelineCompiler.IS_USABLE) {
            compilerUnsupported()
        }

        setupSubscriptions()
    }

    private fun registerListeners() {
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                lifecycleChannel.trySend(LifecycleSignal.Destroy(Reason.USER_REQUESTED))
            }
        })

        viewport.component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!colorPicker.isPicking) {
                    pipelineManager.callViewportTapped()
                }
            }
        })

        pipelineManager.onPipelineChange.attach { colorPicker.stopPicking() }
    }

    fun joinInit() {
        while (!hasFinishedInitializing) {
            Thread.onSpinWait()
        }
    }

    fun close() {
        SwingUtilities.invokeLater {
            frame.isVisible = false
            viewport.dispose()
            frame.dispose()
        }
    }

    private fun updateFrameTitle(title: String, titleMsg: String) {
        frame.title = "$title - $titleMsg"
    }

    fun setTitle(title: String) {
        this.title = title
        if (beforeTitle != title) updateFrameTitle(title, titleMsg)
        beforeTitle = title
    }

    fun setTitleMessage(titleMsg: String) {
        this.titleMsg = titleMsg
        if (beforeTitleMsg != titleMsg) updateFrameTitle(title, titleMsg)
        beforeTitleMsg = titleMsg
    }

    private fun updateTitle() {
        val isBuildRunning = if (pipelineManager.compiledPipelineManager.isBuildRunning) "(Building)" else ""

        val workspaceMsg = " - ${workspaceManager.workspaceFile.absolutePath} $isBuildRunning"

        val isPaused = if (pipelineManager.paused) " (Paused)" else ""
        val isRecording = if (recordingManager.isCurrentlyRecording()) " RECORDING" else ""

        val msg = isRecording + isPaused

        if (pipelineManager.currentPipeline == null) {
            setTitleMessage("No pipeline$msg${workspaceMsg}")
        } else {
            setTitleMessage("${pipelineManager.currentPipelineName}$msg${workspaceMsg}")
        }
    }

    private fun setupSubscriptions() {
        pipelineManager.onPipelineChange {
            colorPicker.stopPicking()
            pipelineStatisticsCalculator.init()

            if(pipelineManager.currentPipeline !is OpMode && pipelineManager.currentPipeline != null) {
                viewport.activate()
                viewport.setRenderHook(pipelineRenderHook)
            } else {
                viewport.deactivate()
                viewport.clearViewport()
            }
        }

        pipelineManager.onUpdate {
            if(pipelineManager.currentPipeline !is OpMode && pipelineManager.currentPipeline != null) {
                viewport.notifyStatistics(
                    pipelineStatisticsCalculator.avgFps,
                    pipelineStatisticsCalculator.avgPipelineTime,
                    pipelineStatisticsCalculator.avgOverheadTime
                )
            }

            updateTitle()
        }

        pipelineManager.onPipelineTimeout {
            dialogFactory.createInformation(
                frame,
                "Current pipeline took too long to ${pipelineManager.lastPipelineAction}",
                "Falling back to DefaultPipeline",
                "Operation failed"
            )
        }
    }

    fun updateTunerFields(fields: List<TunableFieldPanel>) {
        tunerMenuPanel.removeAll()
        tunerMenuPanel.repaint()

        for (fieldPanel in fields) {
            tunerMenuPanel.add(fieldPanel)
            fieldPanel.showFieldPanel()
        }

        tunerCollapsible.isVisible = fields.isNotEmpty()
    }

    fun compilerUnsupported() {
        dialogFactory.createInformation(
            frame,
            "Runtime pipeline builds are not supported on this JVM",
            "For further info, check the EOCV-Sim docs",
            "Operation failed"
        )
    }


    fun selectPipelinesWorkspace() {
        dialogFactory.createFileChooser(frame, DialogFactory.FileChooser.Mode.DIRECTORY_SELECT)
            .addCloseListener { option, selectedFile, _ ->
                if (option == JFileChooser.APPROVE_OPTION && selectedFile != null) {
                    if (!selectedFile.exists()) selectedFile.mkdir()
                    onMainUpdate.once {

                        workspaceManager.workspaceFile = selectedFile
                    }
                }
            }
    }


    fun createVSCodeWorkspace() {
        dialogFactory.createFileChooser(frame, DialogFactory.FileChooser.Mode.DIRECTORY_SELECT)
            .addCloseListener { option, selectedFile, _ ->
                if (option == JFileChooser.APPROVE_OPTION && selectedFile != null) {
                    if (!selectedFile.exists()) selectedFile.mkdir()

                    if (selectedFile.isDirectory && (selectedFile.listFiles()?.size ?: 0) == 0) {
                        workspaceManager.createWorkspaceWithTemplateAsync(selectedFile, GradleWorkspaceTemplate) {
                            askOpenVSCode()
                        }
                    } else {
                        dialogFactory.createYesOrNo(
                            frame,
                            "Open Workspace",
                            "The workspace has been successfully created. Do you want to open it?",
                        ) { result ->
                            if (result == JOptionPane.YES_OPTION) {
                                VSCodeLauncher.launch(selectedFile)
                            }
                        }
                    }
                }
            }
    }

    fun askOpenVSCode() {
        dialogFactory.createYesOrNo(frame, "A new workspace was created. Do you want to open VS Code?", "") { result ->
            if (result == 0) {
                JOptionPane.showMessageDialog(
                    frame,
                    "After opening VS Code, you will need to install the Extension Pack for Java, for proper autocompletion support. Ensure you do so when asked by the editor!"
                )
                VSCodeLauncher.asyncLaunch(workspaceManager.workspaceFile, scope)

            }
        }
    }
}
