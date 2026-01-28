package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.gui.DialogFactory
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.JFileChooserApi
import io.github.deltacv.eocvsim.plugin.api.JMenuApi
import io.github.deltacv.eocvsim.plugin.api.JMenuItemApi
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.filechooser.FileFilter

class JMenuApiImpl(owner: EOCVSimPlugin, val internalMenu: JMenu) : JMenuApi(owner) {
    private val addedMenuItems = mutableListOf<JMenuItem>()
    private val addedSeparators = mutableListOf<JPopupMenu.Separator>()

    override val title: String by liveApiField { internalMenu.text }
    override val clickHook by apiField(SimpleHookApiImpl(owner))

    init {
        internalMenu.addMenuListener(object : javax.swing.event.MenuListener {
            override fun menuSelected(e: javax.swing.event.MenuEvent?) {
                if(this@JMenuApiImpl.isDisabled) return
                clickHook.runListeners()
            }

            override fun menuDeselected(e: javax.swing.event.MenuEvent?) { }

            override fun menuCanceled(e: javax.swing.event.MenuEvent?) { }
        })
    }

    override fun addMenuItem(item: JMenuItem) = apiImpl {
        internalMenu.add(item)
        addedMenuItems.add(item)
        Unit
    }

    override fun removeMenuItem(item: JMenuItem) = apiImpl {
        if(!addedMenuItems.contains(item)) return@apiImpl

        internalMenu.remove(item)
        addedMenuItems.remove(item)
    }

    override fun findSubMenuByTitle(title: String) = apiImpl {
        for(i in 0 until internalMenu.itemCount) {
            val menuItem = internalMenu.getItem(i)
            if(menuItem is JMenu && menuItem.text == title) {
                return@apiImpl JMenuApiImpl(owner, menuItem)
            }
        }
        return@apiImpl null
    }

    override fun findItemByTitle(title: String) = apiImpl {
        for(i in 0 until internalMenu.itemCount) {
            val menuItem = internalMenu.getItem(i)
            if(menuItem is JMenuItem && menuItem.text == title) {
                return@apiImpl JMenuItemApiImpl(owner, menuItem)
            }
        }
        return@apiImpl null
    }

    override fun addSeparator() = apiImpl {
        val separator = JPopupMenu.Separator()
        internalMenu.popupMenu.add(separator)
        addedSeparators.add(separator)
        Unit
    }

    override fun disableApi() {
        for(item in addedMenuItems) {
            internalMenu.remove(item)
        }
        addedMenuItems.clear()

        for(separator in addedSeparators) {
            internalMenu.popupMenu.remove(separator)
        }
        addedSeparators.clear()
    }
}

class JMenuItemApiImpl(owner: EOCVSimPlugin, val internalMenuItem: JMenuItem) : JMenuItemApi(owner) {
    override val title: String by liveApiField { internalMenuItem.text }
    override val clickHook by apiField(SimpleHookApiImpl(owner))

    init {
        internalMenuItem.addActionListener {
            clickHook.runListeners()
        }
    }

    override fun disableApi() { }
}

class JFileChooserApiImpl(owner: EOCVSimPlugin, val internalFileChooser: DialogFactory.FileChooser) : JFileChooserApi(owner) {
    override fun addCloseListener(listener: (Result, File, FileFilter) -> Unit) {
        internalFileChooser.addCloseListener { i, file, filter ->
            when(i) {
                JFileChooser.APPROVE_OPTION -> listener(Result.APPROVE, file, filter)
                JFileChooser.CANCEL_OPTION -> listener(Result.CANCEL, file, filter)
                else -> listener(Result.ERROR, file, filter)
            }
        }
    }

    override fun disableApi() { }
}