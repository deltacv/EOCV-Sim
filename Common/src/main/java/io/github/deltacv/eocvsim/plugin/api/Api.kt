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
import io.github.deltacv.eocvsim.plugin.api.exception.EOCVSimApiException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Base class for all public EOCV-Sim APIs.
 *
 * Each API instance is owned by a single [EOCVSimPlugin] and becomes invalid
 * once the owning plugin is disabled. After that point, any interaction with
 * the API will throw an [EOCVSimApiException].
 *
 * APIs may create and expose other APIs. These are tracked automatically and
 * are disabled together with their parent.
 *
 * All public API members **must** be implemented using [apiImpl] (for methods)
 * and [apiField] / [nullableApiField] (for properties). These helpers enforce
 * lifecycle safety, ownership checks, and automatic child API tracking.
 *
 * Failing to use them will result in undefined behavior when the plugin is
 * disabled or unloaded.
 *
 * @param owner The plugin that owns this API instance
 */
abstract class Api(val owner: EOCVSimPlugin) {
    /**
     * Simple name of the owning plugin class, for error messages
     */
    val ownerName: String get() = owner::class.java.simpleName

    /**
     * Whether this API has been disabled.
     */
    var isDisabled: Boolean = false
        private set

    private var childrenApis = mutableListOf<Api>()

    /**
     * Throws if this API has been disabled.
     *
     * Called internally before executing any API logic.
     */
    protected fun throwIfDisabled() {
        if(isDisabled) {
            throw EOCVSimApiException(
                "The API owned by plugin '$ownerName' has been disabled and cannot be used anymore.",
                this
            )
        }
    }

    /**
     * Ensures another API belongs to the same plugin as this one.
     *
     * Used to prevent mixing APIs across plugin boundaries.
     */
    protected fun throwIfOwnerMismatch(other: Api) {
        if(other.owner != owner) {
            throw EOCVSimApiException(
                "Plugin '${ownerName}' attempted to use an API owned by a different plugin ('${other.ownerName}'). APIs cannot be shared across plugins.",
                this
            )
        }
    }

    /**
     * Called once when this API is being disabled.
     *
     * Implementations should release resources and invalidate internal state.
     */
    protected abstract fun disableApi()

    /**
     * Disables this API and all child APIs recursively.
     *
     * This is invoked internally by the plugin lifecycle and should not be
     * called directly by API implementations.
     * @see ApiDisabler
     */
    internal fun internalDisableApi() {
        if(isDisabled) return

        isDisabled = true
        disableApi()

        for(api in childrenApis) {
            api.internalDisableApi()
        }
    }

    /**
     * Registers child APIs created by this API.
     *
     * Child APIs must belong to the same plugin and will be disabled automatically
     * when this API is disabled.
     *
     * @param apis The child APIs to register
     * @see apiImpl
     */
    protected fun addChildrenApi(vararg apis: Api) {
        for(api in apis) {
            if(childrenApis.contains(api)) continue

            throwIfDisabled()
            throwIfOwnerMismatch(api)

            childrenApis.add(api)
        }
    }

    /**
     * Wraps a public API method implementation.
     *
     * Ensures this API and any passed APIs:
     * - belong to the same plugin
     * - are not disabled
     *
     * If the returned value is another [Api], it is automatically registered
     * as a child API.
     *
     * @param passedApis APIs to check for validity before executing the block
     * @param block The block of code representing the API implementation
     * @throws EOCVSimApiException if any API has an invalidity
     */
    protected fun <R> apiImpl(vararg passedApis: Api, block: () -> R): R {
        for(api in passedApis) {
            throwIfOwnerMismatch(api)
            api.throwIfDisabled()
        }
        throwIfDisabled()

        val returnedValue = block()
        if(returnedValue is Api) {
            throwIfOwnerMismatch(returnedValue)
            addChildrenApi(returnedValue)
        }

        return returnedValue
    }

    /**
     * Variant of [apiImpl] that fails silently.
     *
     * Any [EOCVSimApiException] thrown during execution is swallowed and `null`
     * is returned instead.
     *
     * Intended for APIs where failure is optional or expected.
     */
    protected fun <R> safeApiImpl(vararg passedApis: Api, block: () -> R?): R? {
        return try {
            apiImpl(*passedApis, block = block)
        } catch(_: EOCVSimApiException) {
            // ignore
            null
        }
    }

    companion object {
        /**
         * Delegate for nullable API-backed fields.
         *
         * Handles:
         * - disabled-state checks
         * - optional lazy initialization
         * - automatic child API registration
         *
         * If the produced value is an [Api], it is registered exactly once.
         * - Default: If lazy is true, the value provider is only called once and the value is cached.
         * - If lazy is false, the value provider is called on each access, but API registration only happens once.
         *
         * @param lazy Whether to cache the value after the first access
         * @param valueProvider The function that provides the value
         */
        @JvmStatic
        protected fun <T> nullableApiField(
            lazy: Boolean = true,
            valueProvider: () -> T?
        ): ReadOnlyProperty<Api, T?> = object : ReadOnlyProperty<Api, T?> {
            private var initialized = false
            private var _cachedValue: T? = null

            override fun getValue(thisRef: Api, property: KProperty<*>): T? {
                thisRef.throwIfDisabled()

                // compute the value either from cache or provider
                val value: T? = if (lazy && initialized) {
                    _cachedValue
                } else {
                    val v = valueProvider()

                    // register if it's an Api
                    if (v is Api && !initialized) {
                        thisRef.addChildrenApi(v)
                    }

                    if (lazy) {
                        _cachedValue = v
                        initialized = true
                    } else if (v is Api) {
                        initialized = true // ensure API registration only happens once even in non-lazy mode
                    }

                    v
                }

                return value
            }
        }

        /**
         * Non-nullable variant of [nullableApiField].
         *
         * The value provider must never return null.
         */
        @JvmStatic
        protected fun <T> apiField(lazy: Boolean = true, valueProvider: () -> T): ReadOnlyProperty<Api, T> =
            object : ReadOnlyProperty<Api, T> {
                private val delegate = nullableApiField(lazy, valueProvider)

                override fun getValue(thisRef: Api, property: KProperty<*>): T {
                    return delegate.getValue(thisRef, property)!!
                }
            }

        /**
         * Like [apiField], but recomputes the value on every access.
         */
        @JvmStatic
        protected fun <T> liveApiField(valueProvider: () -> T) = apiField(lazy = false, valueProvider)

        /**
         * Like [nullableApiField], but recomputes the value on every access.
         */
        @JvmStatic
        protected fun <T> liveNullableApiField(valueProvider: () -> T?) = nullableApiField(lazy = false, valueProvider)

        /**
         * API field backed by a constant value.
         */
        @JvmStatic
        protected fun <T> apiField(value: T): ReadOnlyProperty<Api, T> = apiField { value }

        /**
         * Nullable API field backed by a constant value.
         */
        @JvmStatic
        protected fun <T> nullableApiField(value: T?): ReadOnlyProperty<Api, T?> = nullableApiField { value }
    }
}