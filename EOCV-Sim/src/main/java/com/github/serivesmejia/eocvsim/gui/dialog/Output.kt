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

package com.github.serivesmejia.eocvsim.gui.dialog

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel
import com.github.serivesmejia.eocvsim.pipeline.compiler.PipelineCompileStatus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class Output @JvmOverloads constructor(
    parent: JFrame,
    private val eocvSim: EOCVSim,
    private val tabbedPaneIndex: Int = latestIndex,
    private val wasManuallyOpened: Boolean = false
) {

    companion object {
        var isAlreadyOpened = false
            private set

        var latestIndex = 0
            private set
    }

    private val output = JDialog(parent)

    private val buildBottomButtonsPanel = BuildOutputBottomButtonsPanel(::close)
    private val buildOutputPanel = OutputPanel(buildBottomButtonsPanel)

    private val pipelineBottomButtonsPanel = PipelineBottomButtonsPanel(::close)
    private val pipelineOutputPanel = OutputPanel(pipelineBottomButtonsPanel)

    private val tabbedPane = JTabbedPane()

    private val compiledPipelineManager  = eocvSim.pipelineManager.compiledPipelineManager
    private val pipelineExceptionTracker = eocvSim.pipelineManager.pipelineExceptionTracker

    init {
        isAlreadyOpened = true

        eocvSim.visualizer.childDialogs.add(output)

        output.isModal = true
        output.title = "Output"

        tabbedPane.add("Pipeline Output", pipelineOutputPanel)
        tabbedPane.add("Build Output", buildOutputPanel)

        tabbedPane.selectedIndex = tabbedPaneIndex

        output.contentPane.add(tabbedPane)

        updatePipelineOutput()

        if(eocvSim.pipelineManager.paused) {
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
    private fun registerListeners() = GlobalScope.launch(Dispatchers.Swing) {
        output.addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                close()
            }
        })

       pipelineExceptionTracker.onUpdate {
            if(!output.isVisible) {
                it.removeThis()
            } else {
                updatePipelineOutput()
            }
        }

        compiledPipelineManager.onBuildStart {
            if(!output.isVisible) {
                it.removeThis()
            } else {
                buildRunning()
            }
        }

        compiledPipelineManager.onBuildEnd {
            if(!output.isVisible) {
                it.removeThis()
            } else {
                buildEnded()
                tabbedPane.selectedIndex = 1
            }
        }

        eocvSim.pipelineManager.onPause {
            if(!output.isVisible) {
                it.removeThis()
            } else {
                pipelinePaused()
            }
        }

        eocvSim.pipelineManager.onResume {
            if(!output.isVisible) {
                it.removeThis()
            } else {
                pipelineResumed()
            }
        }

        pipelineBottomButtonsPanel.pauseButton.addActionListener {
            eocvSim.pipelineManager.setPaused(pipelineBottomButtonsPanel.pauseButton.isSelected)

            if(pipelineBottomButtonsPanel.pauseButton.isSelected) {
                pipelinePaused()
            } else {
                pipelineResumed()
            }
        }

        pipelineBottomButtonsPanel.clearButton.addActionListener {
            eocvSim.pipelineManager.pipelineExceptionTracker.clear()
        }

        buildBottomButtonsPanel.buildAgainButton.addActionListener {
            eocvSim.visualizer.asyncCompilePipelines()
        }
    }

    private fun updatePipelineOutput() {
        pipelineOutputPanel.outputArea.text = pipelineExceptionTracker.message
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
    ) : OutputPanel.DefaultBottomButtonsPanel(closeCallback) {
        val pauseButton = JToggleButton("Pause")

        override fun create(panel: OutputPanel) {
            add(Box.createRigidArea(Dimension(4, 0)))
            add(pauseButton)
            super.create(panel)
        }
    }

    class BuildOutputBottomButtonsPanel(
        closeCallback: () -> Unit,
    ) : OutputPanel.DefaultBottomButtonsPanel(closeCallback) {
        val buildAgainButton = JButton("Build again")

        override fun create(panel: OutputPanel) {
            add(Box.createRigidArea(Dimension(4, 0)))
            add(buildAgainButton)
            super.create(panel)
        }
    }
}
