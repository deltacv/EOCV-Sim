/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.tuner

import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.component.PopupX
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import org.deltacv.vision.external.util.extension.cvtColor
import com.github.serivesmejia.eocvsim.util.extension.clipUpperZero
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TunableFieldPanelOptions(val fieldPanel: TunableFieldPanel) : JPanel(), KoinComponent {

    val visualizer: Visualizer by inject()


    private val sliderIco    by EOCVSimIconLibrary.icoSlider.lazyResized(15, 15)
    private val textBoxIco   by EOCVSimIconLibrary.icoTextbox.lazyResized(15, 15)
    private val configIco    by EOCVSimIconLibrary.icoConfig.lazyResized(15, 15)
    private val colorPickIco by EOCVSimIconLibrary.icoColorPick.lazyResized(15, 15)

    private val textBoxSliderToggle   = JToggleButton()
    private val configButton          = JButton()
    private val colorPickButton       = JToggleButton()

    val configPanel = TunableFieldPanelConfig(this)

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

            popup.onShow.once { configPanel.panelShow() }
            popup.onHide.once { configPanel.panelHide() }

            //make sure we hide last config so
            //that we don't get a "stuck" popup
            //if the silly user is pressing the
            //button wayy too fast
            lastConfigPopup?.hide()
            lastConfigPopup = popup

            popup.show()
        }

        colorPickButton.addActionListener {
            val colorPicker = visualizer.colorPicker


            //start picking if global color picker is not being used by other panel
            if(!colorPicker.isPicking && colorPickButton.isSelected) {
                startPicking(colorPicker)
            } else { //handles cases when cancelling picking
                colorPicker.stopPicking()
                // if we weren't the ones controlling the last picking,
                // start picking again to gain control for this panel
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
        colorPicker.onPick.once {
            val colorScalar = colorPicker.colorRgb.cvtColor(configPanel.localConfig.pickerColorSpace.cvtCode)

            fieldPanel.fields?.let {
                //setting the scalar value in order from first to fourth field
                for (i in 0..(it.size - 1).clipUpperZero()) {
                    //if we're still in range of the scalar values amount
                    if (i < colorScalar.`val`.size) {
                        val colorVal = colorScalar.`val`[i]

                        val tv = fieldPanel.tunableField.tunableValues.getOrNull(i) as? TunableNumber
                        tv?.setFromGui(colorVal)
                    } else {
                        break
                    } //keep looping until we write the entire scalar value
                }
            }

            colorPickButton.isSelected = false
        }

        //handles cancel cases, mostly when passing control to another panel
        colorPicker.onCancel.once { colorPickButton.isSelected = false }

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
