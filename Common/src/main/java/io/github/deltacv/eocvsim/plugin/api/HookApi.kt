package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin

abstract class HookApi(owner: EOCVSimPlugin) : Api(owner) {
    typealias OnceHook = () -> Unit
    typealias PersistentHook = (Detacher) -> Unit

    abstract fun once(hook: OnceHook)
    abstract fun attach(hook: PersistentHook)

    operator fun invoke(hook: PersistentHook) = attach(hook)

    abstract fun runListeners()

    fun interface Detacher {
        fun detach()
    }
}