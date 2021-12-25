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

package com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.PopupX
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToggleButton
import javax.swing.SwingUtilities

class PipelineSelectorButtonsPanel(eocvSim: EOCVSim) : JPanel(GridBagLayout()) {

    val pipelinePauseBtt  = JToggleButton("Pause")
    val pipelineRecordBtt = JToggleButton("Record")

    val pipelineWorkspaceBtt = JButton("Workspace")

    val workspaceButtonsPanel = JPanel(GridBagLayout())
    val pipelineCompileBtt = JButton("Build java files")

    private var lastWorkspacePopup: PopupX? = null

    init {
        //listener for changing pause state
        pipelinePauseBtt.addActionListener {
            eocvSim.onMainUpdate.doOnce { eocvSim.pipelineManager.setPaused(pipelinePauseBtt.isSelected) }
        }
        pipelinePauseBtt.addChangeListener {
            pipelinePauseBtt.text = if(pipelinePauseBtt.isSelected) "Resume" else "Pause"
        }

        add(pipelinePauseBtt, GridBagConstraints().apply {
            insets = Insets(0, 0, 0, 5)
        })

        pipelineRecordBtt.addActionListener {
            eocvSim.onMainUpdate.doOnce {
                if (pipelineRecordBtt.isSelected) {
                    if (!eocvSim.isCurrentlyRecording()) eocvSim.startRecordingSession()
                } else {
                    if (eocvSim.isCurrentlyRecording()) eocvSim.stopRecordingSession()
                }
            }
        }
        add(pipelineRecordBtt, GridBagConstraints().apply { gridx = 1 })

        pipelineWorkspaceBtt.addActionListener {
            val workspaceLocation = pipelineWorkspaceBtt.locationOnScreen

            val window = SwingUtilities.getWindowAncestor(this)
            val popup = PopupX(window, workspaceButtonsPanel, workspaceLocation.x, workspaceLocation.y)

            popup.onShow {
                popup.setLocation(
                    popup.window.location.x - workspaceButtonsPanel.width / 5,
                    popup.window.location.y
                )
            }

            lastWorkspacePopup?.hide()
            lastWorkspacePopup = popup
            popup.show()
        }
        add(pipelineWorkspaceBtt, GridBagConstraints().apply {
            gridwidth = 2
            gridy = 1

            insets = Insets(5, 0, 0, 0)
            weightx = 1.0

            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
        })

        // WORKSPACE BUTTONS POPUP
        pipelineCompileBtt.addActionListener { eocvSim.visualizer.asyncCompilePipelines() }
        workspaceButtonsPanel.add(pipelineCompileBtt, GridBagConstraints())

        val selectWorkspBtt = JButton("Select workspace")

        selectWorkspBtt.addActionListener { eocvSim.visualizer.selectPipelinesWorkspace() }
        workspaceButtonsPanel.add(selectWorkspBtt, GridBagConstraints().apply { gridx = 1 })
    }

}