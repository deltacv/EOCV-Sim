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
            fields.add(fieldFor((item as PyTuple)[0] as String))
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