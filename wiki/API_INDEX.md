# Current API index

This page is an orientation index for production public APIs implemented today. Source code remains authoritative for exact signatures.

## `engine-core` — `com.samo.engine.core.api`

### Lifecycle and composition

| Type | Purpose |
| --- | --- |
| `EngineSubsystem` | Final lifecycle sequencing around `initialize`, `start`, `stop`, and `close`. |
| `SubsystemGraph` | Declares subsystem dependencies and produces dependency-safe ordering. |
| `SubsystemStartup` | Coordinates ordered startup and bounded rollback/cleanup. |

Usage: [Lifecycle](CORE/LIFECYCLE.md) and [Subsystem composition/startup](CORE/SUBSYSTEM_COMPOSITION.md).

### Timing

| Type | Purpose |
| --- | --- |
| `EngineClock` | Monotonic elapsed-time sampler. |
| `FixedStepAccumulator` | Converts elapsed nanoseconds into exact fixed-rate simulation ticks and interpolation progress. |
| `FixedStepCatchUpPolicy` | Bounds large elapsed-time gaps and per-update catch-up work. |

The engine foundation is currently locked to 60 Hz simulation. Usage: [Timing](CORE/TIMING.md).

### Spatial transforms and geometry

| Type | Purpose |
| --- | --- |
| `Transform` | Mutable local position/rotation/scale plus optional parent and cached world composition. |
| `Ray3f` | Immutable normalized world-space ray with plane/sphere/AABB intersections. |
| `Plane3f` | Immutable normalized plane using `normal dot point + offset = 0`. |
| `Sphere3f` | Immutable world-space sphere with inclusive containment/intersection queries. |
| `Aabb3f` | Immutable axis-aligned box with inclusive containment/intersection queries. |
| `Frustum3f` | Immutable six-inward-plane frustum with point/sphere/AABB classification. |

D-041 defines the engine world as right-handed with +X right, +Y up, -Z forward, meters, radians, and dimensionless scale. D-042 makes JOML the public math type family for `engine-core` spatial APIs.

`Transform` composes local matrices as `T * R * S` and world matrices as `parentWorld * local`. Parent cycles are rejected atomically, and successful local/reparent changes invalidate only the affected transform subtree.

The P4-T06 geometry primitives are immutable and copy JOML inputs. Ray directions and plane equations are normalized. Contact is boundary-inclusive and production queries use exact comparisons without a hidden epsilon. Ray misses return `Float.NaN`. `Frustum3f` consumes six inward-facing planes directly; it does not select projection/NDC/depth conventions or extract planes from matrices.

Usage: [Transforms](CORE/TRANSFORMS.md), [Spatial primitives](CORE/SPATIAL_PRIMITIVES.md), and [Spatial conventions](CORE/SPATIAL_CONVENTIONS.md).

### Input response and tick-aligned player input

| Type | Purpose |
| --- | --- |
| `InputResponseSettings` | Immutable deterministic mouse sensitivity/Y-inversion and controller-axis dead-zone/curve response math. |
| `PlayerInputCommand` | Immutable device-neutral input state/edges for one simulation tick. |
| `PlayerInputCommand.DigitalAction` | Fixed nine-action digital command vocabulary. |
| `PlayerInputCommand.DigitalState` | Immutable scalar plus pressed/held/released state. |
| `PlayerInputCommandCodec` | Explicit fixed-size version-1 ByteBuffer replay/storage codec. |

`PlayerInputCommandCodec` encodes exactly 126 bytes in fixed big-endian field order. It is a replay/storage command format, not a frozen production network packet layout.

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

Usage: [Configuration](CORE/CONFIGURATION.md).

### Diagnostics, ownership, shutdown

| Type | Purpose |
| --- | --- |
| `EngineLogger` | Synchronous structured logging with caller-owned sink semantics. |
| `NativeResourceRegistry` | Tracks explicit native-handle ownership and verifies terminal state. |
| `NativeResourceRegistry.Registration` | One close capability for one registered native handle. |
| `FatalTermination` | One-shot fatal termination orchestration that attempts cleanup/reporting before termination. |

Usage: [Logging](CORE/LOGGING.md), [Native resources](CORE/NATIVE_RESOURCES.md), and [Fatal termination](CORE/FATAL_TERMINATION.md).

## `engine-platform-lwjgl` — `com.samo.engine.platform.api`

| Type | Purpose |
| --- | --- |
| `GlfwWindow` | Owns one production GLFW/OpenGL 4.6 window/context lifetime plus polling, sizing, display modes, focus-safe cursor capture, and renderer-frame input snapshots. |
| `WindowSizeListener` | Keeps logical window dimensions separate from framebuffer pixels. |
| `WindowMode` | Selects windowed, borderless fullscreen, or exclusive fullscreen. |
| `InputSnapshot` | Immutable renderer-frame keyboard/mouse/focus/capture/relative-motion state. |
| `InputKey` | Device-neutral bounded keyboard vocabulary. |
| `InputMouseButton` | Device-neutral bounded mouse-button vocabulary. |
| `InputAction` | Eleven named Phase 3 gameplay actions. |
| `InputActionValueType` | Distinguishes digital and vector action shapes. |
| `InputActionComponent` | Selects value/X/Y binding targets. |
| `InputBinding` | Immutable device-neutral action binding descriptor. |
| `InputActionBindings` | Immutable complete binding set with strict JSON schema v1 loading. |
| `InputBindingLoadException` | Reports binding-file/schema/validation failures. |
| `InputActionEvaluator` | Stateful renderer-frame evaluator from hardware snapshot to action state. |
| `InputActionSnapshot` | Immutable evaluated action view for one hardware frame. |
| `InputActionState` | Immutable per-action values and pressed/held/released transitions. |
| `PlayerInputCommandSampler` | Bridges renderer-frame action snapshots to core simulation-tick commands. |

Usage: [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md), [Platform input and tick commands](PLATFORM/INPUT.md), and [Create a window example](EXAMPLES/CREATE_A_WINDOW.md).

## Not an engine-consumer API

Game composition entry points, build/test utilities, and `feasibility-spikes` are not automatically reusable engine-library APIs.

## Where to go next

- [Lifecycle](CORE/LIFECYCLE.md)
- [Timing](CORE/TIMING.md)
- [Configuration](CORE/CONFIGURATION.md)
- [Spatial conventions](CORE/SPATIAL_CONVENTIONS.md)
- [Transforms](CORE/TRANSFORMS.md)
- [Spatial primitives](CORE/SPATIAL_PRIMITIVES.md)
- [Logging](CORE/LOGGING.md)
- [Native resources](CORE/NATIVE_RESOURCES.md)
- [Fatal termination](CORE/FATAL_TERMINATION.md)
- [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md)
- [Platform input and tick commands](PLATFORM/INPUT.md)
- [Current limitations](LIMITATIONS.md)
