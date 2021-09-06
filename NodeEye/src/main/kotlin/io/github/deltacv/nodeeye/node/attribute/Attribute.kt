package io.github.deltacv.nodeeye.node.attribute

import imgui.extension.imnodes.ImNodes
import io.github.deltacv.nodeeye.id.DrawableIdElement
import io.github.deltacv.nodeeye.node.Node

enum class AttributeMode { INPUT, OUTPUT }

abstract class Attribute : DrawableIdElement {

    abstract val mode: AttributeMode

    override val id by Node.attributes.nextId { this }

    abstract fun drawAttribute()

    override fun draw() {
        if(mode == AttributeMode.INPUT) {
            ImNodes.beginInputAttribute(id)
        } else {
            ImNodes.beginOutputAttribute(id)
        }

        drawAttribute()

        if(mode == AttributeMode.INPUT) {
            ImNodes.endInputAttribute()
        } else {
            ImNodes.endOutputAttribute()
        }
    }

    override fun delete() {
        Node.attributes.removeId(id)
    }

    abstract fun acceptLink(other: Attribute): Boolean

}