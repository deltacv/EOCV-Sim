package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

abstract class PipelineInstantiatorApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract fun instantiatePipeline(
        pipelineClass: Class<*>,
        telemetry: Telemetry
    ): OpenCvPipeline

    abstract fun virtualReflectionFor(
        pipeline: OpenCvPipeline
    ): VirtualReflection

    abstract fun variableTunerTargetFor(
        pipeline: OpenCvPipeline
    ): Any
}

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

    abstract fun addPipelineClass(pipeline: PipelineClass, source: PipelineSource, hidden: Boolean = false): Int?
    abstract fun removePipelineAt(pipelineIndex: Int)
    abstract fun removePipeline(pipelineClass: PipelineClass, source: PipelineSource)
    abstract fun removeAllPipelinesFrom(source: PipelineSource)

    abstract fun getIndexOf(pipelineClass: PipelineClass, source: PipelineSource): Int?
    abstract fun getPipelineDataOf(source: PipelineSource): List<PipelineData>?

    abstract fun changePipelineAnonymous(pipelineClass: PipelineClass, force: Boolean = false)
    abstract fun changePipeline(pipelineIndex: Int, force: Boolean = false)

    abstract fun reloadCurrentPipeline()

    abstract fun addPipelineInstantiator(instantiatorFor: Class<*>, instantiator: PipelineInstantiatorApi)

    abstract fun pollStatistics(): Statistics

    data class Statistics(
        val avgFps: Double,
        val avgPipelineTimeMs: Double,
        val avgOverheadTimeMs: Double,
        val avgTotalFrameTimeMs: Double
    )

    data class PipelineData(
        val source: PipelineSource,
        val clazz: Class<*>,
        val hidden: Boolean
    )

    enum class PipelineSource {
        CLASSPATH, RUNTIME, ANONYMOUS
    }

    enum class PipelinePauseReason {
        NOT_PAUSED, USER_REQUESTED, IMAGE_SINGLE_SHOT
    }
}