package com.github.serivesmejia.eocvsim.util.event

import io.github.deltacv.common.util.loggerOf
import java.util.concurrent.atomic.AtomicLong

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

    private val idCounter = AtomicLong(0L)

    // ------------------------------------------------------------
    // persistent listeners (stable + queues)
    // ------------------------------------------------------------

    private val persistentListeners = HashMap<EventListenerId, EventListener>()

    private val persistentAddQueue = ArrayDeque<Pair<EventListenerId, EventListener>>()
    private val persistentRemoveQueue = ArrayDeque<EventListenerId>()

    private val persistentQueueLock = Any()

    private val contextCache = HashMap<EventListenerId, EventListenerContext>()

    // ------------------------------------------------------------
    // once listeners (swappable double buffer)
    // ------------------------------------------------------------

    private var onceListenersCurrent = ArrayDeque<OnceEventListener>()
    private var onceIdsCurrent = ArrayDeque<EventListenerId>()

    private var onceListenersQueue = ArrayDeque<OnceEventListener>()
    private var onceIdsQueue = ArrayDeque<EventListenerId>()

    private val onceLock = Any()

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
                val remover = contextCache.getOrPut(id) { EventListenerContext(this, id) }
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
        val toRunListeners: ArrayDeque<OnceEventListener>
        val toRunIds: ArrayDeque<EventListenerId>

        synchronized(onceLock) {
            // swap
            toRunListeners = onceListenersQueue
            toRunIds = onceIdsQueue

            onceListenersQueue = onceListenersCurrent
            onceIdsQueue = onceIdsCurrent

            onceListenersCurrent = toRunListeners
            onceIdsCurrent = toRunIds
        }

        while (toRunListeners.isNotEmpty()) {
            val listener = toRunListeners.removeFirst()
            toRunIds.removeFirst() // keep in sync

            try {
                listener()
            } catch (e: Exception) {
                if (e is InterruptedException) throw e
                logger.error("Exception in once listener of EventHandler '$name':", e)
            }
        }

        toRunListeners.clear()
        toRunIds.clear()
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

        synchronized(onceLock) {
            onceListenersQueue.addLast(listener)
            onceIdsQueue.addLast(id)
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
        synchronized(onceLock) {
            val index = onceIdsQueue.indexOf(id)
            if (index >= 0) {
                onceIdsQueue.removeAt(index)
                onceListenersQueue.removeAt(index)
                return@removeListener // removed from once queue, no need to check persistent
            }
        }

        synchronized(persistentQueueLock) {
            persistentRemoveQueue.addLast(id)
        }
    }

    fun removeAllListeners() {
        synchronized(persistentQueueLock) {
            persistentListeners.clear()
            persistentAddQueue.clear()
            persistentRemoveQueue.clear()
        }
        synchronized(onceLock) {
            onceListenersQueue.clear()
            onceIdsQueue.clear()
        }
    }
}