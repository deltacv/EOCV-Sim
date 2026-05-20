/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TelemetryPanel
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SidebarOpModeTabPanel : SidebarPanel.TabJPanel(), KoinComponent {

    val opModeControlsPanel = OpModeControlsPanel()
    val opModeSelectorPanel = OpModeSelectorPanel(opModeControlsPanel)


    val telemetryPanel = TelemetryPanel()


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
