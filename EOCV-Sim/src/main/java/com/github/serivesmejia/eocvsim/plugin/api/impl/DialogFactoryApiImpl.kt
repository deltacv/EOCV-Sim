package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.DialogFactoryApi
import io.github.deltacv.eocvsim.plugin.api.HookApi
import io.github.deltacv.eocvsim.plugin.api.InputSourceApi
import io.github.deltacv.eocvsim.plugin.api.JFileChooserApi
import java.io.File
import javax.swing.filechooser.FileFilter

class DialogFactoryApiImpl(owner: EOCVSimPlugin, val internalVisualizer: Visualizer) : DialogFactoryApi(owner) {
    override fun createYesOrNo(
        message: String,
        subMessage: String,
        result: (Boolean) -> Unit
    ) = apiImpl {
        DialogFactory.createYesOrNo(internalVisualizer.frame, message, subMessage) {
            result(it == 0)
        }
    }

    override fun createFileChooser(
        mode: JFileChooserApi.Mode,
        initialFileName: String,
        vararg filters: FileFilter
    ) = apiImpl {
        val internalMode = when(mode) {
            JFileChooserApi.Mode.FILE_SELECT -> DialogFactory.FileChooser.Mode.FILE_SELECT
            JFileChooserApi.Mode.DIRECTORY_SELECT -> DialogFactory.FileChooser.Mode.DIRECTORY_SELECT
            JFileChooserApi.Mode.SAVE_FILE -> DialogFactory.FileChooser.Mode.SAVE_FILE_SELECT
        }

        val fileChooser = DialogFactory.createFileChooser(
            internalVisualizer.frame,
            internalMode,
            initialFileName,
            *filters
        )

        JFileChooserApiImpl(owner, fileChooser)
    }

    override fun createSourceDialog(
        type: InputSourceApi.Type,
        initialFile: File?
    ) = apiImpl {
        val type = when(type) {
            InputSourceApi.Type.IMAGE -> SourceType.IMAGE
            InputSourceApi.Type.VIDEO -> SourceType.VIDEO
            InputSourceApi.Type.CAMERA -> SourceType.CAMERA
            InputSourceApi.Type.HTTP -> SourceType.HTTP
        }

        DialogFactory.createSourceDialog(internalVisualizer.eocvSim, type, initialFile)
    }

    override fun createSourceDialog() = apiImpl {
        DialogFactory.createSourceExDialog(internalVisualizer.eocvSim)
    }

    override fun createConfigDialog() = apiImpl {
        DialogFactory.createConfigDialog(internalVisualizer.eocvSim)
    }

    override fun createAboutDialog() = apiImpl {
        DialogFactory.createAboutDialog(internalVisualizer.eocvSim)
    }

    override fun createOutputDialog(wasManuallyOpened: Boolean) = apiImpl {
        DialogFactory.createOutput(internalVisualizer.eocvSim, wasManuallyOpened)
    }

    override fun createBuildOutputDialog() = apiImpl {
        DialogFactory.createBuildOutput(internalVisualizer.eocvSim)
    }

    override fun createPipelineOutputDialog() = apiImpl {
        DialogFactory.createPipelineOutput(internalVisualizer.eocvSim)
    }

    override fun createSplashScreen(closeHook: HookApi) = apiImpl {
        val internalCloseHandler = EventHandler("DialogFactoryApiImpl-SplashScreen-CloseHandler")
        closeHook {
            internalCloseHandler.run()
        }

        DialogFactory.createSplashScreen(internalCloseHandler)
    }

    override fun createIAmADialog() = apiImpl {
        DialogFactory.createIAmA(internalVisualizer)
    }

    override fun createIAmAPaperVisionDialog(showWorkspacesButton: Boolean) = apiImpl {
        DialogFactory.createIAmAPaperVision(internalVisualizer, showWorkspacesButton)
    }

    override fun createWorkspaceDialog() = apiImpl {
        DialogFactory.createWorkspace(internalVisualizer)
    }

    override fun createCrashReportDialog(report: String) = apiImpl {
        DialogFactory.createCrashReport(internalVisualizer, report)
    }

    override fun disableApi() { }
}