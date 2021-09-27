package io.github.deltacv.easyvision.attribute.math

import imgui.ImGui
import imgui.type.ImInt
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.type.GenValue

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

    override fun value(codeGen: CodeGen): GenValue.Int {
        if(isInput) {
            return if(hasLink) {
                val linkedAttrib = linkedAttribute()

                raiseAssert(
                    linkedAttrib != null,
                    "Int attribute must have another attribute attached"
                )

                val value = linkedAttrib!!.value(codeGen)
                raiseAssert(value is GenValue.Int, "Attribute attached is not a Int")

                value as GenValue.Int
            } else {
                GenValue.Int(value.get())
            }
        }

        raise("Unexpected point reached while processing int attribute")
    }

}