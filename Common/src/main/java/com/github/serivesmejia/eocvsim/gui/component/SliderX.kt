/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
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
