@file:JvmName("Main")
package com.github.serivesmejia.eocvsim

import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.loggerForThis
import picocli.CommandLine
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

val jvmMainThread: Thread = Thread.currentThread()
var currentMainThread: Thread = jvmMainThread

fun main(args: Array<String>) {
    val result = CommandLine(
        EOCVSimCommandInterface()
    ).setCaseInsensitiveEnumValuesAllowed(true).execute(*args)

    exitProcess(result)
}

@CommandLine.Command(name = "eocvsim", mixinStandardHelpOptions = true, version = [Build.versionString])
class EOCVSimCommandInterface : Runnable {

    @CommandLine.Option(names = ["-w", "--workspace"], description = ["Specifies the workspace that will be used only during this run, path can be relative or absolute"])
    @JvmField var workspacePath = ""

    @CommandLine.Option(names = ["-p", "--pipeline"], description = ["Specifies the pipeline selected when the simulator starts, and the initial runtime build finishes if it was running"])
    @JvmField var initialPipeline = ""
    @CommandLine.Option(names = ["-s", "--source"], description = ["Specifies the source of the pipeline that will be selected when the simulator starts, from the --pipeline argument. Defaults to CLASSPATH. Possible values: \${COMPLETION-CANDIDATES}"])
    @JvmField var initialPipelineSource = PipelineSource.CLASSPATH

    override fun run() {
        val parameters = EOCVSim.Parameters()

        if(workspacePath.trim() != "") {
            var file = File(workspacePath)

            if(!file.exists()) {
                file = Paths.get(System.getProperty("user.dir"), workspacePath).toFile()

                if(!file.exists()) {
                    System.err.println("Workspace path is not valid, folder doesn't exist (tried in \"$workspacePath\" and \"${file.absolutePath})\"")
                    exitProcess(1)
                }
            }

            if(!file.isDirectory) {
                System.err.println("Workspace path is not valid, the specified path is not a folder")
                exitProcess(1)
            }

            parameters.initialWorkspace = file
        }

        if(initialPipeline.trim() != "") {
            parameters.initialPipelineName = initialPipeline
            parameters.initialPipelineSource = initialPipelineSource
        }

        EOCVSim(parameters).init()
    }

}