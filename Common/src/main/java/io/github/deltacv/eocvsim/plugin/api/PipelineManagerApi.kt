/*
 * Copyright (c) 2026 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

/**
 * Creates and adapts OpenCV pipeline instances for the pipeline manager.
 *
 * The [PipelineManagerApi] delegates pipeline construction and introspection
 * to a registered [PipelineInstantiatorApi]. The instantiator is responsible
 * for:
 *
 * - Creating the pipeline instance
 * - Providing a [VirtualReflection] wrapper for variable access
 * - Exposing the object used as the variable tuner target
 *
 * An instantiator is selected based on the pipeline class passed to
 * [PipelineManagerApi.addPipelineInstantiator].
 *
 * When extending this API, all exposed members **must** be implemented using
 * [apiImpl] or declared via [apiField].
 */
abstract class PipelineInstantiatorApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Instantiates a pipeline of the given class.
     *
     * @param pipelineClass the pipeline class to instantiate
     * @param telemetry telemetry instance passed to the pipeline
     * @return the created pipeline instance
     */
    abstract fun instantiatePipeline(
        pipelineClass: Class<*>,
        telemetry: Telemetry
    ): OpenCvPipeline

    /**
     * Returns a virtual reflection wrapper for the given pipeline.
     *
     * This method **must** be implemented using [apiImpl].
     *
     * @param pipeline the pipeline instance
     */
    abstract fun virtualReflectionFor(
        pipeline: OpenCvPipeline
    ): VirtualReflection

    /**
     * Returns the object used as a variable tuning target for the pipeline.
     *
     * This method **must** be implemented using [apiImpl].
     *
     * @param pipeline the pipeline instance
     */
    abstract fun variableTunerTargetFor(
        pipeline: OpenCvPipeline
    ): Any
}

/**
 * Manages the lifecycle, selection, and execution state of OpenCV pipelines.
 *
 * This API is responsible for registering pipeline classes, instantiating them,
 * switching between pipelines, handling pause/resume state, and exposing runtime
 * statistics and reflection data.
 *
 * Pipelines may originate from different [PipelineSource]s and may be switched
 * either programmatically or externally (UI / hotkeys).
 */
abstract class PipelineManagerApi(owner: EOCVSimPlugin) : Api(owner) {

    typealias PipelineClass = Class<out OpenCvPipeline>

    /** Fired when the internal pipeline list is refreshed. */
    abstract val onPipelineListRefreshHook: HookApi

    /** Fired when external pipeline switching becomes enabled. */
    abstract val onExternalSwitchingEnabledHook: HookApi

    /** Fired when external pipeline switching becomes disabled. */
    abstract val onExternalSwitchingDisabledHook: HookApi

    /** Fired after the active pipeline changes. */
    abstract val onPipelineChangeHook: HookApi

    /** Fired when the current pipeline times out. */
    abstract val onPipelineTimeoutHook: HookApi

    /** Fired when the pipeline execution is paused. */
    abstract val onPauseHook: HookApi

    /** Fired when pipeline execution resumes after being paused. */
    abstract val onResumeHook: HookApi

    /** All registered pipelines in execution order. */
    abstract val pipelines: List<PipelineData>

    /** Previously active pipeline instance, if any. */
    abstract val previousPipelineInstance: OpenCvPipeline?

    /** Currently active pipeline instance, if any. */
    abstract val currentPipelineInstance: OpenCvPipeline?

    /** Metadata for the currently active pipeline. */
    abstract val currentPipelineData: PipelineData?

    /** Name of the currently active pipeline, if available. */
    abstract val currentPipelineName: String?

    /** Index of the currently active pipeline. */
    abstract val currentPipelineIndex: Int?

    /** Index of the previously active pipeline. */
    abstract val previousPipelineIndex: Int?

    /** Virtual reflection wrapper for the active pipeline. */
    abstract val virtualReflect: VirtualReflection

    /** Object used as the variable tuner target for the active pipeline. */
    abstract val reflectTarget: Any?

    /** Current pause reason. */
    abstract val pauseReason: PipelinePauseReason

