package io.github.deltacv.easyvision.gui

import imgui.ImColor
import imgui.ImFont
import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.extension.imnodes.flag.ImNodesColorStyle
import imgui.extension.imnodes.flag.ImNodesStyleFlags
import imgui.flag.*
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.io.KeyManager
import io.github.deltacv.easyvision.io.Keys
import io.github.deltacv.easyvision.node.*
import io.github.deltacv.easyvision.util.ElapsedTime
import kotlinx.coroutines.*

class NodeList(val easyVision: EasyVision, val keyManager: KeyManager) {

    companion object {
        val listNodes = IdElementContainer<Node<*>>()
        val listAttributes = IdElementContainer<Attribute>()

        lateinit var categorizedNodes: CategorizedNodes
            private set

        @OptIn(DelicateCoroutinesApi::class)
        private val annotatedNodesJob = GlobalScope.launch(Dispatchers.IO) {
            categorizedNodes = NodeScanner.scan()
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

    fun init() {
        buttonFont = makeFont(plusFontSize)
    }

    fun draw() {
        val size = EasyVision.windowSize

        if(!easyVision.nodeEditor.isNodeFocused && keyManager.released(Keys.Spacebar)) {
            showList()
        }

        if(keyManager.released(Keys.Escape)) {
            closeList()
        }

        // NODES LIST

        if(isNodesListOpen) {
            drawNodesList(size)
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

        if (button != lastButton && !isNodesListOpen && button && openButtonTimeout.millis > 200) {
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

    val nodes by lazy {
        val map = mutableMapOf<Category, MutableList<Node<*>>>()

        for((category, nodeClasses) in categorizedNodes) {
            val list = mutableListOf<Node<*>>()

            for(nodeClass in nodeClasses) {
                val instance = nodeClass.getConstructor().newInstance()

                instance.nodesIdContainer = listNodes
                instance.attributesIdContainer = listAttributes
                //instance.drawAttributesCircles = false
                instance.enable()

                list.add(instance)
            }

            map[category] = list
        }

        map
    }

    private val tablesCategories = mutableMapOf<Category, Table>()

    private var isFirstDraw = true
    private var isSecondDraw = false

    var currentScroll = 0f

    var hoveredNode = -1
        private set

    var isHoverManuallyDetected = false
        private set

    private val categoriesState = mutableMapOf<Category, Boolean>()
    private val drawnNodes = mutableListOf<Int>()

    private fun drawNodesList(size: ImVec2) {

        val scrollValue = when {
            keyManager.pressed(Keys.ArrowUp) -> {
                -1.5f
            }
            keyManager.pressed(Keys.ArrowDown) -> {
                1.5f
            }
            else -> {
                -ImGui.getIO().mouseWheel
            }
        }

        var closeOnClick = true

        ImNodes.editorContextSet(listContext)

        // NODES WINDOW

        ImNodes.getStyle().gridSpacing = 99999f // lol only way to make grid invisible
        ImNodes.pushColorStyle(ImNodesColorStyle.GridBackground, ImColor.floatToColor(0f, 0f, 0f, 0f))

        ImNodes.clearNodeSelection()
        ImNodes.clearLinkSelection()

        ImGui.setNextWindowPos(0f, 0f)
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.55f) // transparent dark nodes list window

        ImGui.begin("nodes",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
                    or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoTitleBar
                    or ImGuiWindowFlags.NoDecoration
        )

        ImNodes.beginNodeEditor()
        for(category in Category.values()) {
            if(nodes.containsKey(category)) {
                val table = tablesCategories[category] ?: continue

                if (categoriesState[category] == true) {
                    for (node in nodes[category]!!) {
                        if(drawnNodes.contains(node.id)) {
                            if (!table.contains(node.id)) {
                                val nodeSize = ImVec2()
                                ImNodes.getNodeDimensions(node.id, nodeSize)

                                table.add(node.id, nodeSize)
                            } else {
                                val pos = table.getPos(node.id)!!
                                ImNodes.setNodeScreenSpacePos(node.id, pos.x, pos.y)
                            }
                        }

                        if(isHoverManuallyDetected && hoveredNode == node.id) {
                            ImNodes.pushColorStyle(ImNodesColorStyle.NodeBackground, EasyVision.imnodesStyle.nodeBackgroundHovered)
                            ImNodes.pushColorStyle(ImNodesColorStyle.TitleBar, EasyVision.imnodesStyle.titleBarHovered)
                        }

                        node.draw()
                        if(!drawnNodes.contains(node.id)) {
                            drawnNodes.add(node.id)
                        }

                        if(isHoverManuallyDetected && hoveredNode == node.id) {
                            ImNodes.popColorStyle()
                            ImNodes.popColorStyle()
                        }
                    }
                }
            }
        }
        ImNodes.endNodeEditor()
        ImGui.end()
        ImGui.popStyleColor()

        ImNodes.editorResetPanning(0f, 0f)

        // HEADERS WINDOW

        ImGui.setNextWindowPos(0f, 0f)
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.0f) // transparent headers window

        ImGui.begin("headers",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
                    or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoTitleBar
                    or ImGuiWindowFlags.NoDecoration or ImGuiWindowFlags.AlwaysVerticalScrollbar
        )

        ImGui.setCursorPos(0f, 0f) // draw the node editor on top of the collapisng headers

        for(category in Category.values()) {
            if(nodes.containsKey(category)) {
                if (!tablesCategories.containsKey(category)) {
                    tablesCategories[category] = Table()
                }

                val isOpen = ImGui.collapsingHeader(
                    category.properName, ImGuiTreeNodeFlags.DefaultOpen
                )
                categoriesState[category] = isOpen

                if (ImGui.isItemHovered()) {
                    closeOnClick = false
                }

                if(isOpen) {
                    val table = tablesCategories[category]!!

                    ImGui.newLine()
                    ImGui.indent(10f)

                    currentScroll = ImGui.getScrollY() + scrollValue * 20.0f
                    ImGui.setScrollY(currentScroll)

                    table.draw()

                    ImGui.newLine()
                    ImGui.unindent(10f)
                }
            }
        }
        ImGui.end()
        ImGui.popStyleColor()

        hoveredNode = ImNodes.getHoveredNode()
        isHoverManuallyDetected = false

        if(hoveredNode < 0) {
            val mousePos = ImGui.getMousePos()

            tableLoop@ for((_, table) in tablesCategories) {
                for((id, rect) in table.currentRects) {
                    if(mousePos.x > rect.min.x && mousePos.x < rect.max.x &&
                            mousePos.y > rect.min.y && mousePos.y < rect.max.y) {
                        hoveredNode = id
                        isHoverManuallyDetected = true
                        break@tableLoop
                    }
                }
            }
        }

        ImNodes.getStyle().gridSpacing = 32f // back to normal
        ImNodes.popColorStyle()

        handleClick(closeOnClick)
    }

    fun handleClick(closeOnClick: Boolean) {
        if(ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            if(hoveredNode >= 0) {
                val nodeClass = listNodes[hoveredNode]!!::class.java
                val instance = nodeClass.getConstructor().newInstance()
                instance.enable()

                if(instance is DrawNode<*>) {
                    val nodePos = ImVec2()
                    ImNodes.getNodeScreenSpacePos(hoveredNode, nodePos)

                    val mousePos = ImGui.getMousePos()

                    val newPosX = mousePos.x - nodePos.x
                    val newPosY = mousePos.y - nodePos.y

                    instance.nextNodePosition = ImVec2(newPosX, newPosY)
                    instance.pinToMouse = true
                }

                closeList()
            } else if(closeOnClick) {
                closeList()
            }
        }
    }

    fun showList() {
        if(!isNodesListOpen) {
            if(!annotatedNodesJob.isCompleted) {
                runBlocking {
                    annotatedNodesJob.join() // wait for the scanning to finish
                }
            }

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
            openButtonTimeout.reset()
        }
    }

}