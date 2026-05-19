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
    private val resolutionComboBox = JComboBox<String>()
    private val fpsComboBox = JComboBox<Int>()
    private val pixelFormatComboBox = JComboBox<Any>()
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

        // ---------------- MODE CONTROLS ----------------

        // simple renderers (defaults are fine, but keep fps/pixfmt readable)
        fpsComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = (value as? Int)?.toString() ?: "-"
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

        // Resolution
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        root.add(JLabel("Resolution:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        root.add(resolutionComboBox, gbc)

        // FPS
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        root.add(JLabel("FPS:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        root.add(fpsComboBox, gbc)

        // Pixel Format
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.weightx = 0.0
        root.add(JLabel("Pixel Format:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        root.add(pixelFormatComboBox, gbc)

        // Name
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.weightx = 0.0
        root.add(JLabel("Source Name:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        root.add(nameTextField, gbc)

        gbc.gridx = 1
        gbc.gridy = 5
        gbc.weightx = 1.0
        root.add(sameCameraCheckBox, gbc)

        // ---------------- BUTTONS ----------------

        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT))

        val cancelButton = JButton("Cancel")

        buttons.add(createButton)
        buttons.add(cancelButton)

        gbc.gridx = 0
        gbc.gridy = 6
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

        // Populate resolution / fps / pixel format combo boxes based on available modes
        resolutionComboBox.removeAllItems()
        fpsComboBox.removeAllItems()
        pixelFormatComboBox.removeAllItems()

        val resolutions = modes
            .map { "${it.width}x${it.height}" }
            .distinct()
            .sortedByDescending { res ->
                val (w, h) = parseResolution(res) ?: Pair(0, 0)
                w * h
            }

        for (r in resolutions) resolutionComboBox.addItem(r)

        if (resolutions.isNotEmpty()) {
            resolutionComboBox.selectedIndex = 0
        }

        // listeners (remove previous to avoid duplicates)
        for (l in resolutionComboBox.actionListeners) resolutionComboBox.removeActionListener(l)
        for (l in fpsComboBox.actionListeners) fpsComboBox.removeActionListener(l)

        resolutionComboBox.addActionListener {
            updateFpsAndPixelFormats()
        }

        fpsComboBox.addActionListener {
            updatePixelFormatsForFps()
        }

        // initialize dependent lists
        if (resolutionComboBox.itemCount > 0) updateFpsAndPixelFormats()
    }

    private fun parseResolution(res: String?): Pair<Int, Int>? {
        if (res == null) return null
        val parts = res.split('x')
        if (parts.size != 2) return null
        return try {
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun updateFpsAndPixelFormats() {
        val res = resolutionComboBox.selectedItem as? String ?: return
        val (w, h) = parseResolution(res) ?: return

        val modesForRes = modes.filter { it.width == w && it.height == h }

        val fpsList = modesForRes.map { it.fps }.distinct().sortedDescending()

        fpsComboBox.removeAllItems()
        for (f in fpsList) fpsComboBox.addItem(f)
        if (fpsComboBox.itemCount > 0) fpsComboBox.selectedIndex = 0

        // populate pixel formats for the initially selected fps
        updatePixelFormatsForFps()
    }

    private fun updatePixelFormatsForFps() {
        val res = resolutionComboBox.selectedItem as? String ?: return
        val (w, h) = parseResolution(res) ?: return
        val selectedFps = fpsComboBox.selectedItem as? Int

        val modesForRes = modes.filter { it.width == w && it.height == h }

        val pfList = if (selectedFps != null) {
            modesForRes.filter { it.fps == selectedFps }.map { it.pixelFormat }
        } else {
            modesForRes.map { it.pixelFormat }
        }

        val distinctPf = pfList.distinct()

        pixelFormatComboBox.removeAllItems()
        for (pf in distinctPf) pixelFormatComboBox.addItem(pf)
        if (pixelFormatComboBox.itemCount > 0) pixelFormatComboBox.selectedIndex = 0
    }

    private fun create() {

        val camIndex = camerasComboBox.selectedIndex

        val info = cameraInfos.getOrNull(camIndex)
            ?: return

        // Build desired VideoMode from selected resolution / fps / pixel format
        val resStr = resolutionComboBox.selectedItem as? String ?: return
        val (w, h) = parseResolution(resStr) ?: return
        val fps = fpsComboBox.selectedItem as? Int
        val pf = pixelFormatComboBox.selectedItem

        var mode: VideoMode? = null

        if (fps != null && pf != null) {
            mode = modes.find { it.width == w && it.height == h && it.fps == fps && it.pixelFormat == pf }
        }

        // fallback: match by resolution + fps
        if (mode == null && fps != null) {
            mode = modes.find { it.width == w && it.height == h && it.fps == fps }
        }

        // fallback: any mode with resolution
        if (mode == null) {
            mode = modes.find { it.width == w && it.height == h }
        }

        mode ?: return

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
