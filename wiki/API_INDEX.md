# Current API index

This page is an orientation index for production public APIs implemented today. Source code remains authoritative for exact signatures.

## `engine-core` — `com.samo.engine.core.api`

### Lifecycle and composition

| Type | Purpose |
| --- | --- |
| `EngineSubsystem` | Final lifecycle sequencing around `initialize`, `start`, `stop`, and `close`. |
| `SubsystemGraph` | Declares subsystem dependency relationships and produces dependency-safe ordering. |
| `SubsystemStartup` | Coordinates ordered subsystem startup and bounded rollback/cleanup when startup fails. |

Usage: [Lifecycle](CORE/LIFECYCLE.md) and [Subsystem composition/startup](CORE/SUBSYSTEM_COMPOSITION.md).

### Timing

| Type | Purpose |
| --- | --- |
| `EngineClock` | Monotonic elapsed-time sampler. |
| `FixedStepAccumulator` | Converts elapsed nanoseconds into exact fixed-rate simulation ticks and interpolation progress. |
| `FixedStepCatchUpPolicy` | Bounds large elapsed-time gaps and per-update catch-up work. |

The engine foundation is locked to a 60 Hz simulation rate at the current stage. Usage: [Timing](CORE/TIMING.md).

### Input response and tick-aligned player input

| Type | Purpose |
| --- | --- |
| `InputResponseSettings` | Immutable deterministic mouse sensitivity/Y-inversion and controller-axis dead-zone/curve response math. |
| `PlayerInputCommand` | Immutable device-neutral input state/edges for one simulation tick. |
| `PlayerInputCommand.DigitalAction` | Fixed nine-action digital command vocabulary independent of platform input types. |
| `PlayerInputCommand.DigitalState` | Immutable scalar plus pressed/held/released state for one digital action. |
| `PlayerInputCommandCodec` | Explicit fixed-size version-1 ByteBuffer codec for replay/storage round trips. |

`InputResponseSettings.defaults()` preserves raw mouse/controller scalar values. Mouse response multiplies by sensitivity before optional Y inversion. `applyControllerAxis(...)` is an axis-local pure mapping: values inside the configured dead zone map to zero, values outside are renormalized and raised to the configured positive exponent, with sign restored. This does not add a production controller capture/binding API.

The tick command carries MOVE X/Y, LOOK X/Y, and the nine digital actions without any GLFW/LWJGL type. `PlayerInputCommandCodec` encodes exactly 126 bytes in fixed big-endian field order and preserves the caller buffer's configured byte order. This is a replay/storage command format, not a frozen production network packet layout.

Usage: [Platform input](PLATFORM/INPUT.md).

### Configuration

| Type | Purpose |
| --- | --- |
| `EngineConfigLoader` | Resolves supported startup configuration sources/layers. |
| `EngineConfigSchema` | Validates raw configuration into typed startup values. |
| `ConfigKey<T>` | Canonical typed configuration key. |
| `ConfigEntry` | Raw value plus diagnostic source. |
| `ConfigSource` | Human-readable source identity for diagnostics. |
| `ConfigError` | One source-aware validation error. |
| `ConfigValidationException` | Aggregates configuration validation failures. |

Current schema keys include `fullscreen.width`, `fullscreen.height`, and the locked `simulation.tickRate`. Usage: [Configuration](CORE/CONFIGURATION.md).

### Diagnostics, ownership, shutdown

| Type | Purpose |
| --- | --- |
| `EngineLogger` | Synchronous structured logging with caller-owned sink semantics. |
| `NativeResourceRegistry` | Tracks explicit native-handle ownership and verifies leak-free terminal state. |
| `NativeResourceRegistry.Registration` | One close capability for one registered native handle. |
| `FatalTermination` | Bounded one-shot fatal termination orchestration that attempts cleanup/reporting before termination. |

Usage: [Logging](CORE/LOGGING.md), [Native resources](CORE/NATIVE_RESOURCES.md), and [Fatal termination](CORE/FATAL_TERMINATION.md).

## `engine-platform-lwjgl` — `com.samo.engine.platform.api`

