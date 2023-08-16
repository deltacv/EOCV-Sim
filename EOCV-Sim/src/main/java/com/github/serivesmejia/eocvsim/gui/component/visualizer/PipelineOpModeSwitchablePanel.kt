package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeControlsPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeSelectorPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.PipelineSelectorPanel
import javax.swing.BoxLayout
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

    private var beforeAllowPipelineSwitching = false

    init {
        pipelinePanel.layout = BoxLayout(pipelinePanel, BoxLayout.Y_AXIS)

        pipelineSelectorPanel.border = EmptyBorder(0, 20, 0, 20)
        sourceSelectorPanel.border = EmptyBorder(0, 20, 0, 20)

        pipelinePanel.add(pipelineSelectorPanel)
        pipelinePanel.add(sourceSelectorPanel)

        opModePanel.layout = BoxLayout(opModePanel, BoxLayout.Y_AXIS)

        opModeSelectorPanel.border = EmptyBorder(0, 20, 0, 20)
        opModeControlsPanel.border = EmptyBorder(0, 20, 0, 20)

        opModePanel.add(opModeSelectorPanel)
        opModePanel.add(opModeControlsPanel)

        add("Pipeline", pipelinePanel)
        add("OpMode", opModePanel)

        addChangeListener {
            val sourceTabbedPane = it.source as JTabbedPane
            val index = sourceTabbedPane.selectedIndex

            if(index == 0) {
                pipelineSelectorPanel.allowPipelineSwitching = beforeAllowPipelineSwitching
            } else if(index == 1) {
                beforeAllowPipelineSwitching = pipelineSelectorPanel.allowPipelineSwitching
                pipelineSelectorPanel.allowPipelineSwitching = false
            }
        }
    }

}