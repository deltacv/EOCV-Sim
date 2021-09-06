package io.github.deltacv.nodeeye.node.attribute.vision

import imgui.ImGui
import io.github.deltacv.nodeeye.node.attribute.Attribute
import io.github.deltacv.nodeeye.node.attribute.AttributeMode

class MatAttribute(override val mode: AttributeMode, val name: String? = null) : Attribute() {

    override fun drawAttribute() {
        ImGui.text("(Image) ${name ?: 
            if(mode == AttributeMode.INPUT) "input" else "output"
        }")
    }

    override fun acceptLink(other: Attribute) = other is MatAttribute

}