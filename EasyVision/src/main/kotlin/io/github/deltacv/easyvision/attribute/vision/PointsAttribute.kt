package io.github.deltacv.easyvision.attribute.vision

import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue

class PointsAttribute (
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(MatAttribute) {

    companion object : Type {
        override val name = "Points"

        override fun new(mode: AttributeMode, variableName: String) = PointsAttribute(mode, variableName)
    }

    override fun value(current: CodeGen.Current) = value<GenValue.Points>(
        current, "a Points"
    ) { it is GenValue.Points }

}