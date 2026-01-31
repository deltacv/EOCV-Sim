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

package com.github.serivesmejia.eocvsim.gui.dialog

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.config.Config
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields
import com.github.serivesmejia.eocvsim.gui.theme.Theme
import com.github.serivesmejia.eocvsim.gui.util.WebcamDriver
import com.github.serivesmejia.eocvsim.pipeline.PipelineFps
import com.github.serivesmejia.eocvsim.pipeline.PipelineTimeout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.*

class Configuration(parent: JFrame, private val eocvSim: EOCVSim) {

    private val dialog = JDialog(parent)
    private val config: Config = eocvSim.configManager.config

    // Declaring the components as 'val'
    private val themeComboBox: JComboBox<String>
    private val superAccessCheckBox: JCheckBox
    private val prefersPaperVisionCheckbox: JCheckBox
    private val pauseOnImageCheckBox: JCheckBox
    private val pipelineTimeoutComboBox: EnumComboBox<PipelineTimeout>
    private val pipelineFpsComboBox: EnumComboBox<PipelineFps>
    private val preferredWebcamDriver: EnumComboBox<WebcamDriver>
    private val videoRecordingSize: SizeFields
    private val videoRecordingFpsComboBox: EnumComboBox<PipelineFps>
    private val acceptButton: JButton

    init {
        eocvSim.visualizer.childDialogs.add(dialog)

        // --- Interface Tab ---
        themeComboBox = JComboBox<String>().apply {
            Theme.entries.forEach { addItem(it.name.replace("_", " ")) }
            selectedIndex = config.simTheme.ordinal
        }
        superAccessCheckBox = JCheckBox("Auto Accept SuperAccess on Trusted Plugins").apply {
            isSelected = config.autoAcceptSuperAccessOnTrusted
        }
        prefersPaperVisionCheckbox = JCheckBox("Focus on PaperVision Upon Startup").apply {
            isSelected = config.flags["prefersPaperVision"] ?: false
        }
        val uiPanel = JPanel(GridLayout(3, 1, 1, 8)).apply {
            add(JPanel(FlowLayout()).apply {
                add(JLabel("Theme: "))
                add(themeComboBox)
            })
            add(JPanel(FlowLayout()).apply { add(superAccessCheckBox) })
            add(JPanel(FlowLayout()).apply { add(prefersPaperVisionCheckbox) })
        }

        // --- Input Sources Tab ---
        pauseOnImageCheckBox = JCheckBox("Pause with Image Sources").apply {
            isSelected = config.pauseOnImages
        }
        preferredWebcamDriver = EnumComboBox(
            "Preferred Webcam Driver: ",
            WebcamDriver::class.java,
            WebcamDriver.entries.toTypedArray()
        ).apply {
            removeEnumOption(WebcamDriver.OpenIMAJ)
            selectedEnum = config.preferredWebcamDriver
        }
        val inputSourcesPanel = JPanel(GridLayout(2, 1, 1, 8)).apply {
            add(JPanel(FlowLayout()).apply { add(pauseOnImageCheckBox) })
            add(preferredWebcamDriver)
        }

        // --- Processing Tab ---
        pipelineTimeoutComboBox = EnumComboBox(
            "Pipeline Process Timeout: ",
            PipelineTimeout::class.java,
            PipelineTimeout.entries.toTypedArray(),
            PipelineTimeout::coolName
        ) { PipelineTimeout.fromCoolName(it) ?: PipelineTimeout.MEDIUM }.apply { selectedEnum = config.pipelineTimeout }

        pipelineFpsComboBox = EnumComboBox(
            "Pipeline Max FPS: ",
            PipelineFps::class.java,
            PipelineFps.entries.toTypedArray(),
            PipelineFps::coolName
        ) { PipelineFps.fromCoolName(it) ?: PipelineFps.MEDIUM }.apply { selectedEnum = config.pipelineMaxFps }

        acceptButton = JButton("Accept") // Initialized here so the listener below can use it

        videoRecordingSize = SizeFields(
            config.videoRecordingSize,
            false,
            "Video Recording Size: "
        ).apply { onChange.doPersistent { acceptButton.isEnabled = valid } }

        videoRecordingFpsComboBox = EnumComboBox(
            "Video Recording FPS: ",
            PipelineFps::class.java,
            PipelineFps.entries.toTypedArray(),
            PipelineFps::coolName
        ) { PipelineFps.fromCoolName(it) ?: PipelineFps.MEDIUM }.apply { selectedEnum = config.videoRecordingFps }

        val processingPanel = JPanel(GridLayout(4, 1, 1, 8)).apply {
            add(pipelineTimeoutComboBox)
            add(pipelineFpsComboBox)
            add(videoRecordingSize)
            add(videoRecordingFpsComboBox)
        }

        // --- Dialog Assembly ---
        val tabbedPane = JTabbedPane(JTabbedPane.LEFT).apply {
            addTab("Interface", uiPanel)
            addTab("Input Sources", inputSourcesPanel)
            addTab("Processing", processingPanel)
        }

        acceptButton.addActionListener {
            eocvSim.onMainUpdate.once {
                applyChanges()
            }

            close()
        }

        val contents = JPanel(GridBagLayout()).apply {
            border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
            val gbc = GridBagConstraints()

            gbc.gridx = 1; gbc.gridy = 1
            add(tabbedPane, gbc)

            gbc.gridy = 2
            add(JPanel(FlowLayout()).apply { add(acceptButton) }, gbc)
        }

        dialog.apply {
            add(contents)
            title = "Settings"
            isModal = true
            isResizable = false
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private fun applyChanges() {
        val selectedThemeName = themeComboBox.selectedItem!!.toString().replace(" ", "_")
        val userSelectedTheme = Theme.valueOf(selectedThemeName)
        val previousTheme = config.simTheme

        config.simTheme = userSelectedTheme
        config.pauseOnImages = pauseOnImageCheckBox.isSelected
        config.preferredWebcamDriver = preferredWebcamDriver.selectedEnum
        config.pipelineTimeout = pipelineTimeoutComboBox.selectedEnum
        config.pipelineMaxFps = pipelineFpsComboBox.selectedEnum
        config.videoRecordingSize = videoRecordingSize.currentSize
        config.videoRecordingFps = videoRecordingFpsComboBox.selectedEnum
        config.autoAcceptSuperAccessOnTrusted = superAccessCheckBox.isSelected
        config.flags["prefersPaperVision"] = prefersPaperVisionCheckbox.isSelected

        eocvSim.configManager.saveToFile()

        if (userSelectedTheme != previousTheme) {
            eocvSim.restart()
        }
    }

    fun close() {
        dialog.isVisible = false
        dialog.dispose()
    }
}