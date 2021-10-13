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
        return if(isInput) {
            if(hasLink) {
                val linkedAttrib = linkedAttribute()

                raiseAssert(
                    linkedAttrib != null,
                    "Scalar attribute must have another attribute attached"
                )

                raiseAssert(
                    linkedAttrib is ScalarRangeAttribute,
                    "Attribute attached is not a Scalar range"
                )

                linkedAttrib!!.value(current) as GenValue.ScalarRange
            } else {
                val values = (super.value(current) as GenValue.List).elements
                val ZERO = GenValue.Range.ZERO

                GenValue.ScalarRange(
                    values.getOr(0, ZERO) as GenValue.Range,
                    values.getOr(1, ZERO) as GenValue.Range,
                    values.getOr(2, ZERO) as GenValue.Range,
                    values.getOr(3, ZERO) as GenValue.Range
                )
            }
        } else {
            val value = getOutputValue(current)
            raiseAssert(value is GenValue.ScalarRange, "Value returned from the node is not a scalar range")

            return value as GenValue.ScalarRange
        }
    }

}

fun <T> Array<T>.getOr(index: Int, or: T) = try {
    this[index]
} catch(ignored: ArrayIndexOutOfBoundsException) {
    or
}