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
import com.github.serivesmejia.eocvsim.gui.component.PopupX
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields
import com.github.serivesmejia.eocvsim.tuner.TunableField
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class TunableFieldPanelConfig(private val fieldOptions: TunableFieldPanelOptions,
                              private val eocvSim: EOCVSim) : JPanel() {

    var localConfig = eocvSim.config.globalTunableFieldsConfig.copy()
        private set

    private var lastApplyPopup: PopupX? = null

    val currentConfig: Config
        get() {
            val config = localConfig.copy()
            applyToConfig(config)
            return config
        }

    private val sliderRangeFieldsPanel = JPanel()

    private var sliderRangeFields     = createRangeFields()
    private val colorSpaceComboBox    = EnumComboBox("Color space: ", PickerColorSpace::class.java, PickerColorSpace.values())

    private val applyToAllButtonPanel = JPanel(GridBagLayout())
    private val applyToAllButton      = JToggleButton("Apply to all fields...")

    private val applyModesPanel             = JPanel()
    private val applyToAllGloballyButton    = JButton("Globally")
    private val applyToAllOfSameTypeButton  = JButton("Of this type")

    private val constCenterBottom = GridBagConstraints()

    private val configSourceLabel = JLabel(localConfig.source.description)

    private val allowsDecimals
        get() = fieldOptions.fieldPanel.tunableField.allowMode == TunableField.AllowMode.ONLY_NUMBERS_DECIMAL

    private val fieldTypeClass = fieldOptions.fieldPanel.tunableField::class.java

    //represents a color space conversion when picking from the viewport. always
    //convert from rgb to the desired color space since that's the color space of
    //the scalar the ColorPicker returns from the viewport after picking.
    enum class PickerColorSpace(val cvtCode: Int) {
        YCrCb(Imgproc.COLOR_RGB2YCrCb),
        HSV(Imgproc.COLOR_RGB2HSV),
        RGB(Imgproc.COLOR_RGBA2RGB),
        Lab(Imgproc.COLOR_RGB2Lab)
    }

    enum class ConfigSource(val description: String) {
        LOCAL("From local config"),
        GLOBAL("From global config"),
        GLOBAL_DEFAULT("From default global config"),
        TYPE_SPECIFIC("From specific config")
    }

    data class Config(var sliderRange: Size,
                      var pickerColorSpace: PickerColorSpace,
                      var fieldPanelMode: TunableFieldPanel.Mode,
                      var source: ConfigSource)

    init {
        layout = GridBagLayout()

        val mConstraints   = GridBagConstraints()
        mConstraints.ipady = 10

        //adding into an individual panel so that we can add
        //and remove later when recreating without much problem
        sliderRangeFieldsPanel.add(sliderRangeFields)

        mConstraints.gridy = 0
        add(sliderRangeFieldsPanel, mConstraints)

        colorSpaceComboBox.onSelect { updateConfigSourceLabel(currentConfig) }
        //combo box to select color space
        colorSpaceComboBox.selectedEnum = localConfig.pickerColorSpace

        mConstraints.gridy = 1
        add(colorSpaceComboBox, mConstraints)

        //centering apply to all button...
        val constCenter    = GridBagConstraints()
        constCenter.anchor = GridBagConstraints.CENTER
        constCenter.fill   = GridBagConstraints.HORIZONTAL
        constCenter.gridy  = 0

        //add apply to all button to a centered pane
        applyToAllButtonPanel.add(applyToAllButton, constCenter)

        mConstraints.gridy = 2
        add(applyToAllButtonPanel, mConstraints)

        //display or hide apply to all mode buttons
        applyToAllButton.addActionListener {
            //create a new popup for displaying the apply modes button
            if(applyToAllButton.isSelected && (lastApplyPopup == null || lastApplyPopup?.window?.isVisible == false)) {
                val window   = SwingUtilities.getWindowAncestor(fieldOptions) //gets the parent frame
                val location = applyToAllButton.locationOnScreen

                val popup      = PopupX(window, applyModesPanel, location.x, location.y)
                lastApplyPopup = popup //set to a "last" variable so that we can hide it later

                //so that the main config popup doesn't get closed
                //when it gets unfocused in favour of this new frame
                fieldOptions.lastConfigPopup?.closeOnFocusLost = false

                popup.onShow {
                    popup.setLocation(
                        popup.window.location.x - applyModesPanel.width / 8,
                        popup.window.location.y + applyModesPanel.height + applyToAllButton.height
                    )
                }

                //untoggle the apply to all button if the popup closes
                popup.onHide {
                    applyToAllButton.isSelected = false

                    fieldOptions.lastConfigPopup?.let {
                        //allow the main config popup to close when losing focus now
                        it.closeOnFocusLost = true

                        //launch the waiting in the background
                        GlobalScope.launch {
                            delay(100)
                            //close config popup if still hasn't focused after a bit
                            launch(Dispatchers.Swing) {
                                if (!it.window.isFocused && (lastApplyPopup == null || lastApplyPopup?.window?.isFocused == false)) {
                                    it.hide()
                                }
                            }
                        }
                    }
                }

                popup.show()
            } else {
                lastApplyPopup?.hide() //close the popup if user un-toggled button
            }
        }

        applyModesPanel.layout = BoxLayout(applyModesPanel, BoxLayout.LINE_AXIS)

        //apply globally button and disable toggle for apply to all button
        applyToAllGloballyButton.addActionListener {
            lastApplyPopup?.hide()
            applyGlobally()
        }

        applyModesPanel.add(applyToAllGloballyButton)

        //creates a space between the apply mode buttons
        applyModesPanel.add(Box.createRigidArea(Dimension(5, 0)))

        //apply of same type button and disable toggle for apply to all button
        applyToAllOfSameTypeButton.addActionListener {
            lastApplyPopup?.hide()
            applyOfSameType()
        }
        applyModesPanel.add(applyToAllOfSameTypeButton)

        //add a bit of space between the upper and lower apply to all buttons
        applyModesPanel.border = EmptyBorder(5, 0, 0, 0)

        //add two apply to all modes buttons to the bottom center
        constCenterBottom.anchor = GridBagConstraints.CENTER
        constCenterBottom.fill   = GridBagConstraints.HORIZONTAL
        constCenterBottom.gridy  = 1

        configSourceLabel.horizontalAlignment = JLabel.CENTER
        configSourceLabel.verticalAlignment = JLabel.CENTER

        mConstraints.gridy = 3
        add(configSourceLabel, mConstraints)

        applyFromEOCVSimConfig()
    }

    //set the current config values and hide apply modes panel when panel show
    fun panelShow() {
        updateConfigGuiFromConfig()
        applyToAllButton.isSelected = false
    }

    //set the slider bounds when the popup gets closed
    fun panelHide() {
        applyToConfig()
        updateFieldGuiFromConfig()
        lastApplyPopup?.hide()
    }

    //applies the config of this tunable field panel globally
    private fun applyGlobally() {
        applyToConfig() //saves the current values to the current local config

        localConfig.source = ConfigSource.GLOBAL //changes the source of the local config to global
        eocvSim.config.globalTunableFieldsConfig = localConfig.copy()

        updateConfigSourceLabel()
        fieldOptions.fieldPanel.requestAllConfigReeval()
    }

    //applies the config of this tunable field to this type specifically
    private fun applyOfSameType() {
        applyToConfig() //saves the current values to the current local config
        val typeClass = fieldOptions.fieldPanel.tunableField::class.java

        localConfig.source = ConfigSource.TYPE_SPECIFIC //changes the source of the local config to type specific
        eocvSim.config.specificTunableFieldConfig[typeClass.name] = localConfig.copy()

        updateConfigSourceLabel()
        fieldOptions.fieldPanel.requestAllConfigReeval()
    }

    //loads the config from global eocv sim config file
    internal fun applyFromEOCVSimConfig() {
        val specificConfigs = eocvSim.config.specificTunableFieldConfig

        //apply specific config if we have one, or else, apply global
        localConfig = if(specificConfigs.containsKey(fieldTypeClass.name)) {
            specificConfigs[fieldTypeClass.name]!!.copy()
        } else {
            eocvSim.config.globalTunableFieldsConfig.copy()
        }

        updateConfigGuiFromConfig()
        updateConfigSourceLabel()
    }

    //applies the current values to the specified config, defaults to local
    @Suppress("UNNECESSARY_SAFE_CALL")
    private fun applyToConfig(config: Config = localConfig) {
        //if user entered a valid number and our max value is bigger than the minimum...
        if(sliderRangeFields.valid) {
            config.sliderRange = sliderRangeFields.currentSize
            //update slider range in gui sliders...
            if(config.sliderRange.height > config.sliderRange.width && config !== localConfig)
                updateFieldGuiFromConfig()
        }

        //set the color space enum to the config if it's not null
        colorSpaceComboBox.selectedEnum?.let {
            config.pickerColorSpace = it
        }

        //sets the panel mode (sliders or textboxes) to config from the current mode
        if(fieldOptions.fieldPanel?.mode != null) {
            config.fieldPanelMode = fieldOptions.fieldPanel.mode
        }
    }

    private fun updateConfigSourceLabel(currentConfig: Config = localConfig) {
        //sets to local if user changed values and hasn't applied locally or globally
        if(currentConfig != localConfig) {
            localConfig.source = ConfigSource.LOCAL
        }

        configSourceLabel.text = localConfig.source.description
    }

    //updates the actual configuration displayed on the field panel gui
    @Suppress("UNNECESSARY_SAFE_CALL")
    fun updateFieldGuiFromConfig() {
        //sets the slider range from config
        fieldOptions.fieldPanel.setSlidersRange(localConfig.sliderRange.width, localConfig.sliderRange.height)
        //sets the panel mode (sliders or textboxes) to config from the current mode
        if(fieldOptions.fieldPanel?.fields != null){
            fieldOptions.fieldPanel.mode = localConfig.fieldPanelMode
        }
    }

    //updates the values displayed in this config's ui to the current config values
    private fun updateConfigGuiFromConfig() {
        sliderRangeFieldsPanel.remove(sliderRangeFields) //remove old fields
        sliderRangeFields = createRangeFields() //need to recreate in order to set new values
        sliderRangeFieldsPanel.add(sliderRangeFields) //add new fields

        //need to reval&repaint as always
        sliderRangeFieldsPanel.revalidate(); sliderRangeFieldsPanel.repaint()

        colorSpaceComboBox.selectedEnum = localConfig.pickerColorSpace
    }

    //simple short hand for a repetitive instantiation...
    private fun createRangeFields(): SizeFields {
        val fields = SizeFields(localConfig.sliderRange, allowsDecimals, true,"Slider range:", " to ")
        fields.onChange {
            updateConfigSourceLabel(currentConfig)
        }

        return fields
    }

}