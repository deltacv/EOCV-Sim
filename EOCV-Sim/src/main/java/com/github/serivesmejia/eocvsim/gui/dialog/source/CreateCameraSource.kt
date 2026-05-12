/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.dialog.source

import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.input.source.CameraSource
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.wpilib.vision.camera.UsbCamera
import org.wpilib.vision.camera.UsbCameraInfo
import org.wpilib.vision.camera.VideoMode
import java.awt.*
import javax.swing.*

class CreateCameraSource : KoinComponent {

    private val inputSourceManager: InputSourceManager by inject()
    private val onMainUpdate: EventHandler by inject(named("onMainLoop"))
    private val visualizer: Visualizer by inject()

    private val camerasComboBox = JComboBox<String>()
    private val modesComboBox = JComboBox<VideoMode>()
    private val nameTextField = JTextField(20)
    private val sameCameraCheckBox = JCheckBox("Match to exact port")

    private val createButton = JButton("Create")

    private val dialog = JDialog(visualizer.frame)

    private var cameraInfos: Array<UsbCameraInfo> = emptyArray()
    private var modes: Array<VideoMode> = emptyArray()

    init {
        cameraInfos = try {
            UsbCamera.enumerateUsbCameras()
        } catch (t: Throwable) {
            t.printStackTrace()
            emptyArray()
        }

        dialog.apply {
            isModal = true
            title = "Create Camera Source"
        }

        val root = JPanel(GridBagLayout())

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = Insets(6, 6, 6, 6)
        }

        // ---------------- CAMERA LIST ----------------

        if (cameraInfos.isEmpty()) {
            camerasComboBox.addItem("No Cameras Found")
            createButton.isEnabled = false
        } else {
            cameraInfos.forEach {
                camerasComboBox.addItem(it.name)
            }

            camerasComboBox.selectedIndex = 0

            updateAutomaticName()
            loadModes(0)
        }

        camerasComboBox.addActionListener {
            updateAutomaticName()
            loadModes(camerasComboBox.selectedIndex)
        }

        // ---------------- MODE RENDERER ----------------

        modesComboBox.renderer = object : DefaultListCellRenderer() {

            override fun getListCellRendererComponent(
                list: JList<*>,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {

                super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
                )

                val mode = value as? VideoMode

                text = if (mode != null) {
                    buildString {
                        append("${mode.width}x${mode.height}")
                        append(" @ ${mode.fps}fps")
                        append(" (${mode.pixelFormat})")
                    }
                } else {
                    "Unknown"
                }

                return this
            }
        }

        // ---------------- FORM ----------------

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        root.add(JLabel("Camera:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        root.add(camerasComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        root.add(JLabel("Mode:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        root.add(modesComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        root.add(JLabel("Name:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        root.add(nameTextField, gbc)

        gbc.gridx = 1
        gbc.gridy = 3
        gbc.weightx = 1.0
        root.add(sameCameraCheckBox, gbc)

        // ---------------- BUTTONS ----------------

        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT))

        val cancelButton = JButton("Cancel")

        buttons.add(createButton)
        buttons.add(cancelButton)

        gbc.gridx = 0
        gbc.gridy = 4
        gbc.gridwidth = 2

        root.add(buttons, gbc)

        // ---------------- EVENTS ----------------

        createButton.addActionListener {
            create()
        }

        cancelButton.addActionListener {
            close()
        }

        // ---------------- DIALOG ----------------

        dialog.contentPane.add(root)

        dialog.pack()
        dialog.minimumSize = dialog.size

        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }

    private fun updateAutomaticName() {
        val info = cameraInfos.getOrNull(camerasComboBox.selectedIndex)
            ?: return

        nameTextField.text = inputSourceManager.tryName(info.name)
    }

    private fun loadModes(index: Int) {

        val info = cameraInfos.getOrNull(index) ?: return

        try {

            val cam = UsbCamera(info.name, info.dev)

            modes = cam.enumerateVideoModes()
                .distinctBy {
                    "${it.width}x${it.height}_${it.fps}_${it.pixelFormat}"
                }
                .sortedWith(
                    compareByDescending<VideoMode> {
                        it.width * it.height
                    }.thenByDescending {
                        it.fps
                    }
                )
                .toTypedArray()

            cam.close()

        } catch (t: Throwable) {

            t.printStackTrace()
            modes = emptyArray()
        }

        modesComboBox.removeAllItems()

        for (mode in modes) {
            modesComboBox.addItem(mode)
        }

        if (modes.isNotEmpty()) {
            modesComboBox.selectedIndex = 0
        }
    }

    private fun create() {

        val camIndex = camerasComboBox.selectedIndex

        val info = cameraInfos.getOrNull(camIndex)
            ?: return

        val mode = modes.getOrNull(modesComboBox.selectedIndex)
            ?: return

        onMainUpdate.once {

            inputSourceManager.addInputSource(
                nameTextField.text.trim(),
                CameraSource(
                    info.name,
                    info.dev,
                    sameCameraCheckBox.isSelected,
                    info.vendorId,
                    info.productId,
                    mode
                ),
                true
            )
        }

        close()
    }

    private fun close() {
        dialog.dispose()
    }
}
