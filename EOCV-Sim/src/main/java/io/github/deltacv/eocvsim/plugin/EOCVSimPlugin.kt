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

package io.github.deltacv.eocvsim.plugin

import io.github.deltacv.eocvsim.plugin.loader.PluginContext

/**
 * Represents a plugin for EOCV-Sim
 */
abstract class EOCVSimPlugin {

    /**
     * The context of the plugin, containing entry point objects
     */
    val context get() = PluginContext.current(this)!!

    /**
     * The EOCV-Sim instance, containing the main functionality of the program
     */
    val eocvSim get() = context.eocvSim

    /**
     * The virtual filesystem assigned to this plugin.
     * With this filesystem, the plugin can access files in a sandboxed environment.
     * Without SuperAccess, the plugin can only access files in its own virtual fs.
     * @see SandboxFileSystem
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
     * @see io.github.deltacv.eocvsim.plugin.loader.PluginClassLoader
     */
    val classpath get() = context.classpath

    var enabled = false
        internal set

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