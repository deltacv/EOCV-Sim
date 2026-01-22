package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.EOCVSimApi
import io.github.deltacv.eocvsim.plugin.api.InputSourceManagerApi
import io.github.deltacv.eocvsim.plugin.api.PipelineManagerApi
import io.github.deltacv.eocvsim.plugin.api.VisualizerApi

class EOCVSimApiImpl(owner: EOCVSimPlugin, val internalEOCVSim: EOCVSim) : EOCVSimApi(owner) {
    private val logger by loggerForThis()

    override val mainLoop by apiField { EventHandlerHookApiImpl(owner, internalEOCVSim.onMainUpdate) }

    override val visualizerApi: VisualizerApi
        get() = TODO("Not yet implemented")
    override val inputSourceManagerApi: InputSourceManagerApi
        get() = TODO("Not yet implemented")
    override val pipelineManagerApi: PipelineManagerApi
        get() = TODO("Not yet implemented")

    override fun disable() {
        logger.info("EOCV-Sim API for {} says: \"aight, time to check out\"", ownerName)
    }
}