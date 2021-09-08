package io.github.deltacv.easyvision.attribute.math

import imgui.ImGui
import imgui.type.ImInt
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.node.Link.Companion.hasLink

class IntAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute("Int") {

    val value = ImInt()

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            ImGui.pushItemWidth(110.0f)
            ImGui.inputInt("", value)
            ImGui.popItemWidth()
        }
    }

}