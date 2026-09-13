# Vision and Common libraries

## Purpose of the shared layers

The `Common` and `Vision` modules are the underlying infrastructure that gives the app its modularity and runtime flexibility. Together, they separate the high-level application logic from the lower-level concerns of lifecycle coordination, image rendering, and OpenCV integration.

The subprojects are important because they let the app be built as a layered system rather than a single giant package with all behaviors tied together.

## `Common` module: shared foundation

The Common module contains the reusable infrastructure that most of the app relies on. It includes things like:

- orchestration abstractions (`Orchestrator`, `Orchestrable`, `PhaseOrchestrableBase`)
- logging helpers and concurrency utilities
- serialization support
- pipeline statistics and utility logic
- shared file and environment helpers
- other general-runtime constructs that are not app-specific

This module is the “hidden backbone” of the architecture. It provides the behavior that keeps the app consistent without duplicating code across subsystems.

### Lifecycle abstraction in Common

The `Orchestrator` pattern is one of the most important parts of the Common layer. It gives the app a standard way to initialize and destroy major subsystems, which is essential when a system has several dependencies and must be assembled in order.

The Common module is therefore where the app’s runtime lifecycle model is defined, even though the actual application logic lives in the main module.

### Utility and general-purpose behavior

The Common layer also includes general-purpose support for logging, serialization, metadata, and other operational functions that are not directly tied to GUI or pipeline logic. In a large Java/Kotlin project, this shared layer is what prevents repeated wiring and repeated helper code.

## `Vision` module: rendering and display adaptation

The Vision module is the part of the project that bridges OpenCV and the Swing desktop app. It is where the simulator translates the live Mat pipeline output into something the user can see and interpret visually.

Important classes include:

- `SwingOpenCvViewport.kt`
- `OpenCvViewRenderer.java`
- `SkiaPanel.kt`
- FTC/OpenCV wrapper classes in the `external` and `internal` packages

### `SwingOpenCvViewport`

`SwingOpenCvViewport` is the desktop viewport implementation. It is responsible for:

- managing the rendering state,
- receiving posted Mats from the pipeline,
- creating the Skia rendering surface,
- scaling and painting the image,
- notifying output posters,
- and supporting pause/resume/activate/deactivate states.

It is the concrete viewport implementation used by the main `Visualizer` screen.

### `OpenCvViewRenderer`

`OpenCvViewRenderer` is the lower-level renderer that draws the live image and overlays. It has a number of responsibilities:

- scale the source image to fit the viewport without distortion,
- draw a black background behind the image to avoid alpha problems,
- render the `Mat` bitmap on a canvas,
- allow user draw hooks to add annotations,
- and draw the FPS and timing overlay when enabled.

This is where the on-screen statistics overlay is treated as a rendering feature rather than a UI button. The renderer knows how to draw the current FPS, pipeline time, and overhead time.

### FPS meter integration

The FPS meter is attached to the viewport through the `setFpsMeterEnabled()` flow and is respected by the renderer. This is a good example of the layered design in action:

- the app’s settings layer toggles a config property,
- the visualizer reads it and forwards it to the viewport,
- the viewport forwards it to the renderer,
- the renderer decides whether to draw the stats overlay.

This keeps the actual rendering behavior in the correct layer and avoids coupling the settings system directly to the low-level canvas logic.

## Relationship between the layers

The separation is intentional:

- `Common` = shared operational infrastructure
- `Vision` = display and OpenCV adaptation layer
- `EOCV-Sim` = app-level runtime, config, GUI, workspace, and pipeline orchestration

This arrangement keeps the code easier to maintain and lets each subsystem specialize in what it does best.

## Why this split is valuable

This multi-module model is important because it keeps the application from becoming a single giant runtime blob.

It enables the project to support:

- app-level orchestration without mixing in rendering code,
- OpenCV viewport logic without embedding it inside the desktop GUI,
- shared utilities without duplicating logic across the app,
- and a cleaner path to future extensions or alternate visual backends.

## Summary

The `Common` and `Vision` modules are the reusable foundation of the simulator. They provide the runtime scaffolding and rendering layer that make the higher-level desktop app possible. Without these layers, the rest of the codebase would be much harder to maintain and considerably less modular.
