/*
 * Copyright (c) 2026 Sebastian Erives
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
import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TelemetryPanel
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

class SidebarPipelineTabPanel(private val eocvSim: EOCVSim) : SidebarPanel.TabJPanel() {

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
        eocvSim.pipelineManager.onUpdate.doOnce {
            eocvSim.pipelineManager.changePipeline(0)
        }
    }

    override fun onDeactivated() {
        pipelineSelectorPanel.isActive = false
    }

}