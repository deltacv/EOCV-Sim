package com.github.serivesmejia.eocvsim.gui.component.tuner


import com.github.serivesmejia.eocvsim.gui.component.tuner.element.TunableComboBox
import com.github.serivesmejia.eocvsim.gui.component.tuner.element.TunableSlider
import com.github.serivesmejia.eocvsim.gui.component.tuner.element.TunableTextField
import com.github.serivesmejia.eocvsim.tuner.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.SoftBevelBorder

class TunableFieldPanel(val tunableField: TunableField<*>) : JPanel(), KoinComponent {



    var fields: Array<TunableTextField?>? = null
    var fieldsPanel: JPanel? = null

    var sliders: Array<TunableSlider?>? = null
    var slidersPanel: JPanel? = null

    var comboBoxes: Array<JComboBox<*>?>? = null

    var panelOptions: TunableFieldPanelOptions? = null

    var mode: Mode = Mode.TEXTBOXES
        set(value) {
            when (value) {
                Mode.TEXTBOXES -> {
                    if (this.mode == Mode.SLIDERS) {
                        slidersPanel?.let { remove(it) }
                    }

                    fields?.let { 
                        for (i in it.indices) {
                            it[i]?.isInControl = true
                            sliders?.get(i)?.inControl = false
                            it[i]?.let { setFieldValue(i, tunableField.tunableValues[i].value) }
                        }
                    }

                    fieldsPanel?.let { add(it) }
                }

                Mode.SLIDERS -> {
                    if (this.mode == Mode.TEXTBOXES) {
                        fieldsPanel?.let { remove(it) }
                    }

                    fields?.let { 
                        for (i in it.indices) {
                            it[i]?.isInControl = false
                            sliders?.get(i)?.inControl = true
                            it[i]?.let { setFieldValue(i, tunableField.tunableValues[i].value) }
                        }
                    }

                    slidersPanel?.let { add(it) }
                }
            }

            field = value

            if (panelOptions?.mode != value) {
                panelOptions?.mode = field
            }

            revalidate()
            repaint()
        }

    private var reevalConfigRequested = false
    private var hasBeenShown = false

    enum class Mode { TEXTBOXES, SLIDERS }

    init {
        tunableField.setTunableFieldPanel(this)
        initComponents()
    }

    private fun initComponents() {
        border = SoftBevelBorder(SoftBevelBorder.RAISED)

        val values = tunableField.tunableValues

        fields = arrayOfNulls(values.size)
        sliders = arrayOfNulls(values.size)
        comboBoxes = arrayOfNulls(values.size)

        fieldsPanel = JPanel()
        slidersPanel = JPanel(GridBagLayout())

        panelOptions = TunableFieldPanelOptions(this)

        val hasFieldsOrSliders = values.any { it is TunableNumber || it is TunableString }

        if (hasFieldsOrSliders) {
            add(panelOptions)
        }

        val fieldNameLabel = JLabel(tunableField.fieldName)
        add(fieldNameLabel)

        for (i in values.indices) {
            val value = values[i]

            if (value is TunableNumber || value is TunableString) {
                val field = TunableTextField(value)
                fields!![i] = field
                field.isEditable = true
                fieldsPanel!!.add(field)

                if (value is TunableNumber) {
                    val sliderLabel = JLabel("0")
                    val slider = TunableSlider(value, sliderLabel)
                    sliders!![i] = slider

                    val cSlider = GridBagConstraints().apply {
                        gridx = 0
                        gridy = i
                    }

                    val cLabel = GridBagConstraints().apply {
                        gridx = 1
                        gridy = i
                    }

                    slidersPanel!!.add(slider, cSlider)
                    slidersPanel!!.add(sliderLabel, cLabel)
                }
            } else if (value is TunableEnum<*>) {
                val comboBox = TunableComboBox(value)
                add(comboBox)
                comboBoxes!![i] = comboBox
            }
        }

        mode = Mode.TEXTBOXES
    }

    fun showFieldPanel() {
        if (hasBeenShown) return
        hasBeenShown = true

        panelOptions?.configPanel?.updateFieldGuiFromConfig()
        if (!tunableField.isOnlyNumbers) {
            mode = Mode.SLIDERS
        }
    }

    fun setFieldValue(index: Int, value: Any) {
        if (fields == null || index >= fields!!.size) return
        val field = fields!![index] ?: return

        if (field.isFocusOwner) return
        if (sliders?.get(index)?.isFocusOwner == true) return

        val tv = tunableField.tunableValues[index]
        
        val text = if (tv is TunableNumber && tv.isOnlyNumbers) {
            (value.toString().toDouble().toInt()).toString()
        } else {
            value.toString()
        }

        field.text = text

        try {
            sliders?.get(index)?.scaledValue = value.toString().toDouble()
        } catch (_: NumberFormatException) { }
    }

    fun setComboBoxSelection(index: Int, selection: Any) {
        comboBoxes?.get(index)?.selectedItem = selection.toString()
    }

    fun requestAllConfigReeval() {
        reevalConfigRequested = true
    }

    fun setSlidersRange(min: Double, max: Double) {
        sliders?.let {
            for (slider in it) {
                slider?.setScaledBounds(min, max)
            }
        }
    }

    fun hasRequestedAllConfigReeval(): Boolean {
        val current = reevalConfigRequested
        reevalConfigRequested = false
        return current
    }

}
