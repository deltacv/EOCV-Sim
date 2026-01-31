package com.github.serivesmejia.eocvsim.util.event

import io.github.deltacv.common.util.loggerOf
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/**
 * Event handler class to manage event listeners using unique listener IDs
 * without per-run allocations and without CME risk.
 */
class EventHandler(val name: String) : Runnable {

    companion object {
        private val bannedClassLoaders = mutableListOf<WeakReference<ClassLoader>>()

        fun banClassLoader(loader: ClassLoader) {
            bannedClassLoaders.add(WeakReference(loader))
        }

        fun isBanned(classLoader: ClassLoader): Boolean {
            for (ref in bannedClassLoaders) {
                if (ref.get() === classLoader) return true
            }
            return false
        }
    }

    val logger by loggerOf("${name}-EventHandler")

    private val persistentLock = Any()
    private val onceLock = Any()

    private val idCounter = AtomicInteger(-1)

    private val persistentListeners = HashMap<EventListenerId, EventListener>()
    private val onceListeners = HashMap<EventListenerId, EventListener>()

    private var runningPersistent = false
    private var runningOnce = false

    private val pendingPersistent = ArrayList<PendingOp>()
    private val pendingOnce = ArrayList<PendingOp>()

    var callRightAway = false

    // ----------------------------------------------------------------
    // core
    // ----------------------------------------------------------------

    override fun run() {
        runListeners(
            lock = persistentLock,
            listeners = persistentListeners,
            pending = pendingPersistent,
            setRunning = { runningPersistent = it },
            isOnce = false
        )

        runListeners(
            lock = onceLock,
            listeners = onceListeners,
            pending = pendingOnce,
            setRunning = { runningOnce = it },
            isOnce = true
        )
    }

    private inline fun runListeners(
        lock: Any,
        listeners: MutableMap<EventListenerId, EventListener>,
        pending: MutableList<PendingOp>,
        setRunning: (Boolean) -> Unit,
        isOnce: Boolean
    ) {
        synchronized(lock) {
            setRunning(true)

            val iterator = listeners.entries.iterator()
            while (iterator.hasNext()) {
                val (id, listener) = iterator.next()

                if (isBanned(listener.javaClass.classLoader)) {
                    iterator.remove()
                    logger.warn("Removed banned listener from ${listener.javaClass.classLoader}")
                    continue
                }

                try {
                    listener.invoke(EventListenerRemover(this, id))
                } catch (ex: Exception) {
                    if (ex is InterruptedException) throw ex
                    logger.error(
                        "Error while running${if (isOnce) " \"once\"" else ""} listener ${listener.javaClass.name}",
                        ex
                    )
                }

                if (isOnce) {
                    iterator.remove()
                }
            }

            setRunning(false)
            applyPending(listeners, pending)
        }
    }

    operator fun invoke(listener: EventListener) = attach(false, listener)

    // ----------------------------------------------------------------
    // attach (unified)
    // ----------------------------------------------------------------

    fun attach(
        once: Boolean = false,
        listener: EventListener,
    ): EventListenerId {

        val id = EventListenerId(idCounter.getAndDecrement())

        val lock: Any
        val listeners: MutableMap<EventListenerId, EventListener>
        val pending: MutableList<PendingOp>
        val running: Boolean

        if (once) {
            lock = onceLock
            listeners = onceListeners
            pending = pendingOnce
            running = runningOnce
        } else {
            lock = persistentLock
            listeners = persistentListeners
            pending = pendingPersistent
            running = runningPersistent
        }

        synchronized(lock) {
            if (running) {
                pending += Add(id, listener)
            } else {
                listeners[id] = listener
            }
        }

        if (callRightAway) {
            listener.invoke(EventListenerRemover(this, id))
            if (once) {
                removeListener(id)
            }
        }

        return id
    }

    @JvmName("attach")
    @JvmOverloads
    fun attach(once: Boolean = false, runnable: Runnable): EventListenerId =
        attach(once, listener = { runnable.run() })

    fun once(listener: EventListener): EventListenerId =
        attach(once = true, listener)

    @JvmName("once")
    fun once(runnable: Runnable): EventListenerId = attach(once = true, runnable)

    // Optional compatibility wrappers (safe to delete)
    @Deprecated("Use attach instead", ReplaceWith("attach(once = false, listener)"))
    fun doPersistent(listener: EventListener): EventListenerId =
        attach(false, listener)

    @Deprecated("Use once instead", ReplaceWith("once(listener)"))
    fun doOnce(listener: EventListener): EventListenerId =
        attach(true, listener)

    // ----------------------------------------------------------------
    // remove (unified)
    // ----------------------------------------------------------------

    fun removeListener(id: EventListenerId) {
        synchronized(persistentLock) {
            if (runningPersistent) {
                pendingPersistent += Remove(id)
            } else {
                persistentListeners.remove(id)
            }
        }

        synchronized(onceLock) {
            if (runningOnce) {
                pendingOnce += Remove(id)
            } else {
                onceListeners.remove(id)
            }
        }
    }

    fun removeAllListeners() {
        synchronized(persistentLock) {
            persistentListeners.clear()
            pendingPersistent.clear()
        }
        synchronized(onceLock) {
            onceListeners.clear()
            pendingOnce.clear()
        }
    }

    // ----------------------------------------------------------------
    // pending ops
    // ----------------------------------------------------------------

    private sealed interface PendingOp {
        fun apply(target: MutableMap<EventListenerId, EventListener>)
    }

    private class Add(
        val id: EventListenerId,
        val listener: EventListener
    ) : PendingOp {
        override fun apply(target: MutableMap<EventListenerId, EventListener>) {
            target[id] = listener
        }
    }

    private class Remove(
        val id: EventListenerId
    ) : PendingOp {
        override fun apply(target: MutableMap<EventListenerId, EventListener>) {
            target.remove(id)
        }
    }

    private fun applyPending(
        target: MutableMap<EventListenerId, EventListener>,
        pending: MutableList<PendingOp>
    ) {
        for (op in pending) {
            op.apply(target)
        }
        pending.clear()
    }
}
