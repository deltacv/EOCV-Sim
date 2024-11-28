package com.github.serivesmejia.eocvsim.gui.dialog

import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.Visualizer
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel

class CreateWorkspace(
    val parent: JFrame,
    val visualizer: Visualizer
) {

    val dialog = JDialog(parent)

    init {
        dialog.isModal = true
        dialog.title = "Create a Workspace"

        dialog.contentPane.layout = GridBagLayout()

        val text = """
            <b>Start visualizing your OpenCV pipelines now</b><br><br>
            A workspace contains the <b>Java source code</b> of the pipelines you<br>
            wish to run and build. Opt for the coding environment of your choice.
        """.trimIndent()

        dialog.contentPane.add(JLabel("<html><div style='text-align: center;'>$text</div></html>").apply {
            font = font.deriveFont(14f)
        }, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
        })

        val buttonsPanel = JPanel().apply {
            layout = GridLayout(1, 2, 10, 10)
        }

        buttonsPanel.add(JButton(
            "<html><div style='text-align: center;'>Create a VS Code Workspace</div></html>",
            EOCVSimIconLibrary.icoVsCode.scaleToFit(60, 60)
        ).apply {
            font = font.deriveFont(14f)
            horizontalTextPosition = JButton.CENTER
            verticalTextPosition = JButton.BOTTOM

            addActionListener {
                dialog.dispose()

                JOptionPane.showConfirmDialog(
                    this@CreateWorkspace.parent,
                    "This feature prefers that you have Visual Studio Code already installed. You can opt to use IntelliJ IDEA instead, but you will have to do so manually.\n\n",
                    "VS Code Workspace",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
                )

                visualizer.createVSCodeWorkspace()
            }
        })

        buttonsPanel.add(JButton(
            "<html><div style='text-align: center;'>Open an Existing Folder</div></html>",
            EOCVSimIconLibrary.icoFolder.scaleToFit(60, 60)
        ).apply {
            font = font.deriveFont(14f)
            horizontalTextPosition = JButton.CENTER
            verticalTextPosition = JButton.BOTTOM

            addActionListener {
                dialog.dispose()
                visualizer.selectPipelinesWorkspace()
            }
        })

        dialog.contentPane.add(buttonsPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            weightx = 1.0
            weighty = 1.0
        })

        dialog.size = Dimension(520, 250)
        dialog.isResizable = false

        dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }

}