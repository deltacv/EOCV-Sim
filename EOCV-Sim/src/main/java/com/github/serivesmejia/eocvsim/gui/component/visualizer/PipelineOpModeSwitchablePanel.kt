package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeControlsPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeSelectorPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.PipelineSelectorPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.SourceSelectorPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

class PipelineOpModeSwitchablePanel(val eocvSim: EOCVSim) : JTabbedPane() {

    val pipelinePanel = JPanel()

    val pipelineSelectorPanel = PipelineSelectorPanel(eocvSim)
    val sourceSelectorPanel = SourceSelectorPanel(eocvSim)

    val opModePanel = JPanel()

    val opModeControlsPanel = OpModeControlsPanel(eocvSim)
    val opModeSelectorPanel = OpModeSelectorPanel(eocvSim, opModeControlsPanel)

    init {
        pipelinePanel.layout = GridBagLayout()

        pipelineSelectorPanel.border = TitledBorder("Pipelines").apply {
            border = EmptyBorder(0, 0, 0, 0)
        }

        pipelinePanel.add(pipelineSelectorPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 0

            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            insets = Insets(10, 20, 5, 20)
        })

        sourceSelectorPanel.border = TitledBorder("Sources").apply {
            border = EmptyBorder(0, 0, 0, 0)
        }

        pipelinePanel.add(sourceSelectorPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 1

            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            insets = Insets(-5, 20, -10, 20)
        })

        opModePanel.layout = GridLayout(2, 1)

        opModeSelectorPanel.border = EmptyBorder(0, 20, 20, 20)
        opModePanel.add(opModeSelectorPanel)

        opModeControlsPanel.border = EmptyBorder(0, 20, 20, 20)
        opModePanel.add(opModeControlsPanel)

        add("Pipeline", JPanel().apply {
            layout = BoxLayout(this, BoxLayout.LINE_AXIS)
            add(pipelinePanel)
        })
        add("OpMode", opModePanel)

        addChangeListener {
            val sourceTabbedPane = it.source as JTabbedPane
            val index = sourceTabbedPane.selectedIndex

            if(index == 0) {
                pipelineSelectorPanel.isActive = true
                opModeSelectorPanel.isActive = false

                opModeSelectorPanel.reset(0)
            } else if(index == 1) {
                opModeSelectorPanel.reset()

                pipelineSelectorPanel.isActive = false
                opModeSelectorPanel.isActive = true
            }
        }
    }

    fun updateSelectorLists() {
        pipelineSelectorPanel.updatePipelinesList()
        opModeSelectorPanel.updateOpModesList()
    }

    fun updateSelectorListsBlocking() = runBlocking {
        launch(Dispatchers.Swing) {
            updateSelectorLists()
        }
    }

    fun enableSwitching() {
        pipelineSelectorPanel.allowPipelineSwitching = true
        opModeSelectorPanel.isActive = true
    }

    fun disableSwitching() {
        pipelineSelectorPanel.allowPipelineSwitching = false
        opModeSelectorPanel.isActive = false
    }

    fun enableSwitchingBlocking() = runBlocking {
        launch(Dispatchers.Swing) {
            enableSwitching()
        }
    }

    fun disableSwitchingBlocking() = runBlocking {
        launch(Dispatchers.Swing) {
            disableSwitching()
        }
    }

}