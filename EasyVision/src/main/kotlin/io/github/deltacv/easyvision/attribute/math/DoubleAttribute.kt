package io.github.deltacv.easyvision.attribute.math

import imgui.ImGui
import imgui.type.ImDouble
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue

class DoubleAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Double"

        override fun new(mode: AttributeMode, variableName: String) = DoubleAttribute(mode, variableName)
    }

    val value = ImDouble()

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            if(inputSameLine) {
                ImGui.sameLine()
            }

            ImGui.pushItemWidth(110.0f)
            ImGui.inputDouble("", value)
            ImGui.popItemWidth()
        }
    }

    override fun value(current: CodeGen.Current) = value(
        current, "a Double", GenValue.Double(value.get())
    ) { it is GenValue.Double }


}