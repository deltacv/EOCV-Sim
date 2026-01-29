package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.component.visualizer.SidebarPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.TopMenuBar
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.DialogFactoryApi
import io.github.deltacv.eocvsim.plugin.api.VisualizerApi
import io.github.deltacv.eocvsim.plugin.api.VisualizerSidebarApi
import io.github.deltacv.eocvsim.plugin.api.VisualizerTopMenuBarApi

class VisualizerApiImpl(owner: EOCVSimPlugin, val internalVisualizer: Visualizer) : VisualizerApi(owner) {
    override val frame by liveNullableApiField { internalVisualizer.frame }

    override val creationHook by apiField(EventHandlerHookApiImpl(owner, internalVisualizer.onPluginGuiAttachment))

    override val topMenuBarApi: VisualizerTopMenuBarApi by apiField { VisualizerTopMenuBarApiImpl(owner, internalVisualizer.menuBar) }
    override val sidebarApi: VisualizerSidebarApi by apiField { VisualizerSidebarApiImpl(owner, internalVisualizer.sidebarPanel) }
    override val dialogFactoryApi: DialogFactoryApi by apiField { DialogFactoryApiImpl(owner, internalVisualizer) }

    override fun disableApi() { }
}

private class VisualizerTopMenuBarApiImpl(owner: EOCVSimPlugin, internalTopMenuBar: TopMenuBar) : VisualizerTopMenuBarApi(owner) {
    override val fileMenuApi by apiField(JMenuApiImpl(owner, internalTopMenuBar.mFileMenu))
    override val workspaceMenuApi by apiField(JMenuApiImpl(owner, internalTopMenuBar.mWorkspMenu))
    override val helpMenuApi by apiField(JMenuApiImpl(owner, internalTopMenuBar.mHelpMenu))

    override fun disableApi() { }
}

private class VisualizerSidebarApiImpl(owner: EOCVSimPlugin, val internalSidebar: SidebarPanel) : VisualizerSidebarApi(owner) {
    val tabs = mutableMapOf<Tab, Int>()

    override val tabChangeHook by apiField(EventHandlerHookApiImpl(owner, internalSidebar.onTabChange))

    override fun addTab(tab: Tab) = apiImpl(tab) {
        // throw if sidebar already has a tab with the same title
        if(internalSidebar.indexOfTab(tab.title) != -1) {
            throw IllegalArgumentException("Sidebar already has a tab with the title '${tab.title}'")
        }

        internalSidebar.addTab(tab.title, object: SidebarPanel.TabJPanel() {
            init {
                tab.create(this)
            }

            override fun onActivated() {
                tab.onActivated()
            }

            override fun onDeactivated() {
                tab.onDeactivated()
            }
        })
        tabs[tab] = internalSidebar.indexOfTab(tab.title)
    }

    override fun removeTab(tab: Tab) = apiImpl(tab) {
        val index = tabs[tab] ?: return@apiImpl
        internalSidebar.removeTabAt(index)
        tabs.remove(tab)
    }

    override fun isActive(tab: Tab) = apiImpl(tab) {
        val index = internalSidebar.indexOfTab(tab.title)
        index != -1 && index == internalSidebar.selectedIndex
    }

    override fun disableApi() {
        tabs.keys.forEach { removeTab(it) }
        tabs.clear()
    }
}