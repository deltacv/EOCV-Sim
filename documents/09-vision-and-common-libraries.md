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

For a deeper look at the runtime infrastructure itself, see [10-runtime-utilities.md](./10-runtime-utilities.md). That companion guide focuses on the most important cross-cutting abstractions: the event system, lifecycle orchestration, and utility classes that support startup and runtime coordination.

## `Vision` module: rendering and display adaptation

The Vision module is the part of the project that bridges OpenCV and the Swing desktop app. It is where the simulator translates the live Mat pipeline output into something the user can see and interpret visually.

In practice, the module is bigger than a simple Swing helper. It includes the compatibility layer that lets EOCV-Sim mimic FTC camera APIs, a desktop viewport layer for rendering frames, and a source abstraction for webcam or image-backed inputs. The result is a system that feels like a real FTC camera pipeline but runs on a desktop JVM.

Important classes include:

- `SwingOpenCvViewport.kt` — desktop Swing + Skia viewport implementation
- `OpenCvViewRenderer.java` — actual canvas drawing and overlay logic
- `SkiaPanel.kt` — Swing host surface used by the render layer
- `OpenCvViewport.java` — FTC/OpenCV viewport contract
- `VisionSource.java` / `VisionSourceBase.java` — abstraction for incoming image sources
- `FrameReceiverOpenCvCamera.java` — bridge between a source and FTC camera semantics
- `VisionPortalImpl.java` — compatibility shim for FTC `VisionPortal` usage
- `VisionProcessor.java` and related FTC wrappers — processor APIs simulated in the desktop runtime

### The bigger scope of the `Vision` module

The `Vision` module is not just “browser-like display code.” It is effectively the app’s compatibility and rendering backbone. It has three distinct responsibilities:

1. display and paint an image stream to a Swing-based viewport,
2. emulate FTC/OpenCV camera and processor abstractions so user code can be written in a familiar API,
3. bridge external input sources (webcams, files, camera-like streams) into the OpenCV pipeline model.

This is why the module is central to the project’s goal: it lets a user pipeline written against FTC-style APIs run in a desktop environment without needing a robot or Android runtime.

### `OpenCvViewport` and the render contract

The `OpenCvViewport` interface is the low-level contract used by the runtime to render and display frames. It defines operations such as:

- `post(Mat frame, Object userContext)` — enqueue a processed frame for display
- `setFpsMeterEnabled(boolean)` — enable/disable the stats overlay
- `pause()` / `resume()` / `activate()` / `deactivate()` — control viewport state
- `notifyStatistics(float fps, int pipelineMs, int overheadMs)` — send render analytics
- `setRenderHook(RenderHook)` — allow user drawings or annotations to be added

The render hook is important because it allows a pipeline’s `onDrawFrame(...)` contract to be mirrored in the desktop app. The user can draw overlays on top of the image, and those hooks are executed in the same conceptual place as they would be on-device.

This means the viewport layer is not purely a display widget. It is part of the same API contract that the pipeline runtime expects from FTC/OpenCV cameras.

### `OpenCvViewRenderer`: scaling, overlays, and frame composition

`OpenCvViewRenderer` is where most of the visual drawing logic sits. It has to do a number of tricky things correctly:

- keep the image centered and scaled to fit an arbitrary viewport size,
- preserve the source aspect ratio,
- draw a black background so transparent or semi-transparent OpenCV output does not produce artifacts,
- convert `Mat` to a `Bitmap` for Android/Skia compatibility,
- apply rotation policies when the image should be optimized for view orientation,
- and draw the FPS / timing overlay when enabled.

The `unifiedDraw()` method is the main rendering pass. It calculates the scaled destination rectangle, draws the bitmap in the correct location, calls any user `RenderHook`, and then overlays the stats box. This is the direct visual equivalent of the FTC camera monitor view: a real image, with optional overlays and diagnostics.

The FPS meter itself is intentionally part of the rendering layer, not the app config layer. Its content is produced from statistics already gathered by the pipeline runtime, then drawn on the canvas by the renderer. That places the logic at the correct layer and keeps it independent from the settings system.

### `SwingOpenCvViewport`: the desktop assembly layer

`SwingOpenCvViewport` is the concrete implementation that actually plugs into the Swing UI. It does several things at once:

- creates and owns a `SkiaLayer` for rendering within Swing,
- holds a frame queue for live preview images,
- uses a `MatRecycler` to avoid repeated allocations during hot render loops,
- tracks active/paused/stopped viewport state,
- posts output mats to downstream posters or screenshot-like output consumers,
- and reuses a last frame when the viewport is paused.

The queue is important. A live preview path should never block the whole app on a big render event. Instead, the viewport keeps a small `EvictingBlockingQueue` of recent frames and chooses the most relevant frame for the next paint cycle. It also returns recycled mats to a buffer so native memory is reused rather than repeatedly allocated.

