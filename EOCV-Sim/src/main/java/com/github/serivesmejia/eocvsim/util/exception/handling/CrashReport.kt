/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.exception.handling

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.Build
import com.github.serivesmejia.eocvsim.util.StrUtil
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import org.deltacv.common.util.loggerForThis
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Class to generate a crash report on an uncaught exception event
 * @param causedByException the exception that caused the crash
 * @param isDummy if the crash report is a dummy one for exporting or testing purposes
 */
class CrashReport(causedByException: Throwable, isDummy: Boolean = false) {

    companion object {
        val OS_ARCH = System.getProperty("os.arch")
        val OS_VERSION = System.getProperty("os.version")
        val OS_NAME = System.getProperty("os.name")
        val SYSUTIL_DETECTED_OS = SysUtil.getOS()
        val JAVA_VERSION = System.getProperty("java.version")
        val JAVA_VENDOR = System.getProperty("java.vendor")

        val dtFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH.mm.ss")

        @JvmStatic val defaultFileName: String get() {
            val dateTimeStr = dtFormatter.format(LocalDateTime.now())
            return "eocvsim-$dateTimeStr.log"
        }

        private val wittyComments = setOf(
            "Oh no! The robot is sad now :(",
            "The robot is not happy about this",
            "Who set us up the TNT?",
            "Everything's going to plan. No, really, that was supposed to happen.",
            "I'm sorry, Dave. I'm afraid I can't do that.",
            "Oops.",
            "Uh... Did I do that?",
            "This is fine.",
            "I feel sad now :(",
            "I let you down. Sorry :(",
            "On the bright side, I bought you a teddy bear!",
            "Daisy, daisy...",
            "Oh - I know what I did wrong!",
            "I blame ESD.",
            "You should try PaperVision!",
            "Don't be sad. I'll do better next time, I promise!",
            "Don't be sad, have a hug! <3",
            "I just don't know what went wrong :(",
            "Quite honestly, I wouldn't worry myself about that.",
            "Do not go gentle into that good night...",
            "Sorry :(",
            "Surprise! Haha. Well, this is awkward.",
            "Hi. I'm EOCV-Sim, and I'm a crashaholic.",
            "Ooh. Shiny.",
            "This doesn't make any sense!",
            "Why is it breaking :(",
            "Don't do that.",
            "But it works on my machine!"
        )

        @JvmStatic val defaultCrashFileName get() = "crashreport-$defaultFileName"
    }


    val logger by loggerForThis()

    private val sb = StringBuilder()

    init {
        val title = "  EOCV-Sim v${EOCVSim.VERSION} crash report"
        val dashes = "-".repeat(title.length)
        val topBorder = "/$dashes\\"
        val bottomBorder = "\\$dashes/"

        sb.appendLine(topBorder).appendLine()
        sb.appendLine(title).appendLine()
        sb.appendLine(bottomBorder).appendLine()

        sb.appendLine("! ${wittyComments.random()}").appendLine()

        sb.appendLine(": Crash stacktrace").appendLine()
        if(!isDummy) {
            sb.appendLine(StrUtil.fromException(causedByException)).appendLine()
        } else {
            sb.appendLine(causedByException.message).appendLine()
        }

        val infoSectionLines = listOf(
            ": EOCV-Sim info",
            "   Version: ${EOCVSim.VERSION}",
            "   Built on: ${Build.buildDate}",
            ": System specs",
            "   OS name: $OS_NAME",
            "   OS version: $OS_VERSION",
            "   Detected OS: $SYSUTIL_DETECTED_OS",
            "   Arch: $OS_ARCH",
            "   Detected Arch: ${SysUtil.ARCH}",
            "   Java version: $JAVA_VERSION",
            "   Java vendor: $JAVA_VENDOR",
            "   Last memory usage: ${SysUtil.getMemoryUsageMB()} MB"
        )
        val infoSectionDivider = "=".repeat(infoSectionLines.maxOf { it.length })

        sb.appendLine(infoSectionDivider).appendLine()

        sb.appendLine(infoSectionLines[0])
        sb.appendLine(infoSectionLines[1])
        sb.appendLine(infoSectionLines[2]).appendLine()

        sb.appendLine(infoSectionLines[3])
        sb.appendLine(infoSectionLines[4])
        sb.appendLine(infoSectionLines[5])
        sb.appendLine(infoSectionLines[6])
        sb.appendLine(infoSectionLines[7])
        sb.appendLine(infoSectionLines[8])
        sb.appendLine(infoSectionLines[9])
        sb.appendLine(infoSectionLines[10])
        sb.appendLine(infoSectionLines[11]).appendLine()

        sb.appendLine(infoSectionDivider).appendLine()

        sb.appendLine(": Full thread dump").appendLine()

        for((thread, stacktrace) in Thread.getAllStackTraces()) {
            sb.appendLine(" > Thread \"${thread.name}\"")

            for(element in stacktrace) {
                sb.appendLine("     $element")
            }
        }
        sb.appendLine()

        sb.appendLine(infoSectionDivider).appendLine()


        sb.appendLine(": Full log").appendLine()

        val lastLogFile = EOCVSimFolder.lastLogFile
        if(lastLogFile != null) {
            sb.appendLine(SysUtil.loadFileStr(lastLogFile)).appendLine()
        } else {
            sb.appendLine("No logs").appendLine()
        }

        sb.appendLine(";")
    }

    /**
     * Save the crash report to a file
     * @param f the file to save the crash report to
     */
    fun saveCrashReport(f: File) {
        SysUtil.saveFileStr(f, toString())
        logger.info("Saved crash report to ${f.absolutePath}")
    }

    /**
     * Save the crash report to a file located in the working directory
     */
    fun saveCrashReport(): File {
        val workingDir = File(System.getProperty("user.dir"))

        val crashLogFile = workingDir + defaultCrashFileName

        saveCrashReport(crashLogFile)

        return crashLogFile
    }

    /**
     * Save the crash report to a file located in the working directory
     * @param filename the name of the file to save the crash report to
     */
    fun saveCrashReport(filename: String): File {
        val workingDir = File(System.getProperty("user.dir"))
        val crashLogFile = workingDir + File.separator + "$filename.log"

        saveCrashReport(crashLogFile)
        return crashLogFile
    }

    /**
     * Get the crash report as a string
     */
    override fun toString() = sb.toString()

}

