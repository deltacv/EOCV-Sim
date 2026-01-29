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
