package io.github.deltacv.eocvsim.virtualreflect.jvm

import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class JvmVirtualField(
    val instance: Any?,
    val field: Field
) : VirtualField {

    override val name: String = field.name
    override val type: Class<*> = field.type

    override val isFinal get() = Modifier.isFinal(field.modifiers)

    override fun get(): Any? = field.get(instance)

    override fun set(value: Any?) {
        field.set(instance, value)
    }

}