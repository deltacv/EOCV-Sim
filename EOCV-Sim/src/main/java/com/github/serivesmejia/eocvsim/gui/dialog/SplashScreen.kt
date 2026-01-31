/*
 * Copyright (c) 2026 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.gui.dialog

import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.Icons
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import java.awt.*
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.SwingUtilities

class SplashScreen(closeHandler: EventHandler? = null) : JDialog() {

    init {
        closeHandler?.once {
            SwingUtilities.invokeLater {
                isVisible = false
                dispose()
            }
        }

        val image = ImagePanel()
        add(image)

        setLocationRelativeTo(null)
        isUndecorated = true
        isAlwaysOnTop = true
        background = Color(0, 0, 0, 0)

        cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)

        pack()

        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val x = (screenSize.width - image.width) / 2
        val y = (screenSize.height - image.height) / 2

        setLocation(x, y)
        isVisible = true
    }

    class ImagePanel : JPanel(GridBagLayout()) {
        val img = EOCVSimIconLibrary.icoEOCVSim

        init {
            isOpaque = false
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.drawImage(img.image, 0, 0, width, height, this)
        }

        override fun getPreferredSize() = Dimension(img.iconWidth / 6, img.iconHeight / 6)
    }

}