/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanelConfig
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import javax.swing.SwingUtilities

abstract class TunableField<T>(
    protected val target: Any,
    val reflectionField: VirtualField,
    val allowMode: AllowMode = AllowMode.TEXT
) : KoinComponent {

    protected val eocvSim: EOCVSim by inject()

    var fieldPanel: TunableFieldPanel? = null
        private set

    protected val initialFieldValue: Any? = reflectionField.get()

    var isIgnoreGuiUpdates: Boolean = false

    @JvmField
    val onValueChange = EventHandler("TunableField-ValueChange")

    abstract fun init()

    open fun update() {
        refreshPipelineObject()
        for (tunableValue in tunableValues) {
            tunableValue.update()
        }
    }

    open fun refreshPipelineObject() {}

    open fun setPipelineFieldValue(newValue: T) {
        val current = reflectionField.get()
        if (current != newValue) {
            reflectionField.set(newValue)
            onValueChange.run()
        }
    }

    abstract val tunableValues: List<TunableValue<*>>

    fun setTunableFieldPanel(fieldPanel: TunableFieldPanel) {
        this.fieldPanel = fieldPanel
        
        for ((index, tunableValue) in tunableValues.withIndex()) {
            tunableValue.onPipelineUpdate.attach {
                if (!isIgnoreGuiUpdates) {
                    SwingUtilities.invokeLater {
                        fieldPanel.setFieldValue(index, tunableValue.value)
                    }
                }
            }
        }
    }

    abstract val value: T

    val fieldName: String
        get() = reflectionField.name

    val fieldTypeName: String
        get() = reflectionField.type.simpleName

    val isOnlyNumbers: Boolean
        get() = allowMode == AllowMode.ONLY_NUMBERS || allowMode == AllowMode.ONLY_NUMBERS_DECIMAL

    fun shouldIgnoreGuiUpdates(): Boolean = isIgnoreGuiUpdates

    enum class AllowMode {
        ONLY_NUMBERS, ONLY_NUMBERS_DECIMAL, TEXT
    }
}

