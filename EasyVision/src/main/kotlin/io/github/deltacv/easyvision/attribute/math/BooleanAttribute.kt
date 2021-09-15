package io.github.deltacv.easyvision.attribute.math

import imgui.ImGui
import imgui.type.ImBoolean
import imgui.type.ImInt
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute

class BooleanAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Boolean"

        override fun new(mode: AttributeMode, variableName: String) = BooleanAttribute(mode, variableName)
    }

    val value = ImBoolean()

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            ImGui.checkbox("", value)
        }
    }

}