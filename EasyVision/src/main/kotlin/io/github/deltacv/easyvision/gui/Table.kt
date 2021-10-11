package io.github.deltacv.easyvision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.internal.ImRect
import io.github.deltacv.easyvision.EasyVision

class Table(val maxRows: Int = 4) {

    private val rects = mutableMapOf<Int, ImVec2>()
    private val currentRects = mutableMapOf<Int, ImVec2>()

    private val columnsId by EasyVision.miscIds.nextId()

    fun add(id: Int, size: ImVec2) {
        rects[id] = size
    }

    fun draw() {
        if(rects.isEmpty()) return

        ImGui.columns(if(rects.size >= maxRows) maxRows else rects.size, "##$columnsId", false)

        for((id, size) in rects) {
            ImGui.invisibleButton("##$id", size.x, size.y)
            if(!currentRects.containsKey(id)) {
                currentRects[id] = ImVec2()
            }

            currentRects[id] = ImGui.getItemRectMin()

            ImGui.nextColumn()
        }

        ImGui.columns(1)
    }

    fun getPos(id: Int) = currentRects[id]

}