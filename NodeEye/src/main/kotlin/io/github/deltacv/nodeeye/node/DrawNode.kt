package io.github.deltacv.nodeeye.node

import imgui.ImGui
import imgui.extension.imnodes.ImNodes

abstract class DrawNode(var title: String) : Node() {

    override fun draw() {
        ImNodes.beginNode(id)
            ImNodes.beginNodeTitleBar()
                ImGui.textUnformatted(title)
            ImNodes.endNodeTitleBar()

            drawNode()
            drawAttributes()
        ImNodes.endNode()
    }

    open fun drawNode() { }

}