/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.dialog.source

import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.dialog.iama.IAmAFirstRobotics
import com.github.serivesmejia.eocvsim.gui.dialog.iama.IAmAGeneralPublic
import com.github.serivesmejia.eocvsim.gui.dialog.iama.IAmAPaperVision
import com.github.serivesmejia.eocvsim.input.SourceType
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CreateSourceEx : KoinComponent {

    private val visualizer: Visualizer by inject()
    private val dialogFactory: DialogFactory by inject()

    val dialog = JDialog(visualizer.frame)


    init {
        dialog.isModal = true
        dialog.title = "Create Input Source"

        dialog.contentPane.layout = GridBagLayout()
        val buttonsPanel = JPanel().apply {
            layout = GridLayout(1, 4, 10, 10)
        }

        buttonsPanel.add(JButton(
            "<html><div style='text-align: center;'>Camera</div></html>",
            EOCVSimIconLibrary.icoCam.resized(50, 50)
        ).apply {
            font = font.deriveFont(14f)
            horizontalTextPosition = JButton.CENTER
            verticalTextPosition = JButton.BOTTOM

            addActionListener {
                dialog.dispose()
                dialogFactory.createSourceDialog(SourceType.CAMERA)
            }
        })

        buttonsPanel.add(JButton(
            "<html><div style='text-align: center;'>Image</div></html>",
            EOCVSimIconLibrary.icoImg.resized(50, 50)
        ).apply {
            font = font.deriveFont(14f)
            horizontalTextPosition = JButton.CENTER
            verticalTextPosition = JButton.BOTTOM

            addActionListener {
                dialog.dispose()
                dialogFactory.createSourceDialog(SourceType.IMAGE)
            }
        })

        buttonsPanel.add(JButton(
            "<html><div style='text-align: center;'>Video</div></html>",
            EOCVSimIconLibrary.icoVid.resized(50, 50)
        ).apply {
            font = font.deriveFont(14f)
            horizontalTextPosition = JButton.CENTER
            verticalTextPosition = JButton.BOTTOM

            addActionListener {
                dialog.dispose()
                dialogFactory.createSourceDialog(SourceType.VIDEO)
            }
        })

        buttonsPanel.add(JButton(
            "<html><div style='text-align: center;'>HTTP Stream</div></html>",
            EOCVSimIconLibrary.icoStream.resized(50, 50)
        ).apply {
            font = font.deriveFont(14f)
            horizontalTextPosition = JButton.CENTER
            verticalTextPosition = JButton.BOTTOM

            addActionListener {
                dialog.dispose()
                dialogFactory.createSourceDialog(SourceType.HTTP)
            }
        })

        buttonsPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

        dialog.contentPane.add(buttonsPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
        })

        dialog.minimumSize = Dimension(520, 160)
        dialog.isResizable = false

        dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        dialog.setLocationRelativeTo(null)

        dialog.isVisible = true
    }

}
