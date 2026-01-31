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
 */

package com.github.serivesmejia.eocvsim.gui.dialog.source

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.input.FileSelector
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import io.github.deltacv.vision.external.util.CvUtil
import com.github.serivesmejia.eocvsim.util.FileFilters
import com.github.serivesmejia.eocvsim.util.StrUtil
import org.opencv.core.Size
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.roundToInt

class CreateImageSource(
    parent: JFrame,
    private val eocvSim: EOCVSim,
    private val initialFile: File?
) {

    val createImageSource = JDialog(parent)

    val nameTextField = JTextField()
    val sizeFieldsInput = SizeFields()
    val imageFileSelector = FileSelector(18, FileFilters.imagesFilter)

    val createButton = JButton("Create")
    private var selectedValidImage = false

    init {
        eocvSim.visualizer.childDialogs.add(createImageSource)
        initCreateImageSource()
    }

    private fun initCreateImageSource() {
        val contentsPanel = JPanel(GridLayout(4, 1))

        // File select part
        imageFileSelector.onFileSelect.doPersistent {
            imageFileSelector.lastSelectedFile?.let { file ->
                imageFileSelected(file)
            }
        }

        initialFile?.let { file ->
            SwingUtilities.invokeLater {
                imageFileSelector.lastSelectedFile = file
            }
        }

        contentsPanel.add(imageFileSelector)

        // Size fields
        sizeFieldsInput.onChange.doPersistent { updateCreateBtt() }
        contentsPanel.add(sizeFieldsInput)
        contentsPanel.border = BorderFactory.createEmptyBorder(15, 0, 0, 0)

        // Name part
        val namePanel = JPanel(FlowLayout())
        val nameLabel = JLabel("Source name: ").apply { horizontalAlignment = JLabel.LEFT }

        nameTextField.text = "ImageSource-${eocvSim.inputSourceManager.sources.size + 1}"
        namePanel.add(nameLabel)
        namePanel.add(nameTextField)

        contentsPanel.add(namePanel)

        // Bottom buttons
        val buttonsPanel = JPanel(FlowLayout())
        createButton.isEnabled = selectedValidImage

        buttonsPanel.add(createButton)

        val cancelButton = JButton("Cancel")
        buttonsPanel.add(cancelButton)

        contentsPanel.add(buttonsPanel)

        // Add contents
        createImageSource.contentPane.add(contentsPanel, BorderLayout.CENTER)

        // Events
        nameTextField.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = updateCreateBtt()
            override fun removeUpdate(e: DocumentEvent?) = updateCreateBtt()
            override fun insertUpdate(e: DocumentEvent?) = updateCreateBtt()
        })

        createButton.addActionListener {
            val file = imageFileSelector.lastSelectedFile
            if (file != null) {
                createSource(
                    nameTextField.text,
                    file.absolutePath,
                    sizeFieldsInput.currentSize
                )
            }
            close()
        }

        cancelButton.addActionListener { close() }

        createImageSource.pack()
        createImageSource.title = "Create image source"
        createImageSource.setSize(370, 200)
        createImageSource.setLocationRelativeTo(null)
        createImageSource.isAlwaysOnTop = true
        createImageSource.isResizable = false
        createImageSource.isVisible = true
    }

    private fun imageFileSelected(f: File) {
        val fileAbsPath = f.absolutePath

        if (CvUtil.checkImageValid(fileAbsPath)) {
            val fileName = StrUtil.getFileBaseName(f.name)
            if (fileName.isNotBlank()) {
                nameTextField.text = eocvSim.inputSourceManager.tryName(fileName)
            }

            val size = CvUtil.scaleToFit(CvUtil.getImageSize(fileAbsPath), EOCVSim.DEFAULT_EOCV_SIZE)
            sizeFieldsInput.widthTextField.text = size.width.roundToInt().toString()
            sizeFieldsInput.heightTextField.text = size.height.roundToInt().toString()

            selectedValidImage = true
        } else {
            imageFileSelector.dirTextField.text = "Unable to load selected file."
            selectedValidImage = false
        }

        updateCreateBtt()
    }

    private fun close() {
        createImageSource.isVisible = false
        createImageSource.dispose()
    }

    private fun createSource(sourceName: String, imgPath: String, size: Size) {
        eocvSim.onMainUpdate.once {
            eocvSim.inputSourceManager.addInputSource(
                sourceName,
                ImageSource(imgPath, size),
                false
            )
        }
    }

    private fun updateCreateBtt() {
        createButton.isEnabled =
            nameTextField.text.trim().isNotEmpty() &&
                    sizeFieldsInput.valid &&
                    selectedValidImage &&
                    !eocvSim.inputSourceManager.isNameOnUse(nameTextField.text)
    }
}
