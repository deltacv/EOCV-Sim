/*
 * Copyright (c) 2026 Sebastian Erives
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

package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin

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