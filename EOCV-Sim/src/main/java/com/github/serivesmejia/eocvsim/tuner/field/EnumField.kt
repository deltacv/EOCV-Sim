package com.github.serivesmejia.eocvsim.tuner.field

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableFieldAcceptor
import com.github.serivesmejia.eocvsim.tuner.scanner.RegisterTunableField
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import org.openftc.easyopencv.OpenCvPipeline

@RegisterTunableField
class EnumField(private val instance: OpenCvPipeline,
                reflectionField: VirtualField,
                eocvSim: EOCVSim) : TunableField<Enum<*>>(instance, reflectionField, eocvSim, AllowMode.TEXT) {

    val values = reflectionField.type.enumConstants

    private val initialValue = initialFieldValue as Enum<*>

    private var currentValue = initialValue
    private var beforeValue: Any? = null

    init {
        guiComboBoxAmount = 1
        guiFieldAmount = 0
    }

    override fun init() {
        fieldPanel.setComboBoxSelection(0, currentValue)
    }

    override fun update() {
        if(hasChanged()) {
            currentValue = value
            updateGuiFieldValues()
        }
        beforeValue = currentValue
    }

    override fun updateGuiFieldValues() {
        fieldPanel.setComboBoxSelection(0, currentValue)
    }

    override fun setFieldValue(index: Int, newValue: Any) {
        reflectionField.set(newValue)
    }

    override fun setComboBoxValueFromGui(index: Int, newValue: String) = setFieldValueFromGui(index, newValue)

    override fun setFieldValueFromGui(index: Int, newValue: String) {
        currentValue = java.lang.Enum.valueOf(initialValue::class.java, newValue)
        setFieldValue(index, currentValue)
    }

    override fun getValue() = currentValue

    override fun getGuiFieldValue(index: Int) = currentValue.name

    override fun getGuiComboBoxValues(index: Int): Array<out Any> {
        return values
    }

    override fun hasChanged() = reflectionField.get() != beforeValue

    class EnumFieldAcceptor : TunableFieldAcceptor {
        override fun accept(clazz: Class<*>) = clazz.isEnum
    }

}