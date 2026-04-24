package com.github.serivesmejia.eocvsim.util.event

import io.github.deltacv.common.util.loggerOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

/**
 * Event handler with:
 * - Persistent listeners (ID-based, queue-based adding)
 * - Once listeners (double buffered queue-based, deferred)
 */
class EventHandler @JvmOverloads constructor(
    val name: String,
    var callRightAway: CallRightAway = CallRightAway.Disabled,
    val catchExceptions: Boolean = true
) : Runnable {

    // ------------------------------------------------------------
    // config
    // ------------------------------------------------------------

    private val logger by loggerOf("EventHandler-$name")

    // ------------------------------------------------------------
    // ids
    // ------------------------------------------------------------

    private val idCounter = AtomicInteger(Int.MIN_VALUE)

    // ------------------------------------------------------------
    // persistent listeners (stable + queues)
    // ------------------------------------------------------------

    private val persistentListeners = HashMap<Int, EventListener>()

    private val persistentAddQueue = ArrayDeque<Pair<Int, EventListener>>()
    private val persistentRemoveQueue = ArrayDeque<Int>()

    private val persistentQueueLock = Any()

    private val persistentContextCache = HashMap<Int, EventListenerContext>()

    // ------------------------------------------------------------
    // once listeners (swappable double buffer)
    // ------------------------------------------------------------

    private var onceListenersCurrent = ArrayDeque<OnceEventListener>()
    private var onceIdsCurrent = ArrayDeque<Int>()

    private var onceListenersQueue = ArrayDeque<OnceEventListener>()
    private var onceIdsQueue = ArrayDeque<Int>()

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
                persistentContextCache.remove(id)
            }
        }

        fun run(id: Int, listener: EventListener) {
            val remover = persistentContextCache.getOrPut(id) { EventListenerContext(this, EventListenerId(id)) }
            listener(remover)
        }

        // execute
        for ((id, listener) in persistentListeners) {
            if(catchExceptions) {
                try {
                    run(id, listener)
                } catch (e: Exception) {
                    if (e is InterruptedException) throw e
                    logger.error("Exception in listener", e)
                }
            } else {
                run(id, listener)
            }
        }
    }

    // ------------------------------------------------------------
    // once execution
    // ------------------------------------------------------------

    fun runOnceListeners() {
        val toRunListeners: ArrayDeque<OnceEventListener>
        val toRunIds: ArrayDeque<Int>

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

            if(catchExceptions) {
                try {
                    listener()
                } catch (e: Exception) {
                    if (e is InterruptedException) throw e
                    logger.error("Exception in once listener", e)
                }
            } else {
                listener()
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
            persistentAddQueue.addLast(id.value to listener)
        }


        when(val mode = callRightAway) {
            CallRightAway.InPlace -> listener(EventListenerContext(this, id))
            is CallRightAway.InScope -> {
                val job = mode.scope.launch {
                    listener(EventListenerContext(this@EventHandler, id))
                }

                if(mode.shouldJoin) {
                    runBlocking { job.join() }
                }
            }
            CallRightAway.Disabled -> {} // do nothing
        }

        return id
    }

    fun once(listener: OnceEventListener): EventListenerId {
        val id = EventListenerId(idCounter.getAndIncrement())

        when(val mode = callRightAway) {
            CallRightAway.InPlace -> listener()
            is CallRightAway.InScope -> {
                val job = mode.scope.launch {
                    listener()
                }

                if(mode.shouldJoin) {
                    runBlocking { job.join() }
                }
            }
            CallRightAway.Disabled -> {
                synchronized(onceLock) {
                    onceListenersQueue.addLast(listener)
                    onceIdsQueue.addLast(id.value)
                }
            }
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
            val index = onceIdsQueue.indexOf(id.value)
            if (index >= 0) {
                onceIdsQueue.removeAt(index)
                onceListenersQueue.removeAt(index)

                return@removeListener // removed from once queue, no need to check persistent
            }
        }

        synchronized(persistentQueueLock) {
            persistentRemoveQueue.addLast(id.value)
        }
    }

    fun removeAllListeners() {
        synchronized(persistentQueueLock) {
            persistentListeners.clear()
            persistentAddQueue.clear()
            persistentRemoveQueue.clear()
            persistentContextCache.clear()
        }
        synchronized(onceLock) {
            onceListenersCurrent.clear()
            onceIdsCurrent.clear()
            onceListenersQueue.clear()
            onceIdsQueue.clear()
        }
    }

    sealed interface CallRightAway {
        data class InScope(val scope: CoroutineScope, val shouldJoin: Boolean = false) : CallRightAway
        object InPlace : CallRightAway
        object Disabled : CallRightAway
    }
}