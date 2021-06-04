package com.github.serivesmejia.eocvsim.gui.util

import java.awt.Image
import java.lang.reflect.InvocationTargetException

object ReflectTaskbar {

    private val taskbarClass by lazy { Class.forName("java.awt.Taskbar") }

    private val isTaskbarSupportedMethod by lazy { taskbarClass.getDeclaredMethod("isTaskbarSupported") }

    private val getTaskbarMethod by lazy { taskbarClass.getDeclaredMethod("getTaskbar") }
    private val setIconImageMethod by lazy { taskbarClass.getDeclaredMethod("setIconImage", Image::class.java) }

    val isUsable by lazy {
        try {
            isTaskbarSupported
        } catch(ex: ClassNotFoundException) { false }
    }
    val isTaskbarSupported get() = isTaskbarSupportedMethod.invoke(null) as Boolean

    val taskbar by lazy { getTaskbarMethod.invoke(null) }

    @Throws(SecurityException::class, UnsupportedOperationException::class)
    fun setIconImage(image: Image) {
        try {
            setIconImageMethod.invoke(taskbar, image)
        } catch(e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

}