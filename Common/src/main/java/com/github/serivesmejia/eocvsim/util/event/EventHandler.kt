package com.github.serivesmejia.eocvsim.util.event

import io.github.deltacv.common.util.loggerOf
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Event handler with:
 * - Persistent listeners (ID-based, queue-based adding)
 * - Once listeners (double buffered queue-based, deferred)
 */
class EventHandler(val name: String) : Runnable {

    // ------------------------------------------------------------
    // config
    // ------------------------------------------------------------

    private val logger by loggerOf("EventHandler-$name")

    var callRightAway = false

    // ------------------------------------------------------------
    // ids
    // ------------------------------------------------------------

    private val idCounter = AtomicInteger(Int.MIN_VALUE)

    // ------------------------------------------------------------
    // persistent listeners (stable + queues)
    // ------------------------------------------------------------

    private val persistentListeners = HashMap<EventListenerId, EventListener>()

    private val persistentAddQueue = ArrayDeque<Pair<EventListenerId, EventListener>>()
    private val persistentRemoveQueue = ArrayDeque<EventListenerId>()

    private val persistentQueueLock = Any()

    private val removerCache = HashMap<EventListenerId, EventListenerContext>()

    // ------------------------------------------------------------
    // once listeners (double buffer)
    // ------------------------------------------------------------

    private val onceRead = HashMap<EventListenerId, OnceEventListener>()
    private val onceWrite = HashMap<EventListenerId, OnceEventListener>()

    private val onceSwapLock = Any()
    private val onceWriteLock = Any()

    // ------------------------------------------------------------
    // run
    // ------------------------------------------------------------

    override fun run() {
        runPersistentListeners()
        runOnceListeners()
    }

    // ------------------------------------------------------------
    // persistent execution
    // ------------------------------------------------------------

    fun runPersistentListeners() {
        // apply pending adds/removes
        synchronized(persistentQueueLock) {
            while (persistentAddQueue.isNotEmpty()) {
                val (id, listener) = persistentAddQueue.removeFirst()
                persistentListeners[id] = listener
            }
            while (persistentRemoveQueue.isNotEmpty()) {
                val id = persistentRemoveQueue.removeFirst()
                persistentListeners.remove(id)
            }
        }

        // execute
        for ((id, listener) in persistentListeners) {
            try {
                val remover = removerCache.getOrPut(id) { EventListenerContext(this, id) }
                listener(remover)
            } catch (e: Exception) {
                if (e is InterruptedException) throw e
                logger.error("Exception while running persistent listener", e)
            }
        }
    }

    // ------------------------------------------------------------
    // once execution
    // ------------------------------------------------------------

    fun runOnceListeners() {
        synchronized(onceSwapLock) {
            onceRead.clear()
            onceRead.putAll(onceWrite)
            onceWrite.clear()
        }

        for (listener in onceRead.values) {
            try {
                listener()
            } catch (e: Exception) {
                if (e is InterruptedException) throw e
                logger.error("Exception in once listener of EventHandler '$name':", e)
            }
        }

        onceRead.clear()
    }

    // ------------------------------------------------------------
    // attach
    // ------------------------------------------------------------

    operator fun invoke(listener: EventListener): EventListenerId = attach(listener)

    fun attach(listener: EventListener): EventListenerId {
        val id = EventListenerId(idCounter.getAndIncrement())

        synchronized(persistentQueueLock) {
            persistentAddQueue.addLast(id to listener)
        }

        if (callRightAway) {
            listener(EventListenerContext(this, id))
        }

        return id
    }

    fun once(listener: OnceEventListener): EventListenerId {
        val id = EventListenerId(idCounter.getAndIncrement())

        synchronized(onceWriteLock) {
            onceWrite[id] = listener
        }

        if (callRightAway) {
            listener()
            removeListener(id)
        }

        return id
    }

    @JvmName("attach")
    fun attach(runnable: Runnable): EventListenerId = attach{ runnable.run() }

    @JvmName("once")
    fun once(runnable: Runnable): EventListenerId = once { runnable.run() }

    // ------------------------------------------------------------
    // remove
    // ------------------------------------------------------------

    @JvmName("removeListener")
    fun removeListener(id: EventListenerId) {
        synchronized(persistentQueueLock) {
            persistentRemoveQueue.addLast(id)
        }
        synchronized(onceWriteLock) {
            onceWrite.remove(id)
        }
    }

    fun removeAllListeners() {
        synchronized(persistentQueueLock) {
            persistentListeners.clear()
            persistentAddQueue.clear()
            persistentRemoveQueue.clear()
        }
        synchronized(onceWriteLock) {
            onceWrite.clear()
        }
    }
}
