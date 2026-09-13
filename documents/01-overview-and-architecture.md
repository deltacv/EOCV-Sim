# Overview and architecture

## Project purpose

EOCV-Sim is a desktop simulator for OpenCV-based pipelines, aimed primarily at FTC and robotics development. The problem it solves is straightforward: developers want to write and test OpenCV pipeline logic in a desktop environment without requiring direct hardware execution. The app gives them a Swing UI, a configurable image/camera input stream, a live pipeline runtime, and a developer workspace that can compile and reload user code on demand.

At a high level the project combines four major ideas:

- a desktop GUI for viewing pipeline output,
- a runtime that mimics FTC/OpenCV pipeline semantics,
- a workspace model for editable Java source code,
- and an extension model for plugins and optional security-sensitive features.

## Repository layout

The repository is split into a few important modules:

### `EOCV-Sim/`

This is the main application module. It contains the desktop runtime, GUI, configuration manager, pipeline engine, workspace logic, plugin integration, and startup lifecycle. This module is the heart of the simulator.

Key classes and packages include:

- `Main.kt` — JVM entry point and command bootstrap
- `EOCVSim.kt` — central application lifecycle and startup coordination
- `Module.kt` — Koin dependency graph setup
- `config/` — config persistence and runtime settings
- `gui/` — Swing UI and tool panels
- `pipeline/` — active pipeline management, instantiation, and exception tracking
- `input/` — source loading and frame acquisition
- `workspace/` — workspace scanning, template creation, file watching and build integration
- `plugin/` — repository loading, plugin API, and security workflows

### `Common/`

The Common module contains reusable infrastructure: orchestration, shared logger utilities, pipeline statistics, and cross-cutting helpers. It acts as the “glue” layer that keeps the app lifecycle consistent across modules without forcing each subsystem to reinvent common behavior.

Examples include:

- lifecycle orchestration classes,
- logging helpers,
- serialization helpers,
- shared pipeline metadata and calculations.

### `Vision/`

This module is responsible for the OpenCV rendering pipeline and GUI adapter layer. It wraps lower-level OpenCV drawing and Java Swing display concerns into a desktop-friendly viewport implementation. Most of the actual visible rendering and FPS overlay logic lives here.

Important classes include:

- `SwingOpenCvViewport.kt` — desktop Swing-backed viewport implementation
- `OpenCvViewRenderer.java` — canvas renderer and FPS overlay
- supporting FTC/OpenCV wrapper classes in the `external` and `internal` packages

### `TeamCode/`

This is a sample area containing FTC-style pipeline examples and test code. It shows how this simulator fits into the FTC ecosystem and how users can author pipelines in a familiar style while running them locally.

## High-level runtime flow

The app uses a layered runtime model:

1. JVM process starts via `Main.kt`
2. Koin creates the dependency graph from `Module.kt`
3. `EOCVSim.start()` proceeds through initialization and startup
4. Config, workspace, plugins, input sources, and GUI are created in dependency order
5. The pipeline manager chooses an active pipeline
6. Input frames are acquired from a source and passed to the active pipeline
7. Output is rendered in the viewport and optionally overlaid with stats
8. The app continues until shutdown or restart

This is a classic runtime shell with several independent subsystems, rather than one monolithic class doing everything.

## Orchestration pattern

The central lifecycle abstraction is the `Orchestrator` and `PhaseOrchestrableBase`. Major subsystems inherit from `PhaseOrchestrableBase`, which gives them a common sequence:

- initialization
- run phase
- destroy/teardown

This matters because code depends on other subsystems being ready at the right time. For example, the visualizer should not be created before the config manager is loaded, and the workspace manager should not trigger compilation before the pipeline manager exists.

Most subsystems therefore participate in the same lifecycle rather than being manually wired ad hoc in a single startup method.

## Dependency injection and event-driven coordination

The project uses Koin as its dependency injection system. In `Module.kt`, the app registers services such as:

- `ConfigManager`
- `WorkspaceManager`
- `PipelineManager`
- `InputSourceManager`
- `TunerManager` — live field inspection and runtime tuning for active pipeline objects
- `PluginManager`
- `CompiledPipelineManager`
- `Visualizer`

This makes it easy for the code to resolve runtime dependencies without constructing a huge object graph manually.

The project also makes heavy use of `EventHandler`. Many cross-cutting behaviors are triggered through event subscriptions instead of direct hard coupling, including:

- pipeline change notifications,
- pipeline exceptions,
- on-main-loop execution,
- source load failures,
- workspace rebuild events,
- plugin output and UI signals.

This pattern keeps the runtime responsive and reduces the need for tightly coupled object references scattered throughout the codebase.

The live tuning system is another cross-cutting concern worth noting: `TunerManager` discovers editable fields from the current pipeline via reflection, wires them to Swing controls, and continuously syncs values between the pipeline object and the GUI. This is how the simulator can expose runtime settings like thresholds, blur values, or draw parameters without forcing a full rebuild or restart.

## OpenCV-first execution model

Although the app is a desktop Swing application, it is still fundamentally OpenCV-first. The pipeline is the computational core, while the GUI is a presentation layer. The runtime does not merely display images; it feeds them into actual pipeline logic, collects telemetry, allows runtime tuning, and renders the resulting annotated frame back to the viewport.

This is why the simulator is useful as a development tool: it preserves the actual pipeline execution flow while letting developers inspect and iterate on it in a desktop environment.

## Design takeaways

The major architectural idea is that EOCV-Sim is not just a viewer; it is a runtime container for user pipeline code. It manages:

- config and user settings,
- file-based sources,
- workspace compilation,
- pipeline execution,
- rendering and overlay statistics,
- plugin loading and trust review,
- and a desktop GUI that ties it all together.

That combination makes the project feel like a small but full-featured robotics development environment rather than a simple demo app.
