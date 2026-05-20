/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.vision.internal.opmode

import org.deltacv.vision.external.util.ThrowableHandler

class RedirectToOpModeThrowableHandler(private val notifier: OpModeNotifier) : ThrowableHandler {
    override fun handle(e: Throwable) {
        notifier.notify(e)
    }
}
