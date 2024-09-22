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
import io.github.deltacv.eocvsim.plugin.loader.PluginLoader
import java.nio.file.*
import java.nio.file.attribute.FileAttribute

class SandboxFileSystem(loader: PluginLoader) : FileSystem() {

    val parent = FileSystems.newFileSystem(loader.fileSystemZipPath, null)

    val logger by loggerForThis()

    init {
        logger.info("Loading filesystem ${loader.hash()}")
        Runtime.getRuntime().addShutdownHook(Thread {
            if(isOpen) {
                logger.info("Unloading filesystem ${loader.hash()} on shutdown")
                close()
            }
        })
    }

    private fun checkForPath(path: Path) = Files.exists(path)

    override fun close() {
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
        checkIfPathIsSandboxed(path)
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

    fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
        checkIfPathIsSandboxed(dir)
        Files.createDirectory(dir, *attrs)
    }

    fun createDirectories(dir: Path, vararg attrs: FileAttribute<*>) {
        checkIfPathIsSandboxed(dir)
        Files.createDirectories(dir, *attrs)
    }

}