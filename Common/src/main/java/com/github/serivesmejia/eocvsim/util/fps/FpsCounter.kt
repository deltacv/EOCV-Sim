/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.fps

import com.qualcomm.robotcore.util.ElapsedTime
import com.qualcomm.robotcore.util.MovingStatistics

class FpsCounter {

    private val elapsedTime = ElapsedTime()

    private val avgFpsStatistics = MovingStatistics(100)

    val avgFps: Double
        get() {
            return avgFpsStatistics.mean
        }

    @Volatile
    private var fpsCount = 0

    @get:Synchronized
    @Volatile
    var fps = 0
        private set

    @Synchronized
    fun update() {
        fpsCount++
        if (elapsedTime.seconds() >= 1) {
            fps = fpsCount; fpsCount = 0
            avgFpsStatistics.add(fps.toDouble())
            elapsedTime.reset()
        }
    }

}
