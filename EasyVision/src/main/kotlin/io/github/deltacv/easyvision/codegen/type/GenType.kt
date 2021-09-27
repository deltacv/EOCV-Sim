package io.github.deltacv.easyvision.codegen.type

import io.github.deltacv.easyvision.codegen.Value
import io.github.deltacv.easyvision.node.vision.Colors

sealed class GenValue {

    data class Mat(val value: Value, val color: Colors) : GenValue()

    data class Enum<E: kotlin.Enum<E>>(val value: E, val clazz: Class<*>) : GenValue()

    data class Int(val value: kotlin.Int) : GenValue()

    sealed class Boolean : GenValue() {
        object True : Boolean()
        object False : Boolean()
    }

    object None : GenValue()

}