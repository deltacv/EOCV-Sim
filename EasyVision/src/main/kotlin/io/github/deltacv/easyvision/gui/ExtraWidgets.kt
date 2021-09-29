package io.github.deltacv.easyvision.gui

import imgui.ImGui
import imgui.type.ImInt
import java.lang.Math.random

object ExtraWidgets {

    fun rangeSliders(min: Int, max: Int,
                     minValue: ImInt, maxValue: ImInt,
                     minId: Int, maxId: Int,
                     width: Float = 110f) {
        ImGui.pushItemWidth(width)
        ImGui.sliderInt("###$minId", minValue.data, min, max)

        ImGui.sameLine()

        ImGui.sliderInt("###$maxId", maxValue.data, min, max)
        ImGui.popItemWidth()
    }

    private val valuesStringCache = mutableMapOf<Class<*>, Array<String>>()

    fun <T: Enum<T>> enumCombo(values: Array<T>, currentItem: ImInt): T {
        val clazz = values[0]::class.java

        val valuesStrings = if (valuesStringCache.containsKey(clazz)) {
            valuesStringCache[clazz]!!
        } else {
            val v = values.map {
                it.name
            }.toTypedArray()
            valuesStringCache[clazz] = v

            v
        }

        ImGui.combo("", currentItem, valuesStrings)

        return values[currentItem.get()]
    }

}