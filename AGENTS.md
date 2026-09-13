# AGENTS

This repository is the EOCV-Sim desktop simulator for OpenCV/FIRST Tech Challenge-style pipelines.

Quick orientation:

- `VisionBench/` contains the main app runtime, workspace tooling, configuration, GUI, plugins, and pipeline orchestration.
- `Common/` contains shared lifecycle/orchestration utilities, event handling, and cross-cutting runtime helpers.
- `Vision/` contains the rendering, camera compatibility, and FTC/OpenCV compatibility layer used to display and process frames.
- `TeamCode/` contains sample FTC-style pipelines and example code.

For deeper architecture and subsystem details, start from the documentation index:

- `documents/README.md` — index of architecture docs and reading order
- `documents/01-overview-and-architecture.md` — repository structure and lifecycle overview
- `documents/02-main-eocvsim.md` — main app bootstrap and runtime shell
- `documents/03-config-and-settings.md` — configuration and settings flow
- `documents/04-gui-and-visualizer.md` — GUI and viewport host
- `documents/05-pipeline-runtime.md` — pipeline execution, timeouts, and failure handling
- `documents/06-input-sources.md` — input source acquisition and runtime sources
- `documents/07-workspaces.md` — workspace compile/reload flow
- `documents/08-plugins-and-superaccess.md` — plugin/security architecture
- `documents/09-vision-and-common-libraries.md` — rendering and shared foundation layers
- `documents/10-runtime-utilities.md` — EventHandler, orchestration, and common runtime utilities
- `documents/11-tunable-fields.md` — live pipeline tuning and reflected field editing

In short: the app is organized around a lifecycle-driven runtime, a pipeline execution engine, a desktop viewport/rendering layer, and a plugin/workspace ecosystem. The docs in `documents/` are the preferred place to understand the architecture before changing code.
