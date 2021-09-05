package io.github.deltacv.nodeeye

import imgui.ImGui
import imgui.app.Application
import imgui.app.Configuration
import imgui.extension.nodeditor.NodeEditor
import imgui.extension.nodeditor.NodeEditorConfig
import imgui.extension.nodeditor.NodeEditorContext
import imgui.extension.nodeditor.flag.NodeEditorPinKind

class NodeEye : Application() {

    private lateinit var context: NodeEditorContext

    fun start() {
        val config = NodeEditorConfig()
        config.settingsFile = null

        context = NodeEditorContext(config)

        launch(this)
    }

    override fun configure(config: Configuration) {
        config.title = "NodeEye"
    }

    override fun process() {
        NodeEditor.setCurrentEditor(context)
        NodeEditor.begin("Editor")

        var uniqueId = 0L

        NodeEditor.beginNode(uniqueId++)
            ImGui.text("Node A")

            NodeEditor.beginPin(uniqueId++, NodeEditorPinKind.Input)
                ImGui.text("-> In")
            NodeEditor.endPin()

            ImGui.sameLine()

            NodeEditor.beginPin(uniqueId++, NodeEditorPinKind.Output)
                ImGui.text("Out ->")
            NodeEditor.endPin()
        NodeEditor.endNode()

        NodeEditor.end()
    }

}

fun main() {
    NodeEye().start()
}