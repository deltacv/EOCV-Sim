package io.github.deltacv.eocvsim.virtualreflect

interface VirtualField {

    val name: String
    val type: Class<*>

    val isFinal: Boolean

    fun get(): Any?
    fun set(value: Any?)
    
}