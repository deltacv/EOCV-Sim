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

package com.github.serivesmejia.eocvsim.gui.component.tuner

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.Icons
import com.github.serivesmejia.eocvsim.gui.component.PopupX
import com.github.serivesmejia.eocvsim.util.extension.cvtColor
import com.github.serivesmejia.eocvsim.util.extension.clipUpperZero
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

class TunableFieldPanelOptions(val fieldPanel: TunableFieldPanel,
                               eocvSim: EOCVSim) : JPanel() {

    private val sliderIco    = Icons.getImageResized("ico_slider", 15, 15)
    private val textBoxIco   = Icons.getImageResized("ico_textbox", 15, 15)
    private val configIco    = Icons.getImageResized("ico_config", 15, 15)
    private val colorPickIco = Icons.getImageResized("ico_colorpick", 15, 15)

    private val textBoxSliderToggle   = JToggleButton()
    private val configButton          = JButton()
    private val colorPickButton       = JToggleButton()

    val configPanel = TunableFieldPanelConfig(this, eocvSim)
    var lastConfigPopup: PopupX? = null
        private set

    //toggle between textbox and slider ico
    var mode = TunableFieldPanel.Mode.TEXTBOXES
        set(value) {
            when(value) {
                TunableFieldPanel.Mode.SLIDERS -> {
                    textBoxSliderToggle.icon = textBoxIco
                    textBoxSliderToggle.isSelected = true
                }
                TunableFieldPanel.Mode.TEXTBOXES -> {
                    textBoxSliderToggle.icon = sliderIco
                    textBoxSliderToggle.isSelected = false
                }
            }

            handleResize()

            if(fieldPanel.mode != value) fieldPanel.mode = value
            configPanel.localConfig.fieldPanelMode = value

            field = value
        }

    init {
        //set initial icon for buttons
        textBoxSliderToggle.icon = sliderIco
        configButton.icon        = configIco
        colorPickButton.icon     = colorPickIco

        add(textBoxSliderToggle)
        add(configButton)
        add(colorPickButton)

        textBoxSliderToggle.addActionListener {
            mode = if(textBoxSliderToggle.isSelected) {
                TunableFieldPanel.Mode.SLIDERS
            } else {
                TunableFieldPanel.Mode.TEXTBOXES
            }
            configPanel.localConfig.fieldPanelMode = mode
        }

        configButton.addActionListener {
            val configLocation = configButton.locationOnScreen
            val buttonHeight   = configButton.height / 2

            val window = SwingUtilities.getWindowAncestor(this)
            val popup  = PopupX(window, configPanel, configLocation.x, configLocation.y - buttonHeight)

            popup.onShow.doOnce { configPanel.panelShow() }
            popup.onHide.doOnce { configPanel.panelHide() }

            //make sure we hide last config so
            //that we don't get a "stuck" popup
            //if the silly user is pressing the
            //button wayy too fast
            lastConfigPopup?.hide()
            lastConfigPopup = popup

            popup.show()
        }

        colorPickButton.addActionListener {
            val colorPicker = fieldPanel.tunableField.eocvSim.visualizer.colorPicker

            //start picking if global color picker is not being used by other panel
            if(!colorPicker.isPicking && colorPickButton.isSelected) {
                startPicking(colorPicker)
            } else { //handles cases when cancelling picking
                colorPicker.stopPicking()
                //if we weren't the ones controlling the last picking,
                //start picking again to gain control for this panel
                if(colorPickButton.isSelected) startPicking(colorPicker)
            }
        }

        fieldPanel.addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) = handleResize()
        })

        addAncestorListener(object: AncestorListener {
            override fun ancestorRemoved(event: AncestorEvent?) {}
            override fun ancestorMoved(event: AncestorEvent?) {}

            override fun ancestorAdded(event: AncestorEvent?) = handleResize()
        })
    }

    private fun startPicking(colorPicker: ColorPicker) {
        //when user picks a color
        colorPicker.onPick.doOnce {
            val colorScalar = colorPicker.colorRgb.cvtColor(configPanel.localConfig.pickerColorSpace.cvtCode)

            //setting the scalar value in order from first to fourth field
            for(i in 0..(fieldPanel.fields.size - 1).clipUpperZero()) {
                //if we're still in range of the scalar values amount
                if(i < colorScalar.`val`.size) {
                    val colorVal = colorScalar.`val`[i]
                    fieldPanel.setFieldValue(i, colorVal)
                    fieldPanel.tunableField.setGuiFieldValue(i, colorVal.toString())
                } else { break } //keep looping until we write the entire scalar value
            }
            colorPickButton.isSelected = false
        }

        //handles cancel cases, mostly when passing control to another panel
        colorPicker.onCancel.doOnce { colorPickButton.isSelected = false }

        //might want to start picking to this panel here...
        colorPicker.startPicking()
    }

    //handling resizes for responsive buttons arrangement
    private fun handleResize() {
        val buttonsHeight = textBoxSliderToggle.height + colorPickButton.height + configButton.height

        layout = if(fieldPanel.height > buttonsHeight && mode == TunableFieldPanel.Mode.SLIDERS) {
            GridLayout(3, 1)
        } else {
            FlowLayout()
        }

        revalAndRepaint()
    }

    //reevaluates the config of this field panel from the eocv sim config
    fun reevaluateConfig() {
        //only reevaluate if our config is not local
        if(configPanel.localConfig.source != TunableFieldPanelConfig.ConfigSource.LOCAL) {
            configPanel.applyFromEOCVSimConfig()
        }
    }

    private fun revalAndRepaint() {
        textBoxSliderToggle.revalidate()
        textBoxSliderToggle.repaint()

        revalidate()
        repaint()
    }

}