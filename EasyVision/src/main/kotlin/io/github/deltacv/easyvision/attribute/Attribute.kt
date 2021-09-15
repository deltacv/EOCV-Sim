package io.github.deltacv.easyvision.attribute

import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.node.Link
import io.github.deltacv.easyvision.node.Node

enum class AttributeMode { INPUT, OUTPUT }

abstract class Attribute : DrawableIdElement {

    abstract val mode: AttributeMode

    override val id by Node.attributes.nextId { this }

    lateinit var parentNode: Node
        internal set

    val links = mutableListOf<Link>()
    val hasLink get() = links.isNotEmpty()

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

        for(link in links.toTypedArray()) {
            link.delete()
            links.remove(link)
        }
    }

    override fun restore() {
        Node.attributes[id] = this
    }

    abstract fun acceptLink(other: Attribute): Boolean

}