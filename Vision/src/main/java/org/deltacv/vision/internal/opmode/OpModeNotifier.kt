/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.vision.internal.opmode

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import java.util.concurrent.ArrayBlockingQueue

class OpModeNotifier(maxNotificationsQueueSize: Int = 100) {

    private val notifications = EvictingBlockingQueue<OpModeNotification>(ArrayBlockingQueue(maxNotificationsQueueSize))
    private val exceptionQueue = EvictingBlockingQueue<Throwable>(ArrayBlockingQueue(maxNotificationsQueueSize))

    private val stateLock = Any()
    var state = OpModeState.STOPPED
        private set
        get() {
            synchronized(stateLock) {
                return field
            }
        }

    private var previousState: OpModeState? = null

    val onStateChange = EventHandler("OpModeNotifier-onStateChange")

    fun notify(notification: OpModeNotification) {
        notifications.offer(notification)
    }

    fun notify(state: OpModeState) {
        synchronized(stateLock) {
            this.state = state
        }

        if(previousState != state) {
            onStateChange.run()
        }

        this.previousState = state
    }

    fun notify(notification: OpModeNotification, state: OpModeState) {
        notifications.offer(notification)

        synchronized(stateLock) {
            this.state = state
        }

        if(previousState != state) {
            onStateChange.run()
        }
    }

    fun notify(e: Throwable){
        exceptionQueue.offer(e)
    }

    fun reset() {
        notifications.clear()
        state = OpModeState.STOPPED
    }

    fun poll(): OpModeNotification {
        return notifications.poll() ?: OpModeNotification.NOTHING
    }

    fun pollException(): Throwable? = exceptionQueue.poll()

}
