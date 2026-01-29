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

package com.github.serivesmejia.eocvsim

import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import picocli.CommandLine
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

val jvmMainThread: Thread = Thread.currentThread()
var currentMainThread: Thread = jvmMainThread

object Main {

    /**
     * Main entry point for the EOCV-Sim CLI
     * @param args the command line arguments
     * @see CommandLine
     */
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("sun.java2d.d3d", "false")
        System.setProperty("apple.awt.application.appearance", "system")
        System.setProperty("apple.awt.application.name", "EasyOpenCV Simulator")

        val result = CommandLine(
            EOCVSimCommandInterface()
        ).setCaseInsensitiveEnumValuesAllowed(true).execute(*args)

        exitProcess(result)
    }
}

/**
 * Command line interface for the EOCV-Sim CLI
 * It uses picocli for command line parsing
 * and allows for the user to specify the workspace, pipeline and OpenCV native path
 * specified in the Parameters object
 * @see CommandLine
 * @see EOCVSim.Parameters
 */
@CommandLine.Command(name = "eocvsim", mixinStandardHelpOptions = true, version = [Build.versionString])
class EOCVSimCommandInterface : Runnable {

    @CommandLine.Option(
        names = ["-w", "--workspace"],
        description = ["Specifies the workspace that will be used only during this run, path can be relative or absolute"]
    )
    @JvmField
    var workspacePath: String? = null

    @CommandLine.Option(
        names = ["-p", "--pipeline"],
        description = ["Specifies the pipeline selected when the simulator starts, and the initial runtime build finishes if it was running"]
    )
    @JvmField
    var initialPipeline: String? = null

    @CommandLine.Option(
        names = ["-s", "--source"],
        description = ["Specifies the source of the pipeline that will be selected when the simulator starts, from the --pipeline argument. Defaults to CLASSPATH. Possible values: \${COMPLETION-CANDIDATES}"]
    )
    @JvmField
    var initialPipelineSource = PipelineSource.CLASSPATH

    @CommandLine.Option(
        names = ["-o", "--opencvpath"],
        description = ["Specifies an alternative path for the OpenCV native to be loaded at runtime"]
    )
    @JvmField
    var opencvNativePath: String? = null

    override fun run() {
        val parameters = EOCVSim.Parameters()

        if (workspacePath != null) {
            parameters.initialWorkspace = checkPath("Workspace", workspacePath!!, true)
        }

        if (initialPipeline != null) {
            parameters.initialPipelineName = initialPipeline
            parameters.initialPipelineSource = initialPipelineSource
        }

        if (opencvNativePath != null) {
            parameters.opencvNativeLibrary = checkPath("OpenCV Native", opencvNativePath!!, false)
        }

        EOCVSim(parameters).init()
    }

    private fun checkPath(parameter: String, path: String, shouldBeDirectory: Boolean): File {
        var file = File(path)

        if (!file.exists()) {
            file = Paths.get(System.getProperty("user.dir"), path).toFile()

            if (!file.exists()) {
                System.err.println("$parameter path is not valid, it doesn't exist (tried in \"$path\" and \"${file.absolutePath})\"")
                exitProcess(1)
            }
        }

        if (shouldBeDirectory && !file.isDirectory) {
            System.err.println("$parameter path is not valid, the specified path is not a folder")
            exitProcess(1)
        }

        return file
    }

}