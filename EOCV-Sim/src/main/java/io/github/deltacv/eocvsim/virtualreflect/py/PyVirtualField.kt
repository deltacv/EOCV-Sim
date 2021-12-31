package io.github.deltacv.eocvsim.virtualreflect.py

import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import org.python.core.Py
import org.python.core.PyObject
import org.python.util.PythonInterpreter

class PyVirtualField(
    override val name: String,
    val interpreter: PythonInterpreter
) : VirtualField {

    override val type: Class<*>
        get() {
            val value = get()

            return if(value == null)
                Void::class.java
            else value::class.java
        }

    override val isFinal = false

    override fun get(): Any? = interpreter[name].__tojava__(Any::class.java)

    override fun set(value: Any?) {
        interpreter.set(name, when (value) {
            is PyObject -> value
            null -> Py.None
            else -> Py.java2py(value)
        })
    }
}