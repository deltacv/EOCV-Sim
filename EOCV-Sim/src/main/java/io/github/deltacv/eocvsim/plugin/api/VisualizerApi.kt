package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import javax.swing.JFrame
import javax.swing.JPanel

abstract class VisualizerApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val frame: JFrame?
    abstract val creationHook: HookApi

    abstract val topMenuBarApi: VisualizerTopMenuBarApi
    abstract val sidebarApi: VisualizerSidebarApi
    abstract val dialogFactoryApi: DialogFactoryApi
}

abstract class VisualizerTopMenuBarApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val fileMenuApi: JMenuApi
    abstract val workspaceMenuApi: JMenuApi
    abstract val helpMenuApi: JMenuApi
}

abstract class VisualizerSidebarApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val tabChangeHook: HookApi

    abstract fun addTab(tab: Tab)
    abstract fun removeTab(tab: Tab)

    abstract class Tab(owner: EOCVSimPlugin) : Api(owner) {
        abstract val title: String

        abstract fun onActivated()
        abstract fun onDeactivated()

        abstract fun create(target: JPanel)

        override fun disableApi() { }
    }
}