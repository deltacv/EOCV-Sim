package io.github.deltacv.easyvision.node

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.gui.style.rgbaColor

enum class Category(val properName: String,
                    val color: Int = EasyVision.imnodesStyle.titleBar,
                    val colorSelected: Int = EasyVision.imnodesStyle.titleBarHovered) {

    FLOW("Pipeline Flow"),
    CODE("Coding"),
    HIGH_LEVEL_CV("High Level"),
    COLOR_OP("Basic Color Operations", rgbaColor(255, 140, 0, 255), rgbaColor(255, 165, 0, 255)),
    SHAPE_DET("Basic Shape Detection"),
    OVERLAY("Overlay Drawing"),
    MATH("Math Operations"),
    MISC("Miscellaneous")

}