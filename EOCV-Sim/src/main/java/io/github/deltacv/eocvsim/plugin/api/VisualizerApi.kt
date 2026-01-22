package io.github.deltacv.eocvsim.plugin.api

import com.github.serivesmejia.eocvsim.gui.Visualizer
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import javax.swing.JFrame
import javax.swing.JPanel

abstract class VisualizerApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val frame: JFrame?
    abstract val creation: HookApi

    abstract val sidebar: Sidebar

    abstract class Sidebar(owner: EOCVSimPlugin) : Api(owner) {
        abstract fun newTab(title: String, contents: JPanel): Tab
        abstract fun removeTab(tab: Tab)

        abstract class Tab(owner: EOCVSimPlugin) : Api(owner) {
            abstract val title: String
            abstract val isOpen: Boolean

            abstract val contents: JPanel
        }
    }
}