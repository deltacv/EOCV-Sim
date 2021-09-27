package io.github.deltacv.easyvision.attribute.misc

import imgui.ImGui
import imgui.type.ImInt
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.type.GenValue

class EnumAttribute<T: Enum<*>>(
    override val mode: AttributeMode,
    val values: Array<T>,
    override var variableName: String?
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Enum"
        override val allowsNew = false
    }

    private val valuesStrings = values.map {
        it.name
    }.toTypedArray()

    val currentItem = ImInt()

    override fun drawAttribute() {
        super.drawAttribute()

        ImGui.pushItemWidth(110.0f)
        ImGui.combo("", currentItem, valuesStrings)
        ImGui.popItemWidth()
    }

    override fun acceptLink(other: Attribute) = other is EnumAttribute<*> && values[0]::class == other.values[0]::class

    override fun value(codeGen: CodeGen): GenValue {
        TODO("Not yet implemented")
    }

}