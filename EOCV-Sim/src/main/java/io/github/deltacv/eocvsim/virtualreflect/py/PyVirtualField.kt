/*
 * Copyright (c) 2022 Sebastian Erives
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

package io.github.deltacv.eocvsim.virtualreflect.py

import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import org.python.core.Py
import org.python.core.PyInstance
import org.python.core.PyObject
import org.python.util.PythonInterpreter

class PyVirtualField(
    override val name: String,
    val interpreter: PythonInterpreter
) : VirtualField {

    private val obj get() = interpreter[name]

    val isLabeled get() = obj is PyInstance && (obj as PyInstance).instclass.__name__ == "LabeledAttribute"

    private var actualObj: PyObject
        set(value) {
            if(isLabeled) {
                obj.__setattr__("obj", value)
            } else interpreter[name] = value
        }
        get() = if(isLabeled) {
            obj.__findattr__("obj")
        } else obj

    override val type: Class<*>
        get() {
            val value = get()

            return if(value == null)
                Void::class.java
            else value::class.java
        }

    override val isFinal = false

    override val label by lazy {
        if(isLabeled) {
            obj.__getattr__("label").asStringOrNull()
        } else null
    }

    override fun get(): Any? = actualObj.__tojava__(Any::class.java)

    override fun set(value: Any?) {
        actualObj = when (value) {
            is PyObject -> value
            null -> Py.None
            else -> Py.java2py(value)
        }
    }
}