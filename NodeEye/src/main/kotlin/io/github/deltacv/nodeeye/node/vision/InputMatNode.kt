package io.github.deltacv.nodeeye.node.vision

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.nodeeye.node.Node
import io.github.deltacv.nodeeye.node.attribute.AttributeMode
import io.github.deltacv.nodeeye.node.attribute.vision.MatAttribute

class InputMatNode : Node() {

    override fun onEnable() {
        nodeAttributes.add(MatAttribute(AttributeMode.OUTPUT, "Input"))
    }

    override fun draw() {
        ImNodes.beginNode(id)
            ImNodes.beginNodeTitleBar()
                ImGui.textUnformatted("Pipeline Input")
            ImNodes.endNodeTitleBar()

            drawAttributes()
        ImNodes.endNode()
    }

}