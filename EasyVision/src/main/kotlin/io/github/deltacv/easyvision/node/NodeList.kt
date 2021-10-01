package io.github.deltacv.easyvision.node

import imgui.ImColor
import imgui.ImFont
import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.extension.imnodes.flag.ImNodesAttributeFlags
import imgui.extension.imnodes.flag.ImNodesColorStyle
import imgui.extension.imnodes.flag.ImNodesStyleFlags
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.gui.makeFont
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.node.vision.CvtColorNode
import io.github.deltacv.easyvision.util.ElapsedTime
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.event.InputEvent

class NodeList(val easyVision: EasyVision) {

    companion object {
        val listNodes = IdElementContainer<Node<*>>()
        val listAttributes = IdElementContainer<Attribute>()
    }

    lateinit var buttonFont: ImFont

    val plusFontSize = 60f

    var isNodesListOpen = false
        private set

    private var wasNodesListOpen = false
    val wasJustClosed get() = isNodesListOpen != wasNodesListOpen && !isNodesListOpen

    private var lastButton = false

    private val openButtonTimeout = ElapsedTime()

    private lateinit var listContext: ImNodesContext

    val testNode = CvtColorNode()

    fun init() {
        buttonFont = makeFont(plusFontSize)

        testNode.nodesIdContainer = listNodes
        testNode.attributesIdContainer = listAttributes
        testNode.drawAttributesCircles = false
        testNode.enable()
    }

    fun draw() {
        val size = EasyVision.windowSize

        if(easyVision.isSpaceReleased) {
            isNodesListOpen = true
        } else if(easyVision.isEscReleased) {
            isNodesListOpen = false
        }

        // NODES LIST

        if(isNodesListOpen) {
            ImGui.setNextWindowPos(0f, 0f)
            ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

            ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.55f) // transparent dark nodes list window

            ImGui.begin("nodes",
                ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
                        or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoTitleBar
                        or ImGuiWindowFlags.NoDecoration //or ImGuiWindowFlags.NoBringToFrontOnFocus
            )

            drawNodesList()

            ImGui.end()

            ImGui.popStyleColor()
        }

        // OPEN/CLOSE BUTTON

        ImGui.setNextWindowPos(size.x - plusFontSize * 1.8f, size.y - plusFontSize * 1.8f)

        if(isNodesListOpen) {
            ImGui.setNextWindowFocus()
        }

        ImGui.begin(
            "floating", ImGuiWindowFlags.NoBackground
                    or ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoDecoration or ImGuiWindowFlags.NoMove
        )

        ImGui.pushFont(buttonFont)
        val buttonSize = ImGui.getFrameHeight()

        val button = ImGui.button(if(isNodesListOpen) "x" else "+", buttonSize, buttonSize)

        if (button != lastButton && button && !isNodesListOpen && openButtonTimeout.millis > 200) {
            isNodesListOpen = true

            if(isNodesListOpen) {
                listContext = ImNodesContext()
            }
        }

        lastButton = button
        wasNodesListOpen = isNodesListOpen

        ImGui.popFont()
        ImGui.end()
    }

    private fun drawNodesList() {
        ImNodes.editorContextSet(listContext)

        ImNodes.getStyle().gridSpacing = 99999f // lol only way to make grid invisible
        ImNodes.pushColorStyle(ImNodesColorStyle.GridBackground, ImColor.floatToColor(0f, 0f, 0f, 0f))

        ImNodes.clearNodeSelection()
        ImNodes.clearLinkSelection()

        ImNodes.beginNodeEditor()
            for(node in listNodes) {
                node.draw()
            }
        ImNodes.endNodeEditor()

        ImNodes.getStyle().gridSpacing = 32f // back to normal
        ImNodes.popColorStyle()

        handleClick()
    }

    fun handleClick() {
        if(ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            val hovered = ImNodes.getHoveredNode()

            if(hovered >= 0) {
                val nodeClass = listNodes[hovered]!!::class.java
                val instance = nodeClass.getConstructor().newInstance()
                instance.enable()

                if(instance is DrawNode<*>) {
                    val nodePos = ImVec2()
                    ImNodes.getNodeScreenSpacePos(hovered, nodePos)

                    val mousePos = ImGui.getMousePos()

                    val newPosX = mousePos.x - nodePos.x
                    val newPosY = mousePos.y - nodePos.y

                    instance.nextNodePosition = ImVec2(newPosX, newPosY)
                    instance.pinToMouse = true
                }
            }

            isNodesListOpen = false
            openButtonTimeout.reset()
        }
    }

}