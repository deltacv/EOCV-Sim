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

    var inputSameLine = false

    open var typeName = "(${type.name})"

    private var isFirstDraw = true
    private var isSecondDraw = false

    private val finalVarName by lazy {
        variableName ?: if (mode == AttributeMode.INPUT) "Input" else "Output"
    }

    val nodeSize = ImVec2()

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
                    "$finalVarName $typeName"
                } else finalVarName

                val textSize = ImVec2()
                ImGui.calcTextSize(textSize, t)

                ImGui.indent((nodeSize.x - textSize.x))
            } else {
                t = if(drawType) {
                    "$typeName $finalVarName"
                } else finalVarName
            }

            ImGui.text(t)
        } else if(!inputSameLine) {
            ImGui.text("")
        }
    }

    protected fun sameLineIfNeeded() {
        if(inputSameLine) {
            ImGui.sameLine()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T : GenValue> value(
        current: CodeGen.Current,
        name: String,
        inputFieldValue: T? = null,
        checkConsumer: (GenValue) -> Boolean
    ): T {
        if(isInput) {
            return if(hasLink || inputFieldValue == null) {
                val linkedAttrib = linkedAttribute()

                raiseAssert(
                    linkedAttrib != null,
                    "$name attribute must have another attribute attached"
                )

                val value = linkedAttrib!!.value(current)
                raiseAssert(checkConsumer(value), "Attribute attached is not $name")

                value as T
            } else {
                inputFieldValue
            }
        } else {
            val value = getOutputValue(current)
            raiseAssert(checkConsumer(value), "Value returned from the node is not $name")

            return value as T
        }
    }

    override fun acceptLink(other: Attribute) = this::class == other::class

}