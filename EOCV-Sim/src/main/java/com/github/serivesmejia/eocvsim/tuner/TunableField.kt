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

package com.github.serivesmejia.eocvsim.tuner

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanelConfig
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

abstract class TunableField<T>(
    protected val target: Any,
    val reflectionField: VirtualField,
    protected val eocvSim: EOCVSim,
    val allowMode: AllowMode = AllowMode.TEXT
) {
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
                    javax.swing.SwingUtilities.invokeLater {
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
