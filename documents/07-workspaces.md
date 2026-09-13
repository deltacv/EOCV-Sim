# Workspaces subsystem

## Purpose

The workspace subsystem is the developer-facing part of the simulator. It allows users to work in a project-like folder structure where source files and resources live separately from the app code itself, then get recompiled and reloaded at runtime.

The main code lives in:

- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/workspace/WorkspaceManager.kt`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/workspace/config/`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/workspace/util/`
- `EOCV-Sim/src/main/java/com/github/serivesmejia/eocvsim/pipeline/compiled/CompiledPipelineManager.kt`

## What a workspace is

A workspace is a directory that contains the project files the app uses to build runtime pipelines. It is not just a random folder; it is a structured source tree that includes:

- source files under a `src` path,
- resources under a separate resources folder,
- excluded directories and file rules,
- configuration metadata for the workspace,
- and information the compiler uses to build classes.

The workspace manager tracks each of these elements and provides accessors for both the source tree and the resource tree.

## `WorkspaceManager`: the central coordinator

`WorkspaceManager` is the main class in this subsystem. It does the following:

- tracks the active `workspaceFile`
- loads or creates the workspace config
- keeps source and resource paths in sync with the config
- discovers Java source files and resource files
- sets up file watching for real-time updates
- triggers rebuilds when files change
- creates workspaces from templates for new projects

The class is also tightly integrated with the pipeline runtime. It uses the `CompiledPipelineManager` to trigger builds whenever the workspace changes.

## Workspace configuration model

The workspace config is stored and managed by the classes in `workspace/config/`. It contains metadata such as:

- source folder path,
- resources folder path,
- excluded relative paths,
- excluded file extensions,
- version metadata used to maintain compatibility.

The manager automatically saves the config when it changes and recreates it if the existing config cannot be parsed or is missing.

The important point is that this is not ad hoc project state; it is part of the app’s runtime model, and the compiler builds against it as a real workspace definition.

## File watching and live updates

One of the most important features of the workspace subsystem is automatic file watch behavior. `WorkspaceManager` sets up a `FileWatcher` so changes in source/resource folders can trigger rebuilds.

The workflow is:

1. workspace files change,
2. the file watcher notices,
3. `CompiledPipelineManager.asyncBuild()` is called,
4. the user code is compiled into runtime classes,
5. the pipeline manager reloads/updates the available pipeline list.

This makes the app feel much more like a live development environment than a static code runner. It is a core part of why the simulator is useful for iterative pipeline development.

## Compiler integration

The workspace is tightly tied to the pipeline compiler. `CompiledPipelineManager` is the bridge between the file system and the running app. It knows where the default workspace lives, creates compiler output folders, and compiles workspace Java sources into a JAR that is then loaded using a custom class loader.

This means users can edit files in the workspace and then immediately see the simulator detect and load their updated pipeline classes without restarting the whole app.

## Default workspace behavior

The app includes a default workspace and will recreate or update it if the version metadata differs. This makes the simulator self-contained and provides beginners with a ready-to-use starting point.

The default workspace is not just a demonstration; it is the project model the app expects if the user does not choose a custom workspace.

## Template support

The workspace system also includes template support (`WorkspaceTemplate`, `DefaultWorkspaceTemplate`, `GradleWorkspaceTemplate`). This allows the app to create new workspace folders with appropriate scaffolding and default contents rather than forcing the user to set up everything manually.

This is the app’s way of smoothing the on-ramp for new developers.

## Why this subsystem matters

Without workspaces, the app would be limited to bundled code and impossible to extend dynamically. The workspace subsystem gives the simulator its “code in a project folder, compile and reload live” behavior. That is arguably one of the project’s biggest differentiators from a simple OpenCV viewer or demo tool.

It is the layer that ties the app together as a true development environment.
