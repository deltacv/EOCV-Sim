/*
 * Copyright (c) 2026 Sebastian Erives
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
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.util.icon.SourcesListIconRenderer
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.extension.clipUpperZero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import javax.swing.*

class SourceSelectorPanel(private val eocvSim: EOCVSim) : JPanel() {

    val sourceSelector = JList<String>()
    val sourceSelectorScroll = JScrollPane()
    var sourceSelectorButtonsContainer = JPanel()
    val sourceSelectorCreateBtt = JButton("Create")
    val sourceSelectorDeleteBtt = JButton("Delete")

    val sourceSelectorLabel = JLabel("Sources")

    private var beforeSelectedSource = ""
    private var beforeSelectedSourceIndex = 0

    var allowSourceSwitching = false

    init {
        layout = GridBagLayout()

        sourceSelectorLabel.font = sourceSelectorLabel.font.deriveFont(20.0f)
        sourceSelectorLabel.horizontalAlignment = JLabel.CENTER

        // add(sourceSelectorLabel, GridBagConstraints().apply {
        //    gridy = 0
        //    ipady = 20
        //})

        sourceSelector.selectionMode = ListSelectionModel.SINGLE_SELECTION

        sourceSelectorScroll.setViewportView(sourceSelector)
        sourceSelectorScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        sourceSelectorScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        add(sourceSelectorScroll, GridBagConstraints().apply {
            gridy = 0

            weightx = 0.5
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            ipadx = 120
            ipady = 20
        })

        //different icons
        sourceSelector.cellRenderer = SourcesListIconRenderer(eocvSim.inputSourceManager)

        sourceSelectorCreateBtt.addActionListener {
            DialogFactory.createSourceExDialog(eocvSim)
        }

        sourceSelectorButtonsContainer = JPanel(FlowLayout(FlowLayout.CENTER))

        sourceSelectorButtonsContainer.add(sourceSelectorCreateBtt)
        sourceSelectorButtonsContainer.add(sourceSelectorDeleteBtt)
        sourceSelectorDeleteBtt.isEnabled = false

        add(sourceSelectorButtonsContainer, GridBagConstraints().apply {
            gridy = 1
            ipady = 20
        })

        registerListeners()
    }

    private fun registerListeners() {
        //listener for changing input sources
        sourceSelector.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                val index = (e.source as JList<*>).locationToIndex(e.point)

                if (index != -1) {
                    if (allowSourceSwitching) {
                        try {
                            if (sourceSelector.selectedIndex != -1) {
                                val model = sourceSelector.model
                                val source = model.getElementAt(sourceSelector.selectedIndex)

                                //enable or disable source delete button depending if source is default or not
                                sourceSelectorDeleteBtt.isEnabled =
                                    eocvSim.inputSourceManager.sources[source]?.isDefault == false

                                if (source != beforeSelectedSource) {
                                    if (!eocvSim.pipelineManager.paused) {
                                        eocvSim.inputSourceManager.requestSetInputSource(source)
                                        beforeSelectedSource = source
                                        beforeSelectedSourceIndex = sourceSelector.selectedIndex
                                    } else {
                                        //check if the user requested the pause or if it was due to one shoot analysis when selecting images
                                        if (eocvSim.pipelineManager.pauseReason !== PipelineManager.PauseReason.IMAGE_ONE_ANALYSIS) {
                                            sourceSelector.setSelectedIndex(beforeSelectedSourceIndex)
                                        } else { //handling pausing
                                            eocvSim.pipelineManager.requestSetPaused(false)
                                            eocvSim.inputSourceManager.requestSetInputSource(source)
                                            beforeSelectedSource = source
                                            beforeSelectedSourceIndex = sourceSelector.selectedIndex
                                        }
                                    }
                                }
                            } else {
                                sourceSelector.setSelectedIndex(1)
                            }
                        } catch (ignored: ArrayIndexOutOfBoundsException) {
                        }
                    }
                }
            }
        })

        // delete selected input source
        sourceSelectorDeleteBtt.addActionListener {
            val index = sourceSelector.selectedIndex
            val source = sourceSelector.model.getElementAt(index)

            eocvSim.onMainUpdate.once {
                eocvSim.inputSourceManager.deleteInputSource(source)
                updateSourcesList()

                sourceSelector.selectedIndex = (index - 1).clipUpperZero()
            }
        }
    }

    fun updateSourcesList(): Job {
        SwingUtilities.invokeLater {
            val listModel = DefaultListModel<String>()

            for (source in eocvSim.inputSourceManager.sortedInputSources) {
                listModel.addElement(source.name)
            }

            sourceSelector.fixedCellWidth = 240

            sourceSelector.model = listModel
            sourceSelector.revalidate()
            sourceSelectorScroll.revalidate()

            sourceSelector.selectedIndex = 0
        }

        return Job() // we can't break ABI here, so we return a dummy Job
    }

    fun getIndexOf(name: String): Int {
        for (i in 0..sourceSelector.model.size) {
            if (sourceSelector.model.getElementAt(i) == name)
                return i
        }

        return 0
    }

}
