package io.github.deltacv.nodeeye.node

import imgui.extension.imnodes.ImNodes
import io.github.deltacv.nodeeye.id.DrawableIdElement
import io.github.deltacv.nodeeye.id.IdElementContainer

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
    }

    override val id by links.nextId { this }

    override fun draw() {
        ImNodes.link(id, a, b)
    }

    override fun delete() {
        links.removeId(id)
    }

}