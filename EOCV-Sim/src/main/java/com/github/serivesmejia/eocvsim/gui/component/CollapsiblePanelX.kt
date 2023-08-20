package com.github.serivesmejia.eocvsim.gui.component

import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.LineBorder
import javax.swing.border.TitledBorder

class CollapsiblePanelX(title: String?, titleCol: Color?) : JPanel() {
    private val border: TitledBorder
    private var visibleSize: Dimension? = null
    private var collapsible = true

    var isHidden = false
        private set

    init {
        border = TitledBorder(title)

        border.titleColor = titleCol
        border.border = LineBorder(Color.white)
        setBorder(border)

        // as Titleborder has no access to the Label we fake the size data ;)
        val l = JLabel(title)
        val size = l.getPreferredSize()

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!collapsible) {
                    return
                }

                val i = getBorder().getBorderInsets(this@CollapsiblePanelX)
                if (e.x < i.left + size.width && e.y < i.bottom + size.height) {

                    for(e in components) {
                        e.isVisible = !isHidden

                        border.title = if(isHidden) {
                            "$title (hidden)"
                        } else {
                            title
                        }

                        isHidden = !isHidden
                    }

                    revalidate()
                    e.consume()
                }
            }
        })
    }

    fun setCollapsible(collapsible: Boolean) {
        this.collapsible = collapsible
    }

    fun setTitle(title: String?) {
        border.title = title
    }
}