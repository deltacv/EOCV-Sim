
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

package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.dialog.Output
import com.github.serivesmejia.eocvsim.gui.util.GuiUtil
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.util.FileFilters
import com.github.serivesmejia.eocvsim.util.exception.handling.CrashReport
import com.github.serivesmejia.eocvsim.workspace.util.VSCodeLauncher
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

class TopMenuBar(visualizer: Visualizer, eocvSim: EOCVSim) : JMenuBar() {

    companion object {
        val docsUrl = URI("https://deltacv.gitbook.io/eocv-sim/")
    }

    @JvmField val mFileMenu   = JMenu("File")
    @JvmField val mWorkspMenu = JMenu("Workspace")
    @JvmField val mEditMenu   = JMenu("Edit")
    @JvmField val mHelpMenu   = JMenu("Help")

    @JvmField val workspCompile = JMenuItem("Build java files")

    init {
        // FILE

        val fileNew = JMenu("New")
        mFileMenu.add(fileNew)

        val fileNewInputSourceSubmenu = JMenu("Input Source")
        fileNew.add(fileNewInputSourceSubmenu)

        //add all input source types to top bar menu
        for (type in SourceType.values()) {
            if (type == SourceType.UNKNOWN) continue //exclude unknown type

            val fileNewInputSourceItem = JMenuItem(type.coolName)

            fileNewInputSourceItem.addActionListener {
                DialogFactory.createSourceDialog(eocvSim, type)
            }

            fileNewInputSourceSubmenu.add(fileNewInputSourceItem)
        }

        val fileSaveMat = JMenuItem("Save current image")

        fileSaveMat.addActionListener {
            GuiUtil.saveMatFileChooser(
                visualizer.frame,
                visualizer.viewport.lastVisualizedMat,
                eocvSim
            )
        }
        mFileMenu.add(fileSaveMat)

        mFileMenu.addSeparator()

        val fileRestart = JMenuItem("Restart")

        fileRestart.addActionListener { eocvSim.onMainUpdate.doOnce { eocvSim.restart() } }
        mFileMenu.add(fileRestart)

        add(mFileMenu)

        //WORKSPACE

        val workspSetWorkspace = JMenuItem("Select workspace")

        workspSetWorkspace.addActionListener { visualizer.selectPipelinesWorkspace() }
        mWorkspMenu.add(workspSetWorkspace)

        workspCompile.addActionListener { visualizer.asyncCompilePipelines() }
        mWorkspMenu.add(workspCompile)

        val workspBuildOutput = JMenuItem("Output")

        workspBuildOutput.addActionListener {
            if(!Output.isAlreadyOpened)
                DialogFactory.createOutput(eocvSim, true)
        }
        mWorkspMenu.add(workspBuildOutput)

        mWorkspMenu.addSeparator()

        val workspVSCode = JMenu("External")

        val workspVSCodeCreate = JMenuItem("Create Gradle workspace")

        workspVSCodeCreate.addActionListener { visualizer.createVSCodeWorkspace() }
        workspVSCode.add(workspVSCodeCreate)

        workspVSCode.addSeparator()

        val workspVSCodeOpen = JMenuItem("Open VS Code here")

        workspVSCodeOpen.addActionListener {
            VSCodeLauncher.asyncLaunch(eocvSim.workspaceManager.workspaceFile)
        }
        workspVSCode.add(workspVSCodeOpen)

        mWorkspMenu.add(workspVSCode)

        add(mWorkspMenu)

        // EDIT

        val editSettings = JMenuItem("Settings")
        editSettings.addActionListener { DialogFactory.createConfigDialog(eocvSim) }

        mEditMenu.add(editSettings)
        add(mEditMenu)

        // HELP

        val helpUsage = JMenuItem("Documentation")
        helpUsage.addActionListener {
            Desktop.getDesktop().browse(docsUrl)
        }

        helpUsage.isEnabled = Desktop.isDesktopSupported()
        mHelpMenu.add(helpUsage)

        mHelpMenu.addSeparator()

        val helpExportLogs = JMenuItem("Export logs")
        helpExportLogs.addActionListener {
            var crashReport: CrashReport

            try {
                throw Exception("Dummy exception, log exported from GUI")
            } catch (e: Exception) {
                crashReport = CrashReport(e, isDummy = true)
            }

            DialogFactory.createFileChooser(visualizer.frame,
                DialogFactory.FileChooser.Mode.SAVE_FILE_SELECT,
                CrashReport.defaultCrashFileName, FileFilters.logFileFilter
            ).addCloseListener { OPTION, selectedFile, _ ->
                    if(OPTION == JFileChooser.APPROVE_OPTION) {
                        var path = selectedFile.absolutePath
                        if (path.endsWith(File.separator))
                            path = path.removeSuffix(File.separator)

                        crashReport.saveCrashReport(File(
                            if(!path.endsWith(".log")) {
                                "$path.log"
                            } else path
                        ))
                    }
                }
        }

        mHelpMenu.add(helpExportLogs)

        val helpAbout = JMenuItem("About")
        helpAbout.addActionListener { DialogFactory.createAboutDialog(eocvSim) }

        mHelpMenu.add(helpAbout)

        add(mHelpMenu)
    }

}
