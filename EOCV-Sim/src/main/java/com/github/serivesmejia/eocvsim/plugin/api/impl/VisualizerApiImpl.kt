package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarPanel
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.VisualizerApi

internal class VisualizerApiImpl(owner: EOCVSimPlugin, val internalVisualizer: Visualizer) : VisualizerApi(owner) {
    override val frame by nullableApiField(lazy = false) { internalVisualizer.frame }

    override val creationHook by apiField(EventHandlerHookApiImpl(owner, internalVisualizer.onPluginGuiAttachment))

    override val sidebar: Sidebar by apiField(SidebarImpl(owner, internalVisualizer.sidebarPanel))

    private class SidebarImpl(owner: EOCVSimPlugin, val internalSidebar: SidebarPanel) : Sidebar(owner) {
        val tabs = mutableMapOf<Tab, Int>()

        override fun addTab(tab: Tab) = apiImpl {
            // throw if sidebar already has a tab with the same title
            if(internalSidebar.indexOfTab(tab.title) != -1) {
                throw IllegalArgumentException("Sidebar already has a tab with the title '${tab.title}'")
            }

            internalSidebar.addTab(tab.title, tab.contents)
            tabs[tab] = internalSidebar.indexOfTab(tab.title)
        }

        override fun removeTab(tab: Tab) = apiImpl {
            val index = tabs[tab] ?: return@apiImpl
            internalSidebar.removeTabAt(index)
            tabs.remove(tab)
        }

        override fun disableApi() {
            tabs.keys.forEach { removeTab(it) }
            tabs.clear()
        }
    }

    override fun disableApi() {}
}