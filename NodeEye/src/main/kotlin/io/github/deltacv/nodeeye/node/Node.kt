package io.github.deltacv.nodeeye.node

import io.github.deltacv.nodeeye.id.DrawableIdElement
import io.github.deltacv.nodeeye.id.IdElementContainer
import io.github.deltacv.nodeeye.id.mutuallyExclude

enum class Attribute {
    INPUT, OUTPUT
}

abstract class Node : DrawableIdElement {

    companion object {
        val nodes = IdElementContainer<Node>()
        val attributes = IdElementContainer<Attribute>()
    }

    override val id by nodes.nextId { this }

    override fun delete() {
        nodes.removeId(id)
    }

}