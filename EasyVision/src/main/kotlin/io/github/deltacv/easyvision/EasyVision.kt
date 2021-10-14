/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
import io.github.deltacv.easyvision.node.NodeEditor
import io.github.deltacv.easyvision.node.NodeList
import io.github.deltacv.easyvision.node.vision.*
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

    val nodeEditor = NodeEditor(this)
    val nodeList = NodeList(this)

    val codeGenManager = CodeGenManager(this)

    private lateinit var defaultFont: ImFont

    fun start() {
        nodeEditor.init()

        launch(this)

        nodeEditor.destroy()
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

        nodeEditor.draw()

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
            codeGenManager.build()
        }
    }

    var isDeleteReleased = false
        private set
    var isEscReleased = false
        private set
    var isSpaceReleased = false
        private set

    var isArrowUpPressed = false
        private set
    var isArrowDownPressed = false
        private set

    private fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if(prevKeyCallback != null) {
            prevKeyCallback!!.invoke(windowId, key, scancode, action, mods) //invoke the imgui callback
        }

        isDeleteReleased = scancode == 119 && action == GLFW_RELEASE
        isEscReleased = scancode == 9 && action == GLFW_RELEASE
        isSpaceReleased = scancode == 65 && action == GLFW_RELEASE

        isArrowUpPressed = scancode == 111 && action == GLFW_PRESS
        isArrowDownPressed = scancode == 116 && action == GLFW_PRESS

        println(scancode)
    }
}

fun main() {
    EasyVision().start()
}