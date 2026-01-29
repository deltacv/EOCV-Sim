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

package com.github.serivesmejia.eocvsim.gui.dialog

import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel.DefaultBottomButtonsPanel
import java.awt.Dimension
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.system.exitProcess

class CrashReportOutput(
    parent: JFrame?,
    crashReport: String
){
    val output by lazy {
        JDialog(parent)
    }

    private val reportPanel by lazy {
        OutputPanel(DefaultBottomButtonsPanel { exitProcess(0) })
    }

    init {
        output.isModal = true
        output.title = "Crash Report"
        output.layout = BoxLayout(output.contentPane, BoxLayout.Y_AXIS)
        output.isAlwaysOnTop = true

        reportPanel.outputArea.text = crashReport
        SwingUtilities.invokeLater {
            reportPanel.resetScroll()
        }

        output.add(JLabel("An unexpected fatal error occurred, please report this to the developers.").apply {
            font = font.deriveFont(Font.BOLD, 18f)
            alignmentX = JLabel.CENTER_ALIGNMENT
        })
        output.add(reportPanel)

        output.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        output.size = Dimension(800, 400)

        output.iconImages = listOf(
            EOCVSimIconLibrary.icoEOCVSim128.image,
            EOCVSimIconLibrary.icoEOCVSim64.image,
            EOCVSimIconLibrary.icoEOCVSim32.image,
            EOCVSimIconLibrary.icoEOCVSim16.image
        )

        output.setLocationRelativeTo(null)
        output.isVisible = true
    }
}