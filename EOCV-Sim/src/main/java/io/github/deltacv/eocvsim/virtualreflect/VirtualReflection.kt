package io.github.deltacv.eocvsim.virtualreflect

interface VirtualReflection {

    fun contextOf(c: Class<*>): VirtualReflectContext?

    fun contextOf(value: Any): VirtualReflectContext?

}