package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.component.PopupX.Companion.popUpXOnThis
import com.qualcomm.robotcore.eventloop.opmode.OpModeType
import kotlinx.coroutines.runBlocking
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*


class OpModeSelectorPanel(val eocvSim: EOCVSim) : JPanel() {

    var selectedIndex = -1
        set(value) {
            field = value
        }

    // <opModeSelector index, PipelineManager index>
    private val indexMap = mutableMapOf<Int, Int>()

    val autonomousButton = JButton()

    val textPanel = JPanel()

    val selectOpModeLabel = JLabel("Select Op Mode")
    val buttonDescriptorLabel = JLabel("<- Autonomous | TeleOp ->")

    val teleopButton = JButton()

    init {
        layout = GridBagLayout()
        textPanel.layout = GridBagLayout()

        autonomousButton.icon = EOCVSimIconLibrary.icoArrowDropdown

        add(autonomousButton, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            ipady = 20

            gridheight = 1
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

        teleopButton.icon = EOCVSimIconLibrary.icoArrowDropdown

        add(teleopButton, GridBagConstraints().apply {
            gridx = 2
            gridy = 0
            ipady = 20

            gridheight = 1
        })

        registerListeners()
    }

    private fun registerListeners() {
        autonomousButton.addActionListener {
            autonomousButton.popUpXOnThis(OpModePopupPanel()).show()
        }
    }

    fun updateOpModesList() = runBlocking {

    }

    fun revalAndRepaint() {
        /* opModeSelector.revalidate()
        opModeSelector.repaint()
        opModeSelectorScroll.revalidate()
        opModeSelectorScroll.repaint() */
    }

}