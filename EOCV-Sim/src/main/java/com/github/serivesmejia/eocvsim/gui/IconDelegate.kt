package com.github.serivesmejia.eocvsim.gui

import kotlin.reflect.KProperty

fun icon(name: String, path: String, allowInvert: Boolean = true) = IconDelegate(name, path, allowInvert)

class IconDelegate(val name: String, val path: String, allowInvert: Boolean = true) {
    init {
        Icons.addFutureImage(name, path, allowInvert)
    }

    operator fun getValue(any: Any, property: KProperty<*>): Icons.NamedImageIcon {
        return Icons.getImage(name)
    }
}