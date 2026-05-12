/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer


import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.common.util.loggerForThis
import java.awt.Font
import java.awt.LayoutManager
import javax.swing.JPanel
import javax.swing.JTabbedPane

import org.koin.core.component.KoinComponent

class SidebarPanel : JTabbedPane(), KoinComponent {

    private var previousActiveIndex = -1

    val onTabChange = EventHandler("SidebarPanel-OnTabChange")

    private val logger by loggerForThis()

    init {
        font = font.deriveFont(Font.PLAIN, 14f)

        addContainerListener(object : java.awt.event.ContainerAdapter() {
            override fun componentAdded(e: java.awt.event.ContainerEvent?) {
                // Activate the first tab added by default
                if (componentCount == 1) {
                    val firstPane = getComponentAt(0)
                    if (firstPane is TabJPanel) {
                        firstPane.isActive = true
                        previousActiveIndex = 0
                    }
                }
            }
        })

        addChangeListener {
            val index = this.selectedIndex
            val currentIndexValid = index in 0 until componentCount
            val previousIndexValid = previousActiveIndex in 0 until componentCount

            // deactivate previous pane first
            val previousPane = if (previousIndexValid) this.getComponentAt(previousActiveIndex) else null

            if (previousPane is TabJPanel && previousActiveIndex != index) {
                previousPane.isActive = false

                val name = this.getTitleAt(previousActiveIndex)
                logger.info("Deactivating $name")
            }

            // activate current pane
            val currentPane = if (currentIndexValid) this.getComponentAt(index) else null
            if (currentPane is TabJPanel && previousActiveIndex != index) {
                currentPane.isActive = true

                val name = this.getTitleAt(index)
                logger.info("Activating $name")
            }

            previousActiveIndex = if (currentIndexValid) index else -1

            onTabChange.run()
        }
    }

    abstract class TabJPanel() : JPanel() {
        constructor(layout: LayoutManager) : this() {
            this.layout = layout
        }

        var isActive: Boolean = false
            set(value) {
                if (field != value) {
                    field = value

                    if (value) {
                        onActivated()
                    } else {
                        onDeactivated()
                    }
                }
            }

        abstract fun onActivated()
        abstract fun onDeactivated()
    }
}
