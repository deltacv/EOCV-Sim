package com.github.serivesmejia.eocvsim.util.io

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.io.File

object EOCVSimFolder : File(SysUtil.getAppData().absolutePath + separator + ".eocvsim") {

    val logger by loggerForThis()

    val lock by lazy { lockDirectory() }
    val couldLock get() = lock != null && lock!!.isLocked

    val logsFolder = File(this, "logs")

    val lastLogFile: File? get() {
        var newestLogFile: File? = null

        logsFolder.listFiles()?.forEach { logFile ->
            if(logFile.lastModified() > newestLogFile?.lastModified() ?: 0L) {
                newestLogFile = logFile
            }
        }

        return newestLogFile
    }

    init {
        mkdir()
        logsFolder.mkdir()

        // delete log files more than 3 days old
        val timeThreshold = (86400000 * 3)

        logsFolder.listFiles()?.forEach { logFile ->
            if(!logFile.isDirectory
                && logFile.name.endsWith(".log")
                && System.currentTimeMillis() - logFile.lastModified() > timeThreshold
            ) {
                logFile.delete()
                logger.trace("Deleted log file $logFile")
            }
        }
    }

}