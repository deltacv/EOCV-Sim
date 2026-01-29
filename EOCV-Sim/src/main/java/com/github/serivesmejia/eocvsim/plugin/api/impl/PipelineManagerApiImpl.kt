package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.instantiator.PipelineInstantiator
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.PipelineInstantiatorApi
import io.github.deltacv.eocvsim.plugin.api.PipelineManagerApi
import io.github.deltacv.eocvsim.plugin.api.PipelineManagerApi.PipelineSource
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

private class EOCVSimPipelineInstantiatorApiDelegate(val instantiator: PipelineInstantiatorApi) : PipelineInstantiator {
    override val isUsable get() = !instantiator.isDisabled

    override fun instantiate(
        clazz: Class<*>,
        telemetry: Telemetry
    ): OpenCvPipeline {
        return instantiator.instantiatePipeline(clazz, telemetry)
    }

    override fun virtualReflectOf(pipeline: OpenCvPipeline) = instantiator.virtualReflectionFor(pipeline)

    override fun variableTunerTarget(pipeline: OpenCvPipeline) = instantiator.variableTunerTargetFor(pipeline)
}

class PipelineManagerApiImpl(owner: EOCVSimPlugin, val internalPipelineManager: PipelineManager) : PipelineManagerApi(owner) {

    override val onPipelineListRefreshHook by apiField { EventHandlerHookApiImpl(owner, internalPipelineManager.onPipelineListRefresh) }

    override val onExternalSwitchingEnabledHook by apiField { EventHandlerHookApiImpl(owner, internalPipelineManager.onExternalSwitchingEnable) }
    override val onExternalSwitchingDisabledHook by apiField { EventHandlerHookApiImpl(owner, internalPipelineManager.onExternalSwitchingDisable) }

    override val onPipelineChangeHook by apiField { EventHandlerHookApiImpl(owner, internalPipelineManager.onPipelineChange) }
    override val onPipelineTimeoutHook by apiField { EventHandlerHookApiImpl(owner, internalPipelineManager.onPipelineTimeout) }
    override val onPauseHook by apiField { EventHandlerHookApiImpl(owner, internalPipelineManager.onPause) }
    override val onResumeHook by apiField { EventHandlerHookApiImpl(owner, internalPipelineManager.onResume) }

    override val pipelines: List<PipelineData> by liveApiField {
        internalPipelineManager.pipelines.map {
            PipelineData(it.source.mapToApi(), it.clazz, it.hidden)
        }
    }
    override val previousPipelineInstance by liveApiField { internalPipelineManager.previousPipeline }
    override val currentPipelineInstance by liveApiField { internalPipelineManager.currentPipeline }

    override val currentPipelineData by liveApiField {
        internalPipelineManager.currentPipelineData?.let {
            PipelineData(it.source.mapToApi(), it.clazz, it.hidden)
        }
    }
    override val currentPipelineName by liveApiField { internalPipelineManager.currentPipelineName }

    override val currentPipelineIndex by liveApiField { internalPipelineManager.currentPipelineIndex }
    override val previousPipelineIndex by liveApiField { internalPipelineManager.previousPipelineIndex }

    override val virtualReflect by liveApiField { internalPipelineManager.virtualReflect }
    override val reflectTarget by liveApiField { internalPipelineManager.reflectTarget }

    override val pauseReason by liveApiField { internalPipelineManager.pauseReason.mapToApi() }

    override val isPaused by liveApiField { internalPipelineManager.paused }

    override fun pause(pauseReason: PipelinePauseReason) = apiImpl {
        internalPipelineManager.setPaused(true, pauseReason.mapToInternal())
    }
    override fun resume() = apiImpl {
        internalPipelineManager.setPaused(false, PipelineManager.PauseReason.NOT_PAUSED)
    }

    override fun addPipelineClass(
        pipeline: PipelineClass,
        source: PipelineSource,
        hidden: Boolean
    ): Int? = apiImpl {
        internalPipelineManager.addPipelineClass(pipeline, source.mapToInternal(), hidden)
    }

    override fun removePipelineAt(pipelineIndex: Int) = apiImpl {
        internalPipelineManager.pipelines.removeAt(pipelineIndex)
        Unit
    }

    override fun removePipeline(
        pipelineClass: PipelineClass,
        source: PipelineSource
    ) = apiImpl {
        for(i in internalPipelineManager.pipelines.indices.reversed()) {
            val p = internalPipelineManager.pipelines[i]
            if(p.clazz == pipelineClass && p.source == source.mapToInternal()) {
                internalPipelineManager.pipelines.removeAt(i)
            }
        }
    }

