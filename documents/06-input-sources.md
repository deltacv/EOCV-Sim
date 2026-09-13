# Input sources subsystem

## Purpose

The input source subsystem is the source-of-truth for media flowing into the simulator. It decides which input stream is active, loads it safely, and converts it into the OpenCV `Mat` format expected by pipelines.

The main runtime classes are:

- `../VisionBench/src/main/java/com/github/serivesmejia/eocvsim/input/InputSourceManager.kt`
- `../VisionBench/src/main/java/com/github/serivesmejia/eocvsim/input/InputSource.kt`
- `../VisionBench/src/main/java/com/github/serivesmejia/eocvsim/input/InputSourceInitializer.kt`
- `../VisionBench/src/main/java/com/github/serivesmejia/eocvsim/input/InputSourceLoader.kt`
- `../VisionBench/src/main/java/com/github/serivesmejia/eocvsim/input/source/`

## High-level role in the app

The subsystem sits directly between the environment and the processing pipeline:

- input sources produce frames or images,
- the pipeline runtime consumes them,
- the viewport displays the processed result.

The producer/consumer chain is therefore:

source → `InputSourceManager` → pipeline runtime → viewport

## `InputSourceManager`: runtime source controller

`InputSourceManager` is the main controller for the whole source layer. It keeps track of:

- the currently active source (`currentInputSource`)
- the most recently acquired frame (`lastMatFromSource`)
- the full set of available sources (`sources`)
- the default source fallbacks
- per-source initialization and failure handling
- source selection and deletion logic

The manager also owns a `MatRecycler` buffer pool. This is a deliberate optimization: instead of repeatedly allocating new native OpenCV matrices for each frame, the runtime reuses buffers for performance and lower churn.

## Source types supported by the app

The codebase includes multiple concrete source implementations under `input/source/`:

- `ImageSource` — loads image files and returns them as frames
- `VideoSource` — reads video streams or files
- `CameraSource` — interacts with a webcam or camera device
- `HttpSource` — streams from an HTTP endpoint
- `NullSource` — an empty placeholder used when there is no actual source

This design matters because the simulator needs to support a broad range of realistic inputs without coupling the pipeline runtime to a single external medium.

## Default startup sources

At startup, the app creates a few default packaged image sources. These are bundled demo assets, such as sample Ultimate Goal images, which let the simulator show meaningful output right away before the user adds their own content.

If no valid source exists, it falls back to a `NullSource` to avoid a crash and keep the app usable.

## Initialization and timeout logic

A source is not simply “used” once it is selected. `InputSourceInitializer` handles initialization tasks such as:

- opening the source,
- waiting for a successful state,
- applying timeouts,
- canceling slow initialization,
- logging and signaling failures.

This is needed because sources may depend on hardware, disks, or network endpoints that are not always immediately ready. The manager surfaces failures by raising `onInputSourceInitError` and can fall back to a previous source or default source if needed.

## Frame acquisition and conversion

During runtime, the manager repeatedly pulls frames from `currentInputSource` and converts them into the RGBA (`CV_8UC4`) format expected by the viewport renderer.

The conversion path effectively does:

- request `update()` from the source,
- check that a valid non-empty `Mat` was produced,
- convert from RGB to RGBA using OpenCV color conversion,
- recycle the old `Mat` buffer in the recycler,
- place the new frame in `lastMatFromSource`.

This is one of the core low-level runtime steps that makes the rest of the simulator work.

## Pause behavior for image sources

The config setting `pauseOnImages` is used here. When enabled, and the selected source is an image, the app can pause the pipeline after the frame is loaded in order to analyze that image once rather than continuously processing a static image with no new frames.

This is especially useful for debugging still-image pipelines, where the user wants single-frame analysis and a stable equation rather than constant reprocessing.

## Loading and persistence of user-created sources

`InputSourceLoader` stores user-created sources to disk, which allows the app to restore the same set of input sources on next launch. This is important because users often create custom image, camera, or URL-based sources and expect them to remain available across sessions.

This gives the app a “project-like” concept of input configuration, not just ephemeral runtime state.

## Relationship to the pipeline runtime

The input system and pipeline runtime are intentionally decoupled:

- the input manager knows how to fetch frames,
- the pipeline manager knows how to execute logic on a given frame,
- the rendering layer knows how to display the output.

This separation makes the app more modular and easier to reason about. A user can change the input source without rewriting the pipeline code, or revisit the pipeline logic without altering how frames are acquired.

## Why this subsystem matters

The input source system is one of the app’s foundational runtime layers. Without it, the simulator has no data to send to the pipeline, and the visualizer would be empty. It also supports the app’s “desktop development” story by letting users load real image samples, local video, or live camera-based sources for experimentation and debugging.
