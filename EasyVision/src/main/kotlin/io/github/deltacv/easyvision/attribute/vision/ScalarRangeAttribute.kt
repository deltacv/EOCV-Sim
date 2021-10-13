package io.github.deltacv.easyvision.attribute.vision

import imgui.ImGui
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.attribute.math.RangeAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.node.vision.Colors

class ScalarRangeAttribute(
    mode: AttributeMode,
    color: Colors,
    variableName: String? = null
) : ListAttribute(mode, RangeAttribute, variableName, color.channels) {

    var color = color
        set(value) {
            fixedLength = value.channels
            field = value
        }

    override fun drawAttributeText(index: Int, attrib: Attribute) {
        if(index < color.channelNames.size) {
            val name = color.channelNames[index]
            val elementName = name + if(name.length == 1) " " else ""

            if(attrib is TypedAttribute) {
                attrib.drawDescriptiveText = false
                attrib.inputSameLine = true
            }

            ImGui.text(elementName)
            ImGui.sameLine()
        }
    }

    override fun value(current: CodeGen.Current): GenValue.ScalarRange {
        val values = (super.value(current) as GenValue.List).elements
        val ZERO = GenValue.Range.ZERO

        val value = GenValue.ScalarRange(
            values.getOr(0, ZERO) as GenValue.Range,
            values.getOr(1, ZERO) as GenValue.Range,
            values.getOr(2, ZERO) as GenValue.Range,
            values.getOr(3, ZERO) as GenValue.Range
        )

        return value(
            current, "a scalar range", value
        ) { it is GenValue.ScalarRange }
    }


}

fun <T> Array<T>.getOr(index: Int, or: T) = try {
    this[index]
} catch(ignored: ArrayIndexOutOfBoundsException) {
    or
}