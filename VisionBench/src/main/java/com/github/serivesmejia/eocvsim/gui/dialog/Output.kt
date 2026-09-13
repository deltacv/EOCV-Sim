/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.dialog

import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.compiled.CompiledPipelineManager
import com.github.serivesmejia.eocvsim.pipeline.compiled.PipelineCompileStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Output @JvmOverloads constructor(
    private val tabbedPaneIndex: Int = latestIndex,
    private val wasManuallyOpened: Boolean = false
) : KoinComponent {

    private val visualizer: Visualizer by inject()

    companion object {
        var isAlreadyOpened = false
            private set

        var latestIndex = 0
            private set
    }

    private val output = JDialog(visualizer.frame)

    private val buildBottomButtonsPanel = BuildOutputBottomButtonsPanel(::close)
    private val buildOutputPanel = OutputPanel(buildBottomButtonsPanel)

    private val pipelineBottomButtonsPanel = PipelineBottomButtonsPanel(::close)
    private val pipelineOutputPanel = OutputPanel(pipelineBottomButtonsPanel)

    private val tabbedPane = JTabbedPane()

    private val pipelineManager: PipelineManager by inject()
    private val compiledPipelineManager: CompiledPipelineManager by inject()
    private val scope: CoroutineScope by inject()

    init {
        isAlreadyOpened = true

        output.isModal = true
        output.title = "Output"

        tabbedPane.add("Pipeline Output", pipelineOutputPanel)
        tabbedPane.add("Build Output", buildOutputPanel)

        tabbedPane.selectedIndex = tabbedPaneIndex

        output.contentPane.add(tabbedPane)

        updatePipelineOutput()

        if(pipelineManager.paused) {
            pipelinePaused()
        } else {
            pipelineResumed()
        }

        buildEnded(true)
        if(compiledPipelineManager.isBuildRunning) {
            buildRunning()
        }

        registerListeners()

        output.pack()
        output.setSize(500, 350)

        output.setLocationRelativeTo(null)
        output.isVisible = true
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun registerListeners() = scope.launch(Dispatchers.Swing) {
        output.addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                close()
            }
        })

       pipelineManager.pipelineExceptionTracker.onUpdate {
            if(!output.isVisible) {
                removeListener()
            } else {
                updatePipelineOutput()
            }
        }

        compiledPipelineManager.onBuildStart {
            if(!output.isVisible) {
                removeListener()
            } else {
                buildRunning()
            }
        }

        compiledPipelineManager.onBuildEnd {
            if(!output.isVisible) {
                removeListener()
            } else {
                buildEnded()
                tabbedPane.selectedIndex = 1
            }
        }

        pipelineManager.onPause {
            if(!output.isVisible) {
                removeListener()
            } else {
                pipelinePaused()
            }
        }

        pipelineManager.onResume {
            if(!output.isVisible) {
                removeListener()
            } else {
                pipelineResumed()
            }
        }

        pipelineBottomButtonsPanel.pauseButton.addActionListener {
            pipelineManager.setPaused(pipelineBottomButtonsPanel.pauseButton.isSelected)

            if(pipelineBottomButtonsPanel.pauseButton.isSelected) {
                pipelinePaused()
            } else {
                pipelineResumed()
            }
        }

        pipelineBottomButtonsPanel.clearButton.addActionListener {
            pipelineManager.pipelineExceptionTracker.clear()
        }

        buildBottomButtonsPanel.buildAgainButton.addActionListener {
            pipelineManager.compiledPipelineManager.asyncBuild()
        }
    }

    private fun updatePipelineOutput() {
        pipelineOutputPanel.outputArea.text = pipelineManager.pipelineExceptionTracker.message
    }

    private fun buildRunning() {
        buildBottomButtonsPanel.buildAgainButton.isEnabled = false
        buildOutputPanel.outputArea.text = "Build running..."
    }

    private fun buildEnded(calledOnInit: Boolean = false) {
        compiledPipelineManager.run {
            if(!wasManuallyOpened &&
                compiledPipelineManager.lastBuildResult!!.status == PipelineCompileStatus.SUCCESS &&
                tabbedPaneIndex == 1 && !calledOnInit
            ) {
                // close if the dialog was automatically opened in the
                // "build output" tab and a new build was successful
                close()
                return@buildEnded
            }

            buildOutputPanel.outputArea.text = when {
                lastBuildOutputMessage != null -> lastBuildOutputMessage!!
                lastBuildResult != null -> lastBuildResult!!.message
                else -> "No output"
            }
        }

        buildBottomButtonsPanel.buildAgainButton.isEnabled = true
    }

    private fun pipelineResumed() {
        pipelineBottomButtonsPanel.pauseButton.isSelected = false
        pipelineBottomButtonsPanel.pauseButton.text = "Pause"
    }

    private fun pipelinePaused() {
        pipelineBottomButtonsPanel.pauseButton.isSelected = true
        pipelineBottomButtonsPanel.pauseButton.text = "Resume"
    }

    fun close() {
        output.isVisible = false
        isAlreadyOpened = false
        latestIndex = tabbedPane.selectedIndex
    }

    class PipelineBottomButtonsPanel(
        closeCallback: () -> Unit
    ) : OutputPanel.DefaultBottomButtonsPanel(closeCallback = closeCallback) {
        val pauseButton = JToggleButton("Pause")

        override fun create(panel: OutputPanel) {
            add(Box.createRigidArea(Dimension(4, 0)))
            add(pauseButton)
            super.create(panel)
        }
    }

    class BuildOutputBottomButtonsPanel(
        closeCallback: () -> Unit,
    ) : OutputPanel.DefaultBottomButtonsPanel(closeCallback = closeCallback) {
        val buildAgainButton = JButton("Build again")

        override fun create(panel: OutputPanel) {
            add(Box.createRigidArea(Dimension(4, 0)))
            add(buildAgainButton)
            super.create(panel)
        }
    }
}

