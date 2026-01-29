/*
 * Copyright (c) 2024 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.awt.Font
import java.awt.LayoutManager
import javax.swing.JPanel
import javax.swing.JTabbedPane

class SidebarPanel(val eocvSim: EOCVSim) : JTabbedPane() {

    private var previousActiveIndex = -1;

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

            // deactivate previous pane first
            val previousPane = if (previousActiveIndex != -1) this.getComponentAt(previousActiveIndex) else null
            if (previousPane is TabJPanel && previousActiveIndex != index) {
                previousPane.isActive = false

                val name = this.getTitleAt(previousActiveIndex)
                logger.info("Deactivating $name")
            }

            // activate current pane
            val currentPane = this.getComponentAt(index)
            if (currentPane is TabJPanel && previousActiveIndex != index) {
                currentPane.isActive = true

                val name = this.getTitleAt(index)
                logger.info("Activating $name")
            }

            previousActiveIndex = index

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