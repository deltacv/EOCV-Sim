/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.exception.handling

import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme
import com.github.serivesmejia.eocvsim.Build
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.dialog.CrashReportOutput
import com.github.serivesmejia.eocvsim.util.SysUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import picocli.CommandLine
import java.io.File
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

object CrashReportOutputMain {
    @CommandLine.Command(name = "report", mixinStandardHelpOptions = true, version = [Build.versionString])
    private class CrashReportOutputCommandInterface : Runnable {
        @CommandLine.Option(names = ["-p", "--path"], description = ["Specifies the path where the crash report was saved"])
        @JvmField var crashReportPath: String? = null

        override fun run() {
            if (crashReportPath == null || crashReportPath!!.isEmpty()) {
                System.err.println("Crash report path is required (-p or --path)")
                exitProcess(1)
            }

            val file = File(crashReportPath!!)
            if (!file.exists()) {
                System.err.println("Crash report file not found: $crashReportPath")
                exitProcess(1)
            }

            SwingUtilities.invokeLater(FlatArcDarkIJTheme::setup)

            try {
                val crashContent = SysUtil.loadFileStr(file)
                CrashReportOutput(null, crashContent ?: "")
            } catch (e: Exception) {
                System.err.println("Failed to display crash report: $ {e.message}")
                e.printStackTrace()
                exitProcess(1)
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val exitCode = CommandLine(CrashReportOutputCommandInterface()).execute(*args)
        exitProcess(exitCode)
    }
}
