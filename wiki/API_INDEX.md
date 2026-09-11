# Current API index

This page is an orientation index for production public APIs implemented today. Source code remains authoritative for exact signatures.

## `engine-core` — `com.samo.engine.core.api`

### Lifecycle and composition

| Type | Purpose |
| --- | --- |
| `EngineSubsystem` | Final lifecycle sequencing around `initialize`, `start`, `stop`, and `close`. |
| `SubsystemGraph` | Declares subsystem dependency relationships and produces dependency-safe ordering. |
| `SubsystemStartup` | Coordinates ordered subsystem startup and bounded rollback/cleanup when startup fails. |

### Timing

| Type | Purpose |
| --- | --- |
| `EngineClock` | Monotonic elapsed-time sampler. |
| `FixedStepAccumulator` | Converts elapsed nanoseconds into exact fixed-rate simulation ticks and interpolation progress. |
| `FixedStepCatchUpPolicy` | Bounds large elapsed-time gaps and per-update catch-up work. |

The engine foundation is locked to a 60 Hz simulation rate at the current stage.

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

Current schema keys include `fullscreen.width`, `fullscreen.height`, and the locked `simulation.tickRate`.

### Diagnostics, ownership, shutdown

| Type | Purpose |
| --- | --- |
| `EngineLogger` | Synchronous structured logging with caller-owned sink semantics. |
| `NativeResourceRegistry` | Tracks explicit native-handle ownership and verifies leak-free terminal state. |
| `NativeResourceRegistry.Registration` | One close capability for one registered native handle. |
| `FatalTermination` | Bounded one-shot fatal termination orchestration that attempts cleanup/reporting before termination. |

## `engine-platform-lwjgl` — `com.samo.engine.platform.api`

| Type | Purpose |
| --- | --- |
| `GlfwWindow` | Owns one production GLFW window and OpenGL 4.6 Core context for one subsystem lifetime. |

`GlfwWindow` is currently the only top-level public production platform type. It intentionally does not expose a raw GLFW window handle, polling/swap API, fullscreen API, size events, or input state yet.

## Not an engine-consumer API

The repository also contains game composition entry points, build/test utilities, and experimental feasibility spikes. Those are not automatically reusable engine-library APIs. In particular, code under `feasibility-spikes` proves isolated feasibility and must not be treated as production usage guidance.

## Where to go next

- [Lifecycle](CORE/LIFECYCLE.md)
- [Timing](CORE/TIMING.md)
- [Configuration](CORE/CONFIGURATION.md)
- [Logging](CORE/LOGGING.md)
- [Native resources](CORE/NATIVE_RESOURCES.md)
- [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md)
