package io.github.deltacv.easyvision.node

import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer

class Link(val a: Int, val b: Int) : DrawableIdElement {

    companion object {
        val links = IdElementContainer<Link>()

        fun getLinkOf(attributeId: Int): Link? {
            for(link in links) {
                if(link.a == attributeId || link.b == attributeId) {
                    return link
                }
            }

            return null
        }

        val Attribute.hasLink get() = getLinkOf(id) != null
    }

    override val id by links.nextId { this }

    override fun draw() {
        ImNodes.link(id, a, b)
    }

    override fun delete() {
        links.removeId(id)
    }

}