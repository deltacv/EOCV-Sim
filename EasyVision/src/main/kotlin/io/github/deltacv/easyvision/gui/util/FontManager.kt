package io.github.deltacv.easyvision.gui.util

import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImGui

class FontManager {

    val fonts = mutableMapOf<String, ImFont>()

    fun makeFont(ttfPath: String, size: Float): ImFont {

    }

    fun makeDefaultFont(size: Float): ImFont {
        val name = "default-$size"
        if(fonts.containsKey(name)) {
            return fonts[name]!!
        }

        val fontConfig = ImFontConfig()
        fontConfig.sizePixels = size
        fontConfig.oversampleH = 1
        fontConfig.oversampleV = 1
        fontConfig.pixelSnapH = false

        val font = ImGui.getIO().fonts.addFontDefault(fontConfig)
        fonts[name] = font

        return font
    }

}