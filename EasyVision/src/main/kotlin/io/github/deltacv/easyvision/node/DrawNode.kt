package io.github.deltacv.easyvision.node

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.codegen.CodeGenSession

abstract class DrawNode<S: CodeGenSession>(
    var title: String? = null,
    allowDelete: Boolean = true
) : Node<S>(allowDelete) {

    override fun draw() {
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