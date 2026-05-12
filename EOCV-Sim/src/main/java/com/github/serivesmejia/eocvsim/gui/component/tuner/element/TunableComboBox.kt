/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.tuner.element

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.tuner.TunableEnum
import org.koin.core.qualifier.named
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.event.ItemEvent
import javax.swing.JComboBox

class TunableComboBox(val tunableValue: TunableEnum<*>) : JComboBox<String>(), KoinComponent {

    private val pipelineManager: PipelineManager by inject()
    private val onMainUpdate: EventHandler by inject(named("onMainLoop"))


    init {
        initComponents()
    }

    private fun initComponents() {
        for (obj in tunableValue.enumValues) {
            addItem(obj.toString())
        }

        addItemListener { evt ->
            onMainUpdate.once {
                if (evt.stateChange == ItemEvent.SELECTED) {
                    val values = tunableValue.enumValues
                    var selected: Any? = null
                    val selectedStr = selectedItem?.toString() ?: return@once

                    for (valObj in values) {
                        if (valObj.toString() == selectedStr) {
                            selected = valObj
                            break
                        }
                    }

                    if (selected != null) {
                        @Suppress("UNCHECKED_CAST")
                        (tunableValue as TunableEnum<Enum<*>>).setFromGui(selected as Enum<*>)
                    }

                    if (pipelineManager.paused) {
                        pipelineManager.requestSetPaused(false)
                    }
                }
            }
        }

    }

}

