/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.component.PopupX.Companion.popUpXOnThis
import com.github.serivesmejia.eocvsim.gui.util.Corner
import com.github.serivesmejia.eocvsim.gui.util.icon.PipelineListIconRenderer
import com.github.serivesmejia.eocvsim.pipeline.PipelineData
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import io.github.deltacv.common.util.loggerForThis
import com.qualcomm.robotcore.eventloop.opmode.*
import com.qualcomm.robotcore.util.Range
import io.github.deltacv.vision.internal.opmode.OpModeState
import java.awt.GridBagConstraints
import java.awt.event.MouseEvent
import javax.swing.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter

class OpModeSelectorPanel(val opModeControlsPanel: OpModeControlsPanel) : JPanel(), KoinComponent {

    private val pipelineManager: PipelineManager by inject()



    private var _selectedIndex = -1


    private val logger by loggerForThis()

    var selectedIndex: Int
        get() = _selectedIndex
        set(value) {
            opModeControlsPanel.opModeSelected(value)
            _selectedIndex = value
        }

    private var pipelinesData = arrayOf<PipelineData>()

    // <Selector index, PipelineManager index>
    private val autonomousIndexMap = mutableMapOf<Int, Int>()

    // <Selector index, PipelineManager index>
    private val teleopIndexMap = mutableMapOf<Int, Int>()

    val autonomousButton = JButton()

    val selectOpModeLabelsPanel = JPanel()
    val opModeNameLabelPanel = JPanel()

    val textPanel = JPanel()

    val opModeNameLabel = JLabel("")

    val selectOpModeLabel = JLabel("Select Op Mode")
    val buttonDescriptorLabel = JLabel("<- Autonomous | TeleOp ->")

    val teleopButton = JButton()

    val autonomousSelector = JList<String>()
    val teleopSelector = JList<String>()

    var isActive = false
        set(value) {
            opModeControlsPanel.isActive = value
            field = value
        }

    var allowOpModeSwitching = false

    init {
        layout = GridBagLayout()
        selectOpModeLabelsPanel.layout = GridBagLayout()

        autonomousSelector.selectionMode = ListSelectionModel.SINGLE_SELECTION
        teleopSelector.selectionMode = ListSelectionModel.SINGLE_SELECTION

        autonomousSelector.cellRenderer = PipelineListIconRenderer(pipelineManager) { autonomousIndexMap }
        teleopSelector.cellRenderer = PipelineListIconRenderer(pipelineManager) { teleopIndexMap }


        autonomousButton.icon = EOCVSimIconLibrary.icoArrowDropdown

        add(autonomousButton, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            ipady = 20

            weightx = 1.0
            anchor = GridBagConstraints.WEST

            gridheight = 1
        })

        selectOpModeLabel.horizontalTextPosition = JLabel.CENTER
        selectOpModeLabel.horizontalAlignment = JLabel.CENTER

        buttonDescriptorLabel.horizontalTextPosition = JLabel.CENTER
        buttonDescriptorLabel.horizontalAlignment = JLabel.CENTER

