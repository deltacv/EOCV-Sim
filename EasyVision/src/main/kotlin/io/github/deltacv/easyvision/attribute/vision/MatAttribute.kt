package io.github.deltacv.easyvision.attribute.vision

import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue

class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Image"

        override fun new(mode: AttributeMode, variableName: String) = MatAttribute(mode, variableName)
    }

    override fun value(current: CodeGen.Current): GenValue.Mat {
        if(isInput) {
            val linkedAttrib = linkedAttribute()

            raiseAssert(
                linkedAttrib != null,
                "Mat attribute must have another attribute attached"
            )

            val value = linkedAttrib!!.value(current)
            raiseAssert(value is GenValue.Mat, "Attribute attached is not a Mat")

            return value as GenValue.Mat
        } else {
            val value = getOutputValue(current)
            raiseAssert(value is GenValue.Mat, "Value returned from the node is not a Mat")

            return value as GenValue.Mat
        }
    }

}