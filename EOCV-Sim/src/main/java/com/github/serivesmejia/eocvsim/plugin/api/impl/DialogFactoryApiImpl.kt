/*
 * Copyright (c) 2026 Sebastian Erives
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import javax.swing.filechooser.FileFilter

class DialogFactoryApiImpl(owner: EOCVSimPlugin, val internalVisualizer: Visualizer) : DialogFactoryApi(owner), KoinComponent {

    val dialogFactory: DialogFactory by inject()

    override fun createYesOrNo(
        message: String,
        subMessage: String,
        result: (Boolean) -> Unit
    ) = apiImpl {
        dialogFactory.createYesOrNo(internalVisualizer.frame, message, subMessage) {
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

        val fileChooser = dialogFactory.createFileChooser(
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

        dialogFactory.createSourceDialog(type, initialFile)
    }

    override fun createSourceDialog() = apiImpl {
        dialogFactory.createSourceExDialog()
    }

    override fun createConfigDialog() = apiImpl {
        dialogFactory.createConfigDialog()
    }

    override fun createAboutDialog() = apiImpl {
        dialogFactory.createAboutDialog()
    }

    override fun createOutputDialog(wasManuallyOpened: Boolean) = apiImpl {
        dialogFactory.createOutput(wasManuallyOpened)
    }

    override fun createBuildOutputDialog() = apiImpl {
        dialogFactory.createBuildOutput()
    }

    override fun createPipelineOutputDialog() = apiImpl {
        dialogFactory.createPipelineOutput()
    }

    override fun createSplashScreen(closeHook: HookApi) = apiImpl {
        val internalCloseHandler = EventHandler("DialogFactoryApiImpl-SplashScreen-CloseHandler")
        closeHook {
            internalCloseHandler.run()
        }

        dialogFactory.createSplashScreen(internalCloseHandler)
    }

    override fun createIAmADialog() = apiImpl {
        dialogFactory.createIAmA()
    }

    override fun createIAmAPaperVisionDialog(showWorkspacesButton: Boolean) = apiImpl {
        dialogFactory.createIAmAPaperVision(showWorkspacesButton)
    }

    override fun createWorkspaceDialog() = apiImpl {
        dialogFactory.createWorkspace()
    }

    override fun createCrashReportDialog(report: String) = apiImpl {
        dialogFactory.createCrashReport(report)
    }

    override fun disableApi() { }
}