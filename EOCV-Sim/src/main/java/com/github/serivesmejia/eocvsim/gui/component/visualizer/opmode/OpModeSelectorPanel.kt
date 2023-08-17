package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.util.icon.PipelineListIconRenderer
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.util.Range
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.event.ListSelectionEvent

class OpModeSelectorPanel(val eocvSim: EOCVSim) : JPanel() {

    var selectedIndex = -1
        set(value) {
            field = value
        }

    // <opModeSelector index, PipelineManager index>
    private val indexMap = mutableMapOf<Int, Int>()

    val autonomousButton = JButton("\\/")

    val textPanel = JPanel()

    val selectOpModeLabel = JLabel("Select Op Mode")
    val buttonDescriptorLabel = JLabel("<- Autonomous | TeleOp ->")

    val teleopButton = JButton("\\/")

    var allowOpModeSwitching = false
    private var beforeSelectedPipeline = -1

    init {
        layout = GridBagLayout()
        textPanel.layout = GridBagLayout()

        add(autonomousButton, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            ipady = 20
        })

        selectOpModeLabel.horizontalTextPosition = JLabel.CENTER
        selectOpModeLabel.horizontalAlignment = JLabel.CENTER

        buttonDescriptorLabel.horizontalTextPosition = JLabel.CENTER
        buttonDescriptorLabel.horizontalAlignment = JLabel.CENTER

        textPanel.add(selectOpModeLabel, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            ipady = 0
        })

        textPanel.add(buttonDescriptorLabel, GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            ipadx = 10
        })

        add(textPanel, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            ipadx = 20
        })

        add(teleopButton, GridBagConstraints().apply {
            gridx = 2
            gridy = 0
            ipady = 20
        })

        registerListeners()
    }

    private fun registerListeners() {

    }

    fun updateOpModesList() = runBlocking {
        /* launch(Dispatchers.Swing) {
            val listModel = DefaultListModel<String>()
            var selectorIndex = Range.clip(listModel.size() - 1, 0, Int.MAX_VALUE)

            indexMap.clear()

            for ((managerIndex, pipeline) in eocvSim.pipelineManager.pipelines.withIndex()) {
                if(ReflectUtil.hasSuperclass(pipeline.clazz, OpMode::class.java)) {
                    listModel.addElement(pipeline.clazz.simpleName)
                    indexMap[selectorIndex] = managerIndex

                    selectorIndex++
                }
            }

            opModeSelector.fixedCellWidth = 240
            opModeSelector.model = listModel

            revalAndRepaint()
        }*/
    }

    fun revalAndRepaint() {
        /* opModeSelector.revalidate()
        opModeSelector.repaint()
        opModeSelectorScroll.revalidate()
        opModeSelectorScroll.repaint() */
    }

}