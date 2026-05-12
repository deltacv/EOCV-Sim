/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.event

import io.github.deltacv.common.util.loggerOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

/**
 * Generic parameterized event handler. Listeners receive a payload of type T.
 * This is a new implementation; the original zero-arg `EventHandler` is kept
 * for compatibility and extends this class with `T = Unit`.
 */
typealias ParamOnceEventListener<T> = (T) -> Unit
typealias ParamEventListener<T> = ParamEventListenerContext<T>.(T) -> Unit

class ParamEventListenerContext<T>(
    private val handler: ParamEventHandler<T>,
    val id: EventListenerId,
) {
    fun removeListener() {
        handler.removeListener(id)
    }
}

open class ParamEventHandler<T> @JvmOverloads constructor(
    val name: String,
    var callRightAway: EventHandler.CallRightAway = EventHandler.CallRightAway.Disabled,
    val catchExceptions: Boolean = true
) {

    private val logger by loggerOf("EventHandler-$name")

    // ids
    protected val idCounter = AtomicInteger(Int.MIN_VALUE)

    // persistent listeners
    protected val persistentListeners = HashMap<Int, ParamEventListener<T>>()
    protected val persistentAddQueue = ArrayDeque<Pair<Int, ParamEventListener<T>>>()
    protected val persistentRemoveQueue = ArrayDeque<Int>()
    protected val persistentQueueLock = Any()
    protected val persistentContextCache = HashMap<Int, ParamEventListenerContext<T>>()

    // once listeners
    protected var onceListenersCurrent = ArrayDeque<ParamOnceEventListener<T>>()
    protected var onceIdsCurrent = ArrayDeque<Int>()

    protected var onceListenersQueue = ArrayDeque<ParamOnceEventListener<T>>()
    protected var onceIdsQueue = ArrayDeque<Int>()
    protected val onceLock = Any()

    // run with payload
    fun run(payload: T) {
        runPersistentListeners(payload)
        runOnceListeners(payload)
    }

    // public attach/once for payload listeners (named to avoid conflict with
    // legacy zero-arg EventHandler overloads)
    fun attachPayload(listener: ParamEventListener<T>): EventListenerId = attachRaw(listener)

    fun oncePayload(listener: ParamOnceEventListener<T>): EventListenerId = onceRaw(listener)

    protected fun runPersistentListeners(payload: T) {
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

        fun run(id: Int, listener: ParamEventListener<T>) {
            val remover = persistentContextCache.getOrPut(id) { ParamEventListenerContext(this, EventListenerId(id)) }
            listener(remover, payload)
        }

        for ((id, listener) in persistentListeners) {
            if (catchExceptions) {
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

    protected fun runOnceListeners(payload: T) {
        val toRunListeners: ArrayDeque<ParamOnceEventListener<T>>
        val toRunIds: ArrayDeque<Int>

        synchronized(onceLock) {
            toRunListeners = onceListenersQueue
            toRunIds = onceIdsQueue

            onceListenersQueue = onceListenersCurrent
            onceIdsQueue = onceIdsCurrent

            onceListenersCurrent = toRunListeners
            onceIdsCurrent = toRunIds
        }

        while (toRunListeners.isNotEmpty()) {
            val listener = toRunListeners.removeFirst()
            toRunIds.removeFirst()

            if (catchExceptions) {
                try {
                    listener(payload)
                } catch (e: Exception) {
                    if (e is InterruptedException) throw e
                    logger.error("Exception in once listener", e)
                }
            } else {
                listener(payload)
            }
        }

        toRunListeners.clear()
        toRunIds.clear()
    }

    // attach/once that don't implement callRightAway semantics; subclasses
    // (like the legacy EventHandler) may implement immediate invocation.
    protected fun attachRaw(listener: ParamEventListener<T>): EventListenerId {
        val id = EventListenerId(idCounter.getAndIncrement())
        synchronized(persistentQueueLock) {
            persistentAddQueue.addLast(id.value to listener)
        }
        return id
    }

    protected fun onceRaw(listener: ParamOnceEventListener<T>): EventListenerId {
        val id = EventListenerId(idCounter.getAndIncrement())

        // For the generic/payload handler we don't attempt to call listeners
        // immediately since we don't have a payload value here. Always enqueue.
        synchronized(onceLock) {
            onceListenersQueue.addLast(listener)
            onceIdsQueue.addLast(id.value)
        }

        return id
    }

    protected fun attachRunnable(runnable: Runnable): EventListenerId = attachRaw { runnable.run() }

    protected fun onceRunnable(runnable: Runnable): EventListenerId = onceRaw { runnable.run() }

    // remove
    open fun removeListener(id: EventListenerId) {
        synchronized(onceLock) {
            val index = onceIdsQueue.indexOf(id.value)
            if (index >= 0) {
                onceIdsQueue.removeAt(index)
                onceListenersQueue.removeAt(index)
                return
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
}

/**
 * Legacy zero-arg EventHandler kept for compatibility. It extends the
 * parameterized implementation with T = Unit and provides the exact old API.
 */

class EventHandler @JvmOverloads constructor(
    name: String,
    callRightAway: CallRightAway = CallRightAway.Disabled,
    catchExceptions: Boolean = true
) : ParamEventHandler<Unit>(name, callRightAway, catchExceptions), Runnable {

    private val logger by loggerOf("EventHandler-$name")

    override fun run() {
        run(Unit)
    }

    // ------------------------------------------------------------
    // attach (legacy API)
    // ------------------------------------------------------------
    operator fun invoke(listener: EventListener): EventListenerId = attach(listener)

    fun attach(listener: EventListener): EventListenerId {
        // wrapper adapts legacy EventListenerContext receiver to ParamEventListenerContext
        val wrapper: ParamEventListener<Unit> = { _ ->
            // `this` is ParamEventListenerContext<Unit>
            val ctx = EventListenerContext(this@EventHandler, EventListenerId(this.id.value))
            listener(ctx)
        }

        val id = attachRaw(wrapper)

        when (val mode = callRightAway) {
            CallRightAway.InPlace -> listener(EventListenerContext(this, id))
            is CallRightAway.InScope -> {
                val job = mode.scope.launch {
                    listener(EventListenerContext(this@EventHandler, id))
                }
                if (mode.shouldJoin) runBlocking { job.join() }
            }
            CallRightAway.Disabled -> {}
        }

        return id
    }

    fun once(listener: OnceEventListener): EventListenerId {
        val wrapper: ParamOnceEventListener<Unit> = { _ -> listener() }

        val id = onceRaw(wrapper)

        when (val mode = callRightAway) {
            CallRightAway.InPlace -> listener()
            is CallRightAway.InScope -> {
                val job = mode.scope.launch { listener() }
                if (mode.shouldJoin) runBlocking { job.join() }
            }
            CallRightAway.Disabled -> {}
        }

        return id
    }

    @JvmName("attach")
    fun attach(runnable: Runnable): EventListenerId = super.attachRunnable(runnable)

    @JvmName("once")
    fun once(runnable: Runnable): EventListenerId = super.onceRunnable(runnable)

    // reuse removeListener from ParamEventHandler

    sealed interface CallRightAway {
        data class InScope(val scope: CoroutineScope, val shouldJoin: Boolean = false) : CallRightAway
        object InPlace : CallRightAway
        object Disabled : CallRightAway
    }

    companion object {
        fun batchOnce(vararg eventHandler: EventHandler, listener: OnceEventListener): List<EventListenerId> {
            return eventHandler.map { it.once(listener) }
        }

        fun batchAttach(vararg eventHandler: EventHandler, listener: EventListener): List<EventListenerId> {
            return eventHandler.map { it.attach(listener) }
        }
    }
}
