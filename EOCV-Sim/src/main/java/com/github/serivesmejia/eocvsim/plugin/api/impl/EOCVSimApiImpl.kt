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
import io.github.deltacv.common.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.ConfigApi
import io.github.deltacv.eocvsim.plugin.api.EOCVSimApi
import io.github.deltacv.eocvsim.plugin.api.InputSourceManagerApi
import io.github.deltacv.eocvsim.plugin.api.PipelineManagerApi
import io.github.deltacv.eocvsim.plugin.api.VariableTunerApi
import io.github.deltacv.eocvsim.plugin.api.VisualizerApi

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import com.github.serivesmejia.eocvsim.config.ConfigManager

class EOCVSimApiImpl(owner: EOCVSimPlugin) : EOCVSimApi(owner), KoinComponent {
    private val logger by loggerForThis()

    private val onMainUpdate: EventHandler by inject(named("onMainLoop"))
    private val visualizer: Visualizer by inject()
    private val inputSourceManager: InputSourceManager by inject()
    private val pipelineManager: PipelineManager by inject()
    private val tunerManager: TunerManager by inject()
    private val configManager: ConfigManager by inject()

    val internalEOCVSim: EOCVSim by inject()

    override val mainLoopHook by apiField { EventHandlerHookApiImpl(owner, onMainUpdate) }

    override val visualizerApi: VisualizerApi by apiField(VisualizerApiImpl(owner, visualizer))
    override val inputSourceManagerApi: InputSourceManagerApi by apiField(InputSourceManagerApiImpl(owner, inputSourceManager))
    override val pipelineManagerApi: PipelineManagerApi by apiField(PipelineManagerApiImpl(owner, pipelineManager))
    override val variableTunerApi: VariableTunerApi by apiField(VariableTunerApiImpl(owner, tunerManager))
    override val configApi: ConfigApi by apiField(ConfigApiImpl(owner, configManager))

    override fun disableApi() {
        logger.info("EOCV-Sim API for {} says: \"ight, imma head out\"", ownerName)
    }
}