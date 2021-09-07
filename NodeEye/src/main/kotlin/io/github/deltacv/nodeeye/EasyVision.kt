package io.github.deltacv.nodeeye

import imgui.ImGui
import imgui.ImVec2
import imgui.app.Application
import imgui.app.Configuration
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.nodeeye.node.*

import io.github.deltacv.nodeeye.node.math.SumIntegerNode
import io.github.deltacv.nodeeye.node.vision.InputMatNode
import io.github.deltacv.nodeeye.node.vision.OutputMatNode

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.glfwGetWindowSize

class NodeEye : Application() {

    private val w = BufferUtils.createIntBuffer(1)
    private val h = BufferUtils.createIntBuffer(1)

    val windowSize: ImVec2 get() {
        w.position(0)
        h.position(0)

        glfwGetWindowSize(handle, w, h)

        return ImVec2(w.get(0).toFloat(), h.get(0).toFloat())
    }

    val editor = NodeEditor()

    override fun configure(config: Configuration) {
        config.title = "NodeEye"
    }

    fun start() {
        editor.init()

        InputMatNode().enable()
        OutputMatNode().enable()

        SumIntegerNode().enable()
        SumIntegerNode().enable()

        launch(this)

        editor.destroy()
    }

    override fun process() {
        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)

        val size = windowSize
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.begin("Editor",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoCollapse
        )

        editor.draw()

        ImGui.end()
    }

}

fun main() {
    NodeEye().start()
}