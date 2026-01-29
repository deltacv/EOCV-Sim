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

package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TelemetryPanel
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class SidebarOpModeTabPanel(eocvSim: EOCVSim) : SidebarPanel.TabJPanel() {

    val opModeControlsPanel = OpModeControlsPanel(eocvSim)
    val opModeSelectorPanel = OpModeSelectorPanel(eocvSim, opModeControlsPanel)

    val telemetryPanel = TelemetryPanel(eocvSim.pipelineManager)

    init {
        font = font.deriveFont(Font.PLAIN, 14f)

        /* OpMode Tab */
        layout = GridLayout(3, 1)
        border = EmptyBorder(20, 20, 20, 20)

        opModeSelectorPanel.border = EmptyBorder(0, 0, 20, 0)
        add(opModeSelectorPanel)

        opModeControlsPanel.border = EmptyBorder(0, 0, 20, 0)
        add(opModeControlsPanel)

        add(telemetryPanel)
    }

    override fun onActivated() {
        opModeSelectorPanel.isActive = true
        opModeSelectorPanel.reset()
    }

    override fun onDeactivated() {
        opModeSelectorPanel.isActive = false
        opModeSelectorPanel.reset(-1)
    }
}