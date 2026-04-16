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

import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

class CollapsiblePanelX @JvmOverloads constructor(
        var titleText: String?,
        titleCol: Color?,
        borderCol: Color? = Color.white
) : JPanel(BorderLayout()) {
    private var collapsible = true

    var isHidden = false
        private set

    val contentPanel = JPanel()

    private val toggleButton = JButton("Hide $titleText").apply {
        isFocusable = false
        if (titleCol != null) foreground = titleCol
        
        addActionListener {
            if (!collapsible) return@addActionListener
            isHidden = !isHidden
            contentPanel.isVisible = !isHidden
            text = if (isHidden) "Show $titleText" else "Hide $titleText"
            this@CollapsiblePanelX.revalidate()
        }
    }

    init {
        border = BorderFactory.createMatteBorder(1, 1, 1, 1, borderCol)

        val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            border = BorderFactory.createEmptyBorder(2, 2, 2, 5)
            add(toggleButton)
        }

        super.add(headerPanel, BorderLayout.NORTH)
        super.add(contentPanel, BorderLayout.CENTER)
    }

    fun setCollapsible(collapsible: Boolean) {
        this.collapsible = collapsible
        toggleButton.isVisible = collapsible
    }

    fun setTitle(title: String?) {
        titleText = title
        toggleButton.text = if (isHidden) "Show $titleText" else "Hide $titleText"
    }
}