package io.github.deltacv.easyvision.attribute.vision

import imgui.ImGui
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.node.vision.Colors

class ScalarAttribute(
    mode: AttributeMode,
    color: Colors,
    variableName: String? = null
) : ListAttribute(mode, RangeAttribute, variableName, color.channels) {

    var color = color
        set(value) {
            fixedLength = value.channels
            field = value
        }

    override fun drawAttributeText(index: Int) {
        if(index < color.channelNames.size) {
            val name = color.channelNames[index]
            val elementName = name + if(name.length == 1) " " else ""

            ImGui.text(elementName)
            ImGui.sameLine()
        }
    }

    override fun value(current: CodeGen.Current): GenValue.ScalarRange {
        val values = (super.value(current) as GenValue.List).elements
        val ZERO = GenValue.Range.ZERO

        return GenValue.ScalarRange(
            values.getOr(0, ZERO) as GenValue.Range,
            values.getOr(1, ZERO) as GenValue.Range,
            values.getOr(2, ZERO) as GenValue.Range,
            values.getOr(3, ZERO) as GenValue.Range
        )
    }

}

fun <T> Array<T>.getOr(index: Int, or: T) = try {
    this[index]
} catch(ignored: ArrayIndexOutOfBoundsException) {
    or
}