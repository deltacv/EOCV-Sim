package com.github.serivesmejia.eocvsim.util.io

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.common.util.loggerOf
import org.slf4j.Logger
import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Class to watch for changes in files in a directory
 * @param watchingDirectories the directories to watch
 * @param watchingFileExtensions the file extensions to watch, if null, all files will be watched
 * @param name the name of the file watcher (for logging purposes)
 */
class FileWatcher(
    watchingDirectories: List<File>,
    watchingFileExtensions: List<String>?,
    name: String
) {

    private val TAG = "FileWatcher-$name"

    /**
     * Event handler for when a change is detected
     */
    val onChange = EventHandler("OnChange-$TAG")

    val logger by loggerOf(TAG)

    // Executor owns the execution, not us
    private val executor: Executor = Executors.newSingleThreadExecutor {
        Thread(it, TAG).apply { isDaemon = true }
    }

    private val runner = Runner(
        watchingDirectories,
        watchingFileExtensions,
        onChange,
        logger
    )

    /**
     * Start the file watcher (async)
     */
    fun init() {
        executor.execute(runner)
    }

    /**
     * Stop the file watcher
     */
    fun stop() {
        runner.stop()
    }

    // ======================================================================

    private class Runner(
        watchingDirectories: List<File>,
        watchingFileExtensions: List<String>?,
        val onChange: EventHandler,
        val logger: Logger
    ) : Runnable {

        private val running = AtomicBoolean(true)
        private val watchService = FileSystems.getDefault().newWatchService()

        private val extensionSet = watchingFileExtensions
            ?.map { it.lowercase() }
            ?.toSet()

        private val roots = watchingDirectories
            .map { it.toPath().toAbsolutePath().normalize() }

        override fun run() {
            val dirList = roots.joinToString("\n") { it.toString() }
            logger.info("Starting to watch directories in:\n$dirList")

            try {
                roots.forEach { registerRecursively(it) }

                while (running.get()) {
                    val key = try {
                        watchService.take()
                    } catch (e: ClosedWatchServiceException) {
                        break
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }

                    val dir = key.watchable() as Path
                    var changeDetected = false

                    for (event in key.pollEvents()) {
                        val kind = event.kind()
                        if (kind == OVERFLOW) continue

                        val relative = event.context() as Path
                        val fullPath = dir.resolve(relative)

                        // Register new subdirectories on the fly
                        if (kind == ENTRY_CREATE && Files.isDirectory(fullPath)) {
                            registerRecursively(fullPath)
                        }

                        if (!matchesExtension(fullPath)) continue

                        if (!changeDetected) {
                            logger.info("Change detected in $fullPath")
                            onChange.run()
                            changeDetected = true
                        }
                    }

                    if (!key.reset()) {
                        logger.warn("WatchKey no longer valid for $dir")
                    }
                }
            } finally {
                try {
                    watchService.close()
                } catch (_: Exception) {
                }

                logger.info("Stopped watching directories:\n$dirList")
            }
        }

        fun stop() {
            running.set(false)
            try {
                watchService.close()
            } catch (_: Exception) {
            }
        }

        // ------------------------------------------------------------------

        private fun registerRecursively(root: Path) {
            if (!Files.isDirectory(root)) return

            Files.walk(root).use { stream ->
                stream
                    .filter { Files.isDirectory(it) }
                    .forEach { dir ->
                        try {
                            dir.register(
                                watchService,
                                ENTRY_CREATE,
                                ENTRY_MODIFY,
                                ENTRY_DELETE
                            )
                        } catch (e: Exception) {
                            logger.warn("Failed to register directory: $dir", e)
                        }
                    }
            }
        }

        private fun matchesExtension(path: Path): Boolean {
            val set = extensionSet ?: return true
            val name = path.fileName?.toString() ?: return false
            val dot = name.lastIndexOf('.')
            if (dot == -1) return false
            return name.substring(dot + 1).lowercase() in set
        }
    }
}