package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.PipelineManagerApi
import io.github.deltacv.eocvsim.plugin.api.PipelineManagerApi.PipelineSource

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
            PipelineData(it.source.mapToApi(), it.clazz)
        }
    }

    override val previousPipelineInstance by liveApiField { internalPipelineManager.previousPipeline }
    override val currentPipelineInstance by liveApiField { internalPipelineManager.currentPipeline }

    override val currentPipelineData by liveApiField {
        internalPipelineManager.currentPipelineData?.let {
            PipelineData(it.source.mapToApi(), it.clazz)
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
        source: PipelineSource
    ): Int = apiImpl {
        internalPipelineManager.addPipelineClass(pipeline, source.mapToInternal())
        internalPipelineManager.getIndexOf(pipeline, source.mapToInternal())!!
    }

    override fun removePipelineAt(pipelineIndex: Int) = apiImpl {
        internalPipelineManager.pipelines.removeAt(pipelineIndex)
        Unit
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
            PipelineData(it.source.mapToApi(), it.clazz)
        }
    }

    override fun changePipeline(pipelineIndex: Int) = apiImpl {
        internalPipelineManager.changePipeline(pipelineIndex)
    }

    override fun reloadCurrentPipeline() = apiImpl {
        internalPipelineManager.reloadPipelineByName()
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
}
private fun com.github.serivesmejia.eocvsim.pipeline.PipelineSource.mapToApi() = when(this) {
    com.github.serivesmejia.eocvsim.pipeline.PipelineSource.CLASSPATH -> PipelineSource.CLASSPATH
    com.github.serivesmejia.eocvsim.pipeline.PipelineSource.COMPILED_ON_RUNTIME -> PipelineSource.RUNTIME
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