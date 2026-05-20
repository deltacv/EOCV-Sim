/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.event

@JvmInline
value class EventListenerId(val value: Int)

typealias OnceEventListener = () -> Unit
typealias EventListener = EventListenerContext.() -> Unit

/**
 * Class to provide context to an event listener, mainly
 * to allow removing itself from the event handler
 * @param handler the event handler
 * @param id the listener ID
 */
class EventListenerContext(
    private val handler: EventHandler,
    private val id: EventListenerId,
) {
    /**
     * Removes the listener from the event handler
     */
    fun removeListener() {
        handler.removeListener(id)
    }
}
