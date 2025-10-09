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
import com.github.serivesmejia.eocvsim.gui.component.input.FileSelector
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields
import com.github.serivesmejia.eocvsim.input.source.VideoSource
import com.github.serivesmejia.eocvsim.util.FileFilters
import io.github.deltacv.vision.external.util.CvUtil
import org.opencv.core.Size
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.roundToInt

class CreateVideoSource(
    parent: JFrame,
    private val eocvsim: EOCVSim,
    initialFile: File? = null
) {
    private val dialog: JDialog = JDialog(parent)

    private lateinit var nameTextField: JTextField
    private lateinit var fileSelector: FileSelector
    private lateinit var sizeFields: SizeFields
    private lateinit var createButton: JButton

    private var selectedValidVideo = false

    init {
        eocvsim.visualizer.childDialogs.add(dialog)

        // Main content panel
        val contentsPanel = JPanel(GridLayout(4, 1)).apply {
            border = BorderFactory.createEmptyBorder(15, 0, 0, 0)
        }

        // File selector
        fileSelector = FileSelector(18, FileFilters.videoMediaFilter).apply {
            onFileSelect.doPersistent { videoFileSelected(lastSelectedFile ?: return@doPersistent) }
        }
        initialFile?.let { file ->
            SwingUtilities.invokeLater { fileSelector.lastSelectedFile = file }
        }
        contentsPanel.add(fileSelector)

        // Size fields
        sizeFields = SizeFields().apply {
            onChange.doPersistent(::updateCreateButton)
        }
        contentsPanel.add(sizeFields)

        // Name input panel
        val namePanel = JPanel(FlowLayout()).apply {
            val sourceCount = eocvsim.inputSourceManager.sources.size + 1
            nameTextField = JTextField("VideoSource-$sourceCount", 15)
            add(JLabel("Source name: "))
            add(nameTextField)
        }
        contentsPanel.add(namePanel)

        // Bottom buttons panel
        val buttonsPanel = JPanel(FlowLayout()).apply {
            createButton = JButton("Create").apply { isEnabled = false }
            add(createButton)
            add(JButton("Cancel").apply { addActionListener { close() } })
        }
        contentsPanel.add(buttonsPanel)

        // Add listeners
        nameTextField.onChange(::updateCreateButton)
        createButton.addActionListener {
            createSource(nameTextField.text, fileSelector.lastSelectedFile?.absolutePath ?: return@addActionListener, sizeFields.currentSize)
            close()
        }

        // Configure and show dialog
        dialog.apply {
            contentPane.add(contentsPanel, BorderLayout.CENTER)
            title = "Create video source"
            isModal = true
            isAlwaysOnTop = true
            isResizable = false
            pack()
            setLocationRelativeTo(null) // Center on screen
            isVisible = true
        }
    }

    private fun videoFileSelected(file: File) {
        val videoMat = CvUtil.readOnceFromVideo(file.absolutePath)
        try {
            // Check if the video frame is valid using a safe let-block
            videoMat?.takeIf { !it.empty() }?.let { mat ->
                // Suggest a name from the filename if it's not blank
                val fileName = file.nameWithoutExtension
                if (fileName.isNotBlank()) {
                    nameTextField.text = eocvsim.inputSourceManager.tryName(fileName)
                }

                // Calculate a fitted size and update the fields
                val newSize = CvUtil.scaleToFit(mat.size(), EOCVSim.DEFAULT_EOCV_SIZE)
                sizeFields.widthTextField.text = newSize.width.roundToInt().toString()
                sizeFields.heightTextField.text = newSize.height.roundToInt().toString()

                selectedValidVideo = true
            } ?: run {
                fileSelector.dirTextField.text = "Unable to load selected file."
                selectedValidVideo = false
            }
        } finally {
            // Always release the matrix to prevent memory leaks
            videoMat?.release()
        }
        updateCreateButton()
    }

    private fun close() {
        dialog.isVisible = false
        dialog.dispose()
    }

    private fun createSource(sourceName: String, videoPath: String, size: Size) {
        eocvsim.onMainUpdate.doOnce {
            eocvsim.inputSourceManager.addInputSource(
                sourceName,
                VideoSource(videoPath, size),
                true
            )
        }
    }

    private fun updateCreateButton() {
        val isNameValid = nameTextField.text.isNotBlank() &&
                !eocvsim.inputSourceManager.isNameOnUse(nameTextField.text)

        createButton.isEnabled = isNameValid && sizeFields.valid && selectedValidVideo
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