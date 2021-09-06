package io.github.deltacv.nodeeye.attribute.vision

import io.github.deltacv.nodeeye.attribute.TypedAttribute
import io.github.deltacv.nodeeye.attribute.AttributeMode

class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute("Image")