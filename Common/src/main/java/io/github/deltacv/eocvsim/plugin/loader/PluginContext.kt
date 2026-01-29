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
@file:Suppress("unused")

package io.github.deltacv.eocvsim.plugin.loader

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.loader.PluginContext.Companion.clearContext
import io.github.deltacv.eocvsim.plugin.loader.PluginContext.Companion.pushContext
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import java.util.*

/**
 * Provides controlled access to loader-owned runtime services for an
 * [EOCVSimPlugin].
 *
 * `PluginContext` exists to decouple plugin code from the internal structure
 * of the plugin loader. It acts as the single, stable entry point through which
 * a plugin can access services such as the sandboxed filesystem, plugin
 * metadata, classpath information, and privileged loader operations, without
 * exposing or leaking the loader implementation itself.
 *
 * This indirection is especially important during plugin instantiation.
 * While an [EOCVSimPlugin] instance is being constructed, the plugin is not yet
 * fully registered with the loader, but may still needs access to
 * loader-managed resources. `PluginContext` provides that access in a safe and
 * lifecycle-aware manner.
 *
 * While most plugins are loaded using a dedicated class loader that implements
 * [PluginContextHolder], this is not a strict requirement. Plugins may be
 * instantiated by alternative loaders or, in some cases, by the application
 * class loader itself. As a result, context resolution cannot rely exclusively
 * on class loader ownership.
 *
 * Plugin authors should treat `PluginContext` as an immutable, read-only view
 * of the loader environment and must not attempt to store or manage contexts
 * manually.
 */
class PluginContext

/**
 * Creates a new [PluginContext] bound to the given [PluginLoader].
 *
 * This constructor is intended to be used exclusively by the plugin loader.
 * A `PluginContext` represents a loader-owned view of the runtime environment
 * for a single plugin and must not be instantiated directly by plugin code.
 *
 * @param loader the loader responsible for managing the lifecycle and
 *               execution environment of the plugin
 */
