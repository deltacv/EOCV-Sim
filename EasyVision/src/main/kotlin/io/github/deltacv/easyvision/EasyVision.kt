package io.github.deltacv.easyvision

import imgui.ImFont
import imgui.ImGui
import imgui.ImVec2
import imgui.app.Application
import imgui.app.Configuration
import imgui.flag.*
import io.github.deltacv.easyvision.codegen.*
import io.github.deltacv.easyvision.gui.PopupBuilder
import io.github.deltacv.easyvision.gui.makeFont
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.node.vision.*
import io.github.deltacv.easyvision.util.ElapsedTime
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*
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

        val miscIds = IdElementContainer<Any>()
    }

    private var prevKeyCallback: GLFWKeyCallback? = null

    val editor = NodeEditor(this)
    val nodeList = NodeList(this)

    lateinit var defaultFont: ImFont
    
    val inputNode = InputMatNode()
    val outputNode = OutputMatNode()

    fun start() {
        editor.init()

        inputNode.enable()
        outputNode.enable()

        CvtColorNode().enable()

        ThresholdNode().enable()

        MaskNode().enable()

        launch(this)

        editor.destroy()
    }

    override fun configure(config: Configuration) {
        config.title = "EasyVision"
    }

    override fun initImGui(config: Configuration?) {
        super.initImGui(config)
        defaultFont = makeFont(13f)
        nodeList.init()
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

        ImGui.pushFont(defaultFont)

        ImGui.begin("Editor",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
                    or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoBringToFrontOnFocus
                    or ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoDecoration
        )

        editor.draw()

        ImGui.end()
        ImGui.popFont()

        nodeList.draw()

        ImGui.pushFont(defaultFont)
        PopupBuilder.draw()
        ImGui.popFont()

        isDeleteReleased = false
        isEscReleased = false
        isSpaceReleased = false

        if(ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
            val timer = ElapsedTime()

            val codeGen = CodeGen("TestPipeline")
            inputNode.startGen(codeGen.currScopeProcessFrame)

            println(codeGen.gen())
            println("took ${timer.seconds}")
        }
    }

    var isDeleteReleased = false
        private set
    var isEscReleased = false
        private set
    var isSpaceReleased = false
        private set

    private fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if(prevKeyCallback != null) {
            prevKeyCallback!!.invoke(windowId, key, scancode, action, mods) //invoke the imgui callback
        }

        isDeleteReleased = scancode == 119 && action == GLFW.GLFW_RELEASE
        isEscReleased = scancode == 9 && action == GLFW.GLFW_RELEASE
        isSpaceReleased = scancode == 65 && action == GLFW.GLFW_RELEASE
    }
}

fun main() {
    EasyVision().start()
}