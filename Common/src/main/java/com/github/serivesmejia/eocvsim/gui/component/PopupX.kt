/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component

import com.github.serivesmejia.eocvsim.gui.util.Corner
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import java.awt.AWTEvent
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

class PopupX @JvmOverloads constructor(private val windowAncestor: Window,
                                       private val panel: JPanel,
                                       private var x: Int,
                                       private var y: Int,
                                       var closeOnFocusLost: Boolean = true,
                                       private val fixX: Boolean = false,
                                       private val fixY: Boolean = true) : Popup() {

    val window = JWindow(windowAncestor)

    val ancestorClickListener = object: MouseListener {
        override fun mouseClicked(e: MouseEvent?) {
        }
        override fun mousePressed(e: MouseEvent?) {
        }
        override fun mouseReleased(e: MouseEvent?) {
            if(closeOnFocusLost) {
                hide()
            }
        }
        override fun mouseEntered(e: MouseEvent?) {
        }
        override fun mouseExited(e: MouseEvent?) {
        }
    }

    @JvmField val onShow = EventHandler("PopupX-OnShow")
    @JvmField val onHide = EventHandler("PopupX-OnHide")

    init {
        window.isFocusable = true
        window.setLocation(x, y)
        window.contentPane = panel

        panel.border = JPopupMenu().border

        window.size = panel.preferredSize

        windowAncestor.addKeyListener(object: KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if(e?.keyCode == KeyEvent.VK_ESCAPE) {
                    hide()
                    windowAncestor.removeKeyListener(this)
                }
            }
        })
    }

    override fun show() {
        window.isVisible = true

        SwingUtilities.invokeLater {
            Toolkit.getDefaultToolkit().addAWTEventListener({
                if(it is MouseEvent && it.id == MouseEvent.MOUSE_CLICKED) {
                    if(it.source != panel && it.source != window && closeOnFocusLost) {
                        hide()
                    }
                }
            }, AWTEvent.MOUSE_EVENT_MASK)
        }

        //fixes position since our panel dimensions
        //aren't known until it's set visible (above)
        if(fixX) x -= panel.width / 4
        if(fixY) y -= panel.height
        setLocation(x, y)

        onShow.run()
    }

    override fun hide() {
        if(!window.isVisible) return

        window.isVisible = false
        onHide.run()
    }

    fun setLocation(width: Int, height: Int) = window.setLocation(width, height)

    companion object {

        fun JComponent.popUpXOnThis(
                panel: JPanel,
                buttonCorner: Corner = Corner.TOP_LEFT,
                popupCorner: Corner = Corner.BOTTOM_LEFT,
                closeOnFocusLost: Boolean = true
        ): PopupX {

            val frame = SwingUtilities.getWindowAncestor(this)
            val location = locationOnScreen

            val cornerLocation: Point = when(buttonCorner) {
                Corner.TOP_LEFT -> Point(location.x, location.y)
                Corner.TOP_RIGHT -> Point(location.x + width, location.y)
                Corner.BOTTOM_LEFT -> Point(location.x, location.y + height)
                Corner.BOTTOM_RIGHT -> Point(location.x + width, location.y + height)
            }

            val popup = PopupX(frame, panel,
                    cornerLocation.x,
                    cornerLocation.y,
                    closeOnFocusLost
            )

            popup.onShow {
                when(popupCorner) {
                    Corner.TOP_LEFT -> popup.setLocation(
                            popup.window.location.x,
                            popup.window.location.y + popup.window.height
                    )
                    Corner.TOP_RIGHT -> popup.setLocation(
                            popup.window.location.x - popup.window.width,
                            popup.window.location.y + popup.window.height
                    )
                    Corner.BOTTOM_LEFT -> popup.setLocation(
                            popup.window.location.x + width,
                            popup.window.location.y + popup.window.height
                    )
                    Corner.BOTTOM_RIGHT -> popup.setLocation(
                            popup.window.location.x - popup.window.width,
                            popup.window.location.y + popup.window.height
                    )
                }
            }

            return popup
        }

    }

}
