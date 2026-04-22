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

package com.github.serivesmejia.eocvsim.di

import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import com.github.serivesmejia.eocvsim.pipeline.compiler.CompiledPipelineManager
import com.github.serivesmejia.eocvsim.util.ClasspathScan
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import io.github.deltacv.eocvsim.plugin.loader.PluginManager

import io.github.deltacv.common.pipeline.util.PipelineStatisticsCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun makeEOCVSimModule() = module {
    single { PipelineStatisticsCalculator() }
    single { ConfigManager() }
    single { ClasspathScan() }

    // global scope for launching coroutines within the app
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { WorkspaceManager() }
    single { PipelineManager() }
    single { InputSourceManager() }
    single { InputSourceInitializer() }
    single { TunerManager() }
    single { PluginManager() }
    single { CompiledPipelineManager() }
    single { Visualizer() }
    single { DialogFactory() }
    single { com.github.serivesmejia.eocvsim.output.RecordingManager() }


    single(named("onMainLoop"))          { EventHandler("MainLoop") }
    single(named("onRestartRequested"))  { EventHandler("RestartRequested") }
    single(named("onDestroyRequested"))  { EventHandler("DestroyRequested") }
}