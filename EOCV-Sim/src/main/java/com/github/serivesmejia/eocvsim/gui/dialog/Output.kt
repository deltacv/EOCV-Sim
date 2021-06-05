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
import com.github.serivesmejia.eocvsim.util.StrUtil
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class Output @JvmOverloads constructor(
    parent: JFrame,
    private val eocvSim: EOCVSim,
    tabbedPaneIndex: Int = latestIndex
) {

    companion object {
        var isAlreadyOpened = false
            private set

        private var latestIndex = 0
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
        output.setSize(500, 350)

        tabbedPane.add("Pipeline Output", pipelineOutputPanel)
        tabbedPane.add("Build Output", buildOutputPanel)

        tabbedPane.selectedIndex = tabbedPaneIndex

        output.contentPane.add(tabbedPane)

        output.setLocationRelativeTo(null)

        updatePipelineOutput()

        if(eocvSim.pipelineManager.paused) {
            pipelinePaused()
        } else {
            pipelineResumed()
        }

        buildEnded()
        if(compiledPipelineManager.isBuildRunning) {
            buildRunning()
        }

        registerListeners()

        output.isVisible = true
    }

    private fun registerListeners() = GlobalScope.launch(Dispatchers.Swing) {
        output.addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                close()
            }
        })

       pipelineExceptionTracker.onUpdate {
            if(!output.isVisible) {
                it.removeThisPersistent()
            } else {
                updatePipelineOutput()
            }
        }

        compiledPipelineManager.onBuildStart {
            if(!output.isVisible) {
                it.removeThisPersistent()
            } else {
                buildRunning()
            }
        }

        compiledPipelineManager.onBuildEnd {
            if(!output.isVisible) {
                it.removeThisPersistent()
            } else {
                buildEnded()
                tabbedPane.selectedIndex = 1
            }
        }

        eocvSim.pipelineManager.onPause {
            if(!output.isVisible) {
                it.removeThisPersistent()
            } else {
                pipelinePaused()
            }
        }

        eocvSim.pipelineManager.onResume {
            if(!output.isVisible) {
                it.removeThisPersistent()
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

    private fun buildEnded() {
        compiledPipelineManager.run {
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
        closeCallback: () -> Unit
    ) : OutputPanel.DefaultBottomButtonsPanel(closeCallback) {
        val buildAgainButton = JButton("Build again")

        override fun create(panel: OutputPanel) {
            add(Box.createRigidArea(Dimension(4, 0)))
            add(buildAgainButton)
            super.create(panel)
        }
    }
}
