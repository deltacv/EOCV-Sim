package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import org.openftc.easyopencv.OpenCvPipeline

abstract class PipelineManagerApi(owner: EOCVSimPlugin) : Api(owner) {
    typealias PipelineClass = Class<out OpenCvPipeline>

    abstract val onPipelineChange: HookApi

    abstract fun addPipelineClass(pipeline: PipelineClass, source: PipelineSource): Int

    abstract fun changePipeline(pipelineIndex: Int)

    abstract fun pollStatistics(): Statistics

    data class Statistics(
        val avgFps: Double,
        val avgPipelineTimeMs: Double,
        val avgOverheadTimeMs: Double,
        val avgTotalFrameTimeMs: Double
    )

    enum class PipelineSource {
        CLASSPATH, RUNTIME
    }
}