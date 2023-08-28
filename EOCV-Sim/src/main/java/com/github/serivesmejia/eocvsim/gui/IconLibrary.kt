package com.github.serivesmejia.eocvsim.gui

import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import kotlin.reflect.KProperty

fun icon(name: String, path: String, allowInvert: Boolean = true) = EOCVSimIconDelegate(name, path, allowInvert)

class EOCVSimIconDelegate(val name: String, val path: String, allowInvert: Boolean = true) {
    init {
        Icons.addFutureImage(name, path, allowInvert)
    }

    operator fun getValue(eocvSimIconLibrary: EOCVSimIconLibrary, property: KProperty<*>): Icons.NamedImageIcon {
        return Icons.getImage(name)
    }
}