package com.github.serivesmejia.eocvsim.tuner.field

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.TunableBoolean
import com.github.serivesmejia.eocvsim.tuner.TunableField
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

class BooleanField(
    instance: Any,
    reflectionField: VirtualField,
    eocvSim: EOCVSim
) : TunableField<Boolean>(instance, reflectionField, eocvSim, AllowMode.TEXT) {

    private var _value: Boolean = initialFieldValue as? Boolean ?: false

    private val tunableValue by lazy { TunableBoolean(_value, { _value }, { updateBoolean(it) }) }

    override val tunableValues by lazy { listOf(tunableValue) }

    private fun updateBoolean(newValue: Boolean) {
        _value = newValue
        setPipelineFieldValue(_value)
    }

    override fun init() {
        reflectionField.set(_value)
    }

    override fun refreshPipelineObject() {
        val current = reflectionField.get() as? Boolean ?: return
        _value = current
    }

    override val value: Boolean
        get() = _value
}
