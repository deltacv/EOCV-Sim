package io.github.deltacv.eocvsim.plugin

import io.github.deltacv.eocvsim.plugin.loader.PluginContext

/**
 * Represents a plugin for EOCV-Sim
 */
@Suppress("UNUSED")
abstract class EOCVSimPlugin {

    /**
     * The context of the plugin, containing entry point objects
     */
    val context get() = PluginContext.current(this)

    /**
     * The EOCV-Sim instance, containing the main functionality of the program
     */
    val eocvSimApi get() = context.eocvSimApi

    /**
     * The virtual filesystem assigned to this plugin.
     * With this filesystem, the plugin can access files in a sandboxed environment.
     * Without SuperAccess, the plugin can only access files in its own virtual fs.
     * @see io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
     */
    val fileSystem get() = context.fileSystem

    /**
     * Whether this plugin comes from a maven repository or a file
     * This affects the classpath resolution and the way the plugin is loaded.
     * @see io.github.deltacv.eocvsim.plugin.loader.PluginSource
     */
    val pluginSource get() = context.pluginSource

    /**
     * The classpath of the plugin, additional to the JVM classpath
     * This classpath is used to load classes from the plugin jar file
     * and other dependencies that come from other jar files.
     */
    val classpath get() = context.classpath

    val enabled get() = context.loader.enabled

    /**
     * Called when the plugin is loaded by the PluginLoader
     */
    abstract fun onLoad()

    /**
     * Called when the plugin is enabled by the PluginLoader
     */
    abstract fun onEnable()

    /*
     * Called when the plugin is disabled by the PluginLoader
     */
    abstract fun onDisable()
}