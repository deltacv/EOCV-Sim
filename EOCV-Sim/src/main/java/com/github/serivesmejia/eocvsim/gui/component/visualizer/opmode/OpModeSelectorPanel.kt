package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.component.PopupX.Companion.popUpXOnThis
import com.github.serivesmejia.eocvsim.gui.util.Location
import com.github.serivesmejia.eocvsim.pipeline.PipelineData
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import com.qualcomm.robotcore.eventloop.opmode.*
import com.qualcomm.robotcore.util.Range
import io.github.deltacv.vision.internal.opmode.OpModeState
import kotlinx.coroutines.runBlocking
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*


class OpModeSelectorPanel(val eocvSim: EOCVSim, val opModeControlsPanel: OpModeControlsPanel) : JPanel() {

    private var _selectedIndex = -1

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
    val teleopSelector     = JList<String>()

    var allowOpModeSwitching = false
        set(value) {
            opModeControlsPanel.allowOpModeSwitching = value
            field = value
        }

    private var lastSwitching: Boolean? = null

    init {
        layout = GridBagLayout()
        selectOpModeLabelsPanel.layout = GridBagLayout()

        autonomousSelector.selectionMode = ListSelectionModel.SINGLE_SELECTION
        teleopSelector.selectionMode = ListSelectionModel.SINGLE_SELECTION

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
            val popup = autonomousButton.popUpXOnThis(OpModePopupPanel(autonomousSelector), Location.BOTTOM)

            opModeControlsPanel.stopCurrentOpMode()

            val listSelectionListener = object : javax.swing.event.ListSelectionListener {
                override fun valueChanged(e: javax.swing.event.ListSelectionEvent?) {
                    if(!e!!.valueIsAdjusting) {
                        popup.hide()
                        autonomousSelector.removeListSelectionListener(this)
                    }
                }
            }

            autonomousSelector.addListSelectionListener(listSelectionListener)

            popup.show()
        }

        teleopButton.addActionListener {
            val popup = teleopButton.popUpXOnThis(OpModePopupPanel(teleopSelector), Location.BOTTOM)

            opModeControlsPanel.stopCurrentOpMode()

            val listSelectionListener = object : javax.swing.event.ListSelectionListener {
                override fun valueChanged(e: javax.swing.event.ListSelectionEvent?) {
                    if(!e!!.valueIsAdjusting) {
                        popup.hide()
                        teleopSelector.removeListSelectionListener(this)
                    }
                }
            }

            teleopSelector.addListSelectionListener(listSelectionListener)

            popup.show()
        }

        autonomousSelector.addListSelectionListener {
            if(!it.valueIsAdjusting) {
                val index = autonomousSelector.selectedIndex
                if(index != -1) {
                    autonomousSelected(index)
                }
            }
        }

        teleopSelector.addListSelectionListener {
            if(!it.valueIsAdjusting) {
                val index = teleopSelector.selectedIndex
                if(index != -1) {
                    teleOpSelected(index)
                }
            }
        }
    }

    private fun teleOpSelected(index: Int) {
        opModeSelected(teleopIndexMap[index]!!, teleopSelector.selectedValue!!)
    }

    private fun autonomousSelected(index: Int) {
        opModeSelected(autonomousIndexMap[index]!!, autonomousSelector.selectedValue!!)
    }

    private fun opModeSelected(managerIndex: Int, name: String) {
        opModeNameLabel.text = name

        textPanel.removeAll()
        textPanel.add(opModeNameLabelPanel)

        _selectedIndex = managerIndex;

        opModeControlsPanel.opModeSelected(managerIndex)
    }

    fun updateOpModesList() {
        val autonomousListModel = DefaultListModel<String>()
        val teleopListModel = DefaultListModel<String>()

        pipelinesData = eocvSim.pipelineManager.pipelines.toArray(arrayOf<PipelineData>())

        var autonomousSelectorIndex = Range.clip(autonomousListModel.size() - 1, 0, Int.MAX_VALUE)
        var teleopSelectorIndex = Range.clip(teleopListModel.size() - 1, 0, Int.MAX_VALUE)

        autonomousIndexMap.clear()
        teleopIndexMap.clear()

        for ((managerIndex, pipeline) in eocvSim.pipelineManager.pipelines.withIndex()) {
            if(ReflectUtil.hasSuperclass(pipeline.clazz, OpMode::class.java)) {
                val type = pipeline.clazz.opModeType

                if(type == OpModeType.AUTONOMOUS) {
                    val autonomousAnnotation = pipeline.clazz.autonomousAnnotation

                    autonomousListModel.addElement(autonomousAnnotation.name)
                    autonomousIndexMap[autonomousSelectorIndex] = managerIndex
                    autonomousSelectorIndex++
                } else if(type == OpModeType.TELEOP) {
                    val teleopAnnotation = pipeline.clazz.teleopAnnotation

                    teleopListModel.addElement(teleopAnnotation.name)
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

        if(eocvSim.pipelineManager.currentPipeline == opModeControlsPanel.currentOpMode) {
            val opMode = opModeControlsPanel.currentOpMode

            opMode?.notifier?.onStateChange?.let {
                it {
                    val state = opMode.notifier.state

                    if(state == OpModeState.STOPPED) {
                        it.removeThis()
                        eocvSim.pipelineManager.requestChangePipeline(nextPipeline)
                    }
                }
            }
        } else {
            eocvSim.pipelineManager.onUpdate.doOnce {
                eocvSim.pipelineManager.requestChangePipeline(nextPipeline)
            }
        }

        _selectedIndex = -1
    }

    fun refreshAndReselectCurrent() {
        val currentIndex = selectedIndex
        if(currentIndex < 0) return

        val beforePipeline = pipelinesData[currentIndex]

        updateOpModesList()

        for((i, pipeline) in pipelinesData.withIndex()) {
            if(pipeline.clazz.name == beforePipeline.clazz.name && pipeline.source == beforePipeline.source) {
                selectedIndex = i
                return
            }
        }

        selectedIndex = -1
    }


    fun setLastSwitching() {
        if(lastSwitching != null) {
            allowOpModeSwitching = lastSwitching!!
            lastSwitching = null
        }
    }

    fun saveLastSwitching() {
        lastSwitching = allowOpModeSwitching
    }

}