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
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflectContext
import org.python.core.PyStringMap
import org.python.core.PyTuple
import org.python.util.PythonInterpreter

class PyVirtualReflectContext(
    val ctxName: String?,
    val interpreter: PythonInterpreter
) : VirtualReflectContext {

    override val name: String
        get() = ctxName ?: "PythonInterpreter"

    override val simpleName get() = name

    private val fieldCache = mutableMapOf<String, PyVirtualField>()

    override fun getFields(): Array<VirtualField> {
        val locals = (interpreter.locals as PyStringMap).items()

        val fields = mutableListOf<PyVirtualField>()

        for(item in locals) {
            val name = (item as PyTuple)[0] as String

            // ignore "protected" members as defined by
            // python naming conventions (variables prefixed
            // with an underscore are considered private and
            // shouldn't be exposed). also filters out python
            // special names such as "__main__" (they are
            // still accessible with getField(name) though)
            if(name.startsWith("_")) continue

            fields.add(fieldFor(name))
        }

        return fields.toTypedArray()
    }

    override fun getField(name: String): VirtualField? {
        if(interpreter[name] == null) return null

        return fieldFor(name)
    }

    private fun fieldFor(name: String): PyVirtualField {
        if(!fieldCache.containsKey(name)) {
            fieldCache[name] = PyVirtualField(name, interpreter)
        }

        return fieldCache[name]!!
    }

}