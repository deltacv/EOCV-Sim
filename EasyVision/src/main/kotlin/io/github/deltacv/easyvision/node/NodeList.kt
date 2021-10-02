package io.github.deltacv.easyvision.node

import imgui.ImColor
import imgui.ImFont
import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.extension.imnodes.flag.ImNodesColorStyle
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.gui.makeFont
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.node.vision.CvtColorNode
import io.github.deltacv.easyvision.util.ElapsedTime
import kotlinx.coroutines.*

class NodeList(val easyVision: EasyVision) {

    companion object {
        val listNodes = IdElementContainer<Node<*>>()
        val listAttributes = IdElementContainer<Attribute>()

        lateinit var annotatedNodes: List<Class<out Node<*>>>
            private set

        @OptIn(DelicateCoroutinesApi::class)
        private val annotatedNodesJob = GlobalScope.launch(Dispatchers.IO) {
            annotatedNodes = NodeScanner.scan()
        }
    }

    lateinit var buttonFont: ImFont

    val plusFontSize = 60f

    var isNodesListOpen = false
        private set

    private var lastButton = false

    private val openButtonTimeout = ElapsedTime()
    private val hoveringPlusTime = ElapsedTime()

    private lateinit var listContext: ImNodesContext

    val testNode = CvtColorNode()

    fun init() {
        buttonFont = makeFont(plusFontSize)

        testNode.nodesIdContainer = listNodes
        testNode.attributesIdContainer = listAttributes
        testNode.drawAttributesCircles = false
        testNode.enable()
    }

    fun draw() {
        if(!annotatedNodesJob.isCompleted) {
            runBlocking {
                annotatedNodesJob.join()
            }
        }

        val size = EasyVision.windowSize

        if(!easyVision.nodeEditor.isNodeFocused && easyVision.isSpaceReleased) {
            showList()
        }

        if(easyVision.isEscReleased) {
            closeList()
        }

        // NODES LIST

        if(isNodesListOpen) {
            ImGui.setNextWindowPos(0f, 0f)
            ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

            ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.55f) // transparent dark nodes list window

            ImGui.begin("nodes",
                ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
                        or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoTitleBar
                        or ImGuiWindowFlags.NoDecoration //or ImGuiWindowFlags.NoBringToFrontOnFocus
            )

            drawNodesList()

            ImGui.end()

            ImGui.popStyleColor()
        }

        // OPEN/CLOSE BUTTON

        ImGui.setNextWindowPos(size.x - plusFontSize * 1.8f, size.y - plusFontSize * 1.8f)

        if(isNodesListOpen) {
            ImGui.setNextWindowFocus()
        }

        ImGui.begin(
            "floating", ImGuiWindowFlags.NoBackground
                    or ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoDecoration or ImGuiWindowFlags.NoMove
        )

        ImGui.pushFont(buttonFont)
        val buttonSize = ImGui.getFrameHeight()

        val button = ImGui.button(if(isNodesListOpen) "x" else "+", buttonSize, buttonSize)

        ImGui.popFont()

        if (button != lastButton && button && !isNodesListOpen && openButtonTimeout.millis > 200) {
            showList()
        }

        if(ImGui.isItemHovered()) {
            if(hoveringPlusTime.millis > 500) {
                ImGui.beginTooltip()
                    ImGui.text(
                        if(isNodesListOpen) {
                            "Press ESCAPE to close the nodes list"
                        } else "Press SPACE to open the nodes list"
                    )
                ImGui.endTooltip()
            }
        } else {
            hoveringPlusTime.reset()
        }

        lastButton = button

        ImGui.end()
    }

    private fun drawNodesList() {
        ImNodes.editorContextSet(listContext)

        ImNodes.getStyle().gridSpacing = 99999f // lol only way to make grid invisible
        ImNodes.pushColorStyle(ImNodesColorStyle.GridBackground, ImColor.floatToColor(0f, 0f, 0f, 0f))

        ImNodes.clearNodeSelection()
        ImNodes.clearLinkSelection()

        ImNodes.beginNodeEditor()
            for(node in listNodes) {
                node.draw()
            }
        ImNodes.endNodeEditor()

        ImNodes.getStyle().gridSpacing = 32f // back to normal
        ImNodes.popColorStyle()

        handleClick()
    }

    fun handleClick() {
        if(ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            val hovered = ImNodes.getHoveredNode()

            if(hovered >= 0) {
                val nodeClass = listNodes[hovered]!!::class.java
                val instance = nodeClass.getConstructor().newInstance()
                instance.enable()

                if(instance is DrawNode<*>) {
                    val nodePos = ImVec2()
                    ImNodes.getNodeScreenSpacePos(hovered, nodePos)

                    val mousePos = ImGui.getMousePos()

                    val newPosX = mousePos.x - nodePos.x
                    val newPosY = mousePos.y - nodePos.y

                    instance.nextNodePosition = ImVec2(newPosX, newPosY)
                    instance.pinToMouse = true
                }
            }

            closeList()
            openButtonTimeout.reset()
        }
    }

    fun showList() {
        if(!isNodesListOpen) {
            if(::listContext.isInitialized) {
                listContext.destroy()
            }
            listContext = ImNodesContext()

            isNodesListOpen = true
        }
    }

    fun closeList() {
        if(isNodesListOpen) {
            isNodesListOpen = false
        }
    }

}