package io.github.deltacv.easyvision.node

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.flag.ImGuiMouseButton
import imgui.type.ImInt
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.PopupBuilder
import io.github.deltacv.easyvision.attribute.AttributeMode

class NodeEditor(val easyVision: EasyVision) {

    val context = ImNodesContext()

    fun init() {
        ImNodes.createContext()
    }

    fun draw() {
        ImNodes.editorContextSet(context)
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

            val input  = if(startAttrib.mode == AttributeMode.INPUT) start else end
            val output = if(startAttrib.mode == AttributeMode.OUTPUT) start else end

            val inputAttrib = Node.attributes[input]!!
            val outputAttrib = Node.attributes[output]!!

            if(startAttrib.mode == endAttrib.mode) {
                return // linked attributes cannot be of the same mode
            }

            if(!startAttrib.acceptLink(endAttrib) || !endAttrib.acceptLink(startAttrib)) {
                PopupBuilder.addWarningToolTip("Couldn't link nodes: Types didn't match")
                return // one or both of the attributes didn't accept the link, abort.
            }

            if(startAttrib.parentNode == endAttrib.parentNode) {
                return // we can't link a node to itself!
            }

            inputAttrib.links.toTypedArray().forEach {
                it.delete() // delete the existing link(s) of the input attribute if there's any
            }

            val link = Link(start, end).enable() // create the link and enable it

            if(Node.checkRecursion(inputAttrib.parentNode, outputAttrib.parentNode)) {
                PopupBuilder.addWarningToolTip("Couldn't link nodes: Recursion problem detected")
                // remove the link if a recursion case was detected (e.g both nodes were attached to each other)
                link.delete()
            }
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