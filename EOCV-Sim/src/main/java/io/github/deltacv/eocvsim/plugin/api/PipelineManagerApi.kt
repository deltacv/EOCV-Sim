package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import org.openftc.easyopencv.OpenCvPipeline

abstract class PipelineManagerApi(owner: EOCVSimPlugin) : Api(owner) {
    typealias PipelineClass = Class<out OpenCvPipeline>

    abstract val onPipelineListRefreshHook: HookApi

    abstract val onExternalSwitchingEnabledHook: HookApi
    abstract val onExternalSwitchingDisabledHook: HookApi

    abstract val onPipelineChangeHook: HookApi
    abstract val onPipelineTimeoutHook: HookApi
    abstract val onPauseHook: HookApi
    abstract val onResumeHook: HookApi

    abstract val pipelines: List<PipelineData>

    abstract val previousPipelineInstance: OpenCvPipeline?
    abstract val currentPipelineInstance: OpenCvPipeline?

    abstract val currentPipelineData: PipelineData?
    abstract val currentPipelineName: String?

    abstract val currentPipelineIndex: Int?
    abstract val previousPipelineIndex: Int?

    abstract val virtualReflect: VirtualReflection
    abstract val reflectTarget: Any?

    abstract val pauseReason: PipelinePauseReason
    abstract val isPaused: Boolean

    abstract fun pause(pauseReason: PipelinePauseReason)
    abstract fun resume()

    abstract fun addPipelineClass(pipeline: PipelineClass, source: PipelineSource): Int
    abstract fun removePipelineAt(pipelineIndex: Int)
    abstract fun removeAllPipelinesFrom(source: PipelineSource)

    abstract fun getIndexOf(pipelineClass: PipelineClass, source: PipelineSource): Int?
    abstract fun getPipelineDataOf(source: PipelineSource): List<PipelineData>?

    abstract fun changePipeline(pipelineIndex: Int)

    abstract fun reloadCurrentPipeline()

    abstract fun pollStatistics(): Statistics

    data class Statistics(
        val avgFps: Double,
        val avgPipelineTimeMs: Double,
        val avgOverheadTimeMs: Double,
        val avgTotalFrameTimeMs: Double
    )

    data class PipelineData(
        val source: PipelineSource,
        val clazz: Class<*>
    )

    enum class PipelineSource {
        CLASSPATH, RUNTIME
    }

    enum class PipelinePauseReason {
        NOT_PAUSED, USER_REQUESTED, IMAGE_SINGLE_SHOT
    }
}