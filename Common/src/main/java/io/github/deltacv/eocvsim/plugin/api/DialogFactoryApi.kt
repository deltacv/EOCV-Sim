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

package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import java.io.File
import javax.swing.filechooser.FileFilter

/**
 * API for creating application-provided dialogs and UI windows.
 *
 * This factory allows plugins to request dialogs without constructing or
 * managing UI components directly. All dialogs are owned and controlled by
 * the host application.
 *
 * @param owner the plugin that owns this API instance
 */
abstract class DialogFactoryApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Creates a modal yes-or-no dialog.
     *
     * The dialog presents a primary message, an optional sub-message, and
     * invokes the provided callback with the user’s choice.
     *
     * @param message the main message shown to the user
     * @param subMessage additional explanatory text displayed below the main message
     * @param result callback invoked with `true` if the user selects "Yes",
     *               or `false` if the user selects "No"
     */
    abstract fun createYesOrNo(
        message: String,
        subMessage: String,
        result: (Boolean) -> Unit
    )

    /**
     * Creates a file chooser dialog.
     *
     * The chooser can operate in different selection modes and may be
     * initialized with a default file name. Optional file filters may be
     * provided to restrict visible file types.
     *
     * @param mode the file selection mode
     * @param initialFileName the initial file name or path to display
     * @param filters optional file filters applied to the chooser
     * @return a configured [JFileChooserApi] instance
     */
    abstract fun createFileChooser(
        mode: JFileChooserApi.Mode = JFileChooserApi.Mode.FILE_SELECT,
        initialFileName: String = "",
        vararg filters: FileFilter
    ): JFileChooserApi

    /**
     * Creates a source selection dialog.
     *
     * The dialog allows the user to select an input source based on the given
     * [InputSourceApi.Type]. An initial file may be provided as a default
     * selection.
     *
     * @param type the type of input source to select
     * @param initialFile an optional file to preselect
     */
    abstract fun createSourceDialog(
        type: InputSourceApi.Type,
        initialFile: File? = null
    )

    /**
     * Creates a generic source selection dialog using default parameters.
     */
    abstract fun createSourceDialog()

    /**
     * Creates the plugin configuration dialog.
     *
     * This dialog is typically used to expose user-configurable settings
     * provided by the plugin.
     */
    abstract fun createConfigDialog()

    /**
     * Creates the application "About" dialog.
     */
    abstract fun createAboutDialog()

    /**
     * Creates an output dialog.
     *
     * @param wasManuallyOpened whether the dialog was opened explicitly by
     *                          the user rather than automatically by the system
     */
    abstract fun createOutputDialog(wasManuallyOpened: Boolean)

    /**
     * Creates the build output dialog.
     *
     * This dialog is used to display build or compilation-related output.
     */
    abstract fun createBuildOutputDialog()

    /**
     * Creates the pipeline output dialog.
     *
     * This dialog is used to display runtime or pipeline execution output.
     */
    abstract fun createPipelineOutputDialog()

    /**
     * Creates the application splash screen.
     *
     * The provided [HookApi] is invoked when the splash screen is closed.
     *
     * @param closeHook callback executed when the splash screen is dismissed
     */
    abstract fun createSplashScreen(closeHook: HookApi)

    /**
     * Creates an "I Am A..." dialog.
     *
     * This dialog is typically used for onboarding or user role selection.
     */
    abstract fun createIAmADialog()

    /**
     * Creates an "I Am A PaperVision" dialog.
     *
     * @param showWorkspacesButton whether a workspace selection button should
     *                             be displayed
     */
    abstract fun createIAmAPaperVisionDialog(showWorkspacesButton: Boolean)

    /**
     * Creates the workspace management dialog.
     */
    abstract fun createWorkspaceDialog()

    /**
     * Creates a crash report dialog.
     *
     * The dialog presents a crash report to the user and may provide options
     * for copying or submitting the report.
     *
     * @param report the crash report content
     */
    abstract fun createCrashReportDialog(report: String)
}