package com.github.serivesmejia.eocvsim.tuner.field

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableString
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

class StringField(
    instance: Any,
    reflectionField: VirtualField
) : TunableField<String>(instance, reflectionField, AllowMode.TEXT) {


    private var _value: String = initialFieldValue as? String ?: ""

    private val tunableValue by lazy { TunableString(_value, { _value }, { updateString(it) }) }

    override val tunableValues by lazy { listOf(tunableValue) }

    private fun updateString(newValue: String) {
        _value = newValue
        setPipelineFieldValue(_value)
    }

    override fun init() {
        reflectionField.set(_value)
    }

    override fun refreshPipelineObject() {
        val current = reflectionField.get() as? String ?: return
        _value = current
    }

    override val value: String
        get() = _value
}
