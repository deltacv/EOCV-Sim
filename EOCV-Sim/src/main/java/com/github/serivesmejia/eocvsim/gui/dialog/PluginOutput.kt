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

import com.github.serivesmejia.eocvsim.LifecycleSignal
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.dialog.component.BottomButtonsPanel
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel
import com.github.serivesmejia.eocvsim.plugin.output.PluginDialogSignal
import com.github.serivesmejia.eocvsim.plugin.output.PluginOutputHandler
import com.github.serivesmejia.eocvsim.plugin.output.VisualPluginOutputHandler
import io.github.deltacv.eocvsim.plugin.loader.FilePluginLoaderImpl
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import io.github.deltacv.eocvsim.plugin.loader.PluginSource
import io.github.deltacv.eocvsim.plugin.repository.PluginRepositoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.*

class PluginOutput(
    val outputHandler: PluginOutputHandler,
    val pluginManager: PluginManager,
    val configManager: ConfigManager,
    val scope: CoroutineScope
) : Appendable, KoinComponent {

    private val lifecycleChannel: Channel<LifecycleSignal> by inject(named("lifecycle"))

    private val output = JDialog()
    private val tabbedPane: JTabbedPane

    private var shouldAskForRestart = false

    private val mavenBottomButtonsPanel: MavenOutputBottomButtonsPanel = MavenOutputBottomButtonsPanel(::close) {
        mavenOutputPanel.outputArea.text
    }

    private val mavenOutputPanel = OutputPanel(mavenBottomButtonsPanel)

    init {
        output.isModal = false
        output.isAlwaysOnTop = true
        output.title = "Plugin Manager"

        tabbedPane = JTabbedPane()

        output.add(tabbedPane.apply {
            addTab("Plugins", makePluginManagerPanel())
            addTab("Output", mavenOutputPanel)
        })

        registerListeners()

        output.pack()
        output.setSize(500, 365)

        // Subscribe to output messages
        outputHandler.onOutput.attachPayload { message ->
            SwingUtilities.invokeLater {
                mavenOutputPanel.outputArea.text += message
                mavenOutputPanel.outputArea.revalidate()
                mavenOutputPanel.outputArea.repaint()
            }
        }

        // Subscribe to dialog signals
        outputHandler.onDialogSignal.attachPayload { signal ->
            SwingUtilities.invokeLater {
                when (signal) {
                    PluginDialogSignal.ShowOutput -> {
                        output.isVisible = true
                        tabbedPane.selectedIndex = tabbedPane.indexOfTab("Output")
                    }
                    PluginDialogSignal.ShowPlugins -> {
                        output.isVisible = true
                        tabbedPane.selectedIndex = tabbedPane.indexOfTab("Plugins")
                        tabbedPane.setComponentAt(0, makePluginManagerPanel())
                    }
                    PluginDialogSignal.Hide -> {
                        output.isVisible = false
                    }
                    PluginDialogSignal.EnableContinue -> {
                        mavenBottomButtonsPanel.continueButton.isEnabled = true
                        mavenBottomButtonsPanel.closeButton.isEnabled = false
                    }
                    PluginDialogSignal.DisableContinue -> {
                        mavenBottomButtonsPanel.continueButton.isEnabled = false
                        mavenBottomButtonsPanel.closeButton.isEnabled = true
                    }
                }
            }
        }

        output.setLocationRelativeTo(null)
        output.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
    }

    private fun registerListeners() {
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
            // Signal the outputHandler that user clicked Continue
            if (outputHandler is VisualPluginOutputHandler) {
                outputHandler.signalContinuation()
            }
            mavenBottomButtonsPanel.continueButton.isEnabled = false
            mavenBottomButtonsPanel.closeButton.isEnabled = true
        }

        tabbedPane.addChangeListener { // remake plugin manager panel
            if (tabbedPane.selectedIndex == tabbedPane.indexOfTab("Plugins")) {
                tabbedPane.setComponentAt(0, makePluginManagerPanel())
            }
        }
    }

    private fun checkShouldAskForRestart() {
        if(shouldAskForRestart) {
            val dialogResult = JOptionPane.showConfirmDialog(
                output,
                "You need to restart the application to apply the changes. Do you want to restart now?",
                "Restart required",
                JOptionPane.YES_NO_OPTION
            )
 
            if(dialogResult == JOptionPane.YES_OPTION) {
                output.isVisible = false
                lifecycleChannel.trySend(LifecycleSignal.Restart)
            }
 
            shouldAskForRestart = false
        }
    }

    private fun makePluginManagerPanel(): JPanel {
        val pluginsPanel = JPanel()
        pluginsPanel.layout = GridBagLayout()

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
            pluginsPanel.add(noPluginsLabel, constraints)
        } else {
            val tabbedPane = JTabbedPane(JTabbedPane.LEFT)

            for(loader in pluginManager.loaders) {
                val pluginPanel = JPanel()
                pluginPanel.layout = GridBagLayout()

                val pluginNameLabel = JLabel(
                    "<html><h1>${loader.pluginInfo.nameWithVersionAndAuthor}</h1></html>",
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

                val authorEmail = if(loader.pluginInfo.authorEmail.isBlank())
                    "The author did not provide contact information."
                else "Contact the author at ${loader.pluginInfo.authorEmail}"

                val source = when(loader.pluginSource) {
                    PluginSource.REPOSITORY -> "from a Maven repository"
                    PluginSource.FILE -> "from a local file"
                    PluginSource.EMBEDDED -> "as an embedded plugin"
                }

                val sourceEnabled = if(loader.shouldEnable) "It was loaded $source." else "It is disabled, it comes $source."

                val superAccess = if(loader.hasSuperAccess)
                    "It has super access."
                else "It does not have super access."

                val wantsSuperAccess = when(loader) {
                    is FilePluginLoaderImpl -> {
                        if(loader.pluginToml.getBoolean("super-access", false))
                            "It requests super access in its manifest."
                        else ""
                    }
                    else -> ""
                }

                val description = loader.pluginInfo.description.ifBlank { "No description available." }

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

                tabbedPane.addTab(loader.pluginInfo.name, pluginPanel)
            }

            pluginsPanel.add(tabbedPane, GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            })
        }

        val panel = JPanel()

        panel.layout = GridBagLayout()
        panel.add(pluginsPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
        })

        val bottomButtonsPanel = JPanel()
        bottomButtonsPanel.layout = BoxLayout(bottomButtonsPanel, BoxLayout.PAGE_AXIS)

        val openPluginsFolderButton = JButton("Open plugins folder")

        openPluginsFolderButton.addActionListener {
            val pluginsFolder = PluginManager.PLUGIN_FOLDER

            if(pluginsFolder.exists() && Desktop.isDesktopSupported()) {
                output.isVisible = false
                Desktop.getDesktop().open(pluginsFolder)
            } else {
                JOptionPane.showMessageDialog(
                    output,
                    "Unable to open plugins folder, the folder does not exist or the operation is unsupported.",
                    "Operation failed",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        val startFreshButton = JButton("Start fresh")

        startFreshButton.addActionListener {
            val dialogResult = JOptionPane.showConfirmDialog(
                output,
                "Are you sure you want to start fresh? This will remove all plugins from all sources.",
                "Start fresh",
                JOptionPane.YES_NO_OPTION
            )

            if(dialogResult == JOptionPane.YES_OPTION) {
                configManager.config.flags["startFresh"] = true
 
                PluginRepositoryManager.REPOSITORY_FILE.delete()
                PluginRepositoryManager.CACHE_FILE.delete()
 
                shouldAskForRestart = true
                checkShouldAskForRestart()
            }

        }

        val closeButton = JButton("Close")

        closeButton.addActionListener {
            close()
        }

        val buttonsPanel = JPanel()
        buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS)
        buttonsPanel.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)

        buttonsPanel.add(Box.createHorizontalGlue())

        buttonsPanel.add(openPluginsFolderButton)
        buttonsPanel.add(Box.createRigidArea(Dimension(5, 0)))
        buttonsPanel.add(startFreshButton)
        buttonsPanel.add(Box.createRigidArea(Dimension(5, 0)))
        buttonsPanel.add(closeButton)

        buttonsPanel.add(Box.createHorizontalGlue())

        bottomButtonsPanel.add(buttonsPanel)

        // Set a thin light gray line border with padding
        bottomButtonsPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10),       // Padding inside the border
            JScrollPane().border
        )

        panel.add(bottomButtonsPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 2
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        })

        return panel
    }

    fun close() {
        output.isVisible = false
    }

    override fun append(csq: CharSequence?): java.lang.Appendable {
        // ...existing code...
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable {
        // ...existing code...
        return this
    }

    override fun append(c: Char): java.lang.Appendable {
        // ...existing code...
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