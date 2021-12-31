package io.github.deltacv.eocvsim.virtualreflect

interface VirtualReflectContext {

    val name: String
    val simpleName: String

    fun getFields(): Array<VirtualField>

    fun getField(name: String): VirtualField?

}