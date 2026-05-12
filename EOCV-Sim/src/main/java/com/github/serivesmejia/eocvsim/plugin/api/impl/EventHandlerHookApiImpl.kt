/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.event.EventListener
import com.github.serivesmejia.eocvsim.util.event.EventListenerId
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.HookApi
import java.lang.ref.WeakReference

class EventHandlerHookApiImpl(owner: EOCVSimPlugin, val eventHandler: EventHandler) : HookApi(owner) {
    private var listeners = mutableListOf<EventListenerId>()

    override fun once(hook: OnceHook) = apiImpl {
        val id = eventHandler.once { hook() }
        listeners.add(id)
        Unit
    }

    override fun attach(hook: PersistentHook) = apiImpl {
        val id = eventHandler {
            hook { removeListener() }
        }

        listeners.add(id)
        Unit
    }

    override fun runListeners() = apiImpl {
        eventHandler.run()
    }

    override fun disableApi() {
        for(id in listeners) {
            eventHandler.removeListener(id)
        }
    }
}
