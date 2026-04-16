package com.github.serivesmejia.eocvsim.tuner.field

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

abstract class NumericField<T : Number>(
    target: Any,
    reflectionField: VirtualField,
    eocvSim: EOCVSim,
    allowMode: AllowMode
) : TunableField<T>(target, reflectionField, eocvSim, allowMode) {

    @Suppress("UNCHECKED_CAST")
    protected var _value: T? = initialFieldValue as T?

    protected val tunableValue by lazy { TunableNumber(_value?.toDouble() ?: 0.0, { _value?.toDouble() ?: 0.0 }, { updateNumber(it) }) }

    override val tunableValues by lazy { listOf(tunableValue) }

    abstract fun createNumber(value: Double): T

    private fun updateNumber(newValue: Double) {
        _value = createNumber(newValue)
        setPipelineFieldValue(_value!!)
    }

    override fun init() {
        reflectionField.set(_value)
    }

    override fun refreshPipelineObject() {
        @Suppress("UNCHECKED_CAST")
        val current = reflectionField.get() as T
        _value = current
    }

    override val value: T
        get() = _value!!
}
