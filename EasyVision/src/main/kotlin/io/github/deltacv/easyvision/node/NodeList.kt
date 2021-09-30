package io.github.deltacv.easyvision.node

import imgui.ImFont
import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.gui.makeFont

class NodeList {

    lateinit var buttonFont: ImFont

    val plusFontSize = 50f

    fun init() {
        buttonFont = makeFont(plusFontSize)
    }

    fun draw() {
        val size = EasyVision.windowSize
        ImGui.setNextWindowPos(size.x - plusFontSize * 2.2f, size.y - plusFontSize * 2.2f)

        ImGui.begin("floating", ImGuiWindowFlags.NoBackground
                or ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoDecoration or ImGuiWindowFlags.NoMove)
            ImGui.pushFont(buttonFont)

            val buttonSize = ImGui.getFrameHeight()

            if(ImGui.button("+", buttonSize, buttonSize)) {

            }

            ImGui.popFont()
        ImGui.end()
    }

}