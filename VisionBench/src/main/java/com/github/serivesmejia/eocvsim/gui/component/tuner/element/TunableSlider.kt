/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
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
