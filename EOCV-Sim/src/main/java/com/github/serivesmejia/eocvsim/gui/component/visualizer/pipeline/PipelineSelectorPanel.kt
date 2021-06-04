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

package com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.util.icon.PipelineListIconRenderer
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.*
import javax.swing.event.ListSelectionEvent

class PipelineSelectorPanel(private val eocvSim: EOCVSim) : JPanel() {

    var selectedIndex: Int
        get() = pipelineSelector.selectedIndex
        set(value) {
            runBlocking {
                launch(Dispatchers.Swing) {
                    pipelineSelector.selectedIndex = value
                }
            }
        }

    val pipelineSelector         = JList<String>()
    val pipelineSelectorScroll   = JScrollPane()

    val pipelineSelectorLabel = JLabel("Pipelines")

    val buttonsPanel = PipelineSelectorButtonsPanel(eocvSim)

    var allowPipelineSwitching = false

    private var beforeSelectedPipeline = -1

    init {
        layout = FlowLayout(FlowLayout.CENTER)

        pipelineSelectorLabel.font = pipelineSelectorLabel.font.deriveFont(20.0f)

        pipelineSelectorLabel.horizontalAlignment = JLabel.CENTER
        add(pipelineSelectorLabel)

        pipelineSelector.setCellRenderer(PipelineListIconRenderer(eocvSim.pipelineManager))
        pipelineSelector.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val pipelineSelectorScrollContainer = JPanel()
        pipelineSelectorScrollContainer.layout = GridLayout()
        pipelineSelectorScrollContainer.border = BorderFactory.createEmptyBorder(0, 20, 0, 20)

        pipelineSelectorScrollContainer.add(pipelineSelectorScroll)

        pipelineSelectorScroll.setViewportView(pipelineSelector)
        pipelineSelectorScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        pipelineSelectorScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        add(pipelineSelectorScrollContainer)

        add(buttonsPanel)

        registerListeners()
    }

    private fun registerListeners() {

        //listener for changing pipeline
        pipelineSelector.addListSelectionListener { evt: ListSelectionEvent ->
            if(!allowPipelineSwitching) return@addListSelectionListener

            if (pipelineSelector.selectedIndex != -1) {
                val pipeline = pipelineSelector.selectedIndex

                if (!evt.valueIsAdjusting && pipeline != beforeSelectedPipeline) {
                    if (!eocvSim.pipelineManager.paused) {
                        eocvSim.pipelineManager.requestChangePipeline(pipeline)
                        beforeSelectedPipeline = pipeline
                    } else {
                        if (eocvSim.pipelineManager.pauseReason !== PipelineManager.PauseReason.IMAGE_ONE_ANALYSIS) {
                            pipelineSelector.setSelectedIndex(beforeSelectedPipeline)
                        } else { //handling pausing
                            eocvSim.pipelineManager.requestSetPaused(false)
                            eocvSim.pipelineManager.requestChangePipeline(pipeline)
                            beforeSelectedPipeline = pipeline
                        }
                    }
                }
            } else {
                pipelineSelector.setSelectedIndex(1)
            }
        }
    }

    fun updatePipelinesList() = runBlocking {
        launch(Dispatchers.Swing) {
            val listModel = DefaultListModel<String>()
            for (pipeline in eocvSim.pipelineManager.pipelines) {
                listModel.addElement(pipeline.clazz.simpleName)
            }

            pipelineSelector.fixedCellWidth = 240
            pipelineSelector.model = listModel

            revalAndRepaint()
        }
    }

    fun revalAndRepaint() {
        pipelineSelector.revalidate()
        pipelineSelector.repaint()
        pipelineSelectorScroll.revalidate()
        pipelineSelectorScroll.repaint()
    }

}
