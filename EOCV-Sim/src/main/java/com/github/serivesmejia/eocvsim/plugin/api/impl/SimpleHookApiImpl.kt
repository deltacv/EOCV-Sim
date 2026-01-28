package com.github.serivesmejia.eocvsim.plugin.api.impl

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.HookApi

class SimpleHookApiImpl(owner: EOCVSimPlugin) : HookApi(owner) {

    private val onceHooks = mutableListOf<OnceHook>()
    private val persistentHooks = mutableListOf<PersistentHook>()

    override fun once(hook: OnceHook) = apiImpl {
        onceHooks += hook
    }

    override fun attach(hook: PersistentHook) = apiImpl {
        persistentHooks += hook
    }

    override fun runListeners() = apiImpl {
        // run & clear once hooks
        val toRunOnce = onceHooks.toList()
        onceHooks.clear()
        toRunOnce.forEach { it() }

        // run persistent hooks
        val iterator = persistentHooks.iterator()
        while (iterator.hasNext()) {
            val hook = iterator.next()
            hook {
                iterator.remove() // allow self-removal
            }
        }
    }

    override fun disableApi() {
        onceHooks.clear()
        persistentHooks.clear()
    }
}
