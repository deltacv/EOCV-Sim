package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.EOCVSimApi
import io.github.deltacv.eocvsim.plugin.api.InputSourceManagerApi
import io.github.deltacv.eocvsim.plugin.api.PipelineManagerApi
import io.github.deltacv.eocvsim.plugin.api.VariableTunerApi
import io.github.deltacv.eocvsim.plugin.api.VisualizerApi

class EOCVSimApiImpl(owner: EOCVSimPlugin, val internalEOCVSim: EOCVSim) : EOCVSimApi(owner) {
    private val logger by loggerForThis()

    override val mainLoopHook by apiField { EventHandlerHookApiImpl(owner, internalEOCVSim.onMainUpdate) }

    override val visualizerApi: VisualizerApi by apiField(VisualizerApiImpl(owner, internalEOCVSim.visualizer))
    override val inputSourceManagerApi: InputSourceManagerApi by apiField(InputSourceManagerApiImpl(owner, internalEOCVSim.inputSourceManager))
    override val pipelineManagerApi: PipelineManagerApi by apiField(PipelineManagerApiImpl(owner, internalEOCVSim.pipelineManager))
    override val variableTunerApi: VariableTunerApi by apiField(VariableTunerApiImpl(owner, internalEOCVSim.tunerManager))

    override fun disableApi() {
        logger.info("EOCV-Sim API for {} says: \"aight, time to check out\"", ownerName)
    }
}