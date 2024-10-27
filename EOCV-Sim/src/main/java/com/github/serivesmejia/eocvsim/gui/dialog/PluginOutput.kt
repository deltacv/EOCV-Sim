/*
 * Copyright (c) 2024 Sebastian Erives
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

import com.github.serivesmejia.eocvsim.gui.dialog.component.BottomButtonsPanel
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.*

class AppendDelegate {
    private val appendables = mutableListOf<Appendable>()

    @Synchronized
    fun subscribe(appendable: Appendable) {
        appendables.add(appendable)
    }

    fun subscribe(appendable: (String) -> Unit) {
        appendables.add(object : Appendable {
            override fun append(csq: CharSequence?): java.lang.Appendable? {
                appendable(csq.toString())
                return this
            }

            override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable? {
                appendable(csq.toString().substring(start, end))
                return this
            }

            override fun append(c: Char): java.lang.Appendable? {
                appendable(c.toString())
                return this
            }
        })
    }

    @Synchronized
    fun append(text: String) {
        appendables.forEach { it.append(text) }
    }

    @Synchronized
    fun appendln(text: String) {
        appendables.forEach { it.appendLine(text) }
    }
}

class PluginOutput(
    appendDelegate: AppendDelegate,
    val onContinue: Runnable
) : Appendable {

    companion object {
        private const val SPECIAL = "13mck"

        const val SPECIAL_OPEN = "$SPECIAL[OPEN]"
        const val SPECIAL_CLOSE = "$SPECIAL[CLOSE]"
        const val SPECIAL_CONTINUE = "$SPECIAL[CONTINUE]"
        const val SPECIAL_FREE = "$SPECIAL[FREE]"
        const val SPECIAL_SILENT = "$SPECIAL[SILENT]"

        fun String.trimSpecials(): String {
            return this
                .replace(SPECIAL_OPEN, "")
                .replace(SPECIAL_CLOSE, "")
                .replace(SPECIAL_CONTINUE, "")
                .replace(SPECIAL_SILENT, "")
                .replace(SPECIAL_FREE, "")
        }
    }

    private val output = JDialog()
    private val tabbedPane: JTabbedPane

    private val mavenBottomButtonsPanel: MavenOutputBottomButtonsPanel = MavenOutputBottomButtonsPanel(::close) {
        mavenOutputPanel.outputArea.text
    }

    private val mavenOutputPanel = OutputPanel(mavenBottomButtonsPanel)

    init {
        output.isModal = true
        output.isAlwaysOnTop = true
        output.title = "Plugin Manager"

        tabbedPane = JTabbedPane()

        output.add(tabbedPane.apply {
            addTab("Plugins", JPanel().apply {
                layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

                add(JLabel("Plugins output will be shown here"))
            })
            addTab("Output", mavenOutputPanel)
        })

        registerListeners()

        output.pack()
        output.setSize(500, 350)

        appendDelegate.subscribe(this)

        output.setLocationRelativeTo(null)
        output.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun registerListeners() = GlobalScope.launch(Dispatchers.Swing) {
        mavenBottomButtonsPanel.continueButton.addActionListener {
            close()
            onContinue.run()

            output.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            mavenBottomButtonsPanel.continueButton.isEnabled = false
            mavenBottomButtonsPanel.closeButton.isEnabled = true
        }
    }

    fun close() {
        output.isVisible = false
    }

    private fun handleSpecials(text: String): Boolean {
        when(text) {
            SPECIAL_FREE -> {
                mavenBottomButtonsPanel.continueButton.isEnabled = false
                mavenBottomButtonsPanel.closeButton.isEnabled = true
            }
            SPECIAL_CLOSE -> close()
            SPECIAL_CONTINUE -> {
                mavenBottomButtonsPanel.continueButton.isEnabled = true
                mavenBottomButtonsPanel.closeButton.isEnabled = false
            }
        }

        if(!text.startsWith(SPECIAL_SILENT) && text != SPECIAL_CLOSE && text != SPECIAL_FREE) {
            SwingUtilities.invokeLater {
                SwingUtilities.invokeLater {
                    tabbedPane.selectedIndex = tabbedPane.indexOfTab("Output") // focus on output tab
                }
                output.isVisible = true
            }
        }

        return text == SPECIAL_OPEN || text == SPECIAL_CLOSE || text == SPECIAL_CONTINUE || text == SPECIAL_FREE
    }

    override fun append(csq: CharSequence?): java.lang.Appendable? {
        val text = csq.toString()

        SwingUtilities.invokeLater {
            if(handleSpecials(text)) return@invokeLater

            mavenOutputPanel.outputArea.text += text.trimSpecials()
            mavenOutputPanel.outputArea.revalidate()
            mavenOutputPanel.outputArea.repaint()
        }
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable? {
        val text = csq.toString().substring(start, end)

        SwingUtilities.invokeLater {
            if(handleSpecials(text)) return@invokeLater

            mavenOutputPanel.outputArea.text += text.trimSpecials()
            mavenOutputPanel.outputArea.revalidate()
            mavenOutputPanel.outputArea.repaint()
        }
        return this
    }

    override fun append(c: Char): java.lang.Appendable? {
        SwingUtilities.invokeLater {
            mavenOutputPanel.outputArea.text += c
            mavenOutputPanel.outputArea.revalidate()
            mavenOutputPanel.outputArea.repaint()
        }

        return this
    }

    class MavenOutputBottomButtonsPanel(
        override val closeCallback: () -> Unit,
        val outputTextSupplier: () -> String
    ) : BottomButtonsPanel() {

        val copyButton  = JButton("Copy")
        val clearButton = JButton("Clear")
        val continueButton = JButton("Continue")
        val closeButton = JButton("Close")

        override fun create(panel: OutputPanel) {
            layout = BoxLayout(this, BoxLayout.LINE_AXIS)

            add(Box.createRigidArea(Dimension(4, 0)))

            copyButton.addActionListener {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(outputTextSupplier()), null)
            }

            add(copyButton)
            add(Box.createRigidArea(Dimension(4, 0)))

            clearButton.addActionListener { panel.outputArea.text = "" }

            add(clearButton)

            add(Box.createHorizontalGlue())

            add(continueButton)
            continueButton.isEnabled = false

            add(Box.createRigidArea(Dimension(4, 0)))

            add(closeButton)
            closeButton.addActionListener { closeCallback() }

            add(Box.createRigidArea(Dimension(4, 0)))
        }
    }
}
