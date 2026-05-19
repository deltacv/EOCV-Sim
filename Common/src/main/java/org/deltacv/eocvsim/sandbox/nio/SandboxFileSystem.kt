/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.sandbox.nio

import org.deltacv.common.util.loggerForThis
import java.nio.file.*
import java.nio.file.attribute.FileAttribute

class SandboxFileSystem(
    val path: Path,
    val hash: String
) : FileSystem() {

    val parent = FileSystems.newFileSystem(path)

    val logger by loggerForThis()

    init {
        logger.info("Loading filesystem $hash")

        Runtime.getRuntime().addShutdownHook(Thread {
            if(isOpen) {
                logger.info("SHUTDOWN - Unloading filesystem $hash")
                close()
            }
        })
    }

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

    override fun getPath(first: String, vararg more: String?) = parent.getPath(first, *more).apply { checkSandboxed(this) }

    override fun getPathMatcher(syntaxAndPattern: String?) = parent.getPathMatcher(syntaxAndPattern)

    override fun getUserPrincipalLookupService() = parent.userPrincipalLookupService

    override fun newWatchService() = parent.newWatchService()

    private val sandboxRoot: Path =
        parent.getPath("/").toRealPath(LinkOption.NOFOLLOW_LINKS)

    private fun checkSandboxed(path: Path) {
        // 1. Must belong to the sandbox filesystem
        if (path.fileSystem !== parent) {
            throw IllegalArgumentException(
                "Path at filesystem ${path.fileSystem} is outside of the plugin sandbox"
            )
        }

        // 2. Resolve against sandbox root and normalize (prevents ../ traversal)
        val resolved = sandboxRoot.resolve(path).normalize()

        // 3. Resolve symlinks WITHOUT following them first
        val realPath = try {
            resolved.toRealPath(LinkOption.NOFOLLOW_LINKS)
        } catch (e: NoSuchFileException) {
            // Allow non-existing paths (e.g. createFile),
            // but still validate containment
            resolved
        }

        // 4. Enforce containment
        if (!realPath.startsWith(sandboxRoot)) {
            throw SecurityException("Path escapes sandbox root: $path")
        }
    }

    fun exists(path: Path, vararg options: LinkOption): Boolean {
        checkSandboxed(path)
        return Files.exists(path, *options)
    }

    fun createFile(path: Path, vararg attrs: FileAttribute<*>): Path {
        checkSandboxed(path)
        return Files.createFile(path, *attrs)
    }

    fun delete(path: Path) {
        checkSandboxed(path)
        Files.delete(path)
    }

    fun copy(source: Path, target: Path, vararg options: CopyOption): Path {
        checkSandboxed(source)
        checkSandboxed(target)
        return Files.copy(source, target, *options)
    }

    fun move(source: Path, target: Path, vararg options: CopyOption): Path {
        checkSandboxed(source)
        checkSandboxed(target)
        return Files.move(source, target, *options)
    }

    fun readAllBytes(path: Path): ByteArray {
        checkSandboxed(path)
        return Files.readAllBytes(path)
    }

    fun write(path: Path, bytes: ByteArray, vararg options: OpenOption): Path {
        checkSandboxed(path)
        return Files.write(path, bytes, *options)
    }

    fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
        checkSandboxed(dir)
        Files.createDirectory(dir, *attrs)
    }

    fun createDirectories(dir: Path, vararg attrs: FileAttribute<*>) {
        checkSandboxed(dir)
        Files.createDirectories(dir, *attrs)
    }

}
