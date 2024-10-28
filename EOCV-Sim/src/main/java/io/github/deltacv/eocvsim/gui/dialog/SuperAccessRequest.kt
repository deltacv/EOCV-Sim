/*
 * Copyright (c) 2024 Sebastian Erives
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

package io.github.deltacv.eocvsim.gui.dialog

import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme
import com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme
import com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.Icons
import com.github.serivesmejia.eocvsim.gui.Icons.getImage
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.*

class SuperAccessRequest(sourceName: String, reason: String, val callback: (Boolean) -> Unit) {

    init {
        FlatArcDarkIJTheme.setup()

        val panel = JPanel(BorderLayout())
        val frame = JDialog()

        frame.title = "EOCV-Sim SuperAccess Request"

        // Create UI components
        val titleLabel = JLabel("SuperAccess request from $sourceName", JLabel.CENTER)

        // Set the font of the JLabel to bold
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 21f)

        // Text Area for Reason Input
        val reasonTextArea = JTextPane().apply {
            isEditable = false
            contentType = "text/html"

            font = font.deriveFont(18f)

            var formattedReason = ""

            for(line in reason.split("<br>")) {
                formattedReason += wrapText(line, 80) + "<br>"
            }

            text = formattedReason
        }
        val scrollPane = JScrollPane(reasonTextArea)

        // Create a panel for buttons
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            val acceptButton = JButton("Accept").apply {
                preferredSize = Dimension(100, 30)
                addActionListener {
                    callback(true)

                    frame.isVisible = false
                    frame.dispose()
                }
            }

            val rejectButton = JButton("Reject").apply {
                preferredSize = Dimension(100, 30)
                addActionListener {
                    callback(false)

                    frame.isVisible = false
                    frame.dispose()
                }
            }

            add(Box.createHorizontalGlue()) // Push buttons to the center
            add(acceptButton)
            add(Box.createRigidArea(Dimension(10, 0))) // Space between buttons
            add(rejectButton)
            add(Box.createHorizontalGlue()) // Push buttons to the center
        }

        // Add components to the panel
        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        // Setup the frame
        frame.contentPane = panel
        frame.isAlwaysOnTop = true
        frame.defaultCloseOperation = JDialog.HIDE_ON_CLOSE
        frame.isResizable = false

        frame.addWindowListener(object: WindowListener {
            override fun windowOpened(e: WindowEvent?) {}
            override fun windowClosing(e: WindowEvent?) {}
            override fun windowIconified(e: WindowEvent?) {}
            override fun windowDeiconified(e: WindowEvent?) {}
            override fun windowActivated(e: WindowEvent?) {}
            override fun windowDeactivated(e: WindowEvent?) {}

            override fun windowClosed(e: WindowEvent?) {
            }
        })

        frame.setIconImage(EOCVSimIconLibrary.icoEOCVSim.image)

        frame.pack()

        frame.setLocationRelativeTo(null) // Center the window on the screen
        frame.isVisible = true
    }

}

private fun wrapText(text: String, maxLineLength: Int): String {
    val words = text.split(" ")
    val wrappedLines = mutableListOf<String>()
    var currentLine = StringBuilder()
    val breakTag = "<br>" // HTML break tag

    for (word in words) {
        // Calculate the length of the line with a break tag
        val lineWithWord = if (currentLine.isEmpty()) {
            word
        } else {
            "$currentLine $word"
        }
        val lineLengthWithBreak = lineWithWord.length + (if (currentLine.isEmpty()) 0 else breakTag.length)

        // Check if adding the next word exceeds the maxLineLength
        if (lineLengthWithBreak > maxLineLength) {
            // If it does, add the current line to the list and start a new line
            wrappedLines.add(currentLine.toString())
            currentLine = StringBuilder(word)
        } else {
            // Append the word to the current line
            currentLine.append(" ").append(word)
        }
    }

    // Add the last line if it's not empty
    if (currentLine.isNotEmpty()) {
        wrappedLines.add(currentLine.toString())
    }

    // Join all lines with <br> for HTML
    return wrappedLines.joinToString(breakTag)
}