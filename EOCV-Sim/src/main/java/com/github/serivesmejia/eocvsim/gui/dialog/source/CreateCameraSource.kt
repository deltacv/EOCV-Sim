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

import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.input.source.CameraSource
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.opencv.core.Size
import org.wpilib.vision.camera.UsbCamera
import org.wpilib.vision.camera.UsbCameraInfo
import java.awt.*
import javax.swing.*

class CreateCameraSource : KoinComponent {

    private val inputSourceManager: InputSourceManager by inject()
    private val onMainUpdate: EventHandler by inject(named("onMainLoop"))
    private val visualizer: Visualizer by inject()

    companion object {
        const val VISIBLE_CHARACTERS_COMBO_BOX = 22

        // common resolutions to offer since cscore doesn't enumerate supported modes
        // until the camera is opened
        private val commonResolutions = listOf(
            Size(640.0, 480.0),
            Size(1280.0, 720.0),
            Size(1920.0, 1080.0),
            Size(320.0, 240.0),
            Size(160.0, 120.0)
        )
    }

    val createCameraSource = JDialog(visualizer.frame)

    private val camerasComboBox = JComboBox<String>()
    private val dimensionsComboBox = JComboBox<String>()
    private val nameTextField = JTextField(15)
    private val createButton = JButton("Create")

    private var cameraInfos = emptyArray<UsbCameraInfo>()

    private var state = State.INITIAL
    private enum class State { INITIAL, NO_WEBCAMS }

    init {
        cameraInfos = try {
            UsbCamera.enumerateUsbCameras()
        } catch (t: Throwable) {
            t.printStackTrace()
            emptyArray()
        }

        createCameraSource.apply {
            isModal = true
            title = "Create camera source"
        }

        val contentsPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(7, 0, 0, 7)
        }

        if (cameraInfos.isEmpty()) {
            camerasComboBox.addItem("No Cameras Detected")
            state = State.NO_WEBCAMS
        } else {
            cameraInfos.forEach { info ->
                val name = info.name.let {
                    if (it.length > VISIBLE_CHARACTERS_COMBO_BOX) it.take(VISIBLE_CHARACTERS_COMBO_BOX) + "..."
                    else it
                }
                camerasComboBox.addItem(name)
            }

            commonResolutions.forEach { res ->
                dimensionsComboBox.addItem("${res.width.toInt()}x${res.height.toInt()}")
            }

            SwingUtilities.invokeLater { camerasComboBox.selectedIndex = 0 }
        }

        val fieldsPanel = JPanel(GridBagLayout()).apply {
            gbc.gridx = 0; gbc.gridy = 0
            add(JLabel("Available cameras: ", JLabel.RIGHT), gbc)
            gbc.gridx = 1
            add(camerasComboBox, gbc)

            gbc.gridx = 0; gbc.gridy = 1
            add(JLabel("Source name: ", JLabel.RIGHT), gbc)
            gbc.gridx = 1
            nameTextField.text = "CameraSource-${inputSourceManager.sources.size + 1}"
            add(nameTextField, gbc)

            gbc.gridx = 0; gbc.gridy = 2
            add(JLabel("Resolution: ", JLabel.RIGHT), gbc)
            gbc.gridx = 1
            add(dimensionsComboBox, gbc)
        }

        gbc.gridx = 0; gbc.gridy = 0
        contentsPanel.add(fieldsPanel, gbc)

        val bottomPanel = JPanel(GridBagLayout())
        val gbcBtts = GridBagConstraints().apply { insets = Insets(0, 0, 0, 10) }
        bottomPanel.add(createButton, gbcBtts)
        gbcBtts.gridx = 1; gbcBtts.insets = Insets(0, 0, 0, 0)
        val cancelButton = JButton("Cancel")
        bottomPanel.add(cancelButton, gbcBtts)

        gbc.insets = Insets(10, 0, 0, 0)
        gbc.gridx = 0; gbc.gridy = 1
        contentsPanel.add(bottomPanel, gbc)

        contentsPanel.border = BorderFactory.createEmptyBorder(8, 15, 15, 0)
        createCameraSource.contentPane.add(contentsPanel, BorderLayout.CENTER)

        createButton.addActionListener { onCreateButton() }
        camerasComboBox.addActionListener { onCameraSelectionChanged() }
        nameTextField.document.addDocumentListener(SimpleDocumentListener { updateCreateButton() })
        cancelButton.addActionListener { close() }

        updateState()

        createCameraSource.apply {
            pack()
            isResizable = false
            isAlwaysOnTop = true
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private fun onCreateButton() {
        val index = camerasComboBox.selectedIndex
        val info = cameraInfos.getOrNull(index) ?: return
        val size = commonResolutions.getOrNull(dimensionsComboBox.selectedIndex) ?: Size(640.0, 480.0)

        onMainUpdate.once {
            inputSourceManager.addInputSource(
                nameTextField.text,
                CameraSource(info.name, size),
                true
            )
        }
        close()
    }

    private fun onCameraSelectionChanged() {
        val info = cameraInfos.getOrNull(camerasComboBox.selectedIndex) ?: return
        nameTextField.text = inputSourceManager.tryName(info.name)
        updateCreateButton()
    }

    private fun updateState() {
        when (state) {
            State.INITIAL -> setInteractables(true)
            State.NO_WEBCAMS -> {
                nameTextField.text = ""
                setInteractables(false)
            }
        }
    }

    private fun setInteractables(enabled: Boolean) {
        createButton.isEnabled = enabled
        nameTextField.isEnabled = enabled
        camerasComboBox.isEnabled = enabled
        dimensionsComboBox.isEnabled = enabled
    }

    private fun close() {
        createCameraSource.isVisible = false
        createCameraSource.dispose()
    }

    private fun updateCreateButton() {
        createButton.isEnabled = nameTextField.text.trim().isNotEmpty() &&
                !inputSourceManager.isNameInUse(nameTextField.text)
    }

    private class SimpleDocumentListener(val onChange: () -> Unit) : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent) = onChange()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent) = onChange()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent) = onChange()
    }
}