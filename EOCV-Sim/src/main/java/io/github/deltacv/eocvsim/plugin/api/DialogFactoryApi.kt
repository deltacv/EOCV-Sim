package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import java.io.File
import javax.swing.filechooser.FileFilter

abstract class DialogFactoryApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract fun createYesOrNo(message: String, subMessage: String, result: (Boolean) -> Unit)

    abstract fun createFileChooser(mode: JFileChooserApi.Mode = JFileChooserApi.Mode.FILE_SELECT, initialFileName: String = "", vararg filters: FileFilter): JFileChooserApi

    abstract fun createSourceDialog(type: InputSourceApi.Type, initialFile: File? = null)

    abstract fun createSourceDialog()
    abstract fun createConfigDialog()
    abstract fun createAboutDialog()

    abstract fun createOutputDialog(wasManuallyOpened: Boolean)
    abstract fun createBuildOutputDialog()
    abstract fun createPipelineOutputDialog()

    abstract fun createSplashScreen(closeHook: HookApi)
    abstract fun createIAmADialog()
    abstract fun createIAmAPaperVisionDialog(showWorkspacesButton: Boolean)

    abstract fun createWorkspaceDialog()
    abstract fun createCrashReportDialog(report: String)
}