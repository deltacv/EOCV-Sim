package io.github.deltacv.easyvision.attribute.math

import imgui.ImGui
import imgui.type.ImInt
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute

class IntAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Int"

        override fun new(mode: AttributeMode, variableName: String) = IntAttribute(mode, variableName)
    }

    val value = ImInt()

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            ImGui.sameLine()

            ImGui.pushItemWidth(110.0f)
            ImGui.inputInt("", value)
            ImGui.popItemWidth()
        }
    }

}