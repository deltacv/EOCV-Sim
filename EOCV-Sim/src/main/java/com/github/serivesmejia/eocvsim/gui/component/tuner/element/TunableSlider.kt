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

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.gui.component.SliderX
import org.koin.core.qualifier.named
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import javax.swing.JLabel
import kotlin.math.roundToInt

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TunableSlider(val tunableValue: TunableNumber,
                    val valueLabel: JLabel? = null,
                    minBound: Double = 0.0,
                    maxBound: Double = 255.0) : SliderX(minBound, maxBound, 10), KoinComponent {

    private val pipelineManager: PipelineManager by inject()
    private val onMainUpdate: EventHandler by inject(named("onMainLoop"))



    var inControl = false

    constructor(tunableValue: TunableNumber, valueLabel: JLabel) : this(tunableValue, valueLabel, 0.0, 255.0)

    constructor(tunableValue: TunableNumber) : this(tunableValue, null, 0.0, 255.0)

    init {
        addChangeListener {
            onMainUpdate.once {
                if(inControl) {
                    tunableValue.setFromGui(scaledValue)

                    if (pipelineManager.paused)
                        pipelineManager.setPaused(false)
                }
            }


            valueLabel?.text = if (!tunableValue.isOnlyNumbers) {
                scaledValue.toString()
            } else {
                scaledValue.roundToInt().toString()
            }
        }

        tunableValue.onValueChange {
            if (!inControl) {
                scaledValue = tunableValue.value
            }
        }
    }

}