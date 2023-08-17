package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import io.github.deltacv.vision.internal.opmode.OpModeNotification
import io.github.deltacv.vision.internal.opmode.OpModeState
import java.awt.BorderLayout
import javax.swing.JPanel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.SwingUtilities

class OpModeControlsPanel(val eocvSim: EOCVSim) : JPanel() {

    val controlButton = JButton()

    private var currentManagerIndex: Int? = null

    init {
        layout = BorderLayout()

        add(controlButton, BorderLayout.CENTER)

        controlButton.isEnabled = false
        controlButton.icon = EOCVSimIconLibrary.icoNotStarted

        controlButton.addActionListener {
            eocvSim.pipelineManager.onUpdate.doOnce {
                if(eocvSim.pipelineManager.currentPipeline !is OpMode) return@doOnce

                val opMode = eocvSim.pipelineManager.currentPipeline as OpMode
                val state = opMode.notifier.state

                opMode.notifier.notify(when(state) {
                    OpModeState.SELECTED -> OpModeNotification.INIT
                    OpModeState.INIT -> OpModeNotification.START
                    OpModeState.START -> OpModeNotification.STOP
                    else -> OpModeNotification.NOTHING
                })
            }
        }
    }

    fun stopCurrentOpMode() {
        if(eocvSim.pipelineManager.currentPipeline !is OpMode) return

        val opMode = eocvSim.pipelineManager.currentPipeline as OpMode
        opMode.notifier.notify(OpModeNotification.STOP)
    }

    private fun notifySelected() {
        if(eocvSim.pipelineManager.currentPipeline !is OpMode) return
        val opMode = eocvSim.pipelineManager.currentPipeline as OpMode

        opMode.notifier.onStateChange {
            val state = opMode.notifier.state

            SwingUtilities.invokeLater {
                updateButtonState(state)
            }

            if(state == OpModeState.STOPPED) {
                opModeSelected(currentManagerIndex!!)
            }
        }

        opMode.notifier.notify(OpModeState.SELECTED)
    }

    private fun updateButtonState(state: OpModeState) {
        when(state) {
            OpModeState.SELECTED -> controlButton.isEnabled = true
            OpModeState.INIT -> controlButton.icon = EOCVSimIconLibrary.icoPlay
            OpModeState.START -> controlButton.icon = EOCVSimIconLibrary.icoStop
            OpModeState.STOP -> {
                controlButton.isEnabled = false
            }
            OpModeState.STOPPED -> {
                controlButton.isEnabled = true

                controlButton.icon = EOCVSimIconLibrary.icoNotStarted
            }
        }
    }

    fun opModeSelected(managerIndex: Int) {
        if (!eocvSim.pipelineManager.paused) {
            eocvSim.pipelineManager.requestForceChangePipeline(managerIndex)
            currentManagerIndex = managerIndex

            eocvSim.pipelineManager.onUpdate.doOnce {
                notifySelected()
            }
        } else {
            if (eocvSim.pipelineManager.pauseReason !== PipelineManager.PauseReason.IMAGE_ONE_ANALYSIS) {
                controlButton.isEnabled = false
            } else { //handling pausing
                eocvSim.pipelineManager.requestSetPaused(false)
                eocvSim.pipelineManager.requestForceChangePipeline(managerIndex)
                currentManagerIndex = managerIndex

                eocvSim.pipelineManager.onUpdate.doOnce {
                    notifySelected()
                }
            }
        }
    }

    fun reset() {
        controlButton.isEnabled = false
        controlButton.icon = EOCVSimIconLibrary.icoNotStarted
    }

}