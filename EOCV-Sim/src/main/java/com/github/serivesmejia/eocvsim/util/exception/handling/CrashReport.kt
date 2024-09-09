/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.util.exception.handling

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.Build
import com.github.serivesmejia.eocvsim.util.StrUtil
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import com.github.serivesmejia.eocvsim.util.loggerForThis
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
            "I'm not even angry. I'm being so sincere right now.",
            "I feel sad now :(",
            "I let you down. Sorry :(",
            "On the bright side, I bought you a teddy bear!",
            "Daisy, daisy...",
            "Oh - I know what I did wrong!",
            "Hey, that tickles! Hehehe!",
            "I blame Dean Kamen.",
            "You should try our sister simulator!",
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
            "Ouch. That hurt :(",
            "This is a token for 1 free hug. Redeem at your nearest local team: [~~HUG~~]",
            "But it works on my machine!",
            "Y a través de las estrellas ella viaja...",
            "Tras el extraño silencio del sol ausente",
            "no lo pienses demasiado, todo estará bien,",
            "encontrarás otro desastre que hacer,",
            "¿qué será de sentarse bajo la tumba del sol sin nunca hacer un desastre?",
            "y por una ultima noche juntos,"

        )

        @JvmStatic val defaultCrashFileName get() = "crashreport-$defaultFileName"
    }


    val logger by loggerForThis()

    private val sb = StringBuilder()

    init {
        sb.appendLine("/--------------------------------\\").appendLine()
        sb.appendLine("  EOCV-Sim v${EOCVSim.VERSION} crash report").appendLine()
        sb.appendLine("\\--------------------------------/").appendLine()

        sb.appendLine("! ${wittyComments.random()}").appendLine()

        sb.appendLine(": Crash stacktrace").appendLine()
        if(!isDummy) {
            sb.appendLine(StrUtil.fromException(causedByException)).appendLine()
        } else {
            sb.appendLine(causedByException.message).appendLine()
        }

        sb.appendLine("==========================================").appendLine()

        sb.appendLine(": EOCV-Sim info")
        sb.appendLine("   Version: ${EOCVSim.VERSION}")
        sb.appendLine("   Built on: ${Build.buildDate}").appendLine()

        sb.appendLine(": System specs")
        sb.appendLine("   OS name: $OS_NAME")
        sb.appendLine("   OS version: $OS_VERSION")
        sb.appendLine("   Detected OS: $SYSUTIL_DETECTED_OS")
        sb.appendLine("   Arch: $OS_ARCH")
        sb.appendLine("   Detected Arch: ${SysUtil.ARCH}")
        sb.appendLine("   Java version: $JAVA_VERSION")
        sb.appendLine("   Java vendor: $JAVA_VENDOR")
        sb.appendLine("   Last memory usage: ${SysUtil.getMemoryUsageMB()} MB").appendLine()

        sb.appendLine("==========================================").appendLine()

        sb.appendLine(": Full thread dump").appendLine()

        for((thread, stacktrace) in Thread.getAllStackTraces()) {
            sb.appendLine(" > Thread \"${thread.name}\"")

            for(element in stacktrace) {
                sb.appendLine("     $element")
            }
        }
        sb.appendLine()

        sb.appendLine("==================================").appendLine()


        sb.appendLine(": Full logs").appendLine()

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
    fun saveCrashReport() {
        val workingDir = File(System.getProperty("user.dir"))

        val crashLogFile = workingDir + defaultCrashFileName

        saveCrashReport(crashLogFile)
    }

    /**
     * Save the crash report to a file located in the working directory
     * @param filename the name of the file to save the crash report to
     */
    fun saveCrashReport(filename: String) {
        val workingDir = File(System.getProperty("user.dir"))
        val crashLogFile = workingDir + File.separator + "$filename.log"

        saveCrashReport(crashLogFile)
    }

    /**
     * Get the crash report as a string
     */
    override fun toString() = sb.toString()

}
