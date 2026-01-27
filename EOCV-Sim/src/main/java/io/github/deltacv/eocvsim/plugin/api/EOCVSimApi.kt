package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin

abstract class EOCVSimApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val mainLoopHook: HookApi

    abstract val visualizerApi: VisualizerApi
    abstract val inputSourceManagerApi: InputSourceManagerApi
    abstract val pipelineManagerApi: PipelineManagerApi
    abstract val variableTunerApi: VariableTunerApi
}