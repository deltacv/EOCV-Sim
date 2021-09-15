package io.github.deltacv.easyvision.attribute.vision

import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type

class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Image"

        override fun new(mode: AttributeMode, variableName: String) = MatAttribute(mode, variableName)
    }

}