    override fun removeAllPipelinesFrom(source: PipelineSource) = apiImpl {
        internalPipelineManager.pipelines.removeIf { it.source == source.mapToInternal() }
        Unit
    }

    override fun getIndexOf(
        pipelineClass: PipelineClass,
        source: PipelineSource
    ) = apiImpl {
        internalPipelineManager.getIndexOf(pipelineClass, source.mapToInternal())
    }

    override fun getPipelineDataOf(source: PipelineSource) = apiImpl {
        internalPipelineManager.getPipelinesFrom(source.mapToInternal()).map {
            PipelineData(it.source.mapToApi(), it.clazz, it.hidden)
        }
    }

    override fun changePipelineAnonymous(
        pipelineClass: PipelineClass,
        force: Boolean
    ) = apiImpl {
        if(force) {
            internalPipelineManager.forceChangePipelineAnonymous(pipelineClass)
        } else {
            internalPipelineManager.changePipelineAnonymous(pipelineClass)
        }
    }

    override fun changePipeline(pipelineIndex: Int, force: Boolean) = apiImpl {
        if(force) {
            internalPipelineManager.forceChangePipeline(pipelineIndex)
        } else {
            internalPipelineManager.changePipeline(pipelineIndex)
        }
    }

    override fun reloadCurrentPipeline() = apiImpl {
        internalPipelineManager.reloadPipelineByName()
    }

    override fun addPipelineInstantiator(instantiatorFor: Class<*>, instantiator: PipelineInstantiatorApi) = apiImpl(instantiator) {
        internalPipelineManager.addInstantiator(instantiatorFor, EOCVSimPipelineInstantiatorApiDelegate(instantiator))
    }

    override fun pollStatistics() = apiImpl {
        val stats = internalPipelineManager.pipelineStatisticsCalculator

        Statistics(
            stats.avgFps.toDouble(),
            stats.avgPipelineTime.toDouble(),
            stats.avgOverheadTime.toDouble(),
            stats.avgTotalFrameTime.toDouble()
        )
    }

    override fun disableApi() {}
}

private fun PipelineSource.mapToInternal() = when(this) {
    PipelineSource.CLASSPATH -> com.github.serivesmejia.eocvsim.pipeline.PipelineSource.CLASSPATH
    PipelineSource.RUNTIME -> com.github.serivesmejia.eocvsim.pipeline.PipelineSource.COMPILED_ON_RUNTIME
    PipelineSource.ANONYMOUS -> com.github.serivesmejia.eocvsim.pipeline.PipelineSource.COMPILED_ON_RUNTIME
}
private fun com.github.serivesmejia.eocvsim.pipeline.PipelineSource.mapToApi() = when(this) {
    com.github.serivesmejia.eocvsim.pipeline.PipelineSource.CLASSPATH -> PipelineSource.CLASSPATH
    com.github.serivesmejia.eocvsim.pipeline.PipelineSource.COMPILED_ON_RUNTIME -> PipelineSource.RUNTIME
    com.github.serivesmejia.eocvsim.pipeline.PipelineSource.ANONYMOUS -> PipelineSource.ANONYMOUS
}

private fun PipelineManagerApi.PipelinePauseReason.mapToInternal() = when(this) {
    PipelineManagerApi.PipelinePauseReason.NOT_PAUSED -> PipelineManager.PauseReason.NOT_PAUSED
    PipelineManagerApi.PipelinePauseReason.USER_REQUESTED -> PipelineManager.PauseReason.USER_REQUESTED
    PipelineManagerApi.PipelinePauseReason.IMAGE_SINGLE_SHOT -> PipelineManager.PauseReason.IMAGE_ONE_ANALYSIS
}
private fun PipelineManager.PauseReason.mapToApi() = when(this) {
    PipelineManager.PauseReason.NOT_PAUSED -> PipelineManagerApi.PipelinePauseReason.NOT_PAUSED
    PipelineManager.PauseReason.USER_REQUESTED -> PipelineManagerApi.PipelinePauseReason.USER_REQUESTED
    PipelineManager.PauseReason.IMAGE_ONE_ANALYSIS -> PipelineManagerApi.PipelinePauseReason.IMAGE_SINGLE_SHOT
}