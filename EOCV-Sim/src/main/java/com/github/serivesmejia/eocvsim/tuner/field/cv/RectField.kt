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
import com.github.serivesmejia.eocvsim.tuner.scanner.RegisterTunableField
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import org.opencv.core.Rect
import org.openftc.easyopencv.OpenCvPipeline
import java.lang.reflect.Field

@RegisterTunableField
class RectField(instance: Any, reflectionField: VirtualField, eocvSim: EOCVSim) :
    TunableField<Rect>(instance, reflectionField, eocvSim, AllowMode.ONLY_NUMBERS_DECIMAL) {

    private var rect = arrayOf(0.0, 0.0, 0.0, 0.0)
    private var lastRect = arrayOf(0.0, 0.0, 0.0, 0.0)

    @Volatile private var hasChanged = false

    private var initialRect = if(initialFieldValue != null)
        (initialFieldValue as Rect).clone()
    else Rect(0, 0, 0, 0)

    init {
        rect[0] = initialRect.x.toDouble()
        rect[1] = initialRect.y.toDouble()
        rect[2] = initialRect.width.toDouble()
        rect[3] = initialRect.height.toDouble()

        guiFieldAmount = 4
    }

    override fun init() {
        reflectionField.set(initialRect)
    }

    override fun update() {
        if(hasChanged()){
            initialRect = reflectionField.get() as Rect

            rect[0] = initialRect.x.toDouble()
            rect[1] = initialRect.y.toDouble()
            rect[2] = initialRect.width.toDouble()
            rect[3] = initialRect.height.toDouble()

            updateGuiFieldValues()
        }
    }

    override fun setFieldValue(index: Int, newValue: Any) {
        try {
            rect[index] = if(newValue is String)
                newValue.toDouble()
            else (newValue as Number).toDouble()
        } catch (e: Exception) {
            throw IllegalArgumentException("Parameter should be a valid numeric value", e)
        }

        initialRect.set(rect.toDoubleArray())
        setPipelineFieldValue(initialRect)

        lastRect[0] = initialRect.x.toDouble()
        lastRect[1] = initialRect.y.toDouble()
        lastRect[2] = initialRect.width.toDouble()
        lastRect[3] = initialRect.height.toDouble()
    }

    override fun updateGuiFieldValues() {
        for((i, value) in rect.withIndex()) {
            fieldPanel.setFieldValue(i, value)
        }
    }

    override fun setFieldValueFromGui(index: Int, newValue: String) {
        setFieldValue(index, newValue)
    }

    override fun getValue(): Rect = Rect(rect.toDoubleArray())

    override fun getGuiFieldValue(index: Int): Any = rect[index]

    override fun hasChanged(): Boolean {
        hasChanged = rect[0] != lastRect[0] || rect[1] != lastRect[1]
                || rect[2] != lastRect[2] || rect[3] != lastRect[3]
        return hasChanged
    }

}