package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.codegen.parse.Value
import io.github.deltacv.easyvision.node.vision.Colors

sealed class GenValue {

    data class Mat(val value: Value, val color: Colors) : GenValue()

    data class Enum<E: kotlin.Enum<E>>(val value: E, val clazz: Class<*>) : GenValue()

    data class Int(val value: kotlin.Int) : GenValue()
    data class Float(val value: kotlin.Float) : GenValue()
    data class Double(val value: kotlin.Double) : GenValue()

    data class Range(val min: kotlin.Double, val max: kotlin.Double) : GenValue(){
        companion object {
            val ZERO = Range(0.0, 0.0)
        }
    }

    data class ScalarRange(val a: Range, val b: Range, val c: Range, val d: Range) : GenValue() {
        companion object {
            val ZERO = ScalarRange(Range.ZERO, Range.ZERO, Range.ZERO, Range.ZERO)
        }
    }

    sealed class Boolean(val value: kotlin.Boolean) : GenValue() {
        object True : Boolean(true)
        object False : Boolean(false)
    }

    data class List(val elements: Array<GenValue>) : GenValue()

    object None : GenValue()

}