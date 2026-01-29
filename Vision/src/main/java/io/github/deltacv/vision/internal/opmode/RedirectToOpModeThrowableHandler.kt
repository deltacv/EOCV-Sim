package io.github.deltacv.vision.internal.opmode

import io.github.deltacv.vision.external.util.ThrowableHandler

class RedirectToOpModeThrowableHandler(private val notifier: OpModeNotifier) : ThrowableHandler {
    override fun handle(e: Throwable) {
        notifier.notify(e)
    }
}