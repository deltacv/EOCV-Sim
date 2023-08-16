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
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.event.ListSelectionEvent

class OpModeSelectorPanel(val eocvSim: EOCVSim) : JPanel() {

    var selectedIndex: Int
        get() = opModeSelector.selectedIndex
        set(value) {
            runBlocking {
                launch(Dispatchers.Swing) {
                    opModeSelector.selectedIndex = value
                }
            }
        }

    val opModeSelector         = JList<String>()
    val opModeSelectorScroll   = JScrollPane()

    val opModeSelectorLabel = JLabel("OpModes")

    // <opModeSelector index, PipelineManager index>
    private val indexMap = mutableMapOf<Int, Int>()

    var allowOpModeSwitching = false

    private var beforeSelectedPipeline = -1

    init {
        layout = GridBagLayout()

        opModeSelectorLabel.font = opModeSelectorLabel.font.deriveFont(20.0f)

        opModeSelectorLabel.horizontalAlignment = JLabel.CENTER

        add(opModeSelectorLabel, GridBagConstraints().apply {
            gridy = 0
            ipady = 20
        })

        opModeSelector.cellRenderer = PipelineListIconRenderer(eocvSim.pipelineManager)
        opModeSelector.selectionMode = ListSelectionModel.SINGLE_SELECTION

        opModeSelectorScroll.setViewportView(opModeSelector)
        opModeSelectorScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        opModeSelectorScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        add(opModeSelectorScroll, GridBagConstraints().apply {
            gridy = 1

            weightx = 0.5
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            ipadx = 120
            ipady = 20
        })

        registerListeners()
    }

    private fun registerListeners() {

        //listener for changing pipeline
        opModeSelector.addListSelectionListener { evt: ListSelectionEvent ->
            if(!allowOpModeSwitching) return@addListSelectionListener

            if (opModeSelector.selectedIndex != -1) {
                val pipeline = indexMap[opModeSelector.selectedIndex] ?: return@addListSelectionListener

                if (!evt.valueIsAdjusting && pipeline != beforeSelectedPipeline) {
                    if (!eocvSim.pipelineManager.paused) {
                        eocvSim.pipelineManager.requestChangePipeline(pipeline)
                        beforeSelectedPipeline = pipeline
                    } else {
                        if (eocvSim.pipelineManager.pauseReason !== PipelineManager.PauseReason.IMAGE_ONE_ANALYSIS) {
                            opModeSelector.setSelectedIndex(beforeSelectedPipeline)
                        } else { //handling pausing
                            eocvSim.pipelineManager.requestSetPaused(false)
                            eocvSim.pipelineManager.requestChangePipeline(pipeline)
                            beforeSelectedPipeline = pipeline
                        }
                    }
                }
            } else {
                opModeSelector.setSelectedIndex(1)
            }
        }
    }

    fun updateOpModesList() = runBlocking {
        launch(Dispatchers.Swing) {
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
        }
    }

    fun revalAndRepaint() {
        opModeSelector.revalidate()
        opModeSelector.repaint()
        opModeSelectorScroll.revalidate()
        opModeSelectorScroll.repaint()
    }

}