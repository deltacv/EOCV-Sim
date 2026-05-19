/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.api

import org.deltacv.eocvsim.plugin.EOCVSimPlugin

/**
 * Base API for managing plugin configuration flags.
 *
 * A configuration flag represents a boolean, named toggle that can be persisted
 * and queried by the plugin. Implementations decide how and where flags are
 * stored (for example, in files, memory, or external sources).
 *
 * This API follows the standard [Api] lifecycle rules:
 *
 * - All public method implementations **must** be wrapped using [apiImpl]
 * - Calls made after the owning plugin is disabled will throw an exception
 *
 * Implementations are responsible for defining the persistence and lookup
 * behavior of flags, but must ensure consistent results within a plugin’s
 * lifecycle.
 *
 * @param owner the plugin that owns this API instance
 */
abstract class ConfigApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Enables or sets the given configuration flag.
     *
     * If the flag already exists, this operation must be idempotent.
     *
     * @param flag the name of the flag to set
     */
    abstract fun putFlag(flag: String)

    /**
     * Clears or disables the given configuration flag.
     *
     * If the flag does not exist, this operation must be a no-op.
     *
     * @param flag the name of the flag to clear
     */
    abstract fun clearFlag(flag: String)

    /**
     * Checks whether the given configuration flag is currently set.
     *
     * @param flag the name of the flag to query
     * @return `true` if the flag is set, `false` otherwise
     */
    abstract fun hasFlag(flag: String): Boolean
}
