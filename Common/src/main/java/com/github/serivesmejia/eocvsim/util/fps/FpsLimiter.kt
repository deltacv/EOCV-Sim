/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.fps

class FpsLimiter(var maxFPS: Double = 30.0) {

    @Volatile private var start = 0.0
    @Volatile private var diff = 0.0
    @Volatile private var wait = 0.0

    @Throws(InterruptedException::class)
    fun sync() {
        wait = 1.0 / (maxFPS / 1000.0)
        diff = System.currentTimeMillis() - start
        if (diff < wait) {
            Thread.sleep((wait - diff).toLong())
        }
        start = System.currentTimeMillis().toDouble()
    }

}
