package com.github.serivesmejia.eocvsim.util

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import java.io.File
import java.util.*

class FileWatcher(private val watchingDirectories: List<File>,
                  watchingFileExtensions: List<String>?,
                  name: String) {

    private val TAG = "FileWatcher-$name"

    val onChange = EventHandler("OnChange-$TAG")

    private val watcherThread = Thread(
        Runner(watchingDirectories, watchingFileExtensions, onChange),
        TAG
    )

    fun init() {
        watcherThread.start()
    }

    fun stop() {
        watcherThread.interrupt()
    }

    private class Runner(val watchingDirectories: List<File>,
                         val fileExts: List<String>?,
                         val onChange: EventHandler) : Runnable {

        private val lastModifyDates = mutableMapOf<String, Long>()

        override fun run() {
            val TAG = Thread.currentThread().name!!

            val directoriesList = StringBuilder()
            for(directory in watchingDirectories) {
                directoriesList.appendLine(directory.absolutePath)
            }

            Log.info(TAG, "Starting to watch directories in:\n$directoriesList")

            while(!Thread.currentThread().isInterrupted) {
                var changeDetected = false

                for(directory in watchingDirectories) {
                    for(file in SysUtil.filesUnder(directory)) {
                        if(fileExts != null && !fileExts.stream().anyMatch { file.name.endsWith(".$it") })
                            continue

                        val path = file.absolutePath
                        val lastModified = file.lastModified()

                        if(lastModifyDates.containsKey(path) && lastModified > lastModifyDates[path]!! && !changeDetected) {
                            Log.info(TAG, "Change detected on ${directory.absolutePath}")

                            onChange.run()
                            changeDetected = true
                        }

                        lastModifyDates[path] = lastModified
                    }
                }

                Thread.sleep(800) //check every 800 ms
            }

            Log.info(TAG, "Stopping watching directories:\n$directoriesList")
        }

    }

}
