/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim

import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.exception.handling.EOCVSimUncaughtExceptionHandler
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
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
        ).setCaseInsensitiveEnumValuesAllowed(true)
            .setExecutionExceptionHandler { exception, line, result ->
                EOCVSimUncaughtExceptionHandler.uncaughtException(currentMainThread, exception)
                -1
            }
            .execute(*args)

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
@CommandLine.Command(name = "eocvsim", mixinStandardHelpOptions = true, versionProvider = BuildInfoVersionProvider::class)
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

    override fun run() {
        val parameters = EOCVSim.Parameters()

        if (workspacePath != null) {
            parameters.initialWorkspace = checkPath("Workspace", workspacePath!!, true)
        }

        if (initialPipeline != null) {
            parameters.initialPipelineName = initialPipeline
            parameters.initialPipelineSource = initialPipelineSource
        }

        GlobalContext.startKoin {
            modules(
                eocvSimModule,
                module {
                    single { EOCVSim() }
                    single { parameters }
                }
            )
        }

        // get eocv-sim from DI and start
        GlobalContext.get().get<EOCVSim>().start()
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

/**
 * Supplies the simulator version string at runtime for Picocli commands.
 */
class BuildInfoVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> = arrayOf(BuildInfo.VERSION_STRING)
}