This is a very desktop-native implementation of the same general idea found in Android camera preview rendering: accept frames from the pipeline, store or queue them, and draw them to the user-facing surface with minimal latency.

### Source abstraction: `VisionSource`, `VisionSourceBase`, and `FrameReceiver`

The desktop motion picture side of the Vision module is not just a viewport. It also exposes a generalized camera-source abstraction for image acquisition.

The key pieces are:

- `VisionSource` — a source that can start, stop, attach receivers, and provide a frame stream
- `VisionSourceBase` — a reusable worker-thread implementation that polls frames and distributes them to attached consumers
- `FrameReceiver` — a consumer contract for a frame stream

This is the part of the code that decouples “where the image comes from” from “how it is rendered.” A source can be backed by a real webcam, a recorded stream, or some other capture mechanism; the rest of the system only cares that it emits `Mat` frames with timestamps and can be attached to a consumer.

`VisionSourceBase` runs a helper thread that repeatedly calls `pullFrame()`, then pushes each frame to any attached `FrameReceiver`s. This architecture allows the app to treat camera-like data as a generic feed rather than tying the rest of the code to a specific hardware implementation.

### Camera compatibility: `FrameReceiverOpenCvCamera`

`FrameReceiverOpenCvCamera` is one of the most revealing classes in the module. It acts as a compatibility shim between the abstracted input source and the FTC/OpenCV camera model.

It extends `OpenCvCameraBase` and implements `OpenCvWebcam` and `FrameReceiver`, which means it can behave like a real FTC webcam in the rest of the codebase while actually receiving frames from a `VisionSource` abstraction. The class does things like:

- open/close the camera device,
- start and stop streaming,
- request image sizes and rotations,
- route camera controls to a `CameraControlMap`,
- call `handleFrameUserCrashable(frame, timestamp)` when a new frame arrives.

This class is critical because it lets EOCV-Sim mimic the same camera lifecycle expected by user code written for FTC. The app is therefore not merely drawing a frozen image; it is reconstructing the expected camera runtime semantics around a desktop input source.

### `VisionPortal` compatibility layer

`VisionPortalImpl` is another important compatibility layer. It recreates the FTC `VisionPortal` lifecycle for processor-based pipelines:

- it creates the underlying camera,
- opens it asynchronously,
- starts the stream at a chosen resolution,
- assigns a `ProcessingPipeline`,
- initializes each attached `VisionProcessor`,
- and updates the enabled/disabled processor state.

The pipeline created here is a `TimestampedOpenCvPipeline`, which is a variant of the standard pipeline that carries capture timestamps. This matches FTC’s real-time processing model more closely than a plain `OpenCvPipeline` and is part of the reason these pipelines feel familiar even when they run on a desktop app.

This is a major part of the Vision module’s hidden work: it is not just presenting images, but preserving the FTC API surface so user code is minimally disrupted when moved from robot hardware to the simulator.

### `VisionProcessor` ecosystem and AprilTag support

The module also contains FTC-compatible processor implementations such as:

- `VisionProcessor.java`
- `ColorBlobLocatorProcessor*`
- `PredominantColorProcessor*`
- AprilTag-related classes under `org.firstinspires.ftc.vision.apriltag`

These provide the expected FTC-style processing objects used by real robotics vision work. The app does not need to reimplement every algorithm in full to be useful; instead, it provides the API surface and compatibility layer so that a vision pipeline can run and be tested against sample input.

This is a good example of how the project intentionally reproduces the platform model rather than trying to invent a completely separate pipeline abstraction. The goal is to make the simulator feel “real” to FTC developers.

### `SkiaPanel` and Swing integration

`SkiaPanel.kt` is the Swing host that allows the viewport to draw through the Skia rendering stack. It sits between Swing/JComponent and the native rendering logic and is responsible for binding the renderer to a GUI component. This keeps the preview surface in the Swing thread model while still using Skia for actual canvas drawing.

The result is a hybrid architecture:

- Swing handles the desktop layout and component lifecycle,
- Skia handles rendering,
- OpenCV handles the image data,
- and the FTC compatibility layer preserves the expected programming model.

### Why this layer is more than a UI wrapper

The Vision module is important because it is the place where EOCV-Sim solves the hardest compatibility problem: how to run FTC-oriented vision code on a desktop app without changing the user-facing API.

That is why the project can offer:

- a live desktop preview,
- camera-like lifecycle semantics,
- processor and pipeline compatibility,
- rotation/rendering policies,
- and output hooks that behave like a real OpenCV viewport.

The layer is therefore not a cosmetic visualizer. It is one of the central runtime abstractions that makes the simulator credible as a development tool.

## Relationship between the layers

The separation is intentional:

- `Common` = shared operational infrastructure
- `Vision` = display and OpenCV adaptation layer
- `VisionBench` = app-level runtime, config, GUI, workspace, and pipeline orchestration

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
