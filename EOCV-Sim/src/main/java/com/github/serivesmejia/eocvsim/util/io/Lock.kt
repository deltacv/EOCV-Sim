package com.github.serivesmejia.eocvsim.util.io

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

class LockFile(pathname: String) : File(pathname) {

    private val raf by lazy { RandomAccessFile(this, "rw") }

    var lock: FileLock? = null
        private set

    val logger by loggerForThis()

    val isLocked get() = try {
        raf
        if(lock != null) !tryLock(false) else false
    } catch(ex: Exception) {
        logger.warn("Can't open lock file $absolutePath")
        true
    }

    init {
        if(isDirectory)
            throw IllegalArgumentException("Lock file cannot be a directory")

        if(!exists())
            SysUtil.saveFileStr(this, "")
    }

    fun tryLock(log: Boolean = true): Boolean {
        return try {
            lock = raf.channel.tryLock()
            if(log) logger.trace("Probably locked file $absolutePath")
            true
        } catch(ex: Exception) {
            if(log) logger.warn("Couldn't lock file $absolutePath", ex);
            false
        }
    }

    fun unlock() {
        lock?.release()
        raf.close()

        lock = null
    }

}

val File.directoryLockFile get() = LockFile(absolutePath + File.separator + ".lock")

val File.isDirectoryLocked: Boolean get() {
    val lock = directoryLockFile
    val isLocked = lock.isLocked

    lock.unlock()
    return isLocked
}

fun File.lockDirectory(): LockFile? {
    if(!isDirectory)
        return null

    val lockFile = directoryLockFile

    if(isDirectoryLocked || !lockFile.tryLock())
        return null

    return lockFile
}