package com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarTabJPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TelemetryPanel
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

class SidebarPipelineTabPanel(eocvSim: EOCVSim) : SidebarTabJPanel() {

    val pipelineSelectorPanel = PipelineSelectorPanel(eocvSim)
    val sourceSelectorPanel = SourceSelectorPanel(eocvSim)

    val telemetryPanel = TelemetryPanel(eocvSim.pipelineManager)

    init {
        font = font.deriveFont(Font.PLAIN, 14f)

        /* Pipeline Tab */
        layout = GridBagLayout()

        pipelineSelectorPanel.border = TitledBorder("Pipelines").apply {
            titleFont = titleFont.deriveFont(Font.BOLD)
            border = EmptyBorder(0, 0, 0, 0)
        }

        add(pipelineSelectorPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 0

            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            insets = Insets(10, 20, 5, 20)
        })

        sourceSelectorPanel.border = TitledBorder("Sources").apply {
            titleFont = titleFont.deriveFont(Font.BOLD)
            border = EmptyBorder(0, 0, 0, 0)
        }

        add(sourceSelectorPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 1

            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            insets = Insets(-5, 20, -10, 20)
        })

        add(telemetryPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 2

            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            insets = Insets(10, 20, 10, 20)
        })
    }

    override fun onActivated() {
        pipelineSelectorPanel.isActive = true
    }

    override fun onDeactivated() {
        pipelineSelectorPanel.isActive = false
    }

}