package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.gui.Visualizer
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.VisualizerApi
import javax.swing.JPanel

internal class VisualizerApiImpl(owner: EOCVSimPlugin, val internalVisualizer: Visualizer) : VisualizerApi(owner) {
    override val frame by nullableApiField(lazy = false) { internalVisualizer.frame }

    override val creation by apiField(EventHandlerHookApiImpl(owner, internalVisualizer.onPluginGuiAttachment))

    override val sidebar: Sidebar by apiField(SidebarImpl(owner))

    private class SidebarImpl(owner: EOCVSimPlugin) : Sidebar(owner) {
        override fun newTab(
            title: String,
            contents: JPanel
        ) = TabImpl(
            owner,
            title,
            contents
        )

        override fun removeTab(tab: Tab) {
            TODO("Not yet implemented")
        }

        override fun disable() {}

        private class TabImpl(
            owner: EOCVSimPlugin,
            override val title: String,
            override val contents: JPanel
        ) : Tab(owner) {
            override val isOpen: Boolean
                get() = TODO("Not yet implemented")

            override fun disable() {}
        }
    }

    override fun disable() {}
}