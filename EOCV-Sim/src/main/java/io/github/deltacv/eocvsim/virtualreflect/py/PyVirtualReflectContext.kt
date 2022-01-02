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