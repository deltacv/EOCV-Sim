/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner.field.cv

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import org.opencv.core.Rect
import org.deltacv.eocvsim.virtualreflect.VirtualField

class RectField(instance: Any, reflectionField: VirtualField) :
    TunableField<Rect>(instance, reflectionField, AllowMode.ONLY_NUMBERS_DECIMAL) {


    private var rect = arrayOf(0.0, 0.0, 0.0, 0.0)

    private var initialRect = if(initialFieldValue != null)
        (initialFieldValue as Rect).clone()
    else Rect(0, 0, 0, 0)

    private val xValue by lazy { TunableNumber(rect[0], { rect[0] }, { updateRect(0, it) }) }
    private val yValue by lazy { TunableNumber(rect[1], { rect[1] }, { updateRect(1, it) }) }
    private val wValue by lazy { TunableNumber(rect[2], { rect[2] }, { updateRect(2, it) }) }
    private val hValue by lazy { TunableNumber(rect[3], { rect[3] }, { updateRect(3, it) }) }

    private fun updateRect(index: Int, newValue: Double) {
        rect[index] = newValue
        initialRect.set(rect.toDoubleArray())
        setPipelineFieldValue(initialRect)
    }

    override val tunableValues by lazy { listOf(xValue, yValue, wValue, hValue) }

    init {
        rect[0] = initialRect.x.toDouble()
        rect[1] = initialRect.y.toDouble()
        rect[2] = initialRect.width.toDouble()
        rect[3] = initialRect.height.toDouble()
    }

    override fun init() {
        reflectionField.set(initialRect)
    }

    override fun refreshPipelineObject() {
        initialRect = reflectionField.get() as Rect

        rect[0] = initialRect.x.toDouble()
        rect[1] = initialRect.y.toDouble()
        rect[2] = initialRect.width.toDouble()
        rect[3] = initialRect.height.toDouble()
    }

    override val value: Rect
        get() = Rect(rect.toDoubleArray())
}
