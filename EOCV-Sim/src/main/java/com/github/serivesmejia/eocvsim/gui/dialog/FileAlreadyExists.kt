/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
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

