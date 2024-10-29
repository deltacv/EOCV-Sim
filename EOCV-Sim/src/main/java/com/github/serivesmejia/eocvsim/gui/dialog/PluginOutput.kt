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

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.dialog.component.BottomButtonsPanel
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import io.github.deltacv.eocvsim.plugin.loader.PluginSource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

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
    val pluginManager: PluginManager,
    val eocvSim: EOCVSim? = null,
    val onContinue: Runnable
) : Appendable {

    companion object {
        const val SPECIAL = "[13mck]"

        const val SPECIAL_OPEN = "$SPECIAL[OPEN]"
        const val SPECIAL_OPEN_MGR = "$SPECIAL[OPEN_MGR]"
        const val SPECIAL_CLOSE = "$SPECIAL[CLOSE]"
        const val SPECIAL_CONTINUE = "$SPECIAL[CONTINUE]"
        const val SPECIAL_FREE = "$SPECIAL[FREE]"
        const val SPECIAL_SILENT = "$SPECIAL[SILENT]"

        fun String.trimSpecials(): String {
            return this
                .replace(SPECIAL_OPEN, "")
                .replace(SPECIAL_OPEN_MGR, "")
                .replace(SPECIAL_CLOSE, "")
                .replace(SPECIAL_CONTINUE, "")
                .replace(SPECIAL_SILENT, "")
                .replace(SPECIAL_FREE, "")
        }
    }

    private val output = JDialog()
    private val tabbedPane: JTabbedPane

    private var shouldAskForRestart = false

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
            addTab("Plugins", makePluginManagerPanel())
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
        output.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                if(mavenBottomButtonsPanel.continueButton.isEnabled) {
                    mavenBottomButtonsPanel.continueButton.doClick()
                }

                checkShouldAskForRestart()
            }
        })

        mavenBottomButtonsPanel.continueButton.addActionListener {
            close()
            onContinue.run()
            mavenBottomButtonsPanel.continueButton.isEnabled = false
            mavenBottomButtonsPanel.closeButton.isEnabled = true
        }

        tabbedPane.addChangeListener(object: ChangeListener {
            override fun stateChanged(e: ChangeEvent?) {
                // remake plugin manager panel
                if(tabbedPane.selectedIndex == tabbedPane.indexOfTab("Plugins")) {
                    tabbedPane.setComponentAt(0, makePluginManagerPanel())
                }
            }
        })
    }

    private fun checkShouldAskForRestart() {
        if(shouldAskForRestart && eocvSim != null) {
            val dialogResult = JOptionPane.showConfirmDialog(
                output,
                "You need to restart the application to apply the changes. Do you want to restart now?",
                "Restart required",
                JOptionPane.YES_NO_OPTION
            )

            if(dialogResult == JOptionPane.YES_OPTION) {
                eocvSim.restart()
            }

            shouldAskForRestart = false
        }
    }

    private fun makePluginManagerPanel(): JPanel {
        val panel = JPanel()
        panel.layout = GridBagLayout()

        if(pluginManager.loaders.isEmpty()) {
            // center vertically and horizontally
            val noPluginsLabel = JLabel("<html><h1>Nothing to see here...</h1><html>")
            noPluginsLabel.horizontalAlignment = SwingConstants.CENTER
            noPluginsLabel.verticalAlignment = SwingConstants.CENTER

            // Use GridBagConstraints to center the label
            val constraints = GridBagConstraints().apply {
                gridx = 0 // Center horizontally
                gridy = 0 // Center vertically
                weightx = 1.0 // Take up the entire horizontal space
                weighty = 1.0 // Take up the entire vertical space
                anchor = GridBagConstraints.CENTER // Center alignment
            }

            // Add the label to the panel with the constraints
            panel.add(noPluginsLabel, constraints)
        } else {
            val tabbedPane = JTabbedPane(JTabbedPane.LEFT)

            for((_, loader) in pluginManager.loaders) {
                val pluginPanel = JPanel()
                pluginPanel.layout = GridBagLayout()

                val pluginNameLabel = JLabel(
                    "<html><h1>${loader.pluginName} v${loader.pluginVersion} by ${loader.pluginAuthor}</h1></html>",
                    SwingConstants.CENTER
                )

                pluginPanel.add(pluginNameLabel, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.CENTER
                })

                val signatureStatus = if(loader.signature.verified)
                    "This plugin has been verified and was signed by ${loader.signature.authority!!.name}."
                else "This plugin is not signed."

                val authorEmail = if(loader.pluginAuthorEmail.isBlank())
                    "The author did not provide contact information."
                else "Contact the author at ${loader.pluginAuthorEmail}"

                val source = if(loader.pluginSource == PluginSource.REPOSITORY)
                    "Maven repository"
                else "local file"

                val sourceEnabled = if(loader.shouldEnable) "It was LOADED from a $source." else "It is DISABLED, it comes from a $source."

                val superAccess = if(loader.hasSuperAccess)
                    "It has super access."
                else "It does not have super access."

                val wantsSuperAccess = if(loader.pluginToml.getBoolean("super-access", false))
                    "It requests super access in its manifest."
                else "It does not request super access in its manifest."

                val description = if(loader.pluginDescription.isBlank())
                    "No description available."
                else loader.pluginDescription

                val font = pluginNameLabel.font.deriveFont(13.0f)

                // add a text area for the plugin description
                val pluginDescriptionArea = JTextArea("""
                    $signatureStatus
                    $authorEmail
                    
                    ** $sourceEnabled $superAccess $wantsSuperAccess **
                    
                    $description
                """.trimIndent())

                pluginDescriptionArea.font = font
                pluginDescriptionArea.isEditable = false
                pluginDescriptionArea.lineWrap = true
                pluginDescriptionArea.wrapStyleWord = true
                pluginDescriptionArea.background = pluginPanel.background

                pluginPanel.add(JScrollPane(pluginDescriptionArea), GridBagConstraints().apply {
                    gridx = 0
                    gridy = 1
                    weightx = 1.0
                    weighty = 1.0
                    fill = GridBagConstraints.BOTH
                })

                val restartWarnLabel = JLabel("Restart to apply changes.")
                restartWarnLabel.isVisible = false

                val disableButton = JButton("Disable")
                val enableButton = JButton("Enable")

                fun refreshButtons() {
                    disableButton.isEnabled = loader.shouldEnable
                    enableButton.isEnabled = !loader.shouldEnable

                }

                refreshButtons()

                enableButton.addActionListener {
                    loader.shouldEnable = true
                    restartWarnLabel.isVisible = true
                    shouldAskForRestart = true

                    refreshButtons()
                }
                disableButton.addActionListener {
                    loader.shouldEnable = false
                    restartWarnLabel.isVisible = true
                    shouldAskForRestart = true

                    refreshButtons()
                }

                // center buttons
                val buttonsPanel = JPanel()
                buttonsPanel.border = BorderFactory.createEmptyBorder(10, 0, 0, 0)

                buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS)

                buttonsPanel.add(restartWarnLabel)
                buttonsPanel.add(Box.createHorizontalGlue())
                buttonsPanel.add(disableButton)
                buttonsPanel.add(Box.createRigidArea(Dimension(5, 0)))
                buttonsPanel.add(enableButton)

                pluginPanel.add(buttonsPanel, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 2
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.CENTER
                })

                pluginPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

                tabbedPane.addTab(loader.pluginName, pluginPanel)
            }

            panel.add(tabbedPane, GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            })
        }

        return panel
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
                    if(text == SPECIAL_OPEN_MGR) {
                        tabbedPane.selectedIndex = tabbedPane.indexOfTab("Plugins") // focus on plugins tab
                    } else {
                        tabbedPane.selectedIndex = tabbedPane.indexOfTab("Output") // focus on output tab
                    }
                }

                tabbedPane.setComponentAt(0, makePluginManagerPanel())
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
