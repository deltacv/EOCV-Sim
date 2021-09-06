package io.github.deltacv.nodeeye

import imgui.ImGui
import imgui.app.Application
import imgui.app.Configuration
import imgui.extension.imnodes.ImNodes
import imgui.type.ImInt
import io.github.deltacv.nodeeye.node.*
import io.github.deltacv.nodeeye.node.attribute.AttributeMode
import io.github.deltacv.nodeeye.node.vision.InputMatNode
import io.github.deltacv.nodeeye.node.vision.OutputMatNode

class NodeEye : Application() {

    fun start() {
        ImNodes.createContext()

        InputMatNode().enable()
        OutputMatNode().enable()

        launch(this)
        ImNodes.destroyContext()
    }

    override fun configure(config: Configuration) {
        config.title = "NodeEye"
    }

    override fun process() {
        ImGui.begin("Editor")

        ImNodes.beginNodeEditor()

        for(node in Node.nodes) {
            node.draw()
        }
        for(link in Link.links) {
            link.draw()
        }

        ImNodes.endNodeEditor()
        checkLinkCreated()

        ImGui.end()
    }

    private val startAttr = ImInt()
    private val endAttr = ImInt()

    private fun checkLinkCreated() {
        if(ImNodes.isLinkCreated(startAttr, endAttr)) {
            val start = startAttr.get()
            val end = endAttr.get()

            val startAttrib = Node.attributes[start]!!
            val endAttrib = Node.attributes[end]!!

            if(startAttrib == endAttrib) {
                return // linked attributes cannot be of the same type
            }

            if(!startAttrib.acceptLink(endAttrib) ||!endAttrib.acceptLink(startAttrib)) {
                return // one or both of the attributes didn't accept the link, abort.
            }

            val inputLink = Link.getLinkOf(
                // determines which, the start or end, of this new link is the input attribute
                if(startAttrib.mode == AttributeMode.INPUT)
                    start
                else end
            )
            inputLink?.delete() // delete the existing link of the input attribute if there's any

            Link(start, end).enable() // create the link and enable it
        }
    }

}

fun main() {
    NodeEye().start()
}