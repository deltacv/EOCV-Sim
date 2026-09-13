# EOCV-Sim Architecture Documentation

This directory contains subsystem guides for the project’s major runtime areas. The codebase is a multi-module Java/Kotlin application built around a desktop Swing UI, an OpenCV pipeline engine, a workspace-based development flow, and a plugin/security extension layer.

## Repository structure

- `../VisionBench/` — main simulator application module
- `Common/` — shared orchestration and utility libraries
- `Vision/` — OpenCV rendering and viewport adaptation layer
- `TeamCode/` — sample FTC-style pipeline code and examples
- `doc/` — additional project documentation and reference material

## Documentation map

- [01-overview-and-architecture.md](./01-overview-and-architecture.md) — project structure, lifecycle, and module responsibilities
- [02-main-eocvsim.md](./02-main-eocvsim.md) — bootstrap flow and central runtime orchestration
- [03-config-and-settings.md](./03-config-and-settings.md) — config model and user settings flow
- [04-gui-and-visualizer.md](./04-gui-and-visualizer.md) — Swing UI, control panels, and viewport host
- [05-pipeline-runtime.md](./05-pipeline-runtime.md) — active pipeline lifecycle, execution, telemetry, and exceptions
- [06-input-sources.md](./06-input-sources.md) — images, cameras, and source acquisition
- [07-workspaces.md](./07-workspaces.md) — editable project folders, compiler integration, and watch-based rebuilds
- [08-plugins-and-superaccess.md](./08-plugins-and-superaccess.md) — plugin loading, repository handling, and security prompts
- [09-vision-and-common-libraries.md](./09-vision-and-common-libraries.md) — rendering layer and foundational shared libraries
- [10-runtime-utilities.md](./10-runtime-utilities.md) — event handling, lifecycle orchestration, and shared runtime infrastructure
- [11-tunable-fields.md](./11-tunable-fields.md) — live pipeline tuning, reflection-driven field discovery, and the runtime editor system

## Core execution path

The runtime follows a consistent application flow:

1. `Main.kt` launches the process.
2. `Module.kt` creates the Koin dependency graph.
3. `EOCVSim.start()` orchestrates the app lifecycle in phases.
4. Config is initialized and loaded from disk.
5. The GUI is built and the viewport is prepared.
6. Input sources and default workspaces are selected and initialized.
7. The active pipeline is chosen and run against frames from the source.
8. The output is rendered and statistics are displayed.
9. Plugins and file watching continue to operate during runtime.

This architecture intentionally separates the “runtime engine” from the “desktop experience,” while still making them work together as one application.

## Reading guide

If you are new to the codebase, a practical reading order is:

- `01-overview-and-architecture.md`
- `02-main-eocvsim.md`
- `03-config-and-settings.md`
- `05-pipeline-runtime.md`
- `06-input-sources.md`
- `04-gui-and-visualizer.md`
- `07-workspaces.md`
- `08-plugins-and-superaccess.md`
- `09-vision-and-common-libraries.md`
- `10-runtime-utilities.md`
- `11-tunable-fields.md`

This order moves from overall architecture to runtime behavior and then to subsystems that depend on that runtime.
