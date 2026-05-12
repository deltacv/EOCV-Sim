/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
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
