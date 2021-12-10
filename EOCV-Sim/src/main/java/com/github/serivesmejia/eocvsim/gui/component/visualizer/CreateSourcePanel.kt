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

package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.component.PopupX
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox
import com.github.serivesmejia.eocvsim.gui.util.WebcamDriver
import com.github.serivesmejia.eocvsim.input.SourceType
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JPanel

class CreateSourcePanel(eocvSim: EOCVSim) : JPanel(GridLayout(2, 1)) {

    private val sourceSelectComboBox = EnumComboBox(
        "", SourceType::class.java, SourceType.values(),
        { it.coolName }, { SourceType.fromCoolName(it) }
    )

    private val cameraDriverComboBox = EnumComboBox(
        "Camera driver: ", WebcamDriver::class.java, WebcamDriver.values(),
        { it.name.replace("_", " ") }, { WebcamDriver.valueOf(it.replace(" ", "_")) }

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
            DialogFactory.createSourceDialog(eocvSim, sourceSelectComboBox.selectedEnum!!)
            popup?.hide()
        }
        nextPanel.add(nextButton)

        add(nextPanel)
    }

}
