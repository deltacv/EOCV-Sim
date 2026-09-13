/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.LifecycleSignal
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.dialog.Output
import com.github.serivesmejia.eocvsim.gui.util.GuiUtil
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.pipeline.compiled.CompiledPipelineManager
import com.github.serivesmejia.eocvsim.plugin.output.PluginDialogSignal
import com.github.serivesmejia.eocvsim.plugin.output.PluginOutputHandler
import com.github.serivesmejia.eocvsim.util.FileFilters
import com.github.serivesmejia.eocvsim.util.exception.handling.CrashReport
import com.github.serivesmejia.eocvsim.workspace.util.VSCodeLauncher
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

import org.deltacv.eocvsim.plugin.loader.PluginManager
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.koin.core.qualifier.named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TopMenuBar : JMenuBar(), KoinComponent {

    val visualizer: Visualizer by inject()
    val dialogFactory: DialogFactory by inject()
    val pluginManager: org.deltacv.eocvsim.plugin.loader.PluginManager by inject()
    val outputHandler: PluginOutputHandler by inject()
    val workspaceManager: WorkspaceManager by inject()
    val pipelineManager: PipelineManager by inject()
    val onMainUpdate: EventHandler by inject(named("onMainLoop"))
    val lifecycleChannel: Channel<LifecycleSignal> by inject(named("lifecycle"))
    val scope: CoroutineScope by inject()

    companion object {
        val docsUrl = URI("https://docs.deltacv.org/eocv-sim/")
    }

    @JvmField val mFileMenu   = JMenu("File")
    @JvmField val mWorkspMenu = JMenu("Workspace")
    @JvmField val mHelpMenu   = JMenu("Help")

    @JvmField val workspCompile = JMenuItem("Build Java Files")

    init {
        val desktop = Desktop.getDesktop()
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
                dialogFactory.createSourceDialog(type)
            }

            fileNewInputSourceSubmenu.add(fileNewInputSourceItem)
        }

        val fileSaveMat = JMenuItem("Screenshot Pipeline")

        fileSaveMat.addActionListener {
            val mat = Mat()
            visualizer.viewport.pollLastFrame(mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR)

            GuiUtil.saveMatFileChooser(
                visualizer.frame,
                mat,
                dialogFactory
            )

            mat.release()
        }
        mFileMenu.add(fileSaveMat)

        mFileMenu.addSeparator()

        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
            desktop.setPreferencesHandler { dialogFactory.createConfigDialog() }
        } else {
            val editSettings = JMenuItem("Settings")
            editSettings.addActionListener { dialogFactory.createConfigDialog() }
            mFileMenu.add(editSettings)
        }

        val filePlugins = JMenuItem("Manage Plugins")
        filePlugins.addActionListener {
            outputHandler.sendDialogSignal(PluginDialogSignal.ShowPlugins)
        }

        mFileMenu.add(filePlugins)

        mFileMenu.addSeparator()

        val fileRestart = JMenuItem("Restart")

        fileRestart.addActionListener { onMainUpdate.once { lifecycleChannel.trySend(LifecycleSignal.Restart) } }

        mFileMenu.add(fileRestart)

        add(mFileMenu)

        //WORKSPACE

        val workspSetWorkspace = JMenuItem("Select Workspace")

        workspSetWorkspace.addActionListener { dialogFactory.createWorkspace() }
        mWorkspMenu.add(workspSetWorkspace)

        val workspClose = JMenuItem("Close Current Workspace")

        workspClose.addActionListener {
            onMainUpdate.once {
                workspaceManager.workspaceFile = CompiledPipelineManager.DEF_WORKSPACE_FOLDER
            }
        }

        mWorkspMenu.add(workspClose)

        mWorkspMenu.addSeparator()

        workspCompile.addActionListener { pipelineManager.compiledPipelineManager.asyncBuild() }

        mWorkspMenu.add(workspCompile)

        val workspBuildOutput = JMenuItem("Output")

        workspBuildOutput.addActionListener {
            if(!Output.isAlreadyOpened)
                dialogFactory.createOutput(true)
        }
        mWorkspMenu.add(workspBuildOutput)

        mWorkspMenu.addSeparator()

        val workspVSCode = JMenu("External")

        val workspVSCodeCreate = JMenuItem("Create Gradle Workspace")

        workspVSCodeCreate.addActionListener { visualizer.createVSCodeWorkspace() }
        workspVSCode.add(workspVSCodeCreate)

        workspVSCode.addSeparator()

        val workspVSCodeOpen = JMenuItem("Open VS Code Here")

        workspVSCodeOpen.addActionListener {
            VSCodeLauncher.asyncLaunch(workspaceManager.workspaceFile, scope)

        }
        workspVSCode.add(workspVSCodeOpen)

        mWorkspMenu.add(workspVSCode)

        add(mWorkspMenu)

        // HELP

        val helpUsage = JMenuItem("Documentation")
        helpUsage.addActionListener {
            desktop.browse(docsUrl)
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

            dialogFactory.createFileChooser(visualizer.frame,
                DialogFactory.FileChooser.Mode.SAVE_FILE_SELECT,
                CrashReport.defaultCrashFileName, FileFilters.logFileFilter
            ).addCloseListener { OPTION, selectedFile, _ ->
                    if(OPTION == JFileChooser.APPROVE_OPTION) {
                        var path = selectedFile?.absolutePath ?: return@addCloseListener
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

        mHelpMenu.addSeparator()

        val helpIAmA = JMenuItem("I am a...")
        helpIAmA.addActionListener { dialogFactory.createIAmA() }

        mHelpMenu.add(helpIAmA)

        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler { dialogFactory.createAboutDialog() }
        }
        else {
            val helpAbout = JMenuItem("About")
            helpAbout.addActionListener { dialogFactory.createAboutDialog() }
            mHelpMenu.add(helpAbout)
        }

        add(mHelpMenu)
    }

}

