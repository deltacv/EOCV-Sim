package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarTabJPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TelemetryPanel
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class SidebarOpModeTabPanel(eocvSim: EOCVSim) : SidebarTabJPanel() {

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
        opModeSelectorPanel.reset(0)
    }
}