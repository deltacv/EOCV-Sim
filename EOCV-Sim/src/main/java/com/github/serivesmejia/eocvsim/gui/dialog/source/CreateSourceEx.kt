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

class CreateSourceEx(
    val parent: JFrame,
    val visualizer: Visualizer
) {

    val dialog = JDialog(parent)

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
                DialogFactory.createSourceDialog(visualizer.eocvSim, SourceType.CAMERA)
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
                DialogFactory.createSourceDialog(visualizer.eocvSim, SourceType.IMAGE)
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
                DialogFactory.createSourceDialog(visualizer.eocvSim, SourceType.VIDEO)
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
                DialogFactory.createSourceDialog(visualizer.eocvSim, SourceType.HTTP)
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