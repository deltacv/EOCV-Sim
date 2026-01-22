package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Api(val owner: EOCVSimPlugin){
    val ownerName: String get() = owner::class.java.simpleName

    var isDisabled: Boolean = false
        private set

    private var registeredApis = mutableListOf<Api>()

    fun throwIfDisabled() {
        if(isDisabled) {
            throw IllegalStateException("The API owned by $ownerName has been disabled and can no longer be used")
        }
    }

    fun throwIfOwnerMismatch(other: Api){
        if(other.owner != this@Api.owner) {
            throw IllegalAccessException("The API is owned by a different plugin (${other.ownerName}) than $ownerName")
        }
    }

    abstract fun disable()

    internal fun internalDisable() {
        if(isDisabled) return

        isDisabled = true
        disable()

        for(api in registeredApis) {
            api.internalDisable()
        }
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
                        thisRef.throwIfOwnerMismatch(v)
                        thisRef.registeredApis.add(v)
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
        protected fun <T : Any> apiField(lazy: Boolean = true, valueProvider: () -> T): ReadOnlyProperty<Api, T> =
            object : ReadOnlyProperty<Api, T> {
                private val delegate = nullableApiField(lazy, valueProvider)

                override fun getValue(thisRef: Api, property: KProperty<*>): T {
                    return delegate.getValue(thisRef, property)!!
                }
            }

        /** Convenience overloads for immediate values */
        @JvmStatic
        protected fun <T : Any> apiField(value: T): ReadOnlyProperty<Api, T> = apiField { value }
        @JvmStatic
        protected fun <T> nullableApiField(value: T?): ReadOnlyProperty<Api, T?> = nullableApiField { value }
    }
}