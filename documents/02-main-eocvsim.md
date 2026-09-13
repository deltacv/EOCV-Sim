# Main application flow and `EOCVSim`

## Entry points

The process starts in `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/Main.kt`. This file is the JVM bootstrap layer. It does things that belong to app startup and platform integration rather than the business logic of the simulator itself, such as:

- setting Java 2D and desktop environment properties,
- configuring OS-specific app naming for macOS,
- creating the command-line entry structure,
- and exiting with a proper status code when the app is done.

This is the outer shell of the app. Most of the actual runtime behavior lives elsewhere.

## The central runtime object

The true app runtime is `EOCVSim` in `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/EOCVSim.kt`. It is the orchestration hub. The class resolves or owns references to the major subsystems:

- `ConfigManager`
- `Visualizer`
- `InputSourceManager`
- `PipelineManager`
- `WorkspaceManager`
- `PluginManager`
- `RecordingManager`
- `DialogFactory`
- `TunerManager`
- `Orchestrator`

The reason this class matters is that it is the place where the application is assembled into a working runtime rather than just a collection of independent libraries.

## Startup sequence in `EOCVSim.start()`

`EOCVSim.start()` runs the application boot sequence in a careful order. A simplified flow is:

1. Validate that the app can claim its lock file, preventing duplicate running instances.
2. Show the splash screen and register the global crash handler.
3. Load required native libraries.
4. Change the orchestrator phase to `INIT` and call `orchestrate()`.
5. Initialize the UI and trigger first-run welcome dialogs when needed.
6. Ensure an input source is selected and persisted.
7. Populate source and pipeline selector lists.
8. Set the default active pipeline.
9. Attach the pipeline output to the viewport.
10. Enable plugins.

This sequence matters because several subsystems depend on each other. For instance, the visualizer needs the config and input manager to be ready, and the workspace/pipeline systems need the UI to be present before source switching is enabled.

## `Module.kt` and the dependency graph

The Koin module in `Module.kt` is how the app wires itself together. It registers singleton components like:

- `PipelineStatisticsCalculator`
- `ConfigManager`
- `WorkspaceManager`
- `PipelineManager`
- `InputSourceManager`
- `InputSourceInitializer`
- `TunerManager`
- `PluginManager`
- `CompiledPipelineManager`
- `Visualizer`
- `DialogFactory`
- `RecordingManager`
- `Orchestrator`

This is a good example of the app’s architecture: services are provided centrally and then resolved via injection rather than created manually in many places.

## Lifecycle and orchestration

The runtime makes use of the shared `Orchestrator` and `PhaseOrchestrableBase` abstractions. Subsystems are initialized in a consistent order and can be stopped cleanly when the app is shutting down.

The major phases are conceptually:

- `INIT` — configure and build runtime state
- `RUN` — let the main loop and runtime run
- `DESTROY` — tear down components and release resources

This lifecycle approach keeps startup, runtime updates, and shutdown behavior consistent across the application instead of relying on brittle manual ordering.

## Crash handling and native initialization

The startup code is protective. It installs `EOCVSimUncaughtExceptionHandler` and is careful about native library loading. If critical libraries are missing or fail to initialize, the app can show a crash report to the user instead of crashing silently.

That is especially important in a project like this, where native OpenCV and WPILib components are involved. Startup failure is not treated as a simple runtime exception; it is treated as a major environment issue.

## Main loop coordination

The app owns a global coroutine scope and a main-loop event handler. This allows the runtime to schedule UI work, pipeline logic, and other state updates without manually synchronizing every background thread.

The key idea is that the app’s runtime is event-driven:

- external signals trigger the UI,
- pipeline state changes update the selection panels,
- input sources can fail or time out,
- the viewport can update when a new frame arrives,
- workspace changes can trigger automatic rebuilds.

This makes the runtime dynamic and resilient, particularly when user code and files are changing while the app is already running.

## Restart and shutdown behavior

The app’s lifecycle also supports restart and destroy signals. It can notify the runtime that it should stop, clean up, or restart a subsystem depending on the conditions. This is useful for plugin changes, reinstallations, or major config-driven state refreshes.

The design is not purely static: the app expects components to be reconfigured and reused in a live environment.

## Summary

`EOCVSim` is the runtime coordinator for the whole simulator. It is not the pipeline engine, nor the rendering layer, nor the settings manager alone. It is the architectural center: it assembles the pieces, sequences them in the correct order, coordinates runtime updates, and exposes the full application lifecycle to the rest of the codebase.
