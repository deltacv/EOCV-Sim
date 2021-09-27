package io.github.deltacv.easyvision.codegen.type

import io.github.deltacv.easyvision.codegen.Value

sealed class GenValue {

    data class Mat(val value: Value) : GenValue()

    object None : GenValue()

}