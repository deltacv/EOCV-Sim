/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim

import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.output.RecordingManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.compiled.CompiledPipelineManager
import com.github.serivesmejia.eocvsim.plugin.output.PluginOutputHandler
import com.github.serivesmejia.eocvsim.plugin.output.VisualPluginOutputHandler
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import com.github.serivesmejia.eocvsim.util.InitClasspathScan
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.orchestration.Orchestrable
import com.github.serivesmejia.eocvsim.util.orchestration.Orchestrator
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import org.deltacv.common.pipeline.PipelineStatisticsCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import org.deltacv.eocvsim.plugin.loader.PluginManager
import org.koin.core.definition.KoinDefinition
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val eocvSimModule = module {
    single { PipelineStatisticsCalculator() }
    single { InitClasspathScan() }.bindOrchestrable()

    // global scope for launching coroutines within the app
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { ConfigManager() }.bindOrchestrable()
    single { WorkspaceManager() }.bindOrchestrable()
    single { PipelineManager() }.bindOrchestrable()
    single { InputSourceManager() }.bindOrchestrable()
    single { InputSourceInitializer() }
    single { TunerManager() }.bindOrchestrable()
    single { PluginManager() }.bindOrchestrable()
    single { CompiledPipelineManager() }.bindOrchestrable()

    single { Visualizer() }.bindOrchestrable()
    single { DialogFactory() }
    single<PluginOutputHandler> { VisualPluginOutputHandler() }

    single { RecordingManager() }

    single { Orchestrator(scope = get(), tasks = getAll<Orchestrable>(), name = "Main") }
    single(named("lifecycle")) { Channel<LifecycleSignal>(Channel.BUFFERED) }
    single(named("onCrash")) { EventHandler("OnCrash") }
    single(named("onMainLoop")) { EventHandler("OnMainLoop") }
}

private fun <T : Orchestrable> KoinDefinition<out T>.bindOrchestrable() = this bind Orchestrable::class
