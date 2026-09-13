# GUI and visualizer subsystem

## Purpose and scope

The GUI subsystem is the desktop shell around the simulator. It is responsible for the user-facing window, viewport display, control panels, dialogs, and interaction points that let a user select sources, choose pipelines, tune variables, and inspect runtime output.

The main implementation is `Visualizer.kt`, but the subsystem also includes related UI components in packages such as:

- `gui/component/visualizer/`
- `gui/component/tuner/`
- `gui/dialog/`
- `gui/component/input/`

This is one of the most visible parts of the app because it is where the actual “simulator” experience is presented.

## The `Visualizer` class

`Visualizer` is the main Swing host. It extends `PhaseOrchestrableBase` and participates in the app lifecycle, which means it has explicit init and teardown behavior and is created as a managed runtime component.

It owns and builds the primary application window, including:

- the main `JFrame`
- the viewport area where OpenCV output is displayed
- the menu bar and window title
- the sidebar pipeline/opmode tabs
- source selector and pipeline selector panels
- the tuner UI area
- plugin-attached GUIs

The visualizer gathers runtime dependencies from Koin, including the config manager, input source manager, pipeline manager, workspace manager, and recording manager.

## Viewport and rendering setup

The visualizer creates a `SwingOpenCvViewport` from the `Vision` module:

- `viewport` is instantiated with a default size and descriptor string
- the FPS meter descriptor is used to generate the overlay label text
- the viewport is initialized and styled based on theme
- the FPS meter is enabled or disabled according to config

This viewport is the place where live image output, pipeline overlay, and timing statistics are visible. It is also the target of the active pipeline’s output publishing.

## Rendering flow from pipeline to screen

The render path is one of the most important parts of the system:

1. the active input source produces a `Mat`
2. the pipeline processes that `Mat`
3. the pipeline manager posts the output to registered output posters
4. the viewport receives those output matrices
5. the viewport renders them to a Swing/Skia canvas
6. if enabled, the FPS meter and timing stats overlay are drawn
7. user pipeline draw hooks can add additional annotations on top

The render hook is a key concept here. `Visualizer` wires an OpenCv viewport `RenderHook` that calls the active pipeline’s `onDrawFrame()` when initialized. This lets user pipelines annotate the output frame without the pipeline manager needing to coordinate UI drawing directly.

## Sidebar and source controls

The app’s UI is built as a collection of stateful panels. The sidebar contains tabs for:

- pipeline selection
- opmode selection
- other control workflows that may be attached by plugins

The visualizer creates and holds references to these panel objects:

- `sidebarPanel`
- `sidebarPipelineTabPanel`
- `sidebarOpModeTabPanel`
- `pipelineSelectorPanel`
- `sourceSelectorPanel`
- `opModeSelectorPanel`
- `tunerCollapsible`

This is the central user control surface of the app. It lets the user switch between sources and pipelines while watching the model run in real time.

## Dialogs and modal windows

The UI relies on `DialogFactory` to centralize modal dialogs. These dialogs cover many runtime actions, including:

- splash screens and initialization messages,
- welcome screens,
- plugin output,
- about and help dialogs,
- workspace creation,
- config dialogs,
- pipeline output and error windows,
- source-loading errors,
- security review prompts such as SuperAccess approval.

This keeps the UI logic cleaner and reduces scattered Swing instantiation across the codebase.

## Event-driven UI behavior

The visualizer responds to runtime events, not just static UI actions. It listens for state changes like:

- source initialization errors,
- plugin GUI attachments,
- pipeline changes,
- lifecycle events,
- startup completion.

This makes the UI feel like a live control surface rather than a static window. It is especially important in a simulator where user code, source files, and plugin behaviors can change at runtime.

## Theme and lifecycle integration

The visualizer is also theme-aware. It installs the configured UI theme at initialization time and adapts the viewport’s dark mode state accordingly. It also manages frame title updates, maximize state, taskbar icon behavior, and the close action depending on lifecycle signals.

This keeps the desktop experience polished while still leaving the low-level rendering and runtime logic in the correct architectural layers.

## Why the GUI subsystem matters

The GUI looks like the app’s “front end,” but it is more than just a shell. It is where the simulator makes its main value visible: it gives users a controlled, interactive environment to run, inspect, and tune OpenCV-based workflows. The actual computations still happen in the pipeline and input systems, but the visualizer is the component responsible for exposing them in a usable way.
