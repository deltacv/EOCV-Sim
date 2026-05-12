/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.dialog.component

import com.formdev.flatlaf.FlatLaf
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.Font
import java.io.InputStream
import javax.swing.*
import kotlin.math.roundToInt


class OutputPanel(
    bottomButtonsPanel: BottomButtonsPanel
) : JPanel(GridBagLayout()) {

    val outputArea = JTextArea("")
    val outputScroll = JScrollPane(outputArea)

    companion object {
        val monoFont: Font by lazy {
            Font.createFont(
                Font.TRUETYPE_FONT,
                this::class.java.getResourceAsStream("/fonts/JetBrainsMono-Medium.ttf")
            )
        }
    }

    init {
        if(bottomButtonsPanel is DefaultBottomButtonsPanel) {
            bottomButtonsPanel.outputTextSupplier = { outputArea.text }
        }

        // JTextArea will use /fonts/JetBrainsMono-Medium.ttf as font
        outputArea.font = monoFont.deriveFont(13f)

        outputArea.isEditable    = false
        outputArea.highlighter   = null

        // set the background color to a darker tone
        outputArea.background = if(FlatLaf.isLafDark()) {
            outputArea.background.darker()
        } else {
            java.awt.Color(
                (outputArea.background.red * 0.95).roundToInt(),
                (outputArea.background.green * 0.95).roundToInt(),
                (outputArea.background.blue * 0.95).roundToInt(),
                255
            )
        }

        outputScroll.verticalScrollBarPolicy   = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        outputScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        add(outputScroll, GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            weightx = 0.5
            weighty = 1.0
        })

        bottomButtonsPanel.create(this)

        add(bottomButtonsPanel, GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            gridy = 1

            weightx = 1.0
            ipadx   = 10
            ipady   = 10
        })
    }

    fun resetScroll() {
        outputScroll.verticalScrollBar.value = 0
    }

    open class DefaultBottomButtonsPanel(
        val hasClearButton: Boolean = true,
        override val closeCallback: () -> Unit
    ) : BottomButtonsPanel() {
        val copyButton  = JButton("Copy")
        val clearButton = JButton("Clear")
        val closeButton = JButton("Close")

        var outputTextSupplier: () -> String = { "" }

        override fun create(panel: OutputPanel){
            layout = BoxLayout(this, BoxLayout.LINE_AXIS)

            add(Box.createHorizontalGlue())
            copyButton.addActionListener {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(outputTextSupplier()), null)
            }

            add(copyButton)
            add(Box.createRigidArea(Dimension(4, 0)))

            if(hasClearButton) {
                clearButton.addActionListener { panel.outputArea.text = "" }

                add(clearButton)
                add(Box.createRigidArea(Dimension(4, 0)))
            }

            closeButton.addActionListener { closeCallback() }

            add(closeButton)
            add(Box.createRigidArea(Dimension(4, 0)))
        }

    }

}

abstract class BottomButtonsPanel : JPanel() {
    abstract val closeCallback: () -> Unit

    abstract fun create(panel: OutputPanel)
}

