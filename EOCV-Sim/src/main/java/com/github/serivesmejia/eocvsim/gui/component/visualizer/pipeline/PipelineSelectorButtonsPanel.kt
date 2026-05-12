/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.output.RecordingManager
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import org.koin.core.qualifier.named

import com.github.serivesmejia.eocvsim.gui.component.PopupX
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToggleButton
import javax.swing.SwingUtilities

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PipelineSelectorButtonsPanel : JPanel(GridBagLayout()), KoinComponent {

    private val pipelineManager: PipelineManager by inject()
    private val recordingManager: RecordingManager by inject()
    private val onMainUpdate: EventHandler by inject(named("onMainLoop"))

    private val dialogFactory: DialogFactory by inject()


    val pipelinePauseBtt  = JToggleButton("Pause")
    val pipelineRecordBtt = JToggleButton("Record")

    val pipelineWorkspaceBtt = JButton("Workspace")

    val workspaceButtonsPanel = JPanel(GridBagLayout())
    val pipelineCompileBtt = JButton("Build Java Files")

    private var lastWorkspacePopup: PopupX? = null

    init {
        //listener for changing pause state
        pipelinePauseBtt.addActionListener {
            onMainUpdate.once { pipelineManager.setPaused(pipelinePauseBtt.isSelected) }
        }

        pipelinePauseBtt.addChangeListener {
            pipelinePauseBtt.text = if(pipelinePauseBtt.isSelected) "Resume" else "Pause"
        }

        add(pipelinePauseBtt, GridBagConstraints().apply {
            insets = Insets(0, 0, 0, 5)
        })

        pipelineRecordBtt.addActionListener {
            onMainUpdate.once {
                if (pipelineRecordBtt.isSelected) {
                    if (!recordingManager.isCurrentlyRecording()) recordingManager.startRecordingSession()
                } else {
                    if (recordingManager.isCurrentlyRecording()) recordingManager.stopRecordingSession()
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

        val selectWorkspBtt = JButton("Select Workspace")

        selectWorkspBtt.addActionListener { dialogFactory.createWorkspace() }
        workspaceButtonsPanel.add(selectWorkspBtt, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
        })

        workspaceButtonsPanel.add(Box.createHorizontalStrut(5), GridBagConstraints().apply {
            gridx = 1
            gridy = 0
        })

        pipelineCompileBtt.addActionListener { pipelineManager.compiledPipelineManager.asyncBuild() }

        workspaceButtonsPanel.add(pipelineCompileBtt, GridBagConstraints().apply {
            gridx = 2
            gridy = 0
        })

        val outputBtt = JButton("Pipeline Output")

        outputBtt.addActionListener { dialogFactory.createPipelineOutput() }

        workspaceButtonsPanel.add(outputBtt, GridBagConstraints().apply {
            gridy = 1
            weightx = 1.0
            gridwidth = 3

            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER

            insets = Insets(3, 0, 0, 0)
        })
    }

}
