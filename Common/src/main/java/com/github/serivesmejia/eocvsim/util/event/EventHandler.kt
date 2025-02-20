/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.util.event

import com.github.serivesmejia.eocvsim.util.loggerOf
import java.lang.ref.WeakReference

/**
 * Event handler class to manage event listeners
 * @param name the name of the event handler
 */
class EventHandler(val name: String) : Runnable {

    companion object {
        private val bannedClassLoaders = mutableListOf<WeakReference<ClassLoader>>()

        /**
         * Ban a class loader from being able to add listeners
         * and lazily remove all listeners from that class loader
         */
        fun banClassLoader(loader: ClassLoader) = bannedClassLoaders.add(WeakReference(loader))

        /**
         * Check if a class loader is banned
         */
        fun isBanned(classLoader: ClassLoader): Boolean {
            for (bannedClassLoader in bannedClassLoaders) {
                if (bannedClassLoader.get() == classLoader) return true
            }

            return false
        }
    }

    val logger by loggerOf("${name}-EventHandler")

    private val lock = Any()
    private val onceLock = Any()

    /**
     * Get all the listeners in this event handler
     * thread-safe operation (synchronized)
     */
    val listeners: Array<EventListener>
        get() {
            synchronized(lock) {
                return internalListeners.toTypedArray()
            }
        }

    /**
     * Get all the "doOnce" listeners in this event handler
     * thread-safe operation (synchronized)
     */
    val onceListeners: Array<EventListener>
        get() {
            synchronized(onceLock) {
                return internalOnceListeners.toTypedArray()
            }
        }

    var callRightAway = false

    private val internalListeners = ArrayList<EventListener>()
    private val internalOnceListeners = ArrayList<EventListener>()

    /**
     * Run all the listeners in this event handler
     */
    override fun run() {
        for (listener in listeners) {
            try {
                runListener(listener, false)
            } catch (ex: Exception) {
                if (ex is InterruptedException) {
                    logger.warn("Rethrowing InterruptedException...")
                    throw ex
                } else {
                    logger.error("Error while running listener ${listener.javaClass.name}", ex)
                }
            }
        }

        val toRemoveOnceListeners = mutableListOf<EventListener>()

        //executing "doOnce" listeners
        for (listener in onceListeners) {
            try {
                runListener(listener, true)
            } catch (ex: Exception) {
                if (ex is InterruptedException) {
                    logger.warn("Rethrowing InterruptedException...")
                    throw ex
                } else {
                    logger.error("Error while running \"once\" ${listener.javaClass.name}", ex)
                    removeOnceListener(listener)
                }
            }

            toRemoveOnceListeners.add(listener)
        }

        synchronized(onceLock) {
            for (listener in toRemoveOnceListeners) {
                internalOnceListeners.remove(listener)
            }
        }
    }

    /**
     * Add a listener to this event handler to only be run once
     * @param listener the listener to add
     */
    fun doOnce(listener: EventListener) {
        if (callRightAway)
            runListener(listener, true)
        else synchronized(onceLock) {
            internalOnceListeners.add(listener)
        }
    }

    /**
     * Add a runnable as a listener to this event handler to only be run once
     * @param runnable the runnable to add
     */
    fun doOnce(runnable: Runnable) = doOnce { runnable.run() }

    /**
     * Add a listener to this event handler to be run every time
     * @param listener the listener to add
     */
    fun doPersistent(listener: EventListener): EventListenerRemover {
        synchronized(lock) {
            internalListeners.add(listener)
        }

        if (callRightAway) runListener(listener, false)

        return EventListenerRemover(this, listener, false)
    }

    /**
     * Add a runnable as a listener to this event handler to be run every time
     * @param runnable the runnable to add
     */
    fun doPersistent(runnable: Runnable) = doPersistent { runnable.run() }

    /**
     * Remove a listener from this event handler
     * @param listener the listener to remove
     */
    fun removePersistentListener(listener: EventListener) {
        if (internalListeners.contains(listener)) {
            synchronized(lock) { internalListeners.remove(listener) }
        }
    }

    /**
     * Remove a "doOnce" listener from this event handler
     * @param listener the listener to remove
     */
    fun removeOnceListener(listener: EventListener) {
        if (internalOnceListeners.contains(listener)) {
            synchronized(onceLock) { internalOnceListeners.remove(listener) }
        }
    }

    /**
     * Remove all listeners from this event handler
     */
    fun removeAllListeners() {
        removeAllPersistentListeners()
        removeAllOnceListeners()
    }

    /**
     * Remove all persistent listeners from this event handler
     */
    fun removeAllPersistentListeners() = synchronized(lock) {
        internalListeners.clear()
    }

    /**
     * Remove all "doOnce" listeners from this event handler
     */
    fun removeAllOnceListeners() = synchronized(onceLock) {
        internalOnceListeners.clear()
    }

    /**
     * Add a listener to this event handler
     * @param listener the listener to add
     */
    operator fun invoke(listener: EventListener) = doPersistent(listener)

    private fun runListener(listener: EventListener, isOnce: Boolean) {
        if (isBanned(listener.javaClass.classLoader)) {
            removeBannedListeners()
            return
        }

        listener.run(EventListenerRemover(this, listener, isOnce))
    }

    private fun removeBannedListeners() {
        for (listener in internalListeners.toArray()) {
            if (isBanned(listener.javaClass.classLoader)) {
                internalListeners.remove(listener)
                logger.warn("Removed banned listener from ${listener.javaClass.classLoader}")
            }
        }

        for (listener in internalOnceListeners.toArray()) {
            if (isBanned(listener.javaClass.classLoader)) internalOnceListeners.remove(listener)
            logger.warn("Removed banned listener from ${listener.javaClass.classLoader}")
        }
    }

}
