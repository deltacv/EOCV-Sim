/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.gui.dialog

import com.github.serivesmejia.eocvsim.gui.Visualizer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.FlowLayout
import javax.swing.*

class FileAlreadyExists : KoinComponent {

    private val visualizer: Visualizer by inject()

    var fileAlreadyExists = JDialog(visualizer.frame)
    var contentsPanel = JPanel()
    var userChoice: UserChoice = UserChoice.NA

    fun run(): UserChoice {
        fileAlreadyExists.isModal = true
        fileAlreadyExists.title = "Warning"

        contentsPanel.layout = BoxLayout(contentsPanel, BoxLayout.Y_AXIS)

        val alreadyExistsPanel = JPanel(FlowLayout())
        val alreadyExistsLabel = JLabel("File already exists in the selected directory")
        alreadyExistsPanel.add(alreadyExistsLabel)

        contentsPanel.add(alreadyExistsPanel)

        val replaceCancelPanel = JPanel(FlowLayout())

        val replaceButton = JButton("Replace")
        replaceCancelPanel.add(replaceButton)

        replaceButton.addActionListener {
            userChoice = UserChoice.REPLACE
            fileAlreadyExists.isVisible = false
        }

        val cancelButton = JButton("Cancel")
        replaceCancelPanel.add(cancelButton)

        cancelButton.addActionListener {
            userChoice = UserChoice.CANCEL
            fileAlreadyExists.isVisible = false
        }

        contentsPanel.add(replaceCancelPanel)

        fileAlreadyExists.add(contentsPanel)
        fileAlreadyExists.pack()
        fileAlreadyExists.isResizable = false
        fileAlreadyExists.setLocationRelativeTo(null)
        fileAlreadyExists.isVisible = true

        return userChoice
    }

    enum class UserChoice { NA, REPLACE, CANCEL }
}
