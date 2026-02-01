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
package com.github.serivesmejia.eocvsim.gui.dialog.source

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.input.source.HttpSource
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CreateHttpSource(parent: JFrame, private val eocvSim: EOCVSim) {

    private val dialog = JDialog(parent)
    private val nameTextField: JTextField
    private val urlField: JTextField
    private val createButton: JButton

    init {
        eocvSim.visualizer.childDialogs.add(dialog)

        // Panel for input fields
        val fieldsPanel = JPanel(GridBagLayout()).apply {
            border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
            val gbc = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(7, 0, 0, 7)
            }

            // URL field
            add(JLabel("URL: "), gbc)
            gbc.gridx = 1
            urlField = JTextField("http://", 15)
            add(urlField, gbc)

            // Name field
            gbc.gridy = 1
            gbc.gridx = 0
            add(JLabel("Source name: "), gbc)
            gbc.gridx = 1
            val sourceCount = eocvSim.inputSourceManager.sources.size + 1
            nameTextField = JTextField("HttpSource-$sourceCount", 15)
            add(nameTextField, gbc)
        }

        // Panel for bottom buttons
        val buttonsPanel = JPanel(FlowLayout())
        createButton = JButton("Create")
        buttonsPanel.add(createButton)
        buttonsPanel.add(JButton("Cancel").apply {
            addActionListener { close() }
        })

        // Main content panel
        val contentsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            add(fieldsPanel)
            add(buttonsPanel)
        }

        // Set up dialog
        dialog.apply {
            contentPane.add(contentsPanel, BorderLayout.CENTER)
            isAlwaysOnTop = true
            title = "Create HTTP Source"
            pack()
            isResizable = false
            setLocationRelativeTo(null) // Center on screen
        }

        // Add event listeners
        nameTextField.onChange { updateCreateButton() }
        urlField.onChange { updateCreateButton() }
        createButton.addActionListener {
            createSource(nameTextField.text, urlField.text)
            close()
        }

        updateCreateButton() // Initial state check
        dialog.isVisible = true
    }

    private fun close() {
        dialog.isVisible = false
        dialog.dispose()
    }

    private fun createSource(sourceName: String, url: String) {
        eocvSim.onMainUpdate.once {
            eocvSim.inputSourceManager.addInputSource(
                sourceName,
                HttpSource(url),
                false
            )
        }
    }

    private fun updateCreateButton() {
        val isNameValid = nameTextField.text.isNotBlank() &&
                !eocvSim.inputSourceManager.isNameOnUse(nameTextField.text)
        val isUrlValid = urlField.text.isNotBlank()
        createButton.isEnabled = isNameValid && isUrlValid
    }

    /**
     * An extension function to simplify adding a DocumentListener for any text change.
     */
    private fun JTextField.onChange(action: () -> Unit) {
        document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = action()
            override fun insertUpdate(e: DocumentEvent?) = action()
            override fun removeUpdate(e: DocumentEvent?) = action()
        })
    }
}