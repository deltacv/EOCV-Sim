package io.github.deltacv.easyvision.codegen.type

import io.github.deltacv.easyvision.codegen.Value
import io.github.deltacv.easyvision.node.vision.Colors

sealed class GenValue {

    data class Mat(val value: Value, val color: Colors) : GenValue()

    data class Enum<E: Enum<E>>(val value: E) : GenValue()

    object None : GenValue()

}