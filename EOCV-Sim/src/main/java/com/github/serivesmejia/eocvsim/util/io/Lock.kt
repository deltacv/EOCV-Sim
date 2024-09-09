package com.github.serivesmejia.eocvsim.util.io

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

/**
 * Class to handle a lock file to prevent multiple instances of a program from running at the same time
 * @param pathname the path of the file to lock
 */
class LockFile(pathname: String) : File(pathname) {

    private val raf by lazy { RandomAccessFile(this, "rw") }

    var lock: FileLock? = null
        private set

    val logger by loggerForThis()

    /**
     * Check if we obtained the lock
     */
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

    /**
     * Try to lock the lockfile
     * @param log if true, logs will be printed
     * @return true if the file was locked, false otherwise
     */
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

    /**
     * Unlock the lockfile to allow other instances to run
     */
    fun unlock() {
        lock?.release()
        raf.close()

        lock = null
    }

}

/**
 * Lock a directory to prevent multiple instances of a program from running at the same time
 * @return the lock file if the directory was locked, null otherwise
 */
val File.directoryLockFile get() = LockFile(absolutePath + File.separator + ".lock")

/**
 * Check if a directory is locked
 */
val File.isDirectoryLocked: Boolean get() {
    val lock = directoryLockFile
    val isLocked = lock.isLocked

    lock.unlock()
    return isLocked
}

/**
 * Lock a directory to prevent multiple instances of a program from running at the same time
 * @return the lock file if the directory was locked, null otherwise
 */
fun File.lockDirectory(): LockFile? {
    if(!isDirectory)
        return null

    val lockFile = directoryLockFile

    if(isDirectoryLocked || !lockFile.tryLock())
        return null

    return lockFile
}