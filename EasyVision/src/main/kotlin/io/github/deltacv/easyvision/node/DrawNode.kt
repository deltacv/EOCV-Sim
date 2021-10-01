package io.github.deltacv.easyvision.node

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.codegen.CodeGenSession

abstract class DrawNode<S: CodeGenSession>(
    var title: String? = null,
    allowDelete: Boolean = true
) : Node<S>(allowDelete) {

    var nextNodePosition: ImVec2? = null

    override fun draw() {
        nextNodePosition?.let {
            ImNodes.setNodeScreenSpacePos(id, it.x, it.y)
            nextNodePosition = null
        }

        ImNodes.setNodeDraggable(id, true)

        ImNodes.beginNode(id)
            if(title != null) {
                ImNodes.beginNodeTitleBar()
                    ImGui.textUnformatted(title!!)
                ImNodes.endNodeTitleBar()
            }

            drawNode()
            drawAttributes()
        ImNodes.endNode()
    }

    open fun drawNode() { }

}