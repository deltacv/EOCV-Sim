package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import javax.swing.JFrame
import javax.swing.JPanel

abstract class VisualizerApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val frame: JFrame?
    abstract val creationHook: HookApi

    abstract val sidebar: Sidebar

    abstract class Sidebar(owner: EOCVSimPlugin) : Api(owner) {
        abstract fun addTab(tab: Tab)
        abstract fun removeTab(tab: Tab)

        abstract class Tab(owner: EOCVSimPlugin) {
            abstract val title: String

            abstract fun onActivated()
            abstract fun onDeactivated()

            abstract val contents: JPanel
        }
    }
}