package io.github.deltacv.eocvsim.plugin.loader

import java.io.File

abstract class FilePluginLoader : PluginLoader() {
    abstract val pluginFile: File
}