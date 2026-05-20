/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager

import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TelemetryPanel
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SidebarPipelineTabPanel : SidebarPanel.TabJPanel(), KoinComponent {

    private val pipelineManager: PipelineManager by inject()


    val pipelineSelectorPanel = PipelineSelectorPanel()
    val sourceSelectorPanel = SourceSelectorPanel()


    val telemetryPanel = TelemetryPanel()


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
        pipelineManager.onUpdate.once {
            pipelineManager.changePipeline(0)
        }
    }


    override fun onDeactivated() {
        pipelineSelectorPanel.isActive = false
    }

}
