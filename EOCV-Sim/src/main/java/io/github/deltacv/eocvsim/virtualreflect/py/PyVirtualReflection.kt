package io.github.deltacv.eocvsim.virtualreflect.py

import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import org.python.util.PythonInterpreter

object PyVirtualReflection : VirtualReflection {

    override fun contextOf(c: Class<*>) = null

    override fun contextOf(value: Any) = if(value is PyWrapper) {
        PyVirtualReflectContext(value.name, value.interpreter)
    } else if(value is PythonInterpreter) {
        PyVirtualReflectContext(null, value)
    } else null
}