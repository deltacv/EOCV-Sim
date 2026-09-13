/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.dialog.source

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.component.input.FileSelector
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import org.deltacv.vision.external.util.CvUtil
import com.github.serivesmejia.eocvsim.util.FileFilters
import com.github.serivesmejia.eocvsim.util.StrUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.opencv.core.Size
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.roundToInt

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class CreateImageSource(
    private val initialFile: File?
) : KoinComponent {

    private val visualizer: Visualizer by inject()
    private val inputSourceManager: InputSourceManager by inject()
    private val onMainLoop: EventHandler by inject(named("onMainLoop"))

    val createImageSource = JDialog(visualizer.frame)

    val nameTextField = JTextField()
    val sizeFieldsInput = SizeFields()
    val imageFileSelector = FileSelector(18, FileFilters.imagesFilter)

    val createButton = JButton("Create")
    private var selectedValidImage = false

    init {
        initCreateImageSource()
    }

    private fun initCreateImageSource() {
        val contentsPanel = JPanel(GridLayout(4, 1))

        // File select part
        imageFileSelector.onFileSelect.attach {
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
        sizeFieldsInput.onChange.attach { updateCreateBtt() }
        contentsPanel.add(sizeFieldsInput)
        contentsPanel.border = BorderFactory.createEmptyBorder(15, 0, 0, 0)

        // Name part
        val namePanel = JPanel(FlowLayout())
        val nameLabel = JLabel("Source Name: ").apply { horizontalAlignment = JLabel.LEFT }

        nameTextField.text = "ImageSource-${inputSourceManager.sources.size + 1}"
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
                nameTextField.text = inputSourceManager.tryName(fileName)
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
        onMainLoop.once {
            inputSourceManager.addInputSource(
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
                    !inputSourceManager.isNameInUse(nameTextField.text)
    }
}

