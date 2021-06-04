package com.github.serivesmejia.eocvsim.gui.dialog

import com.github.serivesmejia.eocvsim.EOCVSim
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class BuildOutput(parent: JFrame, private val eocvSim: EOCVSim) {

    companion object {
        var isAlreadyOpened = false
            private set
    }

    private val buildOutput = JDialog(parent)

    private val buildOutputArea = JTextArea("")

    private val bottomButtonsPanel = JPanel()
    private val compileButton = JButton("Build again")

    private val compiledPipelineManager = eocvSim.pipelineManager.compiledPipelineManager

    init {
        eocvSim.visualizer.childDialogs.add(buildOutput)

        buildOutput.isModal = true
        buildOutput.title = "Build output"
        buildOutput.setSize(500, 350)

        buildOutput.contentPane.layout = GridBagLayout()

        buildOutputArea.isEditable = false
        buildOutputArea.lineWrap   = true

        val buildOutputScroll = JScrollPane(buildOutputArea)
        buildOutputScroll.verticalScrollBarPolicy   = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        buildOutputScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS

        buildOutput.contentPane.add(buildOutputScroll, GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            weightx = 0.5
            weighty = 1.0
        })

        bottomButtonsPanel.layout = BoxLayout(bottomButtonsPanel, BoxLayout.LINE_AXIS)

        bottomButtonsPanel.add(Box.createRigidArea(Dimension(4, 0)))

        compileButton.addActionListener {
            eocvSim.visualizer.asyncCompilePipelines()
        }

        bottomButtonsPanel.add(compileButton)
        bottomButtonsPanel.add(Box.createHorizontalGlue())

        val clearButton = JButton("Clear")
        clearButton.addActionListener { buildOutputArea.text = "" }

        bottomButtonsPanel.add(clearButton)
        bottomButtonsPanel.add(Box.createRigidArea(Dimension(4, 0)))

        val closeButton = JButton("Close")
        closeButton.addActionListener {
            buildOutput.isVisible = false
            isAlreadyOpened = false
        }

        bottomButtonsPanel.add(closeButton)
        bottomButtonsPanel.add(Box.createRigidArea(Dimension(4, 0)))

        buildOutput.contentPane.add(bottomButtonsPanel, GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            gridy = 1

            weightx = 1.0
            ipadx   = 10
            ipady   = 10
        })

        buildOutput.setLocationRelativeTo(null)
        registerListeners()

        isAlreadyOpened = true

        buildEnded()
        if(compiledPipelineManager.isBuildRunning) {
            buildRunning()
        }

        buildOutput.isVisible = true
    }

    private fun registerListeners() {
        buildOutput.addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                isAlreadyOpened = false
            }
        })
        compiledPipelineManager.onBuildStart {
            if(!buildOutput.isVisible) {
                it.removeThisPersistent()
            } else {
                buildRunning()
            }
        }

        compiledPipelineManager.onBuildEnd {
            if(!buildOutput.isVisible) {
                it.removeThisPersistent()
            } else {
                buildEnded()
            }
        }
    }

    private fun buildRunning() {
        compileButton.isEnabled = false
        buildOutputArea.text = "Build running..."
    }

    private fun buildEnded() {
        compiledPipelineManager.run {
            buildOutputArea.text = when {
                lastBuildOutputMessage != null -> lastBuildOutputMessage!!
                lastBuildResult != null -> lastBuildResult!!.message
                else -> "No output"
            }
        }
        compileButton.isEnabled = true
    }

}