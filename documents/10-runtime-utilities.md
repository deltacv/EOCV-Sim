# Runtime utilities and infrastructure

The codebase has a few foundational utility systems that are easy to overlook because they are not tied to a single screen or a single subsystem. These utilities quietly power lifecycle management, listener-driven notifications, and runtime scanning. They are some of the most important pieces to understand when working on the simulator beyond the GUI and pipeline code.

## Event handling: `EventHandler` and `ParamEventHandler`

The shared event system is the app’s main way to react to runtime signals without maintaining direct references between unrelated subsystems. This prevents a large number of managers from tightly coupling to each other.

The base abstraction is `ParamEventHandler<T>`, which supports a typed payload and has a general listener lifecycle:

- `attachPayload(listener)` registers a persistent listener
- `oncePayload(listener)` registers a listener that fires once
- `run(payload)` dispatches the event to all listeners
- `removeListener(id)` removes a specific listener
- `removeAllListeners()` clears the registry

The implementation is intentionally concurrency-aware. It keeps separate queues for persistent and one-shot listeners and defers listener registration/removal while dispatch is in progress. That matters because listeners may add or remove other listeners as an event is firing, and the system needs to avoid a concurrent modification or a mid-dispatch mutation problem.

The legacy zero-argument API is still exposed as `EventHandler` for compatibility. It behaves like `ParamEventHandler<Unit>` and keeps the older syntax:

- `attach { ... }`
- `once { ... }`
- `listener.removeListener()` via the listener context
- `invoke(...)` operator overload

The `EventListenerContext` is the principal way a listener can self-remove. It gives the callback a handle to the owning `EventHandler` and its `EventListenerId`, so a listener can decide to unregister itself when a condition is met.

`callRightAway` controls a useful edge feature: a listener may be invoked immediately after registration, either inline or in a provided coroutine scope. That is helpful for integration points that want an event to trigger once right away instead of waiting a full tick.

Typical uses in the app include:

- pipeline change notifications
- workspace rebuild events
- plugin output and status signals
- UI refresh hooks
- lifecycle-driven update callbacks

In short, this system is the app’s “localized pub-sub layer”: the producer emits an event, and any interested subsystem reacts without the producers needing to know the consumers by name.

## Lifecycle orchestration: `Orchestrator`, `Orchestrable`, and `PhaseOrchestrableBase`

The app’s startup and shutdown flow is coordinated by the orchestration system in `Common`. The core idea is simple: major subsystems expose lifecycle hooks and dependencies, and the orchestrator executes them in the correct order.

### Core types

- `Orchestrable`: a minimal interface with `wire(orchestrator)`
- `PhaseOrchestrable`: adds `init()`, `run()`, and `destroy()` hooks
- `PhaseOrchestrableBase`: convenience base class that holds a `PhaseDependencies` registry
- `Orchestrator`: executes phased tasks in dependency order

### Phase model

The orchestrator supports three lifecycle phases:

- `INIT`
- `RUN`
- `DESTROY`

Each target may register a callback for one or more phases and declare dependencies for that phase. Dependencies are resolved before a target executes, which makes startup ordering deterministic and helps avoid race conditions during app initialization.

A subsystem can declare:

- that it depends on another manager during initialization
- that it can only run once its dependencies are active during runtime
- that it must tear down after other systems finish their teardown

This is exactly the pattern used by the main app to ensure that config, GUI, input sources, workspace, and plugin systems are created in a safe sequence.

### Dependency DSL

The API exposes a small DSL for declaring dependencies. Common patterns look like this:

- `phase(Orchestrator.Phase.INIT) { target { init() } }`
- `dependsOn(ConfigManager::class)`
- `dependsOn(workspaceManager)`
- `dependency(PluginManager::class, Orchestrator.Phase.RUN)`

The orchestrator then builds an execution graph for the phase and resolves it in waves. If a dependency cycle exists, it fails early with a clear dependency error instead of silently continuing into an invalid runtime state.

### Why this matters

This is one of the biggest architecture decisions in the project. Instead of manually calling initialization code in a giant startup method, the app registers subsystems with explicit lifecycle contracts. That provides several benefits:

- predictable startup ordering
- less brittle runtime wiring
- cleaner teardown behavior
- easier extension of managers without touching global bootstrap logic

For example, the visualizer, input source manager, plugin manager, workspace manager, and pipeline manager are all registered as lifecycle-aware components, which lets the runtime treat them as a coherent system rather than a set of loosely related objects.

## Other major utilities that deserve dedicated documentation

A few additional utilities are important enough to merit their own guides in the future:

- `ClasspathScan` — scans the classpath for pipeline classes and plugin bundles using ClassGraph
- `ReflectUtil` — reflective subclass and annotation utility work used during discovery
- `SysUtil` and `JavaProcess` — OS process and runtime helpers used for external launch and environment control
- shared logging and serialization helpers — they provide a common runtime foundation across modules

These are not as central as `EventHandler` and orchestration, but they are the kinds of utilities that can quietly become critical to future maintenance work if left undocumented.

## Summary

If you need to understand how EOCV-Sim keeps the app coordinated without a big monolithic startup method, the answer lives in these utility layers:

- the event system coordinates notifications and UI/runtime callbacks
- the orchestration layer coordinates lifecycle dependencies and phased initialization
- the scanning/utilities layer handles runtime discovery and environment integration

Together they form the backbone of the software architecture and are worth reading before diving into any specific subsystem.
