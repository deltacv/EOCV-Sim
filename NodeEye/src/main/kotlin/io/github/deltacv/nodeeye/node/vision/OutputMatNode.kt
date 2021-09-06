package io.github.deltacv.nodeeye.node.vision

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.nodeeye.node.Node
import io.github.deltacv.nodeeye.node.attribute.AttributeMode
import io.github.deltacv.nodeeye.node.attribute.vision.MatAttribute

class OutputMatNode : Node() {

    override fun onEnable() {
        nodeAttributes.add(MatAttribute(AttributeMode.INPUT, "Output"))
    }

    override fun draw() {
        ImNodes.beginNode(id)
            ImNodes.beginNodeTitleBar()
                ImGui.textUnformatted("Pipeline Output")
            ImNodes.endNodeTitleBar()

            drawAttributes()
        ImNodes.endNode()
    }

}