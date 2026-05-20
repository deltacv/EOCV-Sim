/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim

sealed interface LifecycleSignal {
    class Destroy(val reason: Reason) : LifecycleSignal {
        enum class Reason { USER_REQUESTED, THREAD_EXIT, CRASH }
    }
    object Restart : LifecycleSignal
}
