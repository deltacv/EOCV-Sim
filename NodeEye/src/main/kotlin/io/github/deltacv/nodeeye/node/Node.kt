package io.github.deltacv.nodeeye.node

import imgui.ImGui
import io.github.deltacv.nodeeye.id.DrawableIdElement
import io.github.deltacv.nodeeye.id.IdElementContainer
import io.github.deltacv.nodeeye.attribute.Attribute
import io.github.deltacv.nodeeye.attribute.AttributeMode

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
            for (attribute in nodeAttributes) {
                attribute.delete()
            }

            nodes.removeId(id)
        }
    }

    operator fun Attribute.unaryPlus() = nodeAttributes.add(this)

}