package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeControlsPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeSelectorPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.PipelineSelectorPanel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.border.EmptyBorder

class PipelineOpModeSwitchablePanel(val eocvSim: EOCVSim) : JTabbedPane() {

    val pipelinePanel = JPanel()

    val pipelineSelectorPanel = PipelineSelectorPanel(eocvSim)
    val sourceSelectorPanel = SourceSelectorPanel(eocvSim)

    val opModePanel = JPanel()

    val opModeSelectorPanel = OpModeSelectorPanel(eocvSim)
    val opModeControlsPanel = OpModeControlsPanel()

    private var beforeAllowPipelineSwitching: Boolean? = null

    init {
        pipelinePanel.layout = GridLayout(2, 1)

        pipelineSelectorPanel.border = EmptyBorder(0, 20, 20, 20)
        pipelinePanel.add(pipelineSelectorPanel)

        sourceSelectorPanel.border = EmptyBorder(0, 20, 20, 20)
        pipelinePanel.add(sourceSelectorPanel)

        opModePanel.layout = GridLayout(2, 1)

        opModeSelectorPanel.border = EmptyBorder(0, 20, 20, 20)
        opModePanel.add(opModeSelectorPanel)

        opModeControlsPanel.border = EmptyBorder(0, 20, 20, 20)
        opModePanel.add(opModeControlsPanel)

        add("Pipeline", pipelinePanel)
        add("OpMode", opModePanel)

        addChangeListener {
            val sourceTabbedPane = it.source as JTabbedPane
            val index = sourceTabbedPane.selectedIndex

            if(index == 0 && beforeAllowPipelineSwitching != null) {
                pipelineSelectorPanel.allowPipelineSwitching = beforeAllowPipelineSwitching!!
            } else if(index == 1) {
                beforeAllowPipelineSwitching = pipelineSelectorPanel.allowPipelineSwitching
                pipelineSelectorPanel.allowPipelineSwitching = false
            }
        }
    }

    fun updateSelectorLists() {
        pipelineSelectorPanel.updatePipelinesList()
        opModeSelectorPanel.updateOpModesList()
    }

}