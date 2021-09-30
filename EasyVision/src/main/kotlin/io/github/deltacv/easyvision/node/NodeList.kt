package io.github.deltacv.easyvision.node

import imgui.ImFont
import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.gui.makeFont

class NodeList(val easyVision: EasyVision) {

    lateinit var buttonFont: ImFont

    val plusFontSize = 50f

    private var isNodesListOpen = false
    private var lastButton = false

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

        if(isNodesListOpen) {
            ImGui.setNextWindowPos(0f, 0f)
            ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

            ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.5f)

            //ImGui.setNextWindowFocus()
            ImGui.begin("nodes",
                ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
                        or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoTitleBar
                        or ImGuiWindowFlags.NoDecoration //or ImGuiWindowFlags.NoBringToFrontOnFocus
            )

            drawNodesList()

            ImGui.end()

            ImGui.popStyleColor()
        }

        ImGui.setNextWindowPos(size.x - plusFontSize * 2f, size.y - plusFontSize * 2f)
        ImGui.setNextWindowFocus()

        ImGui.begin(
            "floating", ImGuiWindowFlags.NoBackground
                    or ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoDecoration or ImGuiWindowFlags.NoMove
        )

        ImGui.pushFont(buttonFont)

        val buttonSize = ImGui.getFrameHeight()

        val button = ImGui.button(if(isNodesListOpen) "x" else "+", buttonSize, buttonSize)

        if (button != lastButton && button) {
            isNodesListOpen = !isNodesListOpen
        }

        lastButton = button

        ImGui.popFont()
        ImGui.end()
    }

    private fun drawNodesList() {
    }

}