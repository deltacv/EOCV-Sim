/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.event.EventListenerId
import org.deltacv.eocvsim.plugin.EOCVSimPlugin
import org.deltacv.eocvsim.plugin.api.HookApi

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
