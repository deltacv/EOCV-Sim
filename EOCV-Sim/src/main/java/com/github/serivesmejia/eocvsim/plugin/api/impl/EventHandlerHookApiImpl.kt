package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.event.EventListener
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.HookApi
import java.lang.ref.WeakReference

class EventHandlerHookApiImpl(owner: EOCVSimPlugin, val eventHandler: EventHandler) : HookApi(owner) {
    private var allOnceListeners = mutableListOf<WeakReference<EventListener>>()
    private var allPersistentListeners = mutableListOf<WeakReference<EventListener>>()

    override fun once(hook: OnceHook) {
        throwIfDisabled()

        val listener = EventListener {
            hook()
        }

        allOnceListeners.add(WeakReference(listener))
        eventHandler.doOnce(listener)
    }

    override fun attach(hook: PersistentHook) {
        throwIfDisabled()

        val listener = EventListener {
            hook.invoke(it::removeThis) // pass remover function to hook
        }

        allPersistentListeners.add(WeakReference(listener))
        eventHandler.doPersistent(listener)
    }

    override fun disable() {
        for(listenerRef in allOnceListeners) {
            val listener = listenerRef.get() ?: continue
            eventHandler.removeOnceListener(listener)
        }
        for(listenerRef in allPersistentListeners) {
            val listener = listenerRef.get() ?: continue
            eventHandler.removeOnceListener(listener)
        }
    }
}