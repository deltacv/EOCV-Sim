/*
 * Copyright (c) 2024 Sebastian Erives
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

package io.github.deltacv.eocvsim.sandbox.nio

import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.io.*
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class JimfsWatcher(private val jimfs: FileSystem, private val zipFilePath: Path) {

    private val knownFiles = ConcurrentHashMap<Path, FileTime>()
    private val executor = Executors.newSingleThreadScheduledExecutor()

    val logger by loggerForThis()

    init {
        startPolling()
    }

    private fun startPolling() {
        logger.info("Starting for $zipFilePath ...")

        executor.scheduleAtFixedRate({
            try {
                checkForChanges()
            } catch (e: IOException) {
                logger.warn("IO Exception in executor", e)
            }
        }, 0, 3, TimeUnit.SECONDS) // Poll every 3 seconds
    }

    fun stop() {
        executor.shutdown()
    }

    @Throws(IOException::class)
    private fun checkForChanges() {
        val root = jimfs.getPath("/") // or specify the root path if different
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (Files.isRegularFile(file)) {
                    val lastModifiedTime = Files.getLastModifiedTime(file)
                    val previousTime = knownFiles[file]

                    if (previousTime == null || !lastModifiedTime.equals(previousTime)) {
                        knownFiles[file] = lastModifiedTime
                        copyFileToZip(file, root.relativize(file).toString())
                    }
                }
                return FileVisitResult.CONTINUE
            }
        })
    }

    @Throws(IOException::class)
    private fun copyFileToZip(virtualFile: Path, entryName: String) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFilePath.toFile(), true))).use { zos ->
            // Create a new entry in the ZIP file
            zos.putNextEntry(ZipEntry(entryName))

            // Copy the file content
            Files.copy(virtualFile, zos)

            // Close the current entry
            zos.closeEntry()
        }
    }
}