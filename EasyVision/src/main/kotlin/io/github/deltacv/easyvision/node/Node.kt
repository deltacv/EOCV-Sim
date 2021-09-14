package io.github.deltacv.easyvision.node

import imgui.ImGui
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode

abstract class Node(protected var allowDelete: Boolean = true) : DrawableIdElement {

    override val id by nodes.nextId { this }

    val nodeAttributes = mutableListOf<Attribute>()

    protected fun drawAttributes() {
        for((i, attribute) in nodeAttributes.withIndex()) {
            attribute.parentNode = this
            attribute.draw()

            if(i < nodeAttributes.size - 1) {
                ImGui.newLine() // make a new blank line if this isn't the last attribute
            }
        }
    }

    override fun delete() {
        if(allowDelete) {
            for (attribute in nodeAttributes.toTypedArray()) {
                for(link in attribute.links.toTypedArray()) {
                    link.delete()
                }

                attribute.delete()
                nodeAttributes.remove(attribute)
            }

            nodes.removeId(id)
        }
    }

    operator fun Attribute.unaryPlus() = nodeAttributes.add(this)

    companion object {
        val nodes = IdElementContainer<Node>()
        val attributes = IdElementContainer<Attribute>()

        @JvmStatic protected val INPUT = AttributeMode.INPUT
        @JvmStatic protected val OUTPUT = AttributeMode.OUTPUT

        fun checkRecursion(from: Node, to: Node): Boolean {
            val linksBetween = Link.getLinksBetween(from, to)

            var hasOutputToInput = false
            var hasInputToOutput = false

            for(link in linksBetween) {
                val aNode = link.aAttrib.parentNode!!

                val fromAttrib = if(aNode == from) link.aAttrib else link.bAttrib
                val toAttrib   = if(aNode == to) link.aAttrib else link.bAttrib

                if(!hasOutputToInput)
                    hasOutputToInput = fromAttrib.mode == OUTPUT && toAttrib.mode == INPUT

                if(!hasInputToOutput)
                    hasInputToOutput = fromAttrib.mode == INPUT && toAttrib.mode == OUTPUT

                if(hasOutputToInput && hasInputToOutput)
                    break
            }

            return hasOutputToInput && hasInputToOutput
        }
    }

}