    /** Whether pipeline execution is currently paused. */
    abstract val isPaused: Boolean

    /**
     * Pauses pipeline execution.
     *
     * @param pauseReason reason for pausing the pipeline
     */
    abstract fun pause(pauseReason: PipelinePauseReason)

    /**
     * Resumes pipeline execution if currently paused.
     */
    abstract fun resume()

    /**
     * Registers a pipeline class.
     *
     * @param pipeline pipeline class to register
     * @param source origin of the pipeline
     * @param hidden whether the pipeline should be hidden from external selectors
     * @return index of the added pipeline, or null if registration failed
     */
    abstract fun addPipelineClass(
        pipeline: PipelineClass,
        source: PipelineSource,
        hidden: Boolean = false
    ): Int?

    /**
     * Removes a pipeline by index.
     *
     * @param pipelineIndex index of the pipeline to remove
     */
    abstract fun removePipelineAt(pipelineIndex: Int)

    /**
     * Removes a pipeline by class and source.
     *
     * @param pipelineClass pipeline class to remove
     * @param source pipeline source
     */
    abstract fun removePipeline(
        pipelineClass: PipelineClass,
        source: PipelineSource
    )

    /**
     * Removes all pipelines originating from the given source.
     *
     * @param source pipeline source
     */
    abstract fun removeAllPipelinesFrom(source: PipelineSource)

    /**
     * Returns the index of a pipeline.
     *
     * @param pipelineClass pipeline class to look up
     * @param source pipeline source
     * @return pipeline index, or null if not found
     */
    abstract fun getIndexOf(
        pipelineClass: PipelineClass,
        source: PipelineSource
    ): Int?

    /**
     * Returns all pipelines originating from the given source.
     *
     * @param source pipeline source
     * @return list of pipeline data, or null if none exist
     */
    abstract fun getPipelineDataOf(source: PipelineSource): List<PipelineData>?

    /**
     * Switches to a pipeline class without requiring it to be registered.
     *
     * @param pipelineClass pipeline class to activate
     * @param force whether to force switching even if already active
     */
    abstract fun changePipelineAnonymous(
        pipelineClass: PipelineClass,
        force: Boolean = false
    )

    /**
     * Switches to a registered pipeline by index.
     *
     * @param pipelineIndex index of the pipeline to activate
     * @param force whether to force switching even if already active
     */
    abstract fun changePipeline(
        pipelineIndex: Int,
        force: Boolean = false
    )

    /**
     * Re-instantiates and reactivates the current pipeline.
     */
    abstract fun reloadCurrentPipeline()

    /**
     * Registers a pipeline instantiator for a given pipeline superclass.
     *
     * @param instantiatorFor class or superclass the instantiator applies to
     * @param instantiator instantiator instance
     */
    abstract fun addPipelineInstantiator(
        instantiatorFor: Class<*>,
        instantiator: PipelineInstantiatorApi
    )

    /**
     * Polls runtime statistics for the active pipeline.
     *
     * @return aggregated pipeline statistics
     */
    abstract fun pollStatistics(): Statistics

    /**
     * Aggregated pipeline timing and performance data.
     */
    data class Statistics(
        val avgFps: Double,
        val avgPipelineTimeMs: Double,
        val avgOverheadTimeMs: Double,
        val avgTotalFrameTimeMs: Double
    )

    /**
     * Metadata describing a registered pipeline.
     *
     * @param source origin of the pipeline
     * @param clazz pipeline class
     * @param hidden whether the pipeline is hidden from external selectors
     */
    data class PipelineData(
        val source: PipelineSource,
        val clazz: Class<*>,
        val hidden: Boolean
    )

    /**
     * Origin of a pipeline.
     */
    enum class PipelineSource {
        CLASSPATH,
        RUNTIME,
        ANONYMOUS
    }

    /**
     * Reason why pipeline execution is paused.
     */
    enum class PipelinePauseReason {
        NOT_PAUSED,
        USER_REQUESTED,
        IMAGE_SINGLE_SHOT
    }
}