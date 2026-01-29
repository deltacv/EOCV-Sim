package io.github.deltacv.eocvsim.plugin

import io.github.deltacv.eocvsim.plugin.loader.PluginContext

/**
 * Base class for all EOCV-Sim plugins.
 *
 * An [EOCVSimPlugin] represents a single, loadable extension of the EOCV-Sim
 * runtime. Plugins are instantiated, loaded, enabled, and disabled by the
 * plugin loader according to the application lifecycle.
 *
 * Each plugin is associated with a [PluginContext], which provides controlled
 * access to loader-managed services such as the EOCV-Sim API, sandboxed
 * filesystem, classpath, and plugin metadata.
 */
@Suppress("UNUSED")
abstract class EOCVSimPlugin {

    /**
     * The [PluginContext] associated with this plugin.
     *
     * The context is the main entry point for accessing plugin-related services
     * such as the EOCV-Sim API, filesystem, and classpath. It is resolved lazily
     * and is available throughout the plugin lifecycle, including during plugin
     * instance construction.
     *
     * Plugin code should treat the context as read-only and must not attempt
     * to cache or replace it.
     */
    val context get() = PluginContext.current(this)

    /**
     * Provides access to the public EOCV-Sim API.
     *
     * This API exposes the main public hooks of the application environment.
     * It becomes available once the plugin has completed instantiation and
     * entered a valid lifecycle phase.
     *
     * Accessing this property during plugin construction may result in an
     * exception.
     */
    val eocvSimApi get() = context.eocvSimApi

    /**
     * The sandboxed filesystem assigned to this plugin.
     *
     * All file I/O performed by a plugin should go through this filesystem.
     * Without super access, the plugin is restricted to its own isolated
     * virtual filesystem and cannot access host or other plugin data.
     *
     * @see io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
     */
    val fileSystem get() = context.fileSystem

    /**
     * Indicates the source from which this plugin was loaded.
     *
     * The plugin source affects how the plugin is resolved, loaded, and
     * incorporated into the runtime classpath (for example, whether it was
     * loaded from a local file or a Maven repository).
     *
     * @see io.github.deltacv.eocvsim.plugin.loader.PluginSource
     */
    val pluginSource get() = context.pluginSource

    /**
     * The effective classpath associated with this plugin.
     *
     * This includes the plugin’s own classes as well as any additional
     * dependencies provided by the loader. Classes on this classpath are
     * visible to the plugin’s class loader but not necessarily to the
     * application as a whole.
     */
    val classpath get() = context.classpath

    /**
     * Indicates whether this plugin is currently enabled.
     *
     * A plugin is considered enabled after [onEnable] has been successfully
     * invoked and before [onDisable] is called.
     */
    val enabled get() = context.loader.enabled

    /**
     * Called when the plugin is loaded by the plugin loader.
     *
     * This method is invoked after the plugin instance has been created but
     * before it is enabled. Plugins should perform lightweight initialization
     * here, such as registering resources or validating configuration.
     */
    abstract fun onLoad()

    /**
     * Called when the plugin is enabled by the plugin loader.
     *
     * At this stage, the plugin is fully initialized and may safely interact
     * with the simulation environment, register hooks, and start background
     * work.
     */
    abstract fun onEnable()

    /**
     * Called when the plugin is disabled by the plugin loader.
     *
     * Plugins should release resources, unregister hooks, and stop any ongoing
     * work in this method. After this method returns, the plugin should be
     * considered inactive.
     */
    abstract fun onDisable()
}
