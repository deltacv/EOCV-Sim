package io.github.deltacv.eocvsim.virtualreflect.py

import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import org.python.util.PythonInterpreter

object PyVirtualReflection : VirtualReflection {

    override fun contextOf(c: Class<*>) = null

    override fun contextOf(value: Any) = when(value) {
        value is PythonInterpreter -> {
            PyVirtualReflectContext(null, value as PythonInterpreter)
        }
        value is PyWrapper -> {
            PyVirtualReflectContext((value as PyWrapper).name, value.interpreter)
        }
        else -> null
    }

}