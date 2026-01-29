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

package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.ConfigApi
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
    override val configApi: ConfigApi by apiField(ConfigApiImpl(owner, internalEOCVSim.configManager))

    override fun disableApi() {
        logger.info("EOCV-Sim API for {} says: \"aight, time to check out\"", ownerName)
    }
}