        selectOpModeLabelsPanel.add(selectOpModeLabel, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            ipady = 0
        })

        selectOpModeLabelsPanel.add(buttonDescriptorLabel, GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            ipadx = 10
        })

        textPanel.add(selectOpModeLabelsPanel)

        opModeNameLabelPanel.add(opModeNameLabel)

        add(textPanel, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            ipadx = 20
        })

        teleopButton.icon = EOCVSimIconLibrary.icoArrowDropdown

        add(teleopButton, GridBagConstraints().apply {
            gridx = 2
            gridy = 0
            ipady = 20

            weightx = 1.0
            anchor = GridBagConstraints.EAST

            gridheight = 1
        })

        registerListeners()
    }

    private fun registerListeners() {
        autonomousButton.addActionListener {
            val popup =
                autonomousButton.popUpXOnThis(OpModePopupPanel(autonomousSelector), Corner.BOTTOM_LEFT, Corner.TOP_LEFT)

            opModeControlsPanel.stopCurrentOpMode()

            val listSelectionListener = object : javax.swing.event.ListSelectionListener {
                override fun valueChanged(e: javax.swing.event.ListSelectionEvent?) {
                    if (!e!!.valueIsAdjusting) {
                        popup.hide()
                        autonomousSelector.removeListSelectionListener(this)
                    }
                }
            }

            autonomousSelector.addListSelectionListener(listSelectionListener)

            popup.show()
        }

        teleopButton.addActionListener {
            val popup =
                teleopButton.popUpXOnThis(OpModePopupPanel(teleopSelector), Corner.BOTTOM_RIGHT, Corner.TOP_RIGHT)

            opModeControlsPanel.stopCurrentOpMode()

            val listSelectionListener = object : javax.swing.event.ListSelectionListener {
                override fun valueChanged(e: javax.swing.event.ListSelectionEvent?) {
                    if (!e!!.valueIsAdjusting) {
                        popup.hide()
                        teleopSelector.removeListSelectionListener(this)
                    }
                }
            }

            teleopSelector.addListSelectionListener(listSelectionListener)

            popup.show()
        }

        autonomousSelector.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!isActive || !allowOpModeSwitching) return

                val index = (e.source as JList<*>).locationToIndex(e.point)
                if (index >= 0) {
                    autonomousSelected(index)
                }
            }
        })

        teleopSelector.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!isActive || !allowOpModeSwitching) return

                val index = (e.source as JList<*>).locationToIndex(e.point)
                if (index >= 0) {
                    teleOpSelected(index)
                }
            }
        })

        pipelineManager.onPipelineChange {
            if (!isActive) return@onPipelineChange

            // we are doing this to detect external pipeline changes and reflect them
            // accordingly in the UI.
            //
            // in the event that this change was triggered by us, OpModeSelectorPanel,
            // we need to hold on a cycle so that the state has been fully updated,
            // just to be able to check correctly and, if it was requested by
            // OpModeSelectorPanel, skip this message and not do anything.
            pipelineManager.onUpdate.once {
                if (isActive && opModeControlsPanel.currentOpMode != pipelineManager.currentPipeline && pipelineManager.currentPipeline != null) {
                    val opMode = pipelineManager.currentPipeline

                    if (opMode is OpMode) {
                        val name = if (opMode.opModeType == OpModeType.AUTONOMOUS)
                            opMode.autonomousAnnotation.name
                        else opMode.teleopAnnotation.name

                        logger.info("External change detected \"$name\"")

                        opModeSelected(pipelineManager.currentPipelineIndex, name, false)
                    } else if (isActive) {
                        reset(-1)
                    }
                }
            }
        }

        pipelineManager.onPipelineListRefresh {
            updateOpModesList()
        }

        pipelineManager.onExternalSwitchingEnable {
            allowOpModeSwitching = true
        }
        pipelineManager.onExternalSwitchingDisable {
            allowOpModeSwitching = false
        }

    }

    private fun teleOpSelected(index: Int) {
        opModeSelected(teleopIndexMap[index] ?: run {
            logger.error("teleopIndexMap[$index] is null")
            updateOpModesList()

            SwingUtilities.invokeLater {
                teleOpSelected(index)
            }
            return
        }, teleopSelector.selectedValue ?: return)
    }

    private fun autonomousSelected(index: Int) {
        opModeSelected(autonomousIndexMap[index] ?: run {
            logger.error("autonomousIndexMap[$index] is null")
            updateOpModesList()

            SwingUtilities.invokeLater {
                autonomousSelected(index)
            }
            return
        }, autonomousSelector.selectedValue ?: return)
    }

    private fun opModeSelected(managerIndex: Int, name: String, forceChangePipeline: Boolean = true) {
        if (!isActive) return

        opModeNameLabel.text = name

        textPanel.removeAll()
        textPanel.add(opModeNameLabelPanel)

        _selectedIndex = managerIndex

        opModeControlsPanel.opModeSelected(managerIndex, forceChangePipeline)
    }

    fun updateOpModesList() = SwingUtilities.invokeLater {
        val autonomousListModel = DefaultListModel<String>()
        val teleopListModel = DefaultListModel<String>()

        pipelinesData = pipelineManager.pipelines.toArray(arrayOf<PipelineData>())


        var autonomousSelectorIndex = Range.clip(autonomousListModel.size() - 1, 0, Int.MAX_VALUE)
        var teleopSelectorIndex = Range.clip(teleopListModel.size() - 1, 0, Int.MAX_VALUE)

        autonomousIndexMap.clear()
        teleopIndexMap.clear()

        for ((managerIndex, pipeline) in pipelineManager.pipelines.withIndex()) {

            if (ReflectUtil.hasSuperclass(pipeline.clazz, OpMode::class.java)) {
                val type = pipeline.clazz.opModeType

                if (type == OpModeType.AUTONOMOUS) {
                    val autonomousAnnotation = pipeline.clazz.autonomousAnnotation

                    autonomousListModel.addElement(
                        autonomousAnnotation.name.ifBlank { pipeline.clazz.simpleName }
                    )
                    autonomousIndexMap[autonomousSelectorIndex] = managerIndex
                    autonomousSelectorIndex++
                } else if (type == OpModeType.TELEOP) {
                    val teleopAnnotation = pipeline.clazz.teleopAnnotation

                    teleopListModel.addElement(
                        teleopAnnotation.name.ifBlank { pipeline.clazz.simpleName }
                    )
                    teleopIndexMap[teleopSelectorIndex] = managerIndex
                    teleopSelectorIndex++
                }
            }
        }

        autonomousSelector.fixedCellWidth = 240
        autonomousSelector.model = autonomousListModel

        teleopSelector.fixedCellWidth = 240
        teleopSelector.model = teleopListModel
    }

    fun reset(nextPipeline: Int? = null) {
        textPanel.removeAll()
        textPanel.add(selectOpModeLabelsPanel)

        opModeControlsPanel.reset()

        val opMode = opModeControlsPanel.currentOpMode

        if (pipelineManager.currentPipeline == opMode && opMode != null && opMode.notifier.state != OpModeState.SELECTED) {
            opMode.notifier?.onStateChange?.attach {
                val state = opMode.notifier.state

                if (state == OpModeState.STOPPED) {
                    removeListener()

                    if (nextPipeline == null || nextPipeline >= 0) {
                        pipelineManager.onUpdate.once {
                            pipelineManager.changePipeline(nextPipeline)
                        }
                    }
                }
            }
        } else if (nextPipeline == null || nextPipeline >= 0) {
            pipelineManager.onUpdate.once {
                pipelineManager.requestChangePipeline(nextPipeline)
            }
        }


        _selectedIndex = -1
        opModeControlsPanel.stopCurrentOpMode()
    }

}
