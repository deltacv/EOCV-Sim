package io.github.deltacv.nodeeye

import imgui.ImGui
import imgui.app.Application
import imgui.app.Configuration
import imgui.extension.imnodes.ImNodes
import imgui.type.ImInt
import io.github.deltacv.nodeeye.node.Link
import io.github.deltacv.nodeeye.node.Node
import io.github.deltacv.nodeeye.node.OutputTestNode
import io.github.deltacv.nodeeye.node.InputTestNode

class NodeEye : Application() {

    fun start() {
        ImNodes.createContext()

        val a = InputTestNode()
        a.enable()

        val b = OutputTestNode()
        b.enable()

        val c = OutputTestNode()
        c.enable()

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

            for(link in Link.links) {
                if(link.a == start || link.a == end || link.b == start ||  link.b == end) {
                    link.delete()
                    break
                }
            }

            Link(start, end).enable()

            println("link to $start ${Node.attributes[start]} and $end ${Node.attributes[end]}")
        }
    }

}

fun main() {
    NodeEye().start()
}