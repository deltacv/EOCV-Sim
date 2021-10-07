package io.github.deltacv.easyvision.gui

import imgui.ImGui
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.util.ElapsedTime

object PopupBuilder {

    private val tooltips = mutableListOf<ToolTip>()

    private val labels = mutableMapOf<String, Label>()

    fun addWarningToolTip(message: String, w: Float? = null, h: Float? = null) {
        deleteLabel("WARN")

        val windowSize = EasyVision.windowSize

        var x = windowSize.x * 0.5f
        var y = windowSize.y * 0.85f

        val wW = w ?: message.length * 7.5f
        val wH = h ?: 30f

        x -= wW / 2f
        y += wH / 2f

        addToolTip(x, y, wW, wH, message, 6.0, label = "WARN")
    }

    fun addToolTip(x: Float, y: Float, w: Float? = null, h: Float? = null,
                   message: String, time: Double? = null, label: String = "") {
        addToolTip(x, y, w, h, time, label) {
            ImGui.text(message)
        }
    }


    fun addToolTip(x: Float, y: Float, w: Float? = null, h: Float? = null,
                   time: Double? = null, label: String = "", drawCallback: () -> Unit) {
        val tooltip = ToolTip(x, y, w, h, time, drawCallback)
        tooltips.add(tooltip)

        labels[label] = Label(tooltip) {
            tooltips.remove(tooltip)
        }
    }

    fun deleteLabel(label: String) {
        labels[label]?.deleteCall?.invoke()
        labels.remove(label)
    }

    fun draw() {
        for(tooltip in tooltips.toTypedArray()) {
            if(tooltip.time != null && tooltip.elapsedTime.seconds >= tooltip.time) {
                tooltips.remove(tooltip)

                for(label in labels.values.toTypedArray()) {
                    if(label.any == tooltip) {
                        label.deleteCall()
                    }
                }

                continue
            }

            tooltip.draw()
        }
    }

    private data class ToolTip(val x: Float, val y: Float, val w: Float?, val h: Float?,
                               val time: Double?, val callback: () -> Unit) {

        val elapsedTime by lazy { ElapsedTime() }

        fun draw() {
            elapsedTime.seconds

            ImGui.setNextWindowPos(x, y)
            if(w != null && h != null) {
                ImGui.setNextWindowSize(w, h)
            }

            ImGui.beginTooltip()
                callback()
            ImGui.endTooltip()
        }

    }

    private data class Label(val any: Any, val deleteCall: () -> Unit)

}