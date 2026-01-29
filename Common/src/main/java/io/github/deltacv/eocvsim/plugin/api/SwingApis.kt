/*
 * Copyright (c) 2026 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import java.io.File
import javax.swing.JMenuItem
import javax.swing.filechooser.FileFilter

/**
 * Represents a menu item owned by a plugin.
 *
 * A menu item exposes a display [title] and a [clickHook] that is triggered
 * whenever the user activates the item.
 */
abstract class JMenuItemApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Display title of the menu item.
     */
    abstract val title: String

    /**
     * Hook invoked when the menu item is clicked.
     *
     * Listeners attached to this hook are executed on user interaction.
     */
    abstract val clickHook: HookApi
}

/**
 * Represents a menu that can contain menu items and sub-menus.
 *
 * A menu is itself a [JMenuItemApi], allowing it to be nested inside other menus.
 * Items added to this menu are managed by the application UI.
 */
abstract class JMenuApi(owner: EOCVSimPlugin) : JMenuItemApi(owner) {

    /**
     * Adds a Swing [JMenuItem] to this menu.
     *
     * @param item menu item to add
     */
    abstract fun addMenuItem(item: JMenuItem)

    /**
     * Removes a Swing [JMenuItem] from this menu.
     *
     * @param item menu item to remove
     */
    abstract fun removeMenuItem(item: JMenuItem)

    /**
     * Finds a sub-menu by its title.
     *
     * @param title title of the sub-menu
     * @return matching submenu, or null if not found
     */
    abstract fun findSubMenuByTitle(title: String): JMenuApi?

    /**
     * Finds a menu item by its title.
     *
     * @param title title of the menu item
     * @return matching menu item, or null if not found
     */
    abstract fun findItemByTitle(title: String): JMenuItemApi?

    /**
     * Adds a visual separator to this menu.
     */
    abstract fun addSeparator()
}

/**
 * Abstraction over a file chooser dialog.
 *
 * Allows plugins to listen for dialog close events and inspect the
 * result, selected file, and active filter.
 */
abstract class JFileChooserApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Registers a listener that is invoked when the file chooser closes.
     *
     * @param listener callback receiving the dialog [Result],
     *                 the selected [File], and the active [FileFilter]
     */
    abstract fun addCloseListener(
        listener: (Result, File, FileFilter) -> Unit
    )

    /**
     * File chooser operation mode.
     */
    enum class Mode {
        /** Select an existing file */
        FILE_SELECT,

        /** Select a directory */
        DIRECTORY_SELECT,

        /** Select a file path to save to */
        SAVE_FILE
    }

    /**
     * Result of the file chooser dialog.
     */
    enum class Result {
        /** User approved the selection */
        APPROVE,

        /** User canceled the dialog */
        CANCEL,

        /** An error occurred */
        ERROR
    }
}