/*
 * Copyright (c) 2023 Sebastian Erives
 * Credit where it's due - based off of https://stackoverflow.com/a/52956783
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

import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.LineBorder
import javax.swing.border.TitledBorder

class CollapsiblePanelX @JvmOverloads constructor(
        title: String?,
        titleCol: Color?,
        borderCol: Color? = Color.white
) : JPanel() {
    private val border: TitledBorder
    private var collapsible = true

    var isHidden = false
        private set

    init {
        val titleAndDescriptor = if(isHidden) {
            "$title (click here to expand)"
        } else {
            "$title (click here to hide)"
        }

        border = TitledBorder(titleAndDescriptor)

        border.titleColor = titleCol
        border.border = LineBorder(borderCol)

        setBorder(border)

        // as Titleborder has no access to the Label we fake the size data ;)
        val l = JLabel(titleAndDescriptor)
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
                            "$title (click here to expand)"
                        } else {
                            "$title (click here to hide)"
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