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

package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.event.EventListener
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.HookApi
import java.lang.ref.WeakReference

class EventHandlerHookApiImpl(owner: EOCVSimPlugin, val eventHandler: EventHandler) : HookApi(owner) {
    private var allOnceListeners = mutableListOf<WeakReference<EventListener>>()
    private var allPersistentListeners = mutableListOf<WeakReference<EventListener>>()

    override fun once(hook: OnceHook) = apiImpl {
        throwIfDisabled()

        val listener = EventListener {
            hook()
        }

        allOnceListeners.add(WeakReference(listener))
        eventHandler.doOnce(listener)
    }

    override fun attach(hook: PersistentHook) = apiImpl {
        throwIfDisabled()

        val listener = EventListener {
            hook.invoke(it::removeThis) // pass remover function to hook
        }

        allPersistentListeners.add(WeakReference(listener))
        eventHandler.doPersistent(listener)
        Unit
    }

    override fun runListeners() = apiImpl {
        eventHandler.run()
    }

    override fun disableApi() {
        for(listenerRef in allOnceListeners) {
            val listener = listenerRef.get() ?: continue
            eventHandler.removeOnceListener(listener)
        }
        for(listenerRef in allPersistentListeners) {
            val listener = listenerRef.get() ?: continue
            eventHandler.removePersistentListener(listener)
        }
    }
}