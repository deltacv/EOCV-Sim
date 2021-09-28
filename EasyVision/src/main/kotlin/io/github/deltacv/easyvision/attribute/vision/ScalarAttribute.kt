package io.github.deltacv.easyvision.attribute.vision

import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.node.vision.Colors

class ScalarAttribute(
    mode: AttributeMode,
    color: Colors,
    variableName: String? = null
) : ListAttribute(mode, RangeAttribute, variableName, color.channels)