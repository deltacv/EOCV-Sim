package io.github.deltacv.eocvsim.virtualreflect.py

import org.python.core.PyObject
import org.python.util.PythonInterpreter

fun PythonInterpreter.enableLabeling() {
    try {
        if (this["__labels__"] != null) return // skip if this was already called before
    } catch(ignored: Exception) {  }

    exec("""
        __labels__ = {}
                
        def label(label, variable_name, value):
            __labels__[variable_name] = label
            return value
            
        def label(label, variable_name):
            __labels__[variable_name] = label
    """.trimIndent())
}