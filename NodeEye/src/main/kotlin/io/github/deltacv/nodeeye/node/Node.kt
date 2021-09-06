package io.github.deltacv.nodeeye.node

import io.github.deltacv.nodeeye.id.DrawableIdElement
import io.github.deltacv.nodeeye.id.IdElementContainer
import io.github.deltacv.nodeeye.node.attribute.Attribute

abstract class Node : DrawableIdElement {

    companion object {
        val nodes = IdElementContainer<Node>()
        val attributes = IdElementContainer<Attribute>()
    }

    override val id by nodes.nextId { this }

    val nodeAttributes = mutableListOf<Attribute>()

    fun drawAttributes() {
        for(attribute in nodeAttributes) {
            attribute.draw()
        }
    }

    override fun delete() {
        for(attribute in nodeAttributes) {
            attribute.delete()
        }

        nodes.removeId(id)
    }

}