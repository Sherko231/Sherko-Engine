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
| `GlfwWindow` | Owns one production GLFW/OpenGL 4.6 window/context lifetime, owner-thread event polling, separated logical/framebuffer size delivery, and in-place primary-monitor display-mode transitions. |
| `WindowSizeListener` | Renderer-neutral receiver that keeps logical window dimensions separate from framebuffer pixel dimensions. |
| `WindowMode` | Selects `WINDOWED`, `BORDERLESS_FULLSCREEN`, or `EXCLUSIVE_FULLSCREEN` for a started `GlfwWindow`. |

`GlfwWindow` intentionally exposes no raw GLFW window/monitor handle, buffer-swap API, monitor-selection/custom-video-mode API, focus/input API, or content-scale callback API.

Usage: [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md) and [Create a window example](EXAMPLES/CREATE_A_WINDOW.md).

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
- [Current limitations](LIMITATIONS.md)
