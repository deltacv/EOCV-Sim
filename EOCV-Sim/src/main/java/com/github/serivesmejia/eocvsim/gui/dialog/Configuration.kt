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

import com.github.serivesmejia.eocvsim.LifecycleSignal
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields
import com.github.serivesmejia.eocvsim.gui.theme.Theme
import com.github.serivesmejia.eocvsim.pipeline.PipelineFps
import com.github.serivesmejia.eocvsim.pipeline.PipelineTimeout
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.*

class Configuration : KoinComponent {

    private val visualizer: Visualizer by inject()
    private val lifecycle: Channel<LifecycleSignal> by inject(named("lifecycle"))
    private val dialogFactory: DialogFactory by inject()
    private val configManager: ConfigManager by inject()
    private val onMainLoop: EventHandler by inject(named("onMainLoop"))

    private val config get() = configManager.config

    private val dialog = JDialog(visualizer.frame)

    private val themeComboBox: JComboBox<String>
    private val superAccessCheckBox: JCheckBox
    private val prefersPaperVisionCheckbox: JCheckBox
    private val pauseOnImageCheckBox: JCheckBox
    private val webcamOpenTimeoutSpinner: JSpinner
    private val webcamNewFrameTimeoutSpinner: JSpinner
    private val pipelineTimeoutComboBox: EnumComboBox<PipelineTimeout>
    private val pipelineFpsComboBox: EnumComboBox<PipelineFps>
    private val videoRecordingSize: SizeFields
    private val videoRecordingFpsComboBox: EnumComboBox<PipelineFps>
    private val acceptButton: JButton

    init {
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
        webcamOpenTimeoutSpinner = JSpinner(SpinnerNumberModel(config.webcamOpenTimeoutSec, 0.1, 60.0, 0.1))
        webcamNewFrameTimeoutSpinner = JSpinner(SpinnerNumberModel(config.webcamNewFrameTimeoutSec, 0.1, 60.0, 0.1))
        val inputSourcesPanel = JPanel(GridLayout(3, 1, 1, 8)).apply {
            add(JPanel(FlowLayout()).apply { add(pauseOnImageCheckBox) })
            add(JPanel(FlowLayout()).apply {
                add(JLabel("Timeout to start camera stream (seconds): "))
                add(webcamOpenTimeoutSpinner)
            })
            add(JPanel(FlowLayout()).apply {
                add(JLabel("Timeout for new camera frame (seconds): "))
                add(webcamNewFrameTimeoutSpinner)
            })
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
        ).apply { onChange.attach { acceptButton.isEnabled = valid } }

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
            onMainLoop.once {
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
        config.webcamOpenTimeoutSec = (webcamOpenTimeoutSpinner.value as Number).toDouble()
        config.webcamNewFrameTimeoutSec = (webcamNewFrameTimeoutSpinner.value as Number).toDouble()
        config.pipelineTimeout = pipelineTimeoutComboBox.selectedEnum
        config.pipelineMaxFps = pipelineFpsComboBox.selectedEnum
        config.videoRecordingSize = videoRecordingSize.currentSize
        config.videoRecordingFps = videoRecordingFpsComboBox.selectedEnum
        config.autoAcceptSuperAccessOnTrusted = superAccessCheckBox.isSelected
        config.flags["prefersPaperVision"] = prefersPaperVisionCheckbox.isSelected

        configManager.saveToFile()

        if (userSelectedTheme != previousTheme) {
            dialogFactory.createYesOrNo(dialog, "Applying a new interface theme requires restarting.", "Do you wish to restart now?") {
                lifecycle.trySend(LifecycleSignal.Restart)
            }
        }
    }

    fun close() {
        dialog.isVisible = false
        dialog.dispose()
    }
}