package io.github.deltacv.vision.internal.opmode

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import java.util.concurrent.ArrayBlockingQueue

class OpModeNotifier {

    val notifications = EvictingBlockingQueue<OpModeNotification>(ArrayBlockingQueue(10))

    private val stateLock = Any()
    var state = OpModeState.STOPPED
        private set
        get() {
            synchronized(stateLock) {
                return field
            }
        }

    val onStateChange = EventHandler("OpModeNotifier-onStateChange")

    fun notify(notification: OpModeNotification) {
        notifications.offer(notification)
    }

    fun notify(state: OpModeState) {
        synchronized(stateLock) {
            this.state = state
        }

        onStateChange.run()
    }

    fun notify(notification: OpModeNotification, state: OpModeState) {
        notifications.offer(notification)

        synchronized(stateLock) {
            this.state = state
        }
        onStateChange.run()
    }

    fun reset() {
        notifications.clear()
        state = OpModeState.STOPPED
    }

    fun poll(): OpModeNotification {
        return notifications.poll() ?: OpModeNotification.NOTHING
    }

}