@file:JvmName("Main")
package com.github.serivesmejia.eocvsim

import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val result = CommandLine(
        EOCVSimCommandInterface()
    ).execute(*args)

    exitProcess(result)
}

@CommandLine.Command(name = "eocvsim", mixinStandardHelpOptions = true, version = [Build.versionString])
class EOCVSimCommandInterface : Runnable {

    @CommandLine.Option(names = ["-w", "--workspace"])
    @JvmField var workspacePath = ""

    @CommandLine.Option(names = ["-p", "--pipeline"])
    @JvmField var initialPipeline = ""

    override fun run() {
        println(workspacePath)
        println(initialPipeline)

        EOCVSim().init()
    }

}