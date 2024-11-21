package com.github.serivesmejia.eocvsim.gui.dialog.iama

import com.github.serivesmejia.eocvsim.gui.Visualizer
import java.awt.Desktop
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class IAmAFirstRobotics(
    parent: JFrame,
    visualizer: Visualizer
) {

    private val dialog = JDialog(parent).apply {
        isModal = true
        title = "Welcome !"
        contentPane.layout = GridBagLayout()

        isResizable = false
        setSize(520, 250)
        setLocationRelativeTo(null)
    }

    init {
        // Create the contents panel
        val contentsPanel = JPanel(GridBagLayout())

        contentsPanel.border = BorderFactory.createEmptyBorder(10, 0, 0, 0)

        val contentText = """
            <b>Hey FTC teams! EOCV-Sim helps you develop EasyOpenCV pipelines<br>
            without needing to always have the robot hardware at hand.</b><br><br>
            Work with code identical to the FtcRobotController, iterate with tools like the<br>
            variable tuner, and quickly visualize your code changes using workspaces.<br><br>
            <b>Click on "Open Docs"</b> to learn more about EOCV-Sim and workspaces.<br>
            Come back and <b>click on "Next"</b> to hear about PaperVision...<br>
        """.trimIndent()

        // Add a descriptive JLabel to the contents panel
        contentsPanel.add(
            JLabel("<html><div style='text-align: center;'>$contentText</div></html>").apply {
                font = font.deriveFont(14f)
                horizontalAlignment = JLabel.CENTER
            },
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
        )

        // Create the buttons panel
        val buttonsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)

            add(Box.createHorizontalGlue()) // Align the button to the right

            add(JButton("Open Docs").apply {
                addActionListener {
                    // open webpage
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI("https://docs.deltacv.org/eocv-sim"));
                    }
                }
            })

            add(Box.createHorizontalStrut(10)) // Add some space between the buttons

            add(JButton("Next").apply {
                addActionListener {
                    dialog.dispose() // Close the dialog on click
                    IAmAPaperVision(parent, visualizer)
                }
            })

            border = BorderFactory.createEmptyBorder(0, 10, 10, 10)
        }

        // Add the contents panel to the dialog
        dialog.contentPane.add(
            contentsPanel,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
        )

        // Add the buttons panel to the dialog
        dialog.contentPane.add(
            buttonsPanel,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                weightx = 1.0
                weighty = 0.0
                fill = GridBagConstraints.HORIZONTAL
            }
        )

        // Make the dialog visible
        dialog.isVisible = true
    }
}