package com.github.serivesmejia.eocvsim.util.io

import com.github.serivesmejia.eocvsim.util.extension.appData
import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.io.File

/**
 * Object to store easy access to the EOCV-Sim folder
 * in the user's app data folder (Windows) or home folder (Linux)
 * Also handles locking the folder to prevent multiple instances
 * from running at the same time (which could cause issues).
 */
object EOCVSimFolder : File(appData.absolutePath + separator + ".eocvsim") {

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