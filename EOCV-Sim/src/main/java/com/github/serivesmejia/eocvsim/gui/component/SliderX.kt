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

package com.github.serivesmejia.eocvsim.gui.component

import com.qualcomm.robotcore.util.Range
import javax.swing.JSlider
import kotlin.math.roundToInt

/**
 * Allows for a slider to take a range of type double
 * and return a value of type double, instead of int.
 *
 * Achieved by upscaling the input bounds and the input
 * value by a certain amount (multiplier of 10), and
 * downscaling the value when getting it
 */
open class SliderX(private var minBound: Double,
                   private var maxBound: Double,
                   private val scale: Int) : JSlider() {

    var scaledValue: Double = 0.0
        set(value) {
            field = Range.clip(value * scale, minimum.toDouble(), maximum.toDouble())
            this.value = field.roundToInt()
        }
        get() {
            return Range.clip(this.value.toDouble() / scale, minBound, maxBound)
        }

    init {
        setScaledBounds(minBound, maxBound)
        setMajorTickSpacing(scale)
        setMinorTickSpacing(scale / 4)
    }

    fun setScaledBounds(minBound: Double, maxBound: Double) {
        //for some reason we have to scale min bound when
        //going negative... but not when going positive
        this.minBound = if(minBound > 0) {
            minBound
        } else {
            minBound * scale
        }

        this.maxBound = maxBound * scale

        minimum = this.minBound.roundToInt()
        maximum = this.maxBound.roundToInt()
    }

}