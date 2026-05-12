/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner.field.cv

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import org.opencv.core.Scalar

class ScalarField(
    instance: Any,
    reflectionField: VirtualField
) : TunableField<Scalar>(instance, reflectionField, AllowMode.ONLY_NUMBERS_DECIMAL) {


    private var scalar: Scalar = if (initialFieldValue == null) {
        Scalar(0.0, 0.0, 0.0)
    } else {
        (initialFieldValue as Scalar).clone()
    }

    private val val0 by lazy { TunableNumber(scalar.`val`[0], { scalar.`val`[0] }, { updateScalar(0, it) }) }
    private val val1 by lazy { TunableNumber(scalar.`val`[1], { scalar.`val`[1] }, { updateScalar(1, it) }) }
    private val val2 by lazy { TunableNumber(scalar.`val`[2], { scalar.`val`[2] }, { updateScalar(2, it) }) }
    private val val3 by lazy { TunableNumber(scalar.`val`[3], { scalar.`val`[3] }, { updateScalar(3, it) }) }

    override val tunableValues by lazy { listOf(val0, val1, val2, val3) }

    private fun updateScalar(index: Int, newValue: Double) {
        scalar.`val`[index] = newValue
        setPipelineFieldValue(scalar)
    }

    override fun init() {
        reflectionField.set(scalar)
    }

    override fun refreshPipelineObject() {
        val current = reflectionField.get() as Scalar
        scalar = current
    }

    override val value: Scalar
        get() = scalar
}

