package io.github.deltacv.easyvision.node

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.flag.ImGuiMouseButton
import imgui.type.ImInt
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.AttributeMode

class NodeEditor(val easyVision: EasyVision) {

    fun init() {
        ImNodes.createContext()
    }

    fun draw() {
        ImNodes.beginNodeEditor()

        for(node in Node.nodes) {
            node.draw()
        }
        for(link in Link.links) {
            link.draw()
        }

        ImNodes.endNodeEditor()

        handleDeleteLink()
        handleCreateLink()
        handleDeleteSelection()
    }

    private val startAttr = ImInt()
    private val endAttr = ImInt()

    private fun handleCreateLink() {
        if(ImNodes.isLinkCreated(startAttr, endAttr)) {
            val start = startAttr.get()
            val end = endAttr.get()

            val startAttrib = Node.attributes[start]!!
            val endAttrib = Node.attributes[end]!!

            val input = if(startAttrib.mode == AttributeMode.INPUT) start else end

            val inputAttrib = Node.attributes[input]!!
            val outputAttrib = if(startAttrib.mode == AttributeMode.OUTPUT) start else end

            if(startAttrib.mode == endAttrib.mode) {
                return // linked attributes cannot be of the same mode
            }

            if(!startAttrib.acceptLink(endAttrib) ||!endAttrib.acceptLink(startAttrib)) {
                return // one or both of the attributes didn't accept the link, abort.
            }

            if(startAttrib.parentNode == endAttrib.parentNode) {
                return // we can't link a node to itself!
            }

            val inputLink = Link.getLinkOf(input)
            inputLink?.delete() // delete the existing link of the input attribute if there's any

            Link(start, end).enable() // create the link and enable it
        }
    }

    private fun handleDeleteLink() {
        val hoveredId = ImNodes.getHoveredLink()

        if(ImGui.isMouseClicked(ImGuiMouseButton.Right) && hoveredId >= 0) {
            val hoveredLink = Link.links[hoveredId]
            hoveredLink?.delete()
        }
    }

    private fun handleDeleteSelection() {
        if(easyVision.isDeleteReleased) {
            if(ImNodes.numSelectedNodes() > 0) {
                val selectedNodes = IntArray(ImNodes.numSelectedNodes())
                ImNodes.getSelectedNodes(selectedNodes)

                for(node in selectedNodes) {
                    Node.nodes[node]?.delete()
                }
            }

            if(ImNodes.numSelectedLinks() > 0) {
                val selectedLinks = IntArray(ImNodes.numSelectedLinks())
                ImNodes.getSelectedLinks(selectedLinks)

                for(link in selectedLinks) {
                    Link.links[link]?.delete()
                }
            }
        }
    }

    fun destroy() {
        ImNodes.destroyContext()
    }

}