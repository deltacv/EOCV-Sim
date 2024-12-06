package com.github.serivesmejia.eocvsim.gui.dialog

import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.dialog.component.BottomButtonsPanel
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel
import com.github.serivesmejia.eocvsim.gui.dialog.component.OutputPanel.DefaultBottomButtonsPanel
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.Box
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
        FlatArcDarkIJTheme.setup()

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