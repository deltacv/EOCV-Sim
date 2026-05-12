/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.component.PopupX
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox
import com.github.serivesmejia.eocvsim.input.SourceType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JPanel

class CreateSourcePanel(eocvSim: EOCVSim) : JPanel(GridLayout(2, 1)), KoinComponent {

    private val dialogFactory: DialogFactory by inject()

    private val sourceSelectComboBox = EnumComboBox(
            "", SourceType::class.java, SourceType.values(),
            { it.coolName }, { SourceType.fromCoolName(it) }
    )

    private val sourceSelectPanel    = JPanel(FlowLayout(FlowLayout.CENTER))

    private val nextButton = JButton("Next")
    private val nextPanel  = JPanel()

    var popup: PopupX? = null

    init {
        sourceSelectComboBox.removeEnumOption(SourceType.UNKNOWN) //removes the UNKNOWN enum
        sourceSelectPanel.add(sourceSelectComboBox) //add to separate panel to center element
        add(sourceSelectPanel) //add centered panel to this

        nextButton.addActionListener {
            //creates "create source" dialog from selected enum
            dialogFactory.createSourceDialog(sourceSelectComboBox.selectedEnum!!)
            popup?.hide()
        }
        nextPanel.add(nextButton)

        add(nextPanel)
    }

}
