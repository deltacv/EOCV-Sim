# Tunable fields and the tuning runtime

The tuner system is the part of EOCV-Sim that lets you modify pipeline values while the app is running. It is one of the most important developer-facing features in the project, because it turns a static OpenCV pipeline into a live, inspectable runtime object you can adjust without restarting the simulator.

This subsystem is centered on `TunerManager`, but it also depends on reflection helpers, field registries, and Swing UI panels. Together they provide a loop that reads field values from the active pipeline, presents them in the GUI, and writes new values back into the pipeline object when the user changes them.

## High-level purpose

The tuner system exists to bridge three layers:

- the running pipeline object,
- the GUI controls shown in the visualizer,
- and the reflection metadata needed to discover and mutate fields safely.

The app does not hardcode a list of editable pipeline values. Instead, it inspects the live pipeline instance using `VirtualReflection`, finds fields compatible with a tunable contract, and exposes them through a generic `TunableField` abstraction.

This is why a pipeline author can simply write public fields like `blur`, `threshold`, or `colorRange` and have them appear in the tuner UI automatically.

## `TunerManager`: the runtime coordinator

`TunerManager` is the central coordinator for the tuning subsystem. It is registered as a lifecycle-aware object in the main app orchestration and participates in the same init/run/destroy phases used by other major managers.

Its responsibilities are straightforward:

- listen for pipeline changes,
- refresh the list of tunable fields for the active pipeline,
- create GUI panels for each field,
- update field values from the pipeline while it runs,
- and propagate user edits back into the live object.

The main workflow is:

1. `init()` attaches a listener to `pipelineManager.onPipelineChange`
2. `refreshFields()` inspects `pipelineManager.reflectTarget`
3. `addFieldsFrom(pipeline)` enumerates fields through the virtual reflection layer
4. each field is wrapped in a `TunableField` instance when a registry matches it
5. the visualizer gets a list of `TunableFieldPanel`s and shows them to the user
6. each frame, `run()` walks the field list and calls `field.update()`

This keeps the UI synchronized with the actual runtime state, even while the pipeline is processing frames continuously.

## Discovery: the reflection layer and registry

The system does not discover tunable fields by scanning arbitrary Java types at runtime with ad hoc logic. Instead, it relies on the `VirtualReflection` abstraction and a registry object called `TunableFieldRegistry`.

`TunableFieldRegistry` does two things:

- it maps exact Java types to field handlers, such as `Integer`, `Double`, `Float`, `String`, `Boolean`, `Scalar`, `Point`, `Rect`
- it accepts additional field types through `TunableFieldAcceptor` implementations, allowing more specialized forms such as enum-backed values

Then `TunerManager.addFieldsFrom(pipeline)` does the following:

- gets a reflection context for the current pipeline object
- iterates the context’s `fields`
- asks `newTunableFieldInstanceFor(field, pipeline)` for a specific `TunableField` implementation
- adds it to the manager’s `fields` list if it is supported

The registry intentionally filters out `final` fields, because they cannot be safely mutated at runtime. It also unwraps primitive values into boxed equivalents so the same type logic can be reused consistently.

## `TunableField<T>`: the abstraction for one editable property

Each editable property becomes a `TunableField<T>`. This abstraction wraps:

- the target pipeline object,
- the reflected field (`VirtualField`),
- the permitted editing mode (`AllowMode`),
- a set of `TunableValue` objects representing each editable part of the field,
- and the `TunableFieldPanel` that renders it in the GUI.

The base class is intentionally generic and does not assume whether the property is a number, string, boolean, or enum. It exposes a few critical hooks:

- `init()` — called once when the field is first wired up
- `update()` — called each runtime tick to sync the actual Java field with the UI values
- `setPipelineFieldValue(newValue)` — writes the new value back into the pipeline object when the user edits it
- `tunableValues` — the actual value wrappers that are bound to the panel controls

The manager’s `run()` loop repeatedly invokes `field.update()`, which keeps the pipeline object and the UI in sync without requiring the user to manually refresh anything.

## `TunableValue<T>`: a typed GUI value bridge

A field may contain one or more editable sub-values. For example, a `Point` field might expose two numeric values, while an enum field exposes a current selected constant.

To model that, the code uses `TunableValue<T>` subclasses:

- `TunableNumber`
- `TunableString`
- `TunableBoolean`
- `TunableEnum<T>`

Each `TunableValue` includes:

- the current value,
- a supplier that reads the value from the pipeline,
- a consumer that writes the value back to the pipeline,
- and event hooks for pipeline-driven and GUI-driven updates.

The important distinction is:

- `setFromPipeline(newValue)` updates the value when the pipeline changed,
- `setFromGui(guiValue)` writes the user’s input back into the actual object,
- `onPipelineUpdate` fires when a value changes as a consequence of the pipeline,
- `onValueChange` fires when a user or runtime action modifies the wrapped value.

This design allows the UI to react to field changes without directly mutating the pipeline at the wrong time or causing a feedback loop.

## GUI panel layer: `TunableFieldPanel`

The Swing UI is built by `TunableFieldPanel`. Each field gets a panel that renders the right control widgets for its data type:

- numeric values can render as text boxes and optional sliders,
- string values render as text editors,
- enums render as combo boxes,
- and other supported values can be shown as a typed editor depending on the `TunableField` implementation.

The panel does not just display values; it keeps the controls synchronized with the underlying field values. `setTunableFieldPanel()` wires each `TunableValue` to the panel so that when the pipeline value changes, the UI updates via Swing’s event queue.

Important behaviors in the panel layer include:

- switching between text-box mode and slider mode,
- applying config options such as min/max bounds or custom display settings,
- ensuring the UI does not re-enter an update loop when it is already being edited,
- and allowing a panel to request re-evaluation of all config settings.

This is the tangible “live tuning” feature that users interact with in the simulator.

## Configuration and field options

The tuning system also reads configuration through the Swing panel configuration objects, especially `TunableFieldPanelConfig`. That config layer controls how tunable fields are displayed and constrained.

This includes items such as:

- default value ranges,
- color-space behavior for CV values,
- whether the panel is in text-box or slider mode,
- and per-field or global default options.

The config is important because a pipeline may expose a field that should be constrained to a specific numeric range or rendered differently depending on its type. The tuning system is therefore not just raw reflection; it is a curated runtime configuration layer that tailors the value editing experience.

## Why the system matters in EOCV-Sim

The tuning system is one of the features that makes EOCV-Sim feel like a real vision development tool rather than a simple image viewer. It enables the app to let developers do things like:

- slide a threshold up and down live,
- tweak a blur radius without recompiling,
- inspect and modify a pipeline field while the output updates,
- and immediately compare different parameter values in the same runtime session.

In other words, it turns the simulator into an iterative tuning environment. The user does not need to edit source code and rebuild a pipeline just to change one parameter; they can tune it interactively.

## Summary

The tunable field system is a reflective, UI-driven runtime control layer built on top of the active pipeline object. The major moving pieces are:

- `TunerManager` — lifecycle coordinator for the runtime tuning subsystem
- `TunableFieldRegistry` — type-based registration and discovery of editable fields
- `TunableField` — a generic wrapper around a reflected property
- `TunableValue` — typed read/write bridge for each field sub-value
- `TunableFieldPanel` — Swing control layer that renders and edits values

Without this subsystem, the simulator would be much less useful as a development environment, because the user would have to recompile or restart the app after every small parameter change.
