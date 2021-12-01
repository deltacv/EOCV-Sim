package com.github.serivesmejia.eocvsim.util

object MemoryUtil {

    val memoryUsageWindows: Double get() {
        val pid = SysUtil.getJvmPid()
        val result = SysUtil.runShellCommand("wmic process where processid=$pid get WorkingSetSize")

        return if(result.exitCode != 0) {
            0.0
        } else {
            val lines = result.output.split("\n")
            if(lines[0].trim() == "WorkingSetSize") {
                lines[1].toDouble() / SysUtil.MB
            } else {
                0.0
            }
        }
    }

}