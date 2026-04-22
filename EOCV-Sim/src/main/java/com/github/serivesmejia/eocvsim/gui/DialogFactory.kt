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

package com.github.serivesmejia.eocvsim.gui

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.dialog.*
import com.github.serivesmejia.eocvsim.gui.dialog.SplashScreen // Explicit import to resolve reference
import com.github.serivesmejia.eocvsim.gui.dialog.iama.IAmA
import com.github.serivesmejia.eocvsim.gui.dialog.iama.IAmAPaperVision
import com.github.serivesmejia.eocvsim.gui.dialog.source.*
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import kotlinx.coroutines.CoroutineScope
import java.awt.*
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class DialogFactory : KoinComponent {

    val visualizer: Visualizer by inject()
    val pluginManager: PluginManager by inject()
    val configManager: ConfigManager by inject()
    val onRestartRequested: EventHandler by inject(named("onRestartRequested"))
    val scope: CoroutineScope by inject()

    fun createYesOrNo(parent: Component?, message: String, submessage: String, result: (Int) -> Unit) {
        val panel = JPanel()
        val label1 = JLabel(message)
        panel.add(label1)

        if (submessage.isNotBlank()) {
            val label2 = JLabel(submessage)
            panel.add(label2)
            panel.layout = GridLayout(2, 1)
        }

        invokeLater {
            result(
                JOptionPane.showConfirmDialog(
                    parent, panel, "Confirm",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                )
            )
        }
    }

    @JvmOverloads
    fun createInformation(
        parent: Component?,
        message: String,
        submessage: String? = null,
        title: String = "Information",
        cancelText: String? = null,
        onCancel: (() -> Unit)? = null
    ): JDialog {
        val panel = JPanel().apply {
            layout = GridLayout(if (submessage != null) 2 else 1, 1)
            add(JLabel(message))
            if (submessage != null) add(JLabel(submessage))
        }

        val options = if (cancelText != null) arrayOf(cancelText) else emptyArray<String>()
        val pane = JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options)
        val dialog = pane.createDialog(parent, title)

        if (cancelText != null) {
            pane.addPropertyChangeListener { evt ->
                if (evt.propertyName == JOptionPane.VALUE_PROPERTY) {
                    if (pane.value == cancelText) {
                        onCancel?.invoke()
                    }
                }
            }
        }

        dialog.isModal = false

        invokeLater { dialog.isVisible = true }

        return dialog
    }

    fun createFileChooser(
        parent: Component?,
        mode: FileChooser.Mode?,
        initialFileName: String,
        vararg filters: FileFilter?
    ): FileChooser {
        val fileChooser = FileChooser(parent, mode, initialFileName, *filters)
        invokeLater { fileChooser.init() }
        return fileChooser
    }

    fun createFileChooser(
        parent: Component?,
        mode: FileChooser.Mode?,
        vararg filters: FileFilter?
    ): FileChooser = createFileChooser(parent, mode, "", *filters)

    fun createFileChooser(parent: Component?, vararg filters: FileFilter?): FileChooser =
        createFileChooser(parent, null, "", *filters)

    fun createSourceDialog(type: SourceType, initialFile: File? = null) {
        invokeLater {
            when (type) {
                SourceType.IMAGE -> CreateImageSource(initialFile)
                SourceType.CAMERA -> CreateCameraSource()
                SourceType.VIDEO -> CreateVideoSource(initialFile)
                SourceType.HTTP -> CreateHttpSource()
                else -> {}
            }
        }
    }

    fun createSourceExDialog() {
        invokeLater { CreateSourceEx() }
    }

    fun createConfigDialog() {
        invokeLater { Configuration() }
    }

    fun createAboutDialog() {
        invokeLater { About() }
    }

    fun createOutput(wasManuallyOpened: Boolean = false) {
        invokeLater {
            if (!Output.isAlreadyOpened) {
                Output(Output.latestIndex, wasManuallyOpened)
            }
        }
    }

    fun createBuildOutput() {
        invokeLater {
            if (!Output.isAlreadyOpened) {
                Output(1)
            }
        }
    }

    fun createPipelineOutput() {
        invokeLater {
            if (!Output.isAlreadyOpened) {
                Output(0)
            }
        }
    }

    fun createMavenOutput(onContinue: Runnable?): AppendDelegate {
        val delegate = AppendDelegate()
        invokeLater { PluginOutput(delegate, pluginManager, onRestartRequested, configManager, scope, onContinue ?: Runnable { }) }

        return delegate
    }

    fun createSplashScreen(closeHandler: EventHandler?) {
        invokeLater { SplashScreen(closeHandler) }
    }

    fun createIAmA() {
        invokeLater { IAmA() }
    }

    fun createIAmAPaperVision(showWorkspacesButton: Boolean) {
        invokeLater { IAmAPaperVision(false, showWorkspacesButton) }
    }

    fun createWorkspace() {
        invokeLater { CreateWorkspace() }
    }

    fun createCrashReport(crash: String?) {
        invokeLater { CrashReportOutput(visualizer.frame, crash ?: "") }
    }

    fun createFileAlreadyExistsDialog(): FileAlreadyExists.UserChoice {
        return FileAlreadyExists().run()
    }

    private fun invokeLater(runn: Runnable) {
        SwingUtilities.invokeLater(runn)
    }

    class FileChooser @JvmOverloads constructor(
        private val parent: Component?,
        val mode: Mode? = null,
        initialFileName: String = "",
        vararg filters: FileFilter?
    ) {
        private val chooser: JFileChooser = JFileChooser()
        private val closeListeners = ArrayList<FileChooserCloseListener>()

        init {
            val finalMode = mode ?: Mode.FILE_SELECT
            if (finalMode == Mode.DIRECTORY_SELECT) {
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                chooser.isAcceptAllFileFilterUsed = false
            }

            chooser.selectedFile = File(chooser.selectedFile, initialFileName)

            filters.filterNotNull().forEach { chooser.addChoosableFileFilter(it) }
            if (filters.isNotEmpty()) {
                chooser.fileFilter = filters[0]
            }
        }

        fun init() {
            val returnVal = if (mode == Mode.SAVE_FILE_SELECT) {
                chooser.showSaveDialog(parent)
            } else {
                chooser.showOpenDialog(parent)
            }
            executeCloseListeners(returnVal, chooser.selectedFile, chooser.fileFilter)
        }

        fun addCloseListener(listener: FileChooserCloseListener): FileChooser {
            closeListeners.add(listener)
            return this
        }

        private fun executeCloseListeners(OPTION: Int, selectedFile: File?, selectedFileFilter: FileFilter?) {
            for (listener in closeListeners) {
                listener.onClose(OPTION, selectedFile, selectedFileFilter)
            }
        }

        fun close() {
            chooser.isVisible = false
            executeCloseListeners(JFileChooser.CANCEL_OPTION, File(""), FileNameExtensionFilter("", ""))
        }

        enum class Mode { FILE_SELECT, DIRECTORY_SELECT, SAVE_FILE_SELECT }

        fun interface FileChooserCloseListener {
            fun onClose(OPTION: Int, selectedFile: File?, selectedFileFilter: FileFilter?)
        }
    }
}
