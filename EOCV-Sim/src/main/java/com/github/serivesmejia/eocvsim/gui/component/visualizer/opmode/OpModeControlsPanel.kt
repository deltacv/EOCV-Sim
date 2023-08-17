package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import java.awt.BorderLayout
import javax.swing.JPanel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.SwingUtilities

class OpModeControlsPanel : JPanel() {

    val controlButton = JButton()

    init {
        layout = BorderLayout()

        SwingUtilities.invokeLater {
            updateControlButtonIcon()
        }

        controlButton.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(evt: java.awt.event.ComponentEvent?) {
                updateControlButtonIcon()
            }
        })

        add(controlButton, BorderLayout.CENTER)
    }

    fun updateControlButtonIcon() {
        val size = controlButton.size
        val width = size.width
        val height = size.height

        if(width <= 0 || height <= 0) return

        controlButton.icon = EOCVSimIconLibrary.icoNotStarted.scaleToFit(width, height)
    }

}