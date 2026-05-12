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
    private val nameTextField = JTextField(15)
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
            insets = Insets(5, 5, 5, 5)
        }

        if (cameraInfos.isEmpty()) {
            camerasComboBox.addItem("No Cameras Found")
        } else {
            cameraInfos.forEach {
                camerasComboBox.addItem(it.name)
            }
            camerasComboBox.selectedIndex = 0
            loadModes(0)
        }

        camerasComboBox.addActionListener {
            loadModes(camerasComboBox.selectedIndex)
        }

        modesComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

                val mode = value as? VideoMode

                text = if (mode != null) {
                    "${mode.width}x${mode.height} @ ${mode.fps}fps (${mode.pixelFormat})"
                } else {
                    "Unknown"
                }

                return this
            }
        }

        gbc.gridx = 0; gbc.gridy = 0
        root.add(JLabel("Camera:"), gbc)
        gbc.gridx = 1
        root.add(camerasComboBox, gbc)

        gbc.gridx = 0; gbc.gridy = 1
        root.add(JLabel("Mode:"), gbc)
        gbc.gridx = 1
        root.add(modesComboBox, gbc)

        gbc.gridx = 0; gbc.gridy = 2
        root.add(JLabel("Name:"), gbc)
        gbc.gridx = 1
        nameTextField.text = "CameraSource-${inputSourceManager.sources.size + 1}"
        root.add(nameTextField, gbc)

        val buttons = JPanel()
        buttons.add(createButton)
        val cancel = JButton("Cancel")
        buttons.add(cancel)

        gbc.gridx = 0; gbc.gridy = 3
        gbc.gridwidth = 2
        root.add(buttons, gbc)

        createButton.addActionListener { create() }
        cancel.addActionListener { close() }

        dialog.contentPane.add(root)
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }

    private fun loadModes(index: Int) {
        val info = cameraInfos.getOrNull(index) ?: return

        try {
            val cam = UsbCamera(info.name, info.dev)

            modes = cam.enumerateVideoModes()

            cam.close()
        } catch (t: Throwable) {
            t.printStackTrace()
            modes = emptyArray()
        }

        modesComboBox.removeAllItems()

        for (m in modes) {
            modesComboBox.addItem(m)
        }

        if (modes.isNotEmpty()) {
            modesComboBox.selectedIndex = 0
        }
    }

    private fun create() {
        val camIndex = camerasComboBox.selectedIndex
        val info = cameraInfos.getOrNull(camIndex) ?: return

        val mode = modes.getOrNull(modesComboBox.selectedIndex) ?: return

        onMainUpdate.once {
            inputSourceManager.addInputSource(
                nameTextField.text,
                CameraSource(info.name, mode),
                true
            )
        }

        close()
    }

    private fun close() {
        dialog.dispose()
    }
}