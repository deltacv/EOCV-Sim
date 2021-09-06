package io.github.deltacv.nodeeye.attribute

import imgui.ImGui

abstract class TypedAttribute(var typeName: String) : Attribute() {

    abstract var variableName: String?

    protected val finalVarName get() = variableName ?: if(mode == AttributeMode.INPUT) "Input" else "Output"

    override fun drawAttribute() {
        ImGui.text("($typeName) $finalVarName")
    }

    override fun acceptLink(other: Attribute) = this::class == other::class

}