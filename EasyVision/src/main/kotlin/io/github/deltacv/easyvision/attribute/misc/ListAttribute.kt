package io.github.deltacv.easyvision.attribute.misc

import imgui.ImGui
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue

open class ListAttribute(
    override val mode: AttributeMode,
    val elementType: Type,
    override var variableName: String? = null,
    length: Int? = null,
    private val allowAddOrDelete: Boolean = true
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "List"
        override val allowsNew = false
    }

    val listAttributes = mutableListOf<TypedAttribute>()
    val deleteQueue = mutableListOf<TypedAttribute>()

    private var beforeHasLink = false

    private var previousLength: Int? = 0
    var fixedLength = length
        set(value) {
            field = value
            onEnable()
        }

    private val allowAod get() = allowAddOrDelete && fixedLength == null

    override fun onEnable() {
        // oh god... (it's been only 10 minutes and i have already forgotten how this works)
        if(previousLength != fixedLength) {
            if(fixedLength != null && (previousLength == null || previousLength == 0)) {
                repeat(fixedLength!!) {
                    createElement()
                }
            } else if(previousLength != null || previousLength != 0) {
                val delta = (fixedLength ?: 0) - (previousLength ?: 0)

                if(delta < 0) {
                    repeat(-delta) {
                        val last = listAttributes[listAttributes.size - 1]
                        last.delete()

                        listAttributes.remove(last)
                        deleteQueue.add(last)
                    }
                } else {
                    repeat(delta) {
                        if(deleteQueue.isNotEmpty()) {
                            val last = deleteQueue[deleteQueue.size - 1]
                            last.restore()

                            listAttributes.add(last)
                            deleteQueue.remove(last)
                        } else {
                            createElement()
                        }
                    }
                }
            } else {
                for(attribute in listAttributes.toTypedArray()) {
                    attribute.delete()
                }
            }
        }

        previousLength = fixedLength
    }

    override fun draw() {
        super.draw()

        for(attrib in listAttributes) {
            if(beforeHasLink != hasLink) {
                if(hasLink) {
                    // delete attributes if a link has been created
                    attrib.delete()
                } else {
                    // restore list attribs if they were previously deleted
                    // after destroying a link with another node
                    attrib.restore()
                }
            }

            if(!hasLink) { // only draw attributes if there's not a link attached
                attrib.draw()
            }
        }

        beforeHasLink = hasLink
    }

    override fun value(current: CodeGen.Current): GenValue {
        TODO("Not yet implemented")
    }

    override fun drawAttribute() {
        ImGui.text("[${elementType.name}] $variableName")

        if(!hasLink && elementType.allowsNew && allowAod && mode == AttributeMode.INPUT) {
            // idk wat the frame height is, i just stole it from
            // https://github.com/ocornut/imgui/blob/7b8bc864e9af6c6c9a22125d65595d526ba674c5/imgui_widgets.cpp#L3439

            val buttonSize = ImGui.getFrameHeight()

            val style = ImGui.getStyle()

            ImGui.sameLine(0.0f, style.itemInnerSpacingX * 2.0f)

            if(ImGui.button("+", buttonSize, buttonSize)) { // creates a new element with the + button
                // uses the "new" function from the attribute's companion Type
                createElement()
            }

            // display the - button only if the attributes list is not empty
            if(listAttributes.isNotEmpty()) {
                ImGui.sameLine(0.0f, style.itemInnerSpacingX)

                if(ImGui.button("-", buttonSize, buttonSize)) {
                    // remove the last element from the list when - is pressed
                    listAttributes.removeLastOrNull()
                        ?.delete() // also delete it from the element id registry
                }
            }
        }
    }

    override fun acceptLink(other: Attribute) = other is ListAttribute && other.elementType == elementType

    private fun createElement() {
        val count = listAttributes.size.toString()
        val elementName = count + if(count.length == 1) " " else ""

        val element = elementType.new(AttributeMode.INPUT, elementName)
        element.enable() //enables the new element

        element.parentNode = parentNode
        element.drawType = false // hides the variable type

        listAttributes.add(element)
    }
}