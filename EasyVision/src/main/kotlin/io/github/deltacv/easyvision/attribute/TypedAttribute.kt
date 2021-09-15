package io.github.deltacv.easyvision.attribute

import imgui.ImGui

interface Type {
    val name: String
    val allowsNew: Boolean get() = true

    fun new(mode: AttributeMode, variableName: String): TypedAttribute {
        throw UnsupportedOperationException("Cannot instantiate a List attribute with new")
    }
}

abstract class TypedAttribute(var type: Type) : Attribute() {

    abstract var variableName: String?

    var drawDescriptiveText = true
    var drawType = true

    protected val finalVarName by lazy {
        variableName ?: if (mode == AttributeMode.INPUT) "Input" else "Output"
    }

    override fun drawAttribute() {
        if(drawDescriptiveText) {
            val t = if(drawType) {
                "(${type.name}) "
            } else ""

            ImGui.text("$t$finalVarName")
        } else {
            ImGui.text("")
        }
    }

    override fun acceptLink(other: Attribute) = this::class == other::class

}