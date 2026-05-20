/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.loader

import java.io.File

/**
 * Base [PluginLoader] implementation for plugins loaded from a file.
 *
 * This loader variant is backed by a single plugin file, typically a JAR,
 * and is responsible for resolving the plugin classpath and metadata from it.
 */
abstract class FilePluginLoader : PluginLoader() {

    /**
     * The file from which the plugin is loaded.
     *
     * This is usually the plugin JAR, but may represent any file-based
     * plugin source supported by the loader implementation.
     */
    abstract val pluginFile: File
}
