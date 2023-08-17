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

import com.github.serivesmejia.eocvsim.gui.util.Location
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import java.awt.Window
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.*

class PopupX @JvmOverloads constructor(windowAncestor: Window,
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
        window.addWindowFocusListener(this)
        window.isVisible = true

        //fixes position since our panel dimensions
        //aren't known until it's set visible (above)
        if(fixX) x -= panel.width / 4
        if(fixY) y -= panel.height
        setLocation(x, y)

        onShow.run()
    }

    override fun hide() {
        if(!window.isVisible) return

        window.removeWindowFocusListener(this)
        window.isVisible = false
        onHide.run()
    }

    override fun windowGainedFocus(e: WindowEvent?) {}

    override fun windowLostFocus(e: WindowEvent?) {
        if(closeOnFocusLost) {
            hide()
        }
    }

    fun setLocation(width: Int, height: Int) = window.setLocation(width, height)

    companion object {

        fun JComponent.popUpXOnThis(panel: JPanel,
                                    popupLocation: Location = Location.TOP,
                                    closeOnFocusLost: Boolean = true,
                                    fixX: Boolean = false,
                                    fixY: Boolean = true): PopupX {

            val frame = SwingUtilities.getWindowAncestor(this)
            val location = locationOnScreen

            val popup = PopupX(frame, panel, location.x,
                    if(popupLocation == Location.TOP) location.y else location.y + height,
                    closeOnFocusLost, fixX, fixY
            )

            popup.onShow {
                popup.setLocation(
                        popup.window.location.x - width / 3,
                        if(popupLocation == Location.TOP) popup.window.location.y else popup.window.location.y + popup.window.height
                )

                val topRightPointX = popup.window.location.x + popup.window.width

                if(topRightPointX > frame.width) {
                    popup.setLocation(
                            popup.window.location.x - ((topRightPointX - frame.width) / 2),
                            popup.window.location.y
                    )
                }
            }

            return popup
        }

    }

}