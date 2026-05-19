/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner.field

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import org.deltacv.eocvsim.virtualreflect.VirtualField

abstract class NumericField<T : Number>(
    target: Any,
    reflectionField: VirtualField,
    allowMode: AllowMode,
    initialValue: T
) : TunableField<T>(target, reflectionField, allowMode) {


    protected var _value: T = initialValue

    protected val tunableValue by lazy { TunableNumber(_value.toDouble(), { _value.toDouble() }, { updateNumber(it) }) }

    override val tunableValues by lazy { listOf(tunableValue) }

    abstract fun createNumber(value: Double): T

    private fun updateNumber(newValue: Double) {
        _value = createNumber(newValue)
        setPipelineFieldValue(_value)
    }

    override fun init() {
        reflectionField.set(_value)
    }

    override fun refreshPipelineObject() {
        _value = createNumber((reflectionField.get() as? Number)?.toDouble() ?: 0.0)
    }

    override val value: T
        get() = _value
}

