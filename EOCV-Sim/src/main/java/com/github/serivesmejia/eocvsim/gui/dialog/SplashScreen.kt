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
        if(closeHandler != null) {
            closeHandler {
                SwingUtilities.invokeLater {
                    isVisible = false
                    dispose()
                }
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