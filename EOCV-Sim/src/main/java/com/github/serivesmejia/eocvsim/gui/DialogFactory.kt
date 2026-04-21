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
import com.github.serivesmejia.eocvsim.gui.dialog.*
import com.github.serivesmejia.eocvsim.gui.dialog.SplashScreen // Explicit import to resolve reference
import com.github.serivesmejia.eocvsim.gui.dialog.iama.IAmA
import com.github.serivesmejia.eocvsim.gui.dialog.iama.IAmAPaperVision
import com.github.serivesmejia.eocvsim.gui.dialog.source.*
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import java.awt.*
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter

object DialogFactory {

    @JvmStatic
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

    @JvmStatic
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

    @JvmStatic
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

    @JvmStatic
    fun createFileChooser(
        parent: Component?,
        mode: FileChooser.Mode?,
        vararg filters: FileFilter?
    ): FileChooser = createFileChooser(parent, mode, "", *filters)

    @JvmStatic
    fun createFileChooser(parent: Component?, vararg filters: FileFilter?): FileChooser =
        createFileChooser(parent, null, "", *filters)

    @JvmStatic
    @JvmOverloads
    fun createSourceDialog(eocvSim: EOCVSim, type: SourceType, initialFile: File? = null) {
        invokeLater {
            when (type) {
                SourceType.IMAGE -> CreateImageSource(eocvSim.visualizer.frame, eocvSim, initialFile)
                SourceType.CAMERA -> CreateCameraSource(eocvSim.visualizer.frame, eocvSim)
                SourceType.VIDEO -> CreateVideoSource(eocvSim.visualizer.frame, eocvSim, initialFile)
                SourceType.HTTP -> CreateHttpSource(eocvSim.visualizer.frame, eocvSim)
                else -> {}
            }
        }
    }

    @JvmStatic
    fun createSourceExDialog(eocvSim: EOCVSim) {
        invokeLater { CreateSourceEx(eocvSim.visualizer.frame, eocvSim.visualizer) }
    }

    @JvmStatic
    fun createConfigDialog(eocvSim: EOCVSim) {
        invokeLater { Configuration(eocvSim.visualizer.frame, eocvSim) }
    }

    @JvmStatic
    fun createAboutDialog(eocvSim: EOCVSim) {
        invokeLater { About(eocvSim.visualizer.frame, eocvSim) }
    }

    @JvmStatic
    @JvmOverloads
    fun createOutput(eocvSim: EOCVSim, wasManuallyOpened: Boolean = false) {
        invokeLater {
            if (!Output.isAlreadyOpened) {
                Output(eocvSim.visualizer.frame, eocvSim, Output.latestIndex, wasManuallyOpened)
            }
        }
    }

    @JvmStatic
    fun createBuildOutput(eocvSim: EOCVSim) {
        invokeLater {
            if (!Output.isAlreadyOpened) {
                Output(eocvSim.visualizer.frame, eocvSim, 1)
            }
        }
    }

    @JvmStatic
    fun createPipelineOutput(eocvSim: EOCVSim) {
        invokeLater {
            if (!Output.isAlreadyOpened) {
                Output(eocvSim.visualizer.frame, eocvSim, 0)
            }
        }
    }

    @JvmStatic
    fun createMavenOutput(manager: PluginManager, onContinue: Runnable?): AppendDelegate {
        val delegate = AppendDelegate()
        invokeLater { PluginOutput(delegate, manager, manager.eocvSim, onContinue ?: Runnable { }) }
        return delegate
    }

    @JvmStatic
    fun createSplashScreen(closeHandler: EventHandler?) {
        invokeLater { SplashScreen(closeHandler) }
    }

    @JvmStatic
    fun createIAmA(visualizer: Visualizer) {
        invokeLater { IAmA(visualizer.frame, visualizer) }
    }

    @JvmStatic
    fun createIAmAPaperVision(visualizer: Visualizer, showWorkspacesButton: Boolean) {
        invokeLater { IAmAPaperVision(visualizer.frame, visualizer, false, showWorkspacesButton) }
    }

    @JvmStatic
    fun createWorkspace(visualizer: Visualizer) {
        invokeLater { CreateWorkspace(visualizer.frame, visualizer) }
    }

    @JvmStatic
    fun createCrashReport(visualizer: Visualizer?, crash: String?) {
        invokeLater { CrashReportOutput(visualizer?.frame, crash ?: "") }
    }

    @JvmStatic
    fun createFileAlreadyExistsDialog(eocvSim: EOCVSim): FileAlreadyExists.UserChoice {
        return FileAlreadyExists(eocvSim.visualizer.frame, eocvSim).run()
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
