/*
 * Copyright (c) 2026 Sebastian Erives
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

import com.github.serivesmejia.eocvsim.util.event.EventHandler

sealed class TunableValue<T: Any>(
    initialValue: T,
    val supplier: () -> T,
    val consumer: (T) -> Unit
) {
    private var _value: T = initialValue

    var value: T
        get() = _value
        set(newValue) {
            if (_value != newValue) {
                _value = newValue
                onValueChange.run()
            }
        }

    val onValueChange = EventHandler("TunableValue-ValueChange")
    val onPipelineUpdate = EventHandler("TunableValue-PipelineUpdate")

    fun setFromPipeline(newValue: T) {
        if (_value != newValue) {
            _value = newValue
            onPipelineUpdate.run()
        }
    }

    fun update() {
        setFromPipeline(supplier())
    }

    fun setFromGui(guiValue: T) {
        consumer(guiValue)
        value = guiValue
    }
    
    abstract fun setAnyFromGui(guiValue: Any)
}

class TunableNumber(initialValue: Double, supplier: () -> Double, consumer: (Double) -> Unit, val isOnlyNumbers: Boolean = false) : TunableValue<Double>(initialValue, supplier, consumer) {
    override fun setAnyFromGui(guiValue: Any) { 
        if (guiValue is Number) setFromGui(guiValue.toDouble()) 
        else throw IllegalArgumentException("Expected Number but got ${guiValue::class.java.name}")
    }
}
class TunableString(initialValue: String, supplier: () -> String, consumer: (String) -> Unit) : TunableValue<String>(initialValue, supplier, consumer) {
    override fun setAnyFromGui(guiValue: Any) { 
        if (guiValue is String) setFromGui(guiValue) 
        else throw IllegalArgumentException("Expected String but got ${guiValue::class.java.name}")
    }
}
class TunableBoolean(initialValue: Boolean, supplier: () -> Boolean, consumer: (Boolean) -> Unit) : TunableValue<Boolean>(initialValue, supplier, consumer) {
    override fun setAnyFromGui(guiValue: Any) { 
        if (guiValue is Boolean) setFromGui(guiValue) 
        else throw IllegalArgumentException("Expected Boolean but got ${guiValue::class.java.name}")
    }
}
class TunableEnum<T: Enum<*>>(initialValue: T, val enumValues: Array<Any>, supplier: () -> T, consumer: (T) -> Unit) : TunableValue<T>(initialValue, supplier, consumer) {
    override fun setAnyFromGui(guiValue: Any) {
        if (value::class.java.isInstance(guiValue)) {
            @Suppress("UNCHECKED_CAST")
            setFromGui(guiValue as T)
        } else {
            throw IllegalArgumentException("Expected ${value::class.qualifiedName} but got ${guiValue::class.qualifiedName}")
        }
    }
}