constructor(
    val loader: PluginLoader
) {
    companion object {
        private val currentCache = Collections.synchronizedMap(WeakHashMap<EOCVSimPlugin, PluginContext>())
        private val contextHolder = ThreadLocal<PluginContext?>()

        /**
         * Resolves the [PluginContext] for the given plugin instance.
         *
         * ## Context resolution strategy
         *
         * A `PluginContext` is resolved using the first applicable mechanism below,
         * in order of precedence:
         *
         * 1. **Cached plugin association**
         *    Once a plugin instance has been created, the resolved context is cached
         *    using weak references to avoid repeated resolution and to allow garbage
         *    collection when a plugin is unloaded.
         *
         * 2. **ClassLoader-provided context**
         *    If the plugin’s defining class loader implements [PluginContextHolder],
         *    the context is resolved directly from it. This is the preferred and most
         *    efficient resolution path during normal runtime execution.
         *
         * 3. **Thread-local context (highest priority)**
         *    During plugin instantiation, the loader temporarily binds the active
         *    `PluginContext` to the current thread. This guarantees that constructor
         *    code and field initializers can access loader services even when no class
         *    loader association exists.
         *
         * The thread-local mechanism is therefore not merely a bootstrap convenience,
         * but a required fallback for plugins that are not loaded through a
         * `PluginContextHolder`-aware class loader.
         *
         * If no context can be resolved, this method throws an
         * [IllegalStateException], which typically indicates that the
         * plugin is being accessed outside of a valid lifecycle phase.
         *
         * @param plugin the plugin instance whose context is requested
         * @return the resolved {@link PluginContext}
         * @throws IllegalStateException if no context can be found
         */
        @JvmStatic
        fun current(plugin: EOCVSimPlugin): PluginContext {
            // 1. Cached fallback (optimization only)
            val cached = currentCache[plugin]
            if (cached != null) {
                return cached
            }

            // 2. Authoritative: plugin classloader
            val cl = plugin.javaClass.classLoader
            if (cl is PluginContextHolder) {
                val ctx = cl.pluginContext
                currentCache[plugin] = ctx
                return ctx
            }

            // 3. Construction-time: thread-local
            val localCtx = contextHolder.get()
            if (localCtx != null && localCtx.loader.pluginClass == plugin::class.java) {
                currentCache[plugin] = localCtx
                return localCtx
            }

            // 4. Hard failure
            throw IllegalStateException(
                "Could not find PluginContext for plugin ${plugin::class.java.name}. " +
                        "Make sure the plugin is loaded properly."
            )
        }

        /**
         * Pushes a [PluginContext] into thread-local storage.
         *
         * This method is intended to be invoked by the plugin loader **before**
         * an [EOCVSimPlugin] instance is created. It allows code executed during:
         *
         * - plugin construction
         * - early lifecycle execution
         *
         * to resolve a [PluginContext] even though the plugin instance does not yet
         * exist.
         *
         * The context is scoped to the current thread only and is not globally visible.
         * It **must** be cleared once plugin construction is complete to avoid leaking
         * loader state across plugin boundaries.
         *
         * @param ctx the [PluginContext] to associate with the current thread
         * @see clearContext
         */
        fun pushContext(ctx: PluginContext) {
            contextHolder.set(ctx)
        }

        /**
         * Clears the thread-local [PluginContext].
         *
         * When a plugin instance is supplied, this method only clears the context if the
         * currently bound thread-local context was created for that plugin’s class.
         * This guard exists to protect against concurrent plugin loading, which is not
         * expected under a correct lifecycle but may still occur due to misuse or
         * non-standard loaders.
         *
         * If the context matches, it is first promoted into the internal context cache,
         * making it resolvable through [current] without relying on thread-local state.
         * The thread-local reference is then removed.
         *
         * When `plugin` is `null`, the thread-local context is cleared unconditionally,
         * bypassing all ownership checks and cache promotion.
         *
         * @param plugin the plugin instance whose context should be finalized, or `null`
         *               to forcibly clear the current thread-local context
         * @see pushContext
         */
        fun clearContext(plugin: EOCVSimPlugin? = null) {
            if(plugin != null) {
                val ctx = contextHolder.get()
                if(ctx != null && ctx.loader.pluginClass == plugin::class.java) {
                    current(plugin) // push to cache now that the plugin instance is available
                    contextHolder.remove() // forget thread local
                }
            } else {
                contextHolder.remove() // don't care, just remove
            }
        }
    }

    /**
     * Provides access to the EOCV-Sim public API for this plugin.
     *
     * This property is initialized lazily and is only available once the plugin
     * has completed its instantiation phase and entered a valid lifecycle state.
     *
     * Attempting to access this API while the plugin is still being constructed
     * (for example, from a constructor or field initializer) will result in an
     * [IllegalAccessException]. This restriction exists to prevent plugins from
     * interacting with the simulation environment before the loader has fully
     * initialized its internal state.
     *
     * Plugins should access this property from lifecycle callbacks such as
     * `onLoad()` or later.
     *
     * @throws IllegalAccessException if accessed before the API is available
     */
    val eocvSimApi by lazy {
        loader.eocvSimApi ?: throw IllegalAccessException(
            "Tried to access the EOCV-Sim API while the plugin was being instantiated. Try accessing it later in the plugin lifecycle."
        )
    }

    /**
     * Returns the sandboxed filesystem assigned to this plugin.
     *
     * The returned [SandboxFileSystem] provides controlled access to the host
     * filesystem, enforcing isolation and permission boundaries defined by the
     * loader. All file I/O performed by a plugin should be done through this
     * filesystem rather than directly accessing the host environment.
     */
    val fileSystem: SandboxFileSystem get() = loader.fileSystem

    /**
     * Returns the plugin instance associated with this context.
     *
     * This reference is guaranteed to be non-null once the plugin has completed
     * instantiation. During early construction phases, access to the plugin
     * instance itself should be avoided in favor of using the surrounding
     * [PluginContext].
     */
    val plugin get() = loader.plugin

    /**
     * Returns the source from which this plugin was loaded.
     */
    val pluginSource get() = loader.pluginSource

    /**
     * Returns the effective classpath associated with this plugin.
     *
     * This includes the plugin’s own classes as well as any dependencies made
     * available by the loader. The returned value reflects the classpath as seen
     * by the plugin’s class loader.
     */
    val classpath get() = loader.classpath

    /**
     * Indicates whether this plugin has been granted elevated (super) access.
     *
     * Super access allows a plugin to bypass all sandbox restrictions and
     * interact more directly with the running application. This capability is
     * intentionally guarded and should be used sparingly.
     */
    val hasSuperAccess get() = loader.hasSuperAccess

    /**
     * Requests elevated (super) access for this plugin.
     *
     * The provided `reason` is recorded by the loader and may be surfaced to the
     * user. Granting super access is at the discretion of the user prompt and
     * the loader, and is not guaranteed.
     *
     * Plugins should only request super access when strictly necessary, as it
     * weakens isolation guarantees.
     *
     * @param reason a human-readable explanation of why elevated access is required
     */
    fun requestSuperAccess(reason: String) = loader.requestSuperAccess(reason)
}