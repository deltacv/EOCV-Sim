package io.github.deltacv.common.pipeline.util

import com.qualcomm.robotcore.util.ElapsedTime
import com.qualcomm.robotcore.util.MovingStatistics
import kotlin.math.roundToInt

/**
 * Utility class to calculate pipeline statistics
 */
class PipelineStatisticsCalculator {

    private lateinit var msFrameIntervalRollingAverage: MovingStatistics
    private lateinit var msUserPipelineRollingAverage: MovingStatistics
    private lateinit var msTotalFrameProcessingTimeRollingAverage: MovingStatistics
    private lateinit var timer: ElapsedTime

    private var currentFrameStartTime = 0L
    private var pipelineStart = 0L

    /**
     * Average frames per second
     */
    var avgFps = 0f
        private set

    /**
     * Average milliseconds per frame
     */
    var avgPipelineTime = 0
        private set

    /**
     * Average milliseconds of overhead time per frame
     * (total frame time - pipeline time)
     */
    var avgOverheadTime = 0
        private set

    /**
     * Average milliseconds of total frame time
     */
    var avgTotalFrameTime = 0
        private set

    /**
     * Initializes the calculator
     */
    fun init() {
        msFrameIntervalRollingAverage = MovingStatistics(30)
        msUserPipelineRollingAverage = MovingStatistics(30)
        msTotalFrameProcessingTimeRollingAverage = MovingStatistics(30)
        timer = ElapsedTime()
    }

    /**
     * Should be called at the start of a new input frame
     */
    fun newInputFrameStart() {
        currentFrameStartTime = System.currentTimeMillis();
    }

    /**
     * Should be called at the start of a new pipeline frame
     */
    fun newPipelineFrameStart() {
        msFrameIntervalRollingAverage.add(timer.milliseconds())
        timer.reset()

        val secondsPerFrame = msFrameIntervalRollingAverage.mean / 1000.0
        avgFps = (1.0 / secondsPerFrame).toFloat()
    }

    /**
     * Should be called before processing a frame
     */
    fun beforeProcessFrame() {
        pipelineStart = System.currentTimeMillis()
    }

    /**
     * Should be called after processing a frame
     */
    fun afterProcessFrame() {
        msUserPipelineRollingAverage.add((System.currentTimeMillis() - pipelineStart).toDouble())
        avgPipelineTime = msUserPipelineRollingAverage.mean.roundToInt()
    }

    /**
     * Should be called at the end of a frame
     */
    fun endFrame() {
        msTotalFrameProcessingTimeRollingAverage.add((System.currentTimeMillis() - currentFrameStartTime).toDouble())

        avgTotalFrameTime = msTotalFrameProcessingTimeRollingAverage.mean.roundToInt()
        avgOverheadTime = avgTotalFrameTime - avgPipelineTime
    }

}