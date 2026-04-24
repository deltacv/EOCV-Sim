package com.github.serivesmejia.eocvsim

import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.output.RecordingManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.compiler.CompiledPipelineManager
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import com.github.serivesmejia.eocvsim.util.AutoClasspathScan
import com.github.serivesmejia.eocvsim.util.ClasspathScan
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.event.Orchestrator
import com.github.serivesmejia.eocvsim.workspace.WorkspaceManager
import io.github.deltacv.common.pipeline.util.PipelineStatisticsCalculator
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val eocvSimModule = module {
    single { PipelineStatisticsCalculator() }
    single<ClasspathScan> { AutoClasspathScan() }

    // global scope for launching coroutines within the app
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { ConfigManager() }
    single { WorkspaceManager() }
    single { PipelineManager() }
    single { InputSourceManager() }
    single { InputSourceInitializer() }
    single { TunerManager() }
    single { PluginManager() }
    single { CompiledPipelineManager() }

    single { Visualizer() }
    single { DialogFactory() }

    single { RecordingManager() }

    single(named("init")) { Orchestrator("Init", scope = get()) }
    single(named("lifecycle")) { Channel<LifecycleSignal>(Channel.BUFFERED) }
    single(named("onMainLoop")) { EventHandler("OnMainLoop") }
}