package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeControlsPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeSelectorPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.PipelineSelectorPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.border.EmptyBorder

class PipelineOpModeSwitchablePanel(val eocvSim: EOCVSim) : JTabbedPane() {

    val pipelinePanel = JPanel()

    val pipelineSelectorPanel = PipelineSelectorPanel(eocvSim)
    val sourceSelectorPanel = SourceSelectorPanel(eocvSim)

    val opModePanel = JPanel()

    val opModeControlsPanel = OpModeControlsPanel(eocvSim)
    val opModeSelectorPanel = OpModeSelectorPanel(eocvSim, opModeControlsPanel)

    init {
        pipelinePanel.layout = GridLayout(2, 1)

        pipelineSelectorPanel.border = EmptyBorder(0, 20, 20, 20)
        pipelinePanel.add(pipelineSelectorPanel)

        sourceSelectorPanel.border = EmptyBorder(0, 20, 20, 20)
        pipelinePanel.add(sourceSelectorPanel)

        opModePanel.layout = GridLayout(2, 1)

        opModeSelectorPanel.border = EmptyBorder(0, 20, 20, 20)
        opModePanel.add(opModeSelectorPanel)

        opModeControlsPanel.border = EmptyBorder(0, 20, 20, 20)
        opModePanel.add(opModeControlsPanel)

        add("Pipeline", pipelinePanel)
        add("OpMode", opModePanel)

        addChangeListener {
            val sourceTabbedPane = it.source as JTabbedPane
            val index = sourceTabbedPane.selectedIndex

            if(index == 0) {
                opModeSelectorPanel.reset(0)
            } else if(index == 1) {
                opModeSelectorPanel.reset()
            }
        }
    }

    fun updateSelectorLists() {
        pipelineSelectorPanel.updatePipelinesList()
        opModeSelectorPanel.updateOpModesList()
    }

    fun updateSelectorListsBlocking() = runBlocking {
        launch(Dispatchers.Swing) {
            updateSelectorLists()
        }
    }

    fun refreshAndReselectCurrent() {
        saveLastSwitching()
        disableSwitching()

        pipelineSelectorPanel.refreshAndReselectCurrent()
        opModeSelectorPanel.refreshAndReselectCurrent()

        setLastSwitching()
    }

    fun refreshAndReselectCurrentBlocking() = runBlocking {
        launch(Dispatchers.Swing) {
            refreshAndReselectCurrent()
        }
    }

    fun enableSwitching() {
        pipelineSelectorPanel.allowPipelineSwitching = true
        opModeSelectorPanel.allowOpModeSwitching = true
    }

    fun disableSwitching() {
        pipelineSelectorPanel.allowPipelineSwitching = false
        opModeSelectorPanel.allowOpModeSwitching = false
    }

    fun saveLastSwitching() {
        pipelineSelectorPanel.saveLastSwitching()
        opModeSelectorPanel.saveLastSwitching()
    }

    fun setLastSwitching() {
        pipelineSelectorPanel.setLastSwitching()
        opModeSelectorPanel.setLastSwitching()
    }

    fun enableSwitchingBlocking() = runBlocking {
        launch(Dispatchers.Swing) {
            enableSwitching()
        }
    }

    fun disableSwitchingBlocking() = runBlocking {
        launch(Dispatchers.Swing) {
            disableSwitching()
        }
    }

}