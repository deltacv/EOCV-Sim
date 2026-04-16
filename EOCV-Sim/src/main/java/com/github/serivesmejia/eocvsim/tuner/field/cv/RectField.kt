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

package com.github.serivesmejia.eocvsim.tuner.field.cv

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import org.opencv.core.Rect
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

class RectField(instance: Any, reflectionField: VirtualField, eocvSim: EOCVSim) :
    TunableField<Rect>(instance, reflectionField, eocvSim, AllowMode.ONLY_NUMBERS_DECIMAL) {

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