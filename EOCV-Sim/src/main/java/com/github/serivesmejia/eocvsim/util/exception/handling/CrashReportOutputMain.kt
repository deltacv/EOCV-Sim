package com.github.serivesmejia.eocvsim.util.exception.handling

import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme
import com.github.serivesmejia.eocvsim.Build
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.util.SysUtil
import kotlinx.coroutines.Runnable
import picocli.CommandLine
import java.io.File

object CrashReportOutputMain {
    @CommandLine.Command(name = "report", mixinStandardHelpOptions = true, version = [Build.versionString])
    private class CrashReportOutputCommandInterface : Runnable {
        @CommandLine.Option(names = ["-p", "--path"], description = ["Specifies the path where the crash report was saved"])
        @JvmField var crashReportPath: String? = null

        override fun run() {
            DialogFactory.createCrashReport(null, SysUtil.loadFileStr(File(crashReportPath ?: "")))
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        CommandLine(CrashReportOutputCommandInterface()).execute(*args)
    }
}