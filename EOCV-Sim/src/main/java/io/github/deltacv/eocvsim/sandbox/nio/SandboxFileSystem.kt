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
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Feature
import com.google.common.jimfs.Jimfs
import com.google.common.jimfs.PathType
import io.github.deltacv.eocvsim.plugin.loader.PluginLoader
import java.io.FileNotFoundException
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.util.zip.ZipFile

class SandboxFileSystem(loader: PluginLoader) : FileSystem() {

    val parent = Jimfs.newFileSystem(
        Configuration.builder(PathType.unix())
            .setRoots("/")
            .setWorkingDirectory("/")
            .setAttributeViews("basic")
            .setSupportedFeatures(Feature.SECURE_DIRECTORY_STREAM, Feature.FILE_CHANNEL)
            .build()
    )
    private val jimfsWatcher = JimfsWatcher(parent, loader.fileSystemZipPath)

    private val zipFile = ZipFile(loader.fileSystemZipPath.toFile())

    val logger by loggerForThis()

    init {
        logger.info("Loading filesystem ${loader.hash()}")
    }

    private fun checkForPath(path: Path): Boolean {
        return if (Files.exists(path)) {
            true
        } else {
            // Attempt to load the file if it doesn't exist
            try {
                loadFileToJimfs(path)
            } catch(_: FileNotFoundException) {}

            true
        }
    }

    private fun loadFileToJimfs(path: Path) {
        // Convert the path to a string relative to the root
        val zipEntryPath = convertPathToZipEntryPath(path)
        val zipEntry = zipFile.getEntry(zipEntryPath) ?: throw FileNotFoundException("File not found in ZIP: $zipEntryPath")

        // Ensure the parent directories exist in Jimfs before writing the file
        val parentDir = path.parent
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir)
        }

        zipFile.getInputStream(zipEntry).use { inputStream ->
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun convertPathToZipEntryPath(path: Path): String {
        // Ensure the path is normalized and create a relative path for ZIP entries
        val normalizedPath = path.normalize()
        // Convert the path to a string with a leading directory (e.g., "work/")
        return normalizedPath.toString().replace("\\", "/")
    }

    override fun close() {
        jimfsWatcher.stop()
        parent.close()
    }

    override fun provider() = parent.provider()

    override fun isOpen() = parent.isOpen

    override fun isReadOnly() = parent.isReadOnly

    override fun getSeparator() = parent.separator

    override fun getRootDirectories() = parent.rootDirectories

    override fun getFileStores() = parent.fileStores

    override fun supportedFileAttributeViews() = parent.supportedFileAttributeViews()

    override fun getPath(first: String, vararg more: String?) = parent.getPath(first, *more).apply { checkForPath(this) }

    override fun getPathMatcher(syntaxAndPattern: String?) = parent.getPathMatcher(syntaxAndPattern)

    override fun getUserPrincipalLookupService() = parent.userPrincipalLookupService

    override fun newWatchService() = parent.newWatchService()

    private fun checkIfPathIsSandboxed(path: Path) {
        if(path.fileSystem != parent)
            throw IllegalArgumentException("Path at filesystem ${path.fileSystem} is outside of the plugin sandbox")

        checkForPath(path)
    }

    fun exists(path: Path, vararg options: LinkOption): Boolean {
        checkForPath(path)
        return Files.exists(path, *options)
    }

    fun createFile(path: Path, vararg attrs: FileAttribute<*>): Path {
        checkIfPathIsSandboxed(path)
        return Files.createFile(path, *attrs)
    }

    fun delete(path: Path) {
        checkIfPathIsSandboxed(path)
        Files.delete(path)
    }

    fun copy(source: Path, target: Path, vararg options: CopyOption): Path {
        checkIfPathIsSandboxed(source)
        checkIfPathIsSandboxed(target)
        return Files.copy(source, target, *options)
    }

    fun move(source: Path, target: Path, vararg options: CopyOption): Path {
        checkIfPathIsSandboxed(source)
        checkIfPathIsSandboxed(target)
        return Files.move(source, target, *options)
    }

    fun readAllBytes(path: Path): ByteArray {
        checkIfPathIsSandboxed(path)
        return Files.readAllBytes(path)
    }

    fun write(path: Path, bytes: ByteArray, vararg options: OpenOption): Path {
        checkIfPathIsSandboxed(path)
        return Files.write(path, bytes, *options)
    }

}