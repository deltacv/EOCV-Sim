package io.github.deltacv.easyvision.node

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.flag.ImGuiMouseButton
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.codegen.CodeGenSession

abstract class DrawNode<S: CodeGenSession>(
    var title: String? = null,
    allowDelete: Boolean = true
) : Node<S>(allowDelete) {

    var nextNodePosition: ImVec2? = null

    var pinToMouse = false
    private var lastPinToMouse = false

    private var pinToMouseOffset = ImVec2()

    var isFirstDraw = true

    open fun init() {}

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

        if(isFirstDraw) {
            init()
            isFirstDraw = false
        }

        nextNodePosition?.let {
            ImNodes.setNodeScreenSpacePos(id, it.x, it.y)
            nextNodePosition = null
        }

        if(pinToMouse) {
            val mousePos = ImGui.getMousePos()

            if(pinToMouse != lastPinToMouse) {
                val nodeDims = ImVec2()
                ImNodes.getNodeDimensions(id, nodeDims)

                pinToMouseOffset = ImVec2(
                    nodeDims.x / 2,
                    nodeDims.y / 2
                )
            }

            val newPosX = mousePos.x - pinToMouseOffset.x
            val newPosY = mousePos.y - pinToMouseOffset.y

            ImNodes.setNodeEditorSpacePos(id, newPosX, newPosY)

            if(ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
                pinToMouse = false
            }
        }

        lastPinToMouse = pinToMouse
    }

    protected fun noValue(attrib: Attribute): Nothing {
        raise("Attribute $attrib is not an output of this node or not handled by this")
    }

    open fun drawNode() { }

}