| Type | Purpose |
| --- | --- |
| `GlfwWindow` | Owns one production GLFW/OpenGL 4.6 window/context lifetime, owner-thread event polling, size delivery, display-mode transitions, focus-safe cursor capture, and renderer-frame hardware snapshot production. |
| `WindowSizeListener` | Renderer-neutral receiver that keeps logical window dimensions separate from framebuffer pixel dimensions. |
| `WindowMode` | Selects `WINDOWED`, `BORDERLESS_FULLSCREEN`, or `EXCLUSIVE_FULLSCREEN` for a started `GlfwWindow`. |
| `InputSnapshot` | Immutable renderer-frame keyboard/mouse/focus/capture/relative-motion state captured from one started `GlfwWindow`. |
| `InputKey` | Device-neutral bounded keyboard vocabulary used by `InputSnapshot` and input bindings. |
| `InputMouseButton` | Device-neutral bounded mouse-button vocabulary used by `InputSnapshot` and input bindings. |
| `InputAction` | The eleven named Phase 3 gameplay actions; each declares its `DIGITAL` or `VECTOR2` value type. |
| `InputActionValueType` | Distinguishes `DIGITAL` and `VECTOR2` action shapes. |
| `InputActionComponent` | Selects `VALUE`, `X`, or `Y` as the target component of one binding. |
| `InputBinding` | Immutable device-neutral descriptor mapping one key/button/mouse-delta control to an action component with signed scale. |
| `InputActionBindings` | Immutable complete action-binding set with strict versioned JSON loading. |
| `InputBindingLoadException` | Reports binding-file read/schema/validation failures without exposing Jackson. |
| `InputActionEvaluator` | Caller-owned stateful renderer-frame evaluator from one `InputSnapshot` + binding set to action state; applies configured mouse response before binding scale/aggregation. |
| `InputActionSnapshot` | Immutable complete evaluated action view for one source hardware frame ID. |
| `InputActionState` | Immutable per-action pressed/held/released state plus scalar or X/Y analog value. |
| `PlayerInputCommandSampler` | Caller-owned bridge that retains renderer-frame edges/LOOK until the next due simulation tick and emits `engine-core` `PlayerInputCommand` values. |

`GlfwWindow.setCursorCaptured(boolean)` controls cursor lock. Focus loss clears held hardware state and releases effective capture; focus regain never recaptures automatically.

`GlfwWindow.captureInputSnapshot(long frameId)` captures the current held levels plus pending hardware press/release edges and accumulated relative mouse delta without polling GLFW itself. A successful snapshot consumes pending edges and mouse delta while leaving held levels intact.

`InputActionBindings.load(Path)` loads strict schema version 1. All eleven actions must appear exactly once with at least one binding. Public descriptors reuse `InputKey` / `InputMouseButton` plus relative mouse X/Y controls; Jackson remains an implementation detail.

`InputActionEvaluator(InputActionBindings)` uses neutral `InputResponseSettings.defaults()`. The overload accepting `InputResponseSettings` and `setResponseSettings(...)` allow explicit caller-owned response policy. Settings replacement affects future evaluations only and does not reset the prior frame/activity baseline. For mouse-delta controls the evaluator applies sensitivity/Y inversion first, then existing binding scale and additive aggregation. Key/mouse-button behavior is unchanged.

`InputActionEvaluator.evaluate(InputSnapshot)` adds binding contributions by target component without general clamping or normalization. DIGITAL activity is `value != 0`; VECTOR2 activity is `x != 0 || y != 0`. The evaluator emits action-level pressed/held/released transitions relative to its previous successful frame and preserves a complete one-frame key/button tap only when the same bound control reports both press and release. Later successful frame IDs must be strictly increasing; failed evaluations do not advance evaluator state.

`PlayerInputCommandSampler.submit(InputActionSnapshot)` retains latest MOVE/digital level state, accumulates LOOK deltas, and OR-retains pending digital pressed/released edges until `nextCommand(long tickId)` emits them. When multiple ticks occur without another submitted renderer frame, later commands repeat latest level state but emit zero LOOK and no repeated edges.

`GlfwWindow` intentionally exposes no raw GLFW window/monitor handle, buffer-swap API, monitor-selection/custom-video-mode API, public raw-mouse toggle, controller capture API, or content-scale callback API. P3-T10 defines controller response math only; controller discovery/polling/vocabulary/bindings remain unimplemented.

Usage: [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md), [Platform input and tick commands](PLATFORM/INPUT.md), and [Create a window example](EXAMPLES/CREATE_A_WINDOW.md).

## Not an engine-consumer API

The repository also contains game composition entry points, build/test utilities, and experimental feasibility spikes. Those are not automatically reusable engine-library APIs. In particular, code under `feasibility-spikes` proves isolated feasibility and must not be treated as production usage guidance.

## Where to go next

- [Lifecycle](CORE/LIFECYCLE.md)
- [Subsystem composition/startup](CORE/SUBSYSTEM_COMPOSITION.md)
- [Timing](CORE/TIMING.md)
- [Configuration](CORE/CONFIGURATION.md)
- [Logging](CORE/LOGGING.md)
- [Native resources](CORE/NATIVE_RESOURCES.md)
- [Fatal termination](CORE/FATAL_TERMINATION.md)
- [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md)
- [Platform input and tick commands](PLATFORM/INPUT.md)
- [Current limitations](LIMITATIONS.md)
