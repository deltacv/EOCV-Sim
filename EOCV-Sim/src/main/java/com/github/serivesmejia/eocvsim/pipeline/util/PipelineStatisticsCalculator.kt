package com.github.serivesmejia.eocvsim.pipeline.util

import com.qualcomm.robotcore.util.ElapsedTime
import com.qualcomm.robotcore.util.MovingStatistics
import kotlin.math.roundToInt

class PipelineStatisticsCalculator {

    private lateinit var msFrameIntervalRollingAverage: MovingStatistics
    private lateinit var msUserPipelineRollingAverage: MovingStatistics
    private lateinit var msTotalFrameProcessingTimeRollingAverage: MovingStatistics
    private lateinit var timer: ElapsedTime

    private var currentFrameStartTime = 0L
    private var pipelineStart = 0L

    var avgFps = 0f
        private set
    var avgPipelineTime = 0
        private set
    var avgOverheadTime = 0
        private set
    var avgTotalFrameTime = 0
        private set

    fun init() {
        msFrameIntervalRollingAverage = MovingStatistics(30)
        msUserPipelineRollingAverage = MovingStatistics(30)
        msTotalFrameProcessingTimeRollingAverage = MovingStatistics(30)
        timer = ElapsedTime()
    }

    fun newInputFrameStart() {
        currentFrameStartTime = System.currentTimeMillis();
    }

    fun newPipelineFrameStart() {
        msFrameIntervalRollingAverage.add(timer.milliseconds())
        timer.reset()

        val secondsPerFrame = msFrameIntervalRollingAverage.mean / 1000.0
        avgFps = (1.0 / secondsPerFrame).toFloat()
    }

    fun beforeProcessFrame() {
        pipelineStart = System.currentTimeMillis()
    }

    fun afterProcessFrame() {
        msUserPipelineRollingAverage.add((System.currentTimeMillis() - pipelineStart).toDouble())
        avgPipelineTime = msUserPipelineRollingAverage.mean.roundToInt()
    }

    fun endFrame() {
        msTotalFrameProcessingTimeRollingAverage.add((System.currentTimeMillis() - currentFrameStartTime).toDouble())

        avgTotalFrameTime = msTotalFrameProcessingTimeRollingAverage.mean.roundToInt()
        avgOverheadTime = avgTotalFrameTime - avgPipelineTime
    }

}