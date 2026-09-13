# Pipeline runtime subsystem

## Purpose and scope

The pipeline runtime is the execution engine of the simulator. It is the subsystem that turns source frames into processed output and makes the app feel like an actual robotics vision playground rather than a static image viewer.

The pipeline stack is spread across several classes and packages:

- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/PipelineManager.kt`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/compiled/CompiledPipelineManager.kt`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/compiled/PipelineCompiler.kt`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/compiled/PipelineClassLoader.kt`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/util/PipelineExceptionTracker.kt`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/util/PipelineSnapshot.kt`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/handler/`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/instantiator/`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/DefaultPipeline.java`

Broadly speaking, the runtime is responsible for:

- discovering available pipeline classes,
- selecting a current pipeline,
- instantiating and switching between pipelines,
- processing source frames,
- handling exceptions and telemetry,
- forwarding output to the viewport,
- and supporting warm-reload of compiled user code.

## The central class: `PipelineManager`

`PipelineManager` is the main orchestration class. It sits in the center of the subsystem and owns most of the runtime state, event handlers, and pipeline switching logic. It is not the actual algorithm implementation of a pipeline; it is the scheduler and coordinator that makes pipeline objects usable inside the simulator.

The most important runtime state it tracks includes:

- `pipelines` — all discovered pipeline definitions
- `currentPipeline` — the active instance being processed
- `currentPipelineData` — metadata about the active pipeline
- `currentPipelineName` and `currentPipelineIndex` — selection state
- `previousPipeline` — prior pipeline for change events
- `paused` and `pauseReason` — whether processing is temporarily halted
- `latestSnapshot` and `lastInitialSnapshot` — profiling or inspection snapshots
- `pipelineOutputPosters` — objects receiving processed output mats
- `pipelineFpsCounter` — throughput measurement
- `pipelineExceptionTracker` — crash/exception monitoring

This state is what the GUI uses to show the active pipeline, the app uses to decide whether to continue processing, and the exception system uses to show the right diagnostics.

## Pipeline discovery and instantiation

The app supports more than one kind of runtime pipeline source. During startup, `PipelineManager.init()` performs the crucial registration steps:

1. adds the built-in default pipeline,
2. scans the classpath for additional pipeline classes,
3. registers the common pipeline instantiators,
4. prepares the runtime to swap to an initial pipeline.

The most notable instantiators are:

- `DefaultPipelineInstantiator`
- `ProcessorInstantiator`

These map classes or runtime types to actual objects that can be executed. The patterns in this package allow the manager to support:

- standard `OpenCvPipeline` implementations,
- FTC-style `VisionProcessor` based implementations,
- and potentially custom pipeline classes introduced by user workspace code.

This is a major reason the app can mimic FTC development and still feel native to a desktop simulation environment.

## Initialization flow

A typical initialization path looks like this:

- `EOCVSim` creates the runtime and injects `PipelineManager`
- `PipelineManager.init()` runs as part of the orchestrator lifecycle
- the default pipeline is added
- the app scans the classpath for pipeline classes
- instantiators are registered
- the runtime subscribes event handlers for lifecycle and exception behavior
- the active pipeline is selected after startup or build completion

This is one reason the subsystem is important: it is not just a collection of classes, but a real orchestrated pipeline environment in which the user code is loaded and executed according to app lifecycle rules.

## The event-driven runtime model

The pipeline manager uses `EventHandler` heavily instead of relying on direct calls from every class. That keeps the system loosely coupled while still allowing many components to react to pipeline-level state changes.

Key event handlers include:

- `onUpdate` — fired when the pipeline loop runs
- `onPipelineChange` — fired when switching to another pipeline
- `onPipelineTimeout` — used when the pipeline exceeds allowed processing time
- `onPause` and `onResume` — user or system driven pause handling
- `onPipelineListRefresh` — used to refresh GUI selection state

These events are not ornamental. They are part of the runtime’s communication layer. For example, when a pipeline change occurs, the manager resets counters, clears exception output windows, and calls the lifecycle hooks for each attached handler. That means other systems can respond without knowing the details of the pipeline internals.

## Pipeline handlers and wrappers

The class `PipelineHandler` and its `SpecificPipelineHandler` implementation serve as adapters between the manager and different runtime behaviors. This is the place where a pipeline can be enriched with extra execution logic without bleeding that logic into the central manager itself.

This allows patterns such as:

- telemetry injection,
- pipeline output forwarding,
- timing observation,
- custom exceptions or output logs,
- user hook integrations.

The architecture is intentionally layered: the manager is the orchestration layer, the handlers are the behavior adapters, and the actual user pipeline object is the execution logic.

## Output posting and viewport rendering

The pipeline runtime does not render graphics directly; it posts the output `Mat` to registered poster objects. `pipelineOutputPosters` is a list of `MatPoster`s, and the viewport object is one of them.

This is important because it decouples the pipeline execution from the actual visual front end. The pipeline can produce a frame and push it into the output chain without needing to know whether the output is going to a Swing component, a file, or another debugging pipeline stage.

The render path is roughly:

- pipeline returns a processed `Mat`
- manager posts output to visualizer viewport
- viewport creates/copies the data for rendering
- the renderer draws the output canvas
- user hook methods (like `onDrawFrame`) may annotate the frame

## Telemetry and FTC compatibility

The app has a strong FTC compatibility story. It includes its own telemetry implementation (`EOCVSimTelemetryImpl`) and wrappers around FTC runtime concepts. This is not just a convenience layer; it is a core compatibility feature.

Pipeline user code often expects:

- a `Telemetry` object,
- status messages,
- error items,
- `addData`-style updates,
- and FTC-like lifecycle semantics.

The simulator counteracts this by integrating telemetry into the pipeline runtime. The manager tracks `currentTelemetry` and updates it with runtime status, warnings, and error messages. That gives user code a familiar environment even though it is actually running on a desktop JVM.

## Exception handling and crash containment

A pipeline is user-supplied logic, and user logic can fail. `PipelineExceptionTracker` is therefore a critical piece of the pipeline subsystem.

The tracker is hooked into the runtime and reacts to exceptions by:

- recording the event,
- notifying the GUI if needed,
- updating telemetry,
- clearing the exception state when recovered,
- and surfacing the issue in a user-friendly output window.

This behavior matters because the app is designed to be a development tool. It should fail gracefully when a pipeline throws, rather than crashing the whole application because one custom class misbehaved.

## Compiled pipeline flow and live rebuilds

The workspace-driven compile flow is probably the most distinctive pipeline feature in the project. It is handled by `CompiledPipelineManager` and related classes under `pipeline/compiled`.

This subsystem is responsible for:

- preparing the compile output directories,
- respecting workspace configuration,
- compiling Java source files from the workspace,
- writing a generated pipeline JAR,
- loading the resulting classes with `PipelineClassLoader`,
- and registering them so the user can switch to the newly built pipeline.

The compile process is intentionally asynchronous. It does not freeze the UI while user code compiles, and it disables the compile button or menu action while a build is running.

### Compiler details

The core file `PipelineCompiler.kt` is responsible for compiling user workspace files into a JAR. It uses a custom file manager and class path handling to build Java classes in a controlled environment. Once the build is successful, `CompiledPipelineManager.loadFromPipelinesJar()` loads the classes from the jar and asks `PipelineManager` to add them to the runtime.

This is one of the defining features of the simulator: user code in a workspace can be changed and then recompiled and reloaded without restarting the application.

## Snapshot and reflective tuning support

The app also supports runtime introspection of pipeline state via `PipelineSnapshot` and virtual reflection utilities. This is used when the simulator wants to show live field values or let users tune them without recompiling the whole project.

This is highly relevant to the “variable tuner” feature in the GUI. The underlying mechanism allows the app to:

- see fields in the current pipeline,
- determine if they are tunable,
- capture values in a snapshot,
- and optionally restore or update them at runtime.

That is the reason the project can support interactive tuning while a pipeline is actively processing frames.

## Runtime execution loop

The rough runtime loop is:

1. source produces a `Mat`
2. `PipelineManager` receives the frame through the update cycle
3. the active pipeline processes it
4. exceptions are monitored at every step
5. output is forwarded to the viewport
6. the main loop updates telemetry and stats
7. the renderer paints the result and overlays timing information

This simple loop is repeated continuously during the app’s runtime and is the reason the simulator feels dynamic and responsive.

## Relationship with other subsystems

The pipeline runtime has strong coupling to multiple other subsystems:

- `InputSourceManager` provides the frame source
- `WorkspaceManager` provides source code and rebuild triggers
- `Visualizer` displays the output and controls the viewport
- `ConfigManager` supplies runtime settings like FPS and pause behavior
- `PluginManager` can add or alter capabilities and UI hooks
- `TunerManager` uses reflection and snapshotting to expose tunable fields

This is an example of a healthy layered architecture: each subsystem owns a coherent concern, but they are all connected through runtime events and lifecycle orchestration.

## Why this subsystem matters

This is the core of the app. If the pipeline runtime is strong, the simulator is useful. If it is brittle, the whole experience feels broken even if the GUI is polished.

The simulator’s value comes from this subsystem being able to:

- run user pipeline code,
- mimic FTC semantics,
- compile workspace sources live,
- show errors without killing the app,
- and present processed frames in real time.

That is what makes the app more than a simple viewer: it is a working pipeline development environment.
