package io.github.deltacv.easyvision.node

import imgui.ImGui
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode

abstract class Node(protected var allowDelete: Boolean = true) : DrawableIdElement {

    companion object {
        val nodes = IdElementContainer<Node>()
        val attributes = IdElementContainer<Attribute>()

        @JvmStatic protected val INPUT = AttributeMode.INPUT
        @JvmStatic protected val OUTPUT = AttributeMode.OUTPUT
    }

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
            for(link in Link.links.elements.toTypedArray()) {
                for (attribute in nodeAttributes) {
                    if(link.a == attribute.id || link.b == attribute.id) {
                        // deleting links that were attached
                        // to any of this node's attributes
                        link.delete()
                    }
                }
            }

            for (attribute in nodeAttributes.toTypedArray()) {
                attribute.delete()
                nodeAttributes.remove(attribute)
            }

            nodes.removeId(id)
        }
    }

    operator fun Attribute.unaryPlus() = nodeAttributes.add(this)

}