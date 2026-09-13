# Configuration and settings

## Purpose of the config system

The configuration subsystem is responsible for persistent runtime preferences and defaults. It sits at the boundary between the user-facing simulator settings and the more operational systems such as the input manager, pipeline manager, workspace system, and plugins.

This subsystem is implemented mainly in:

- `../VisionBench/src/main/java/com/github/serivesmejia/eocvsim/config/Config.java`
- `../VisionBench/src/main/java/com/github/serivesmejia/eocvsim/config/ConfigManager.kt`
- `../VisionBench/src/main/java/com/github/serivesmejia/eocvsim/gui/dialog/Configuration.kt`

## `Config.java`: the settings model

The central data model is `Config`, a plain Java object with a set of persistent runtime fields. It stores user-visible and app-level preferences such as:

- `simTheme` — the active UI theme
- `pipelineMaxFps` and `pipelineTimeout` — processing constraints for pipeline execution
- `pauseOnImages` — pause behavior when switching to image sources
- `showFpsMeter` — toggles the viewport overlay
- `webcamOpenTimeoutSec` and `webcamNewFrameTimeoutSec` — camera/source timeouts
- `videoRecordingSize` and `videoRecordingFps` — output recording settings
- `workspacePath` — the default project workspace used by the simulator
- `globalTunableFieldsConfig` — default style and behavior for tunable fields
- `specificTunableFieldConfig` — per-field overrides
- `autoAcceptSuperAccessOnTrusted` — trust/approval behavior for plugins
- `flags` — a dictionary of feature toggles and user state flags

The class is intentionally simple and serialization-friendly. It is not a framework abstraction; it is the state container the rest of the app reads and writes.

## `ConfigManager`: loading and saving

`ConfigManager` is the lifecycle owner for the config. It is responsible for:

- loading config from the app’s persisted file,
- creating a default config when missing,
- saving the current config back to disk,
- exposing the live object through `config`.

This manager follows the project’s broader pattern: a top-level runtime service exposes state to the rest of the app via dependency injection, while also handling persistence and validation.

The typical flow is:

1. app startup loads the config file,
2. config values are read by the systems that need them,
3. the settings UI updates the live config,
4. the user accepts changes,
5. `saveToFile()` writes everything back to disk.

## Settings dialog and UX

The user-facing settings window is `Configuration.kt`. It is the main place where the user edits runtime settings. The dialog organizes settings into categories such as:

- `Interface` — UI themes, startup preferences, FPS overlay, visible behavior
- `Input Sources` — image/camera settings and timeouts
- `Processing` — pipeline timing and output behavior
- potentially other advanced options tied to workspace and plugin behavior

The important UX distinction is that the dialog does not mutate the config object immediately in all cases; instead, it gathers user selections and then applies them when the user confirms. This has the benefit of preserving a clean “accept/cancel” flow and avoids accidental mid-edit state changes.

## Relationship to other subsystems

The configuration object is a central dependency for many modules. For example:

- `Visualizer` reads `config.simTheme` and `config.showFpsMeter` to set up the viewport and theme
- `InputSourceManager` reads `pauseOnImages` when selecting image sources
- `PipelineManager` and related logic use timing settings for processing constraints
- plugin trust preferences affect `PluginManager` behavior
- workspace settings influence the default project root and compiler behavior

This is a good example of the project’s architecture: instead of each subsystem keeping duplicate settings, the app relies on the central config manager as the single source of truth.

## Important settings and their role

A few values are especially important in the runtime:

- `pauseOnImages` — used to pause image sources for single-frame analysis or inspection
- `showFpsMeter` — toggles the live visual statistics overlay in the viewport
- `autoAcceptSuperAccessOnTrusted` — helps trusted plugins work without repeated approval prompts
- `workspacePath` — default root folder for the user workspace
- `flags` — arbitrary feature toggles used for first-run guides, startup preferences, plugin disclaimers, and other user state

These settings are not “cosmetic only.” They affect the actual behavior of the live simulator.

## Why this subsystem matters

The app’s quality as a development tool depends heavily on this settings layer. Without a central config model, the app would constantly scatter preferences across many classes and duplicate logic. By consolidating all user configuration in one model, the project keeps the runtime easier to reason about and easier to save, restore, and expose in the UI.
