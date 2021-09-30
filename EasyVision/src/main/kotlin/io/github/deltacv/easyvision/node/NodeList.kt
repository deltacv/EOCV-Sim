package io.github.deltacv.easyvision.node

import imgui.ImColor
import imgui.ImFont
import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.extension.imnodes.flag.ImNodesAttributeFlags
import imgui.extension.imnodes.flag.ImNodesColorStyle
import imgui.extension.imnodes.flag.ImNodesStyleFlags
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.gui.makeFont

class NodeList(val easyVision: EasyVision) {

    lateinit var buttonFont: ImFont

    val plusFontSize = 60f

    private var isNodesListOpen = false
    private var lastButton = false


    private lateinit var listContext: ImNodesContext

    fun init() {
        buttonFont = makeFont(plusFontSize)
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

        if (button != lastButton && button) {
            isNodesListOpen = !isNodesListOpen
            if(isNodesListOpen) {
                listContext = ImNodesContext()
            }
        }

        lastButton = button

        ImGui.popFont()
        ImGui.end()
    }

    private fun drawNodesList() {
        ImNodes.editorContextSet(listContext)

        ImNodes.getStyle().gridSpacing = 99999f // lol only way to make grid invisible
        ImNodes.pushColorStyle(ImNodesColorStyle.GridBackground, ImColor.floatToColor(0f, 0f, 0f, 0f))

        ImNodes.clearNodeSelection()

        ImNodes.beginNodeEditor()
            ImNodes.beginNode(3213)
                ImNodes.beginNodeTitleBar()
                    ImGui.text("yes")
                ImNodes.endNodeTitleBar()

                ImGui.text("aaaaaaa")
            ImNodes.endNode()

        ImNodes.endNodeEditor()

        ImNodes.getStyle().gridSpacing = 32f // back to normal
        ImNodes.popColorStyle()
    }

}