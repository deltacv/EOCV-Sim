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

package com.github.serivesmejia.eocvsim.gui.dialog.component

import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.*

class OutputPanel(
    private val bottomButtonsPanel: BottomButtonsPanel
) : JPanel(GridBagLayout()) {

    val outputArea = JTextArea("")

    constructor(closeCallback: () -> Unit) : this(DefaultBottomButtonsPanel(closeCallback))

    init {
        if(bottomButtonsPanel is DefaultBottomButtonsPanel) {
            bottomButtonsPanel.outputTextSupplier = { outputArea.text }
        }

        outputArea.isEditable    = false
        outputArea.highlighter   = null
        outputArea.lineWrap      = true
        outputArea.wrapStyleWord = true

        val outputScroll = JScrollPane(outputArea)
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

    open class DefaultBottomButtonsPanel(
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

            clearButton.addActionListener { panel.outputArea.text = "" }

            add(clearButton)
            add(Box.createRigidArea(Dimension(4, 0)))

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
