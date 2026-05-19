/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.api

import org.deltacv.eocvsim.plugin.EOCVSimPlugin

/**
 * Root API entry point exposed to plugins by EOCV-Sim.
 *
 * This API provides access to the main subsystems of the application,
 * including visualization, input sources, pipelines, configuration,
 * and the main simulation loop.
 *
 * @param owner the plugin that owns this API instance
 */
abstract class EOCVSimApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Hook into the main application loop.
     */
    abstract val mainLoopHook: HookApi

    /**
     * API for the GUI of the application.
     */
    abstract val visualizerApi: VisualizerApi

    /**
     * API for managing input sources used by pipelines.
     */
    abstract val inputSourceManagerApi: InputSourceManagerApi

    /**
     * API for creating and managing pipelines.
     */
    abstract val pipelineManagerApi: PipelineManagerApi

    /**
     * API for exposing tunable variables to the UI.
     */
    abstract val variableTunerApi: VariableTunerApi

    /**
     * API for saved configuration.
     */
    abstract val configApi: ConfigApi
}

