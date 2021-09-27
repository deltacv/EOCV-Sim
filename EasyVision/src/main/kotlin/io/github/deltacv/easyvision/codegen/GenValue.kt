package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.codegen.build.Value
import io.github.deltacv.easyvision.node.vision.Colors

sealed class GenValue {

    data class Mat(val value: Value, val color: Colors) : GenValue()

    data class Enum<E: kotlin.Enum<E>>(val value: E, val clazz: Class<*>) : GenValue()

    data class Int(val value: kotlin.Int) : GenValue()
    data class Float(val value: kotlin.Float) : GenValue()
    data class Double(val value: kotlin.Double) : GenValue()

    sealed class Boolean(val value: kotlin.Boolean) : GenValue() {
        object True : Boolean(true)
        object False : Boolean(false)
    }

    object None : GenValue()

}