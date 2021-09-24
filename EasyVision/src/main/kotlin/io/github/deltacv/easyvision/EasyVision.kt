package io.github.deltacv.easyvision

import imgui.ImGui
import imgui.ImVec2
import imgui.app.Application
import imgui.app.Configuration
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.codegen.Scope
import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.node.NodeEditor
import io.github.deltacv.easyvision.node.math.SumIntegerNode
import io.github.deltacv.easyvision.node.vision.CvtColorNode
import io.github.deltacv.easyvision.node.vision.InputMatNode
import io.github.deltacv.easyvision.node.vision.OutputMatNode
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwSetKeyCallback
import org.lwjgl.glfw.GLFWKeyCallback

class EasyVision : Application() {

    companion object {
        private var ptr = 0L

        private val w = BufferUtils.createIntBuffer(1)
        private val h = BufferUtils.createIntBuffer(1)

        val windowSize: ImVec2 get() {
            w.position(0)
            h.position(0)

            glfwGetWindowSize(ptr, w, h)

            return ImVec2(w.get(0).toFloat(), h.get(0).toFloat())
        }
    }

    private var prevKeyCallback: GLFWKeyCallback? = null

    val editor = NodeEditor(this)

    fun start() {
        editor.init()

        InputMatNode().enable()
        OutputMatNode().enable()

        SumIntegerNode().enable()
        SumIntegerNode().enable()

        CvtColorNode().enable()

        launch(this)

        editor.destroy()
    }

    override fun configure(config: Configuration) {
        config.title = "EasyVision"
    }

    override fun process() {
        if(prevKeyCallback == null) {
            ptr = handle
            // register a new key callback that will call the previous callback and handle some special keys
            prevKeyCallback = glfwSetKeyCallback(handle, ::keyCallback)
        }

        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)

        val size = windowSize
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.begin("Editor",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoCollapse
        )

        editor.draw()

        ImGui.end()

        PopupBuilder.draw()

        isDeleteReleased = false
    }

    var isDeleteReleased = false

    private fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if(prevKeyCallback != null) {
            prevKeyCallback!!.invoke(windowId, key, scancode, action, mods) //invoke the imgui callback
        }

        isDeleteReleased = scancode == 119 && action == GLFW.GLFW_RELEASE
    }

}

fun main() {
    val scope = Scope()
    scope.instanceVariable(Visibility.PUBLIC, "int", "number", "0", isFinal = true)
    scope.localVariable("Mat", "input", "new Mat()")
    scope.methodCall("Imgproc", "cvtColor", "yes", "no", "maybe")

    println(scope.get())

    EasyVision().start()
}