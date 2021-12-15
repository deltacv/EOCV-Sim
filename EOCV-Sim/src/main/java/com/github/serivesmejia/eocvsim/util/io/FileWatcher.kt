/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.util.io

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.Log
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

                Thread.sleep(1200) //check every 800 ms
            }

            Log.info(TAG, "Stopping watching directories:\n$directoriesList")
        }

    }

}
