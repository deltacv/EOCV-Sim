package io.github.deltacv.eocvsim.virtualreflect.py

import org.python.core.PyObject
import org.python.util.PythonInterpreter

fun PythonInterpreter.enableLabeling() {
    try {
        if (this["LabeledAttribute"] != null) return
    } catch(ignored: Exception) {  }

    exec("""
        class LabeledAttribute():
            def __init__(self, label, obj):
                self.label = label
                self.obj = obj
                
            def __get__(self, obj, owner):
                return self.obj
            
            def __set__(self, obj, value):
                self.obj = value
                
        def label(label, value):
            return LabeledAttribute(label, value)
    """.trimIndent())
}

fun PythonInterpreter.getLabelOf(attribute: PyObject) =
    if(attribute.type.name == "LabeledAttribute") {
        try {
            attribute.__findattr__("label")?.asStringOrNull()
        } catch(ignored: Exception) { null }
    } else null