package io.github.deltacv.easyvision.attribute.vision

import imgui.type.ImInt
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.gui.ExtraWidgets

class RangeAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : Type {
        override val name = "Range"

        override fun new(mode: AttributeMode, variableName: String) = RangeAttribute(mode, variableName)
    }

    var min = 0
    var max = 255

    val minValue = ImInt(min)
    val maxValue = ImInt(max)

    private val minId by EasyVision.miscIds.nextId()
    private val maxId by EasyVision.miscIds.nextId()

    override fun drawAttribute() {
        if(!hasLink) {
            ExtraWidgets.rangeSliders(
                min, max,
                minValue, maxValue,
                minId, maxId,
                width = 95f
            )

            val mn = minValue.get()
            val mx = maxValue.get()

            if(mn > mx) {
                minValue.set(mx)
            }
            if(mx < mn) {
                maxValue.set(mn)
            }
        }
    }

    override fun value(current: CodeGen.Current): GenValue.Range {
        if(isInput) {
            return if(hasLink) {
                val linkedAttrib = linkedAttribute()

                raiseAssert(
                    linkedAttrib != null,
                    "Range attribute must have another attribute attached"
                )

                val value = linkedAttrib!!.value(current)
                raiseAssert(value is GenValue.Range, "Attribute attached is not a Range")

                value as GenValue.Range
            } else {
                GenValue.Range(
                    minValue.get().toDouble(),
                    maxValue.get().toDouble()
                )
            }
        } else {
            val value = getOutputValue(current)
            raiseAssert(value is GenValue.Range, "Value returned from the node is not a Range")

            return value as GenValue.Range
        }
    }


}