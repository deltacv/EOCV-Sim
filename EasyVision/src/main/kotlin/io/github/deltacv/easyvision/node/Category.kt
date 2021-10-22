package io.github.deltacv.easyvision.node

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.gui.style.rgbaColor

enum class Category(val properName: String,
                    val color: Int = EasyVision.imnodesStyle.titleBar,
                    val colorSelected: Int = EasyVision.imnodesStyle.titleBarHovered) {

    FLOW("Pipeline Flow"),
    CODE("Coding"),
    HIGH_LEVEL_CV("High Level"),

    COLOR_OP("Basic Color Operations",
        rgbaColor(255, 140, 0, 255), // normal color
        rgbaColor(255, 165, 0, 255)), // hovered/selected color

    SHAPE_DET("Basic Shape Detection",
        rgbaColor(0, 128, 128, 255),
        rgbaColor(0, 139, 139, 255)),

    OVERLAY("Overlay Drawing"),
    MATH("Math Operations"),
    MISC("Miscellaneous")

}