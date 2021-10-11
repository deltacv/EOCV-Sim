package io.github.deltacv.easyvision.attribute

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.exception.AttributeGenException
import io.github.deltacv.easyvision.node.Link

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

    private var isFirstDraw = true
    private var isSecondDraw = false

    private val finalVarName by lazy {
        variableName ?: if (mode == AttributeMode.INPUT) "Input" else "Output"
    }

    private val nodeSize = ImVec2()

    override fun drawAttribute() {
        if(isSecondDraw) {
            ImNodes.getNodeDimensions(parentNode.id, nodeSize)

            isSecondDraw = false
        }

        if(isFirstDraw) {
            isSecondDraw = true
            isFirstDraw = false
        }

        if(drawDescriptiveText) {
            val t: String

            if(mode == AttributeMode.OUTPUT && parentNode.nodeAttributes.size > 1) {
                t = if(drawType) {
                    "$finalVarName (${type.name})"
                } else finalVarName

                val textSize = ImVec2()
                ImGui.calcTextSize(textSize, t)

                ImGui.indent((nodeSize.x - textSize.x))
            } else {
                t = if(drawType) {
                    "(${type.name}) $finalVarName"
                } else finalVarName
            }

            ImGui.text(t)
        } else {
            ImGui.text("")
        }
    }

    override fun acceptLink(other: Attribute) = this::class == other::class

}