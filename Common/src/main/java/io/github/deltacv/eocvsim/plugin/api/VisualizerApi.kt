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
import javax.swing.JFrame
import javax.swing.JPanel

/**
 * Main entry point for interacting with the visualizer UI.
 *
 * Provides access to the main window, UI lifecycle hooks, menus,
 * sidebar tabs, and dialog creation facilities.
 */
abstract class VisualizerApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * The visualizer window frame.
     *
     * May be null if the visualizer has not been created yet or
     * is running in headless mode.
     */
    abstract val frame: JFrame?

    /**
     * Fired when the visualizer UI is created and ready.
     *
     * Hooks attached here are guaranteed to run after UI components
     * are initialized.
     */
    abstract val creationHook: HookApi

    /**
     * Access to the top menu bar APIs.
     */
    abstract val topMenuBarApi: VisualizerTopMenuBarApi

    /**
     * Access to the visualizer sidebar APIs.
     */
    abstract val sidebarApi: VisualizerSidebarApi

    /**
     * Factory for creating dialogs tied to the visualizer.
     */
    abstract val dialogFactoryApi: DialogFactoryApi
}

/**
 * API for accessing and modifying the visualizer's top menu bar.
 */
abstract class VisualizerTopMenuBarApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * The "File" menu.
     */
    abstract val fileMenuApi: JMenuApi

    /**
     * The "Workspace" menu.
     */
    abstract val workspaceMenuApi: JMenuApi

    /**
     * The "Help" menu.
     */
    abstract val helpMenuApi: JMenuApi
}

/**
 * API for interacting with the visualizer sidebar.
 *
 * The sidebar is composed of tabs that can be added or removed
 * dynamically by plugins.
 */
abstract class VisualizerSidebarApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Fired whenever the active sidebar tab changes.
     */
    abstract val tabChangeHook: HookApi

    /**
     * Adds a new tab to the sidebar.
     *
     * @param tab tab instance to add
     */
    abstract fun addTab(tab: Tab)

    /**
     * Removes an existing tab from the sidebar.
     *
     * @param tab tab instance to remove
     */
    abstract fun removeTab(tab: Tab)

    /**
     * Checks whether the given tab is currently active.
     *
     * @param tab tab to check
     * @return true if the tab is active
     */
    abstract fun isActive(tab: Tab): Boolean

    /**
     * Represents a single sidebar tab.
     *
     * Implementations are responsible for creating their UI and reacting
     * to activation state changes.
     *
     * All API members implemented by a tab subclass **must** follow the standard
     * [Api] contract: method implementations should be wrapped with [apiImpl],
     * and exposed fields should be declared using [apiField], to ensure proper
     * lifecycle tracking and automatic invalidation when the owning plugin
     * is disabled.
     */
    abstract class Tab(owner: EOCVSimPlugin) : Api(owner) {

        /**
         * Display title of the tab.
         */
        abstract val title: String

        /**
         * Called when the tab becomes active.
         */
        abstract fun onActivated()

        /**
         * Called when the tab is no longer active.
         */
        abstract fun onDeactivated()

        /**
         * Creates the UI contents of this tab.
         *
         * @param target panel into which the tab UI should be built
         */
        abstract fun create(target: JPanel)

        /**
         * Sidebar tabs do not manage external resources and
         * require no explicit cleanup by default.
         */
        override fun disableApi() { }
    }
}