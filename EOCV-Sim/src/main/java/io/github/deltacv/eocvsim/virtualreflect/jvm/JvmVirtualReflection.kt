package io.github.deltacv.eocvsim.virtualreflect.jvm

import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection

object JvmVirtualReflection : VirtualReflection {

    override fun contextOf(c: Class<*>) = JvmVirtualReflectContext(null, c)

    override fun contextOf(value: Any) = JvmVirtualReflectContext(value, value::class.java)

}