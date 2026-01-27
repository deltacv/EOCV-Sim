package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Api(val owner: EOCVSimPlugin){
    val ownerName: String get() = owner::class.java.simpleName

    var isDisabled: Boolean = false
        private set

    private var childrenApis = mutableListOf<Api>()

    protected fun throwIfDisabled() {
        if(isDisabled) {
            throw IllegalStateException("The API owned by $ownerName has been disabled and can no longer be used")
        }
    }

    protected fun throwIfOwnerMismatch(other: Api){
        if(other.owner != this@Api.owner) {
            throw IllegalAccessException("The API is owned by a different plugin (${other.ownerName}) than $ownerName")
        }
    }

    protected abstract fun disableApi()

    internal fun internalDisableApi() {
        if(isDisabled) return

        isDisabled = true
        disableApi()

        for(api in childrenApis) {
            api.internalDisableApi()
        }
    }

    protected fun addChildrenApi(vararg apis: Api) {
        for(api in apis) {
            if(childrenApis.contains(api)) continue

            throwIfDisabled()
            throwIfOwnerMismatch(api)

            childrenApis.add(api)
        }
    }

    /**
     * Helper to wrap API implementations that need to check for disabled state
     * Meant to be used for every public API method implementation, for consistency
     * @throws IllegalStateException if the API is disabled
     */
    protected fun <R> apiImpl(vararg passedApis: Api, block: () -> R): R {
        for(api in passedApis) {
            throwIfOwnerMismatch(api)
        }
        throwIfDisabled()

        val returnedValue = block()
        if(returnedValue is Api) {
            throwIfOwnerMismatch(returnedValue)
            addChildrenApi(returnedValue)
        }

        return returnedValue
    }

    companion object {
        /**
         * Base delegate for nullable values. Handles lazy init and API registration
         *
         * - Default: If lazy is true, the value provider is only called once and the value is cached.
         * - If lazy is false, the value provider is called on each access, but API registration only happens once.
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
         * Non-nullable variant, unwraps nullable base safely
         *
         * - Default: If lazy is true, the value provider is only called once and the value is cached.
         * - If lazy is false, the value provider is called on each access, but API registration only happens once.
         * */
        @JvmStatic
        protected fun <T> apiField(lazy: Boolean = true, valueProvider: () -> T): ReadOnlyProperty<Api, T> =
            object : ReadOnlyProperty<Api, T> {
                private val delegate = nullableApiField(lazy, valueProvider)

                override fun getValue(thisRef: Api, property: KProperty<*>): T {
                    return delegate.getValue(thisRef, property)!!
                }
            }

        /**
         * apiField with lazy = false, for fields that are recomputed on each access
         * (i.e. values that need to be recomputed on each access)
         */
        @JvmStatic
        protected fun <T> liveApiField(valueProvider: () -> T) = apiField(lazy = false, valueProvider)

        /**
         * nullableApiField with lazy = false, for fields that are recomputed on each access
         */
        @JvmStatic
        protected fun <T> liveNullableApiField(valueProvider: () -> T?) = nullableApiField(lazy = false, valueProvider)

        /**
         * apiField with a constant value
         */
        @JvmStatic
        protected fun <T> apiField(value: T): ReadOnlyProperty<Api, T> = apiField { value }
        /**
         * nullableApiField with a constant value
         */
        @JvmStatic
        protected fun <T> nullableApiField(value: T?): ReadOnlyProperty<Api, T?> = nullableApiField { value }
    }
}