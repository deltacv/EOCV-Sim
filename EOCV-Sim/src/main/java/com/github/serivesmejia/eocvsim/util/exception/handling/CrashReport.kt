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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.CharBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

        @JvmStatic val defaultCrashFileName get() = "crashreport-$defaultFileName"
    }


    val logger by loggerForThis()

    private val sb = StringBuilder()

    init {
        sb.appendLine("/--------------------------------\\").appendLine()
        sb.appendLine("  EOCV-Sim v${EOCVSim.VERSION} crash report").appendLine()
        sb.appendLine("\\--------------------------------/").appendLine()

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
        sb.appendLine("   Architecture: $OS_ARCH")
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

    fun saveCrashReport(f: File) {
        SysUtil.saveFileStr(f, toString())
        logger.info("Saved crash report to ${f.absolutePath}")
    }

    fun saveCrashReport() {
        val workingDir = File(System.getProperty("user.dir"))

        val crashLogFile = workingDir + defaultCrashFileName

        saveCrashReport(crashLogFile)
    }

    fun saveCrashReport(filename: String) {
        val workingDir = File(System.getProperty("user.dir"))
        val crashLogFile = workingDir + "/$filename.log"

        saveCrashReport(crashLogFile)
    }

    override fun toString() = sb.toString()

}
