package io.github.deltacv.nodeeye.node

import imgui.ImGui
import imgui.extension.imnodes.ImNodes

class OutputTestNode : Node() {

    val outputAttributeId by attributes.nextId(Attribute.OUTPUT)

    override fun draw() {
        ImNodes.beginNode(id)
            ImNodes.beginNodeTitleBar()
                ImGui.textUnformatted("Test Node A")
            ImNodes.endNodeTitleBar()

            ImNodes.beginOutputAttribute(outputAttributeId)
                ImGui.text("Output Pin")
            ImNodes.endOutputAttribute()
        ImNodes.endNode()
    }

}

class InputTestNode : Node() {

    val inputAttributeId by attributes.nextId(Attribute.INPUT)

    override fun draw() {
        ImNodes.beginNode(id)
            ImNodes.beginNodeTitleBar()
                ImGui.textUnformatted("Test Node B")
            ImNodes.endNodeTitleBar()

            ImNodes.beginInputAttribute(inputAttributeId)
                ImGui.text("Input Pin")
            ImNodes.endInputAttribute()
        ImNodes.endNode()
    }

}