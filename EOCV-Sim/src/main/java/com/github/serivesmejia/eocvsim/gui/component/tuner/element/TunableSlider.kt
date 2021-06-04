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

package com.github.serivesmejia.eocvsim.gui.component.tuner.element

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.SliderX
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.util.event.EventListener
import javax.swing.JLabel
import kotlin.math.roundToInt

class TunableSlider(val index: Int,
                    val tunableField: TunableField<*>,
                    val eocvSim: EOCVSim,
                    val valueLabel: JLabel? = null,
                    minBound: Double = 0.0,
                    maxBound: Double = 255.0) : SliderX(minBound, maxBound, 10) {

    var inControl = false

    constructor(i: Int, tunableField: TunableField<Any>, eocvSim: EOCVSim, valueLabel: JLabel) : this(i, tunableField, eocvSim, valueLabel, 0.0, 255.0)

    constructor(i: Int, tunableField: TunableField<Any>, eocvSim: EOCVSim) : this(i, tunableField, eocvSim, null, 0.0, 255.0)

    private val changeFieldValue = EventListener {
        if(inControl) {
            tunableField.setGuiFieldValue(index, scaledValue.toString())

            if (eocvSim.pipelineManager.paused)
                eocvSim.pipelineManager.setPaused(false)
        }
    }

    init {

        addChangeListener {
            eocvSim.onMainUpdate.doOnce(changeFieldValue)

            valueLabel?.text = if (tunableField.allowMode == TunableField.AllowMode.ONLY_NUMBERS_DECIMAL) {
                scaledValue.toString()
            } else {
                scaledValue.roundToInt().toString()
            }
        }

        tunableField.onValueChange {
            if (!inControl) {
                scaledValue = try {
                    tunableField.getGuiFieldValue(index).toString().toDouble()
                } catch(ignored: NumberFormatException) { 0.0 }
            }
        }
    }

}