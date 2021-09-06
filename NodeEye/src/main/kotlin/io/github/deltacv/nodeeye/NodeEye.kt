package io.github.deltacv.nodeeye

import imgui.ImGui
import imgui.ImVec2
import imgui.app.Application
import imgui.app.Configuration
import imgui.extension.imnodes.ImNodes
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import io.github.deltacv.nodeeye.node.*
import io.github.deltacv.nodeeye.attribute.AttributeMode
import io.github.deltacv.nodeeye.node.math.SumIntegerNode
import io.github.deltacv.nodeeye.node.vision.InputMatNode
import io.github.deltacv.nodeeye.node.vision.OutputMatNode

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.glfwGetWindowSize

class NodeEye : Application() {

    private val w = BufferUtils.createIntBuffer(1)
    private val h = BufferUtils.createIntBuffer(1)

    val windowSize: ImVec2 get() {
        w.position(0)
        h.position(0)

        glfwGetWindowSize(handle, w, h)

        return ImVec2(w.get(0).toFloat(), h.get(0).toFloat())
    }

    fun start() {
        ImNodes.createContext()

        InputMatNode().enable()
        OutputMatNode().enable()

        SumIntegerNode().enable()
        SumIntegerNode().enable()

        launch(this)

        ImNodes.destroyContext()
    }

    override fun configure(config: Configuration) {
        config.title = "NodeEye"
    }

    override fun process() {
        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)

        val size = windowSize
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.begin("Editor",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoCollapse
        )

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

            if(startAttrib.mode == endAttrib.mode) {
                return // linked attributes cannot be of the same mode
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