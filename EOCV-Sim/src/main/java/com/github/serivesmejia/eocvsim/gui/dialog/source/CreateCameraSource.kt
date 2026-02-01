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
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox
import com.github.serivesmejia.eocvsim.gui.util.WebcamDriver
import com.github.serivesmejia.eocvsim.input.source.CameraSource
import io.github.deltacv.steve.Webcam
import io.github.deltacv.steve.WebcamRotation
import io.github.deltacv.steve.commonResolutions
import io.github.deltacv.steve.opencv.OpenCvWebcam
import io.github.deltacv.steve.opencv.OpenCvWebcamBackend
import io.github.deltacv.steve.openpnp.OpenPnpBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import org.opencv.core.Size
import java.awt.*
import javax.swing.*

class CreateCameraSource(
    parent: JFrame,
    private val eocvSim: EOCVSim
) {

    companion object {
        const val VISIBLE_CHARACTERS_COMBO_BOX = 22
        private val sizes = mutableMapOf<String, List<Size>>()
    }

    val createCameraSource = JDialog(parent)
    private val statusLabel = JLabel("", SwingConstants.CENTER)

    private val camerasComboBox = JComboBox<String>()
    private val dimensionsComboBox = JComboBox<String>()
    private val rotationComboBox = EnumComboBox(
        "",
        WebcamRotation::class.java,
        WebcamRotation.entries.toTypedArray(), // Correction: Using .entries
        WebcamRotation::displayName
    ) { WebcamRotation.fromDisplayName(it) ?: WebcamRotation.UPRIGHT } // Correction: Lambda moved out
    private val nameTextField = JTextField(15)
    private val createButton = JButton()
    private var wasCancelled = false

    private var webcams = listOf<Webcam>()
    private val indexes = mutableMapOf<String, Int>()
    private var usingOpenCvDiscovery = false

    private var state = State.INITIAL
    private enum class State { INITIAL, CLICKED_TEST, TEST_SUCCESSFUL, TEST_FAILED, NO_WEBCAMS, UNSUPPORTED }

    init {
        eocvSim.visualizer.childDialogs.add(createCameraSource)

        // Force preferred driver fallback
        if (eocvSim.config.preferredWebcamDriver == WebcamDriver.OpenIMAJ)
            eocvSim.config.preferredWebcamDriver = WebcamDriver.OpenPnp

        val preferredDriver = eocvSim.config.preferredWebcamDriver

        when (preferredDriver) {
            WebcamDriver.OpenPnp -> {
                Webcam.backend = OpenPnpBackend
                webcams = try {
                    Webcam.availableWebcams
                } catch (t: Throwable) {
                    t.printStackTrace()
                    emptyList()
                }
            }

            WebcamDriver.OpenCV -> {
                webcams = emptyList()
                Webcam.backend = OpenCvWebcamBackend
                usingOpenCvDiscovery = true
            }

            else -> webcams = emptyList()
        }

        createCameraSource.apply {
            isModal = true
            title = "Create camera source"
        }

        // Build UI
        val contentsPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(7, 0, 0, 7)
        }

        // Camera combo box
        val idLabel = JLabel("Available cameras: ", JLabel.RIGHT)
        if (webcams.isEmpty()) {
            camerasComboBox.addItem("No Cameras Detected")
            state = State.NO_WEBCAMS
        } else {
            webcams.forEachIndexed { index, webcam ->
                val name = webcam.name.let {
                    if (it.length > VISIBLE_CHARACTERS_COMBO_BOX) it.take(VISIBLE_CHARACTERS_COMBO_BOX) + "..."
                    else it
                }

                camerasComboBox.addItem(name)
                indexes[name] = index

                if (!sizes.containsKey(name)) {
                    val resolutions = if (webcam is OpenCvWebcam) {
                        commonResolutions
                    } else webcam.supportedResolutions.ifEmpty {
                        println("Webcam $name has no resolutions, skipping")
                        return@forEachIndexed
                    }

                    sizes[name] = resolutions
                }
            }

            SwingUtilities.invokeLater { camerasComboBox.selectedIndex = 0 }
        }

        val fieldsPanel = JPanel(GridBagLayout()).apply {
            add(idLabel, gbc)
            gbc.gridx = 1; add(camerasComboBox, gbc)

            // Name field
            gbc.gridx = 0; gbc.gridy = 1
            add(JLabel("Source name: ", JLabel.RIGHT), gbc)
            gbc.gridx = 1
            nameTextField.text = "CameraSource-${eocvSim.inputSourceManager.sources.size + 1}"
            add(nameTextField, gbc)

            // Suggested resolution
            gbc.gridx = 0; gbc.gridy = 2
            add(JLabel("Suggested resolutions: ", JLabel.RIGHT), gbc)
            gbc.gridx = 1
            add(dimensionsComboBox, gbc)

            // Rotation
            gbc.gridx = 0; gbc.gridy = 3
            add(JLabel("Camera rotation: ", JLabel.RIGHT), gbc)
            gbc.gridx = 1
            add(rotationComboBox.comboBox, gbc)
        }

        gbc.gridx = 0; gbc.gridy = 0
        contentsPanel.add(fieldsPanel, gbc)

        gbc.gridy = 1
        contentsPanel.add(statusLabel, gbc)

        // Bottom buttons
        val bottomPanel = JPanel(GridBagLayout())
        val gbcBtts = GridBagConstraints().apply { insets = Insets(0, 0, 0, 10) }
        bottomPanel.add(createButton, gbcBtts)
        gbcBtts.gridx = 1; gbcBtts.insets = Insets(0, 0, 0, 0)
        val cancelButton = JButton("Cancel")
        bottomPanel.add(cancelButton, gbcBtts)

        gbc.insets = Insets(10, 0, 0, 0)
        gbc.gridx = 0; gbc.gridy = 2
        contentsPanel.add(bottomPanel, gbc)

        contentsPanel.border = BorderFactory.createEmptyBorder(8, 15, 15, 0)
        createCameraSource.contentPane.add(contentsPanel, BorderLayout.CENTER)

        // Event listeners
        createButton.addActionListener { onCreateButton() }

        camerasComboBox.addActionListener { onCameraSelectionChanged() }

        nameTextField.document.addDocumentListener(SimpleDocumentListener { updateCreateButton() })
        cancelButton.addActionListener { wasCancelled = true; close() }

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
        if (state == State.TEST_SUCCESSFUL) {
            val webcam = webcams[getSelectedIndex()]
            val dim = sizes[camerasComboBox.selectedItem]!![dimensionsComboBox.selectedIndex]
            val rotation = rotationComboBox.selectedEnum ?: WebcamRotation.UPRIGHT // Correction: Handle null case

            if (usingOpenCvDiscovery) {
                val index = if (webcam is OpenCvWebcam) webcam.index else camerasComboBox.selectedIndex
                createSource(nameTextField.text, index, dim, rotation)
            } else {
                createSource(nameTextField.text, webcam.name, dim, rotation)
            }
            close()
        } else {
            state = State.CLICKED_TEST
            updateState()
            CoroutineScope(Dispatchers.IO).launch {
                val webcam = webcams[getSelectedIndex()]
                val dim = sizes[camerasComboBox.selectedItem]!![dimensionsComboBox.selectedIndex]

                webcam.resolution = dim
                val success = testCamera(webcam)

                SwingUtilities.invokeLater {
                    if (!wasCancelled) {
                        state = if (success) State.TEST_SUCCESSFUL else State.TEST_FAILED
                        updateState()
                    }
                }
            }
        }
    }

    private fun onCameraSelectionChanged() {
        val webcam = webcams.getOrNull(getSelectedIndex()) ?: run {
            state = State.UNSUPPORTED
            updateState()
            return
        }

        nameTextField.text = eocvSim.inputSourceManager.tryName(webcam.name)
        dimensionsComboBox.removeAllItems()

        CoroutineScope(Dispatchers.IO).launch {
            val webcamSizes = sizes[camerasComboBox.selectedItem] ?: return@launch
            SwingUtilities.invokeLater {
                dimensionsComboBox.removeAllItems()
                webcamSizes.forEach { dimensionsComboBox.addItem("${it.width.toInt()}x${it.height.toInt()}") }
                state = State.INITIAL
                updateCreateButton()
                updateState()
            }
        }
    }

    private fun testCamera(webcam: Webcam): Boolean {
        webcam.open()
        var success = webcam.isOpen
        if (success) {
            val m = Mat()
            try { webcam.read(m) } catch (_: Exception) { success = false }
            m.release()
            webcam.close()
        }
        return success
    }

    private fun updateState() {
        when (state) {
            State.INITIAL -> {
                statusLabel.text = "Click \"test\" to test camera."
                createButton.text = "Test"
                setInteractables(true)
            }
            State.CLICKED_TEST -> {
                statusLabel.text = "Trying to open camera, please wait..."
                setInteractables(false)
            }
            State.TEST_SUCCESSFUL -> {
                statusLabel.text = "Camera was opened successfully."
                createButton.text = "Create"
                setInteractables(true)
            }
            State.TEST_FAILED -> {
                statusLabel.text = "Failed to open camera, try another one."
                createButton.text = "Test"
                setInteractables(true)
            }
            State.NO_WEBCAMS -> {
                statusLabel.text = "No cameras detected."
                createButton.text = "Test"
                nameTextField.text = ""
                setInteractables(false)
            }
            State.UNSUPPORTED -> {
                statusLabel.text = "This camera is currently unavailable."
                createButton.text = "Test"
                nameTextField.text = ""
                setInteractables(false)
                camerasComboBox.isEnabled = true
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

    private fun createSource(name: String, camName: String, size: Size, rotation: WebcamRotation) {
        eocvSim.onMainUpdate.once { eocvSim.inputSourceManager.addInputSource(name, CameraSource(camName, size, rotation), true) }
    }

    private fun createSource(name: String, camIndex: Int, size: Size, rotation: WebcamRotation) {
        eocvSim.onMainUpdate.once { eocvSim.inputSourceManager.addInputSource(name, CameraSource(camIndex, size, rotation), true) }
    }

    private fun updateCreateButton() {
        createButton.isEnabled = nameTextField.text.trim().isNotEmpty() &&
                !eocvSim.inputSourceManager.isNameOnUse(nameTextField.text)
    }

    private fun getSelectedIndex() = indexes[camerasComboBox.selectedItem] ?: 0

    // Simple helper for DocumentListener in Kotlin
    private class SimpleDocumentListener(val onChange: () -> Unit) : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent) = onChange()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent) = onChange()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent) = onChange()
    }
}