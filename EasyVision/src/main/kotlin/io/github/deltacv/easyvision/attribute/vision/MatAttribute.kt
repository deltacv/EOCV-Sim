package io.github.deltacv.easyvision.attribute.vision

import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.attribute.AttributeMode

class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute("Image")