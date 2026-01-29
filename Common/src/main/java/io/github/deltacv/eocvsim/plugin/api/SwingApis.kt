package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import java.io.File
import javax.swing.JMenuItem
import javax.swing.filechooser.FileFilter

abstract class JMenuItemApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val title: String
    abstract val clickHook: HookApi
}

abstract class JMenuApi(owner: EOCVSimPlugin) : JMenuItemApi(owner) {
    abstract fun addMenuItem(item: JMenuItem)
    abstract fun removeMenuItem(item: JMenuItem)

    abstract fun findSubMenuByTitle(title: String): JMenuApi?
    abstract fun findItemByTitle(title: String): JMenuItemApi?

    abstract fun addSeparator()
}

abstract class JFileChooserApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract fun addCloseListener(listener: (Result, File, FileFilter) -> Unit)

    enum class Mode {
        FILE_SELECT, DIRECTORY_SELECT, SAVE_FILE
    }
    enum class Result {
        APPROVE, CANCEL, ERROR
    }
}