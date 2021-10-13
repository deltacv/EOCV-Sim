package io.github.deltacv.easyvision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.internal.ImRect
import io.github.deltacv.easyvision.EasyVision

// this is an extremely hacky "dummy" table which uses the columns api.
// mostly used for the nodes list, because nodes don't respect any sort of
// imgui constraint, whether they are being used within the columns api or anything.
// they are just placed on the default position regardless of anything.
// (that is in fact a good thing for a node editor, but in this case we are trying to
// make a nice looking table by using another node editor to be able to display the nodes
// as they are...)
//
// to use the columns api with the nodes within a node editor, we create the columns as normal,
// except that instead of putting the nodes in the columns directly, we place "dummy" invisible
// elements which have the same size as the node that would correspond in each cell.
//
// later on, we get the position of the dummy element of each cell and set it to the corresponding
// node, which is independent of our table, i could *technically* make the calculations myself to place
// the nodes in they corresponding positions without this bad hack, but hey if imgui can do it for us
// why not use it instead?
//
// ...i hate this so much
class Table(val maxRows: Int = 4) {

    private val rects = mutableMapOf<Int, ImVec2>()
    private val currentRects = mutableMapOf<Int, ImVec2>()

    private val columnsId by EasyVision.miscIds.nextId()

    /**
     * Adds a dummy rectangle to the table with the specified size.
     */
    fun add(id: Int, size: ImVec2) {
        rects[id] = size
    }

    fun draw() {
        if(rects.isEmpty()) return

        ImGui.columns(if(rects.size >= maxRows) maxRows else rects.size, "###$columnsId", false)

        for((id, size) in rects) {
            // our dummy element, it's an invisible button because
            // i couldn't find anything better with an adjustable
            // rectangle shape.
            ImGui.invisibleButton("###$id", size.x, size.y)
            if(!currentRects.containsKey(id)) {
                currentRects[id] = ImVec2()
            }

            // store the invisible button position
            currentRects[id] = ImGui.getItemRectMin()

            ImGui.nextColumn()
        }

        ImGui.columns(1)
    }

    /**
     * Gets the absolute position of a dummy rectangle with the specified id
     */
    fun getPos(id: Int) = currentRects[id]

}