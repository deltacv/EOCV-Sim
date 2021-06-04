
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
import com.github.serivesmejia.eocvsim.gui.dialog.BuildOutput
import com.github.serivesmejia.eocvsim.gui.util.GuiUtil
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.workspace.util.VSCodeLauncher
import java.awt.Desktop
import java.net.URI
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

class TopMenuBar(visualizer: Visualizer, eocvSim: EOCVSim) : JMenuBar() {

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
            if(!BuildOutput.isAlreadyOpened)
                DialogFactory.createBuildOutput(eocvSim)
        }
        mWorkspMenu.add(workspBuildOutput)

        mWorkspMenu.addSeparator()

        val workspVSCode = JMenu("VS Code")

        val workspVSCodeOpen = JMenuItem("Open in current workspace")

        workspVSCodeOpen.addActionListener {
            VSCodeLauncher.asyncLaunch(eocvSim.workspaceManager.workspaceFile)
        }
        workspVSCode.add(workspVSCodeOpen)

        val workspVSCodeCreate = JMenuItem("Create VS Code workspace")

        workspVSCodeCreate.addActionListener { visualizer.createVSCodeWorkspace() }
        workspVSCode.add(workspVSCodeCreate)

        mWorkspMenu.add(workspVSCode)

        add(mWorkspMenu)

        // EDIT

        val editSettings = JMenuItem("Settings")
        editSettings.addActionListener { DialogFactory.createConfigDialog(eocvSim) }

        mEditMenu.add(editSettings)
        add(mEditMenu)

        // HELP

        val helpUsage = JMenuItem("Usage")
        helpUsage.addActionListener {
            Desktop.getDesktop().browse(URI("https://github.com/serivesmejia/EOCV-Sim/blob/master/USAGE.md"))
        }

        helpUsage.isEnabled = Desktop.isDesktopSupported()
        mHelpMenu.add(helpUsage)

        mHelpMenu.addSeparator()

        val helpAbout = JMenuItem("About")
        helpAbout.addActionListener { DialogFactory.createAboutDialog(eocvSim) }

        mHelpMenu.add(helpAbout)
        add(mHelpMenu)
    }

}