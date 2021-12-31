package io.github.deltacv.eocvsim.virtualreflect.jvm

import io.github.deltacv.eocvsim.virtualreflect.VirtualReflectContext
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import java.lang.reflect.Field

class JvmVirtualReflectContext(
    val instance: Any? = null,
    val clazz: Class<*>
) : VirtualReflectContext {

    override val name = clazz.name
    override val simpleName = clazz.simpleName

    private val cachedVirtualFields = mutableMapOf<Field, JvmVirtualField>()

    override fun getFields(): Array<VirtualField> = clazz.fields.map { virtualFieldFor(it) }.toTypedArray()

    override fun getField(name: String): VirtualField? {
        val field = clazz.getField(name) ?: return null
        return virtualFieldFor(field)
    }

    private fun virtualFieldFor(field: Field): JvmVirtualField {
        if(!cachedVirtualFields.containsKey(field)) {
            cachedVirtualFields[field] = JvmVirtualField(instance, field)
        }

        return cachedVirtualFields[field]!!
    }

}