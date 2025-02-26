/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.gui.component

import com.github.serivesmejia.eocvsim.gui.util.Corner
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import java.awt.Point
import java.awt.Window
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.*

class PopupX @JvmOverloads constructor(private val windowAncestor: Window,
                                       private val panel: JPanel,
                                       private var x: Int,
                                       private var y: Int,
                                       var closeOnFocusLost: Boolean = true,
                                       private val fixX: Boolean = false,
                                       private val fixY: Boolean = true) : Popup(), WindowFocusListener {

    val window = JWindow(windowAncestor)

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
        windowAncestor.addWindowFocusListener(this)

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

    override fun windowGainedFocus(e: WindowEvent?) {
        if(closeOnFocusLost) {
            hide()
        }
    }

    override fun windowLostFocus(e: WindowEvent?) {
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