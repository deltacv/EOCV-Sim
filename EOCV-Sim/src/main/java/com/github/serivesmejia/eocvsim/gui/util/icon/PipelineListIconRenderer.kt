package com.github.serivesmejia.eocvsim.gui.util.icon

import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.Icons
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource

import javax.swing.*
import java.awt.*

class PipelineListIconRenderer(
    private val pipelineManager: PipelineManager,
    private val indexMapProvider: () -> Map<Int, Int>
) : DefaultListCellRenderer() {

    private val gearsIcon  by EOCVSimIconLibrary.icoGears.lazyResized(15, 15)
    private val hammerIcon by EOCVSimIconLibrary.icoHammer.lazyResized(15, 15)

    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val label = super.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus
        ) as JLabel

        val source = pipelineManager.pipelines[indexMapProvider()[index]!!].source

        label.icon = when(source) {
            PipelineSource.COMPILED_ON_RUNTIME -> gearsIcon
            else -> hammerIcon
        }

        return label
    }

}
