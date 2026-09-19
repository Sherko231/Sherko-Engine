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

### Spatial transforms, camera math, and geometry

| Type | Purpose |
| --- | --- |
| `Transform` | Mutable local position/rotation/scale plus optional parent and cached world-matrix composition. |
| `TransformQuantization` | Deterministic bounded position/quaternion value quantization for later storage/network adapters. |
| `TransformQuantization.QuantizedPosition` | Immutable three-short quantized canonical position value. |
| `TransformQuantization.QuantizedRotation` | Immutable smallest-three quaternion value with omitted-component index. |
| `CameraMatrices` | Static right-handed view and finite perspective projection construction into caller-owned JOML matrices. |
| `ScreenRays` | Static screen/viewport-to-world `Ray3f` construction from read-only view/projection matrices. |
| `Ray3f` | Immutable normalized world-space ray with plane/sphere/AABB intersections. |
| `Plane3f` | Immutable normalized plane using `normal dot point + offset = 0`. |
| `Sphere3f` | Immutable world-space sphere with inclusive containment/intersection queries. |
| `Aabb3f` | Immutable axis-aligned box with inclusive containment/intersection queries. |
| `Frustum3f` | Immutable six-inward-plane frustum with point/sphere/AABB classification. |

`Transform` follows D-041: right-handed world, +X right, +Y up, -Z forward, meters, radians, and dimensionless scale. Local composition is `T * R * S`; world composition is `parentWorld * local`.

D-042 makes JOML the public math type family for `engine-core` spatial APIs. `Transform` setters accept JOML read-only value interfaces and copy them; getters/world-matrix reads copy into caller-owned mutable JOML destinations. No internal mutable vector/quaternion/matrix is exposed.

Transform parent cycles are rejected atomically, and successful local/reparent changes explicitly invalidate only the affected transform subtree while unrelated branches remain cached.

D-045 defines camera matrices: view space is right-handed with camera forward on `-Z`; perspective uses vertical FOV radians, positive aspect and near plane with `far > near`, conventional finite non-reversed depth, and OpenGL NDC z `[-1,+1]`. `CameraMatrices` mutates only the caller-provided destination after validation and has no renderer/LWJGL dependency.

D-046 defines screen-to-world rays: screen/viewport origin is top-left, Y increases downward, raster pixel centers are at `index + 0.5`, screen samples and viewport coordinates must use the same coordinate domain, and closed viewport edges map to NDC `±1`. `ScreenRays.worldRay(...)` unprojects D-045 near/far clip depths through inverse `projection * view`, starts the returned `Ray3f` on the near plane, and points it toward the corresponding far point. It performs no GLFW logical/framebuffer conversion.

D-047 defines transform value quantization. Position uses one signed `short` per axis at `1/64 m` over `[-512.0, 511.984375] m`, with maximum per-axis round-trip error `1/128 m`. Rotation uses deterministic smallest-three encoding: normalize first, omit the largest-absolute component with lowest-index tie breaking, canonicalize sign so `q` and `-q` encode identically, store the remaining components in `[-32767,+32767]`, and reserve `Short.MIN_VALUE` as malformed. Valid quaternion round trips are bounded to `0.0002 rad` angular error. `TransformQuantization` does not define packet layout, byte order, protocol/version fields, authority, origin policy, transport, or scale quantization.

D-044 defines the geometry semantics: primitives are immutable and copy JOML inputs; ray directions and plane equations are normalized; contact is boundary-inclusive with exact production comparisons and no hidden epsilon; ray misses return `Float.NaN`; and `Frustum3f` consumes six inward-facing planes directly.

Usage: [Transforms](CORE/TRANSFORMS.md), [Spatial primitives](CORE/SPATIAL_PRIMITIVES.md), and [Spatial conventions](CORE/SPATIAL_CONVENTIONS.md).

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
| `GlfwWindow` | Owns one production GLFW/OpenGL 4.6 window/context lifetime, optional OpenGL debug diagnostics, owner-thread event polling/presentation, size delivery, display-mode transitions, focus-safe cursor capture, and renderer-frame hardware snapshot production. |
| `OpenGlDebugMode` | Explicit per-window debug policy; default `DISABLED`, optional `FAIL_ON_HIGH_SEVERITY` for development/test diagnostics. |
| `OpenGlThreadGuard` | Stable non-owning OpenGL thread-affinity guard; future GPU-facing wrappers call `assertOwnerThread()` before native OpenGL entry. |
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

`GlfwWindow` intentionally exposes no raw GLFW/OpenGL callback or window/monitor handle, monitor-selection/custom-video-mode API, public raw-mouse toggle, controller capture API, or content-scale callback API. `present()` is the only public buffer-swap boundary and does not expose the native window handle. P3-T10 defines controller response math only; controller discovery/polling/vocabulary/bindings remain unimplemented.

Usage: [GLFW/OpenGL window](PLATFORM/GLFW_WINDOW.md), [Platform input and tick commands](PLATFORM/INPUT.md), and [Create a window example](EXAMPLES/CREATE_A_WINDOW.md).

## `engine-render-opengl` — `com.samo.engine.render.api`

| Type | Purpose |
| --- | --- |
| `OpenGlRenderer` | Bounded production OpenGL renderer composition: owns one known indexed triangle plus fixed renderer-internal P5-T09 material values, a neutral-gray sRGB reference texture/sampler, accepted shaders/uniform blocks, and two material-driven reference draws per `render(...)` call. |

`OpenGlRenderer.create(window.openGlThreadGuard(), nativeResources)` requires an already-started production OpenGL context on the owner thread. `render(view, projection, framebufferWidth, framebufferHeight)` consumes caller-supplied D-041/D-045 matrices and positive framebuffer pixel dimensions. The current fixed renderer path samples an internal neutral-gray sRGB reference texture in linear space and draws the same owned mesh twice through two internal immutable material values: a baseline left material and a tinted right material with explicit shader/textures/scalars/blend/depth/cull policy. Presentation still receives exactly one sRGB encode: hardware `GL_FRAMEBUFFER_SRGB` when the actual default buffer is sRGB, or the bounded internal fragment fallback when it is linear. These materials are implementation details; the renderer still exposes no native handles, arbitrary mesh/texture/public-material API, asset loading, lighting, or world/ECS submission.

A normal Gradle consumer depends on `engine-render-opengl` only. Its compile variant exposes `OpenGlRenderer` plus the required transitive signature dependencies while keeping renderer `.internal` classes out of the supported compile surface; runtime resolution still supplies the full implementation and shader resources.

Presentation is separate and platform-owned: call `GlfwWindow.present()` after rendering.

Usage: [OpenGL renderer](RENDERER/OPENGL_RENDERER.md).

## Not an engine-consumer API

The repository also contains game composition entry points, build/test utilities, and experimental feasibility spikes. Those are not automatically reusable engine-library APIs. In particular, code under `feasibility-spikes` proves isolated feasibility and must not be treated as production usage guidance.

## Where to go next

- [Lifecycle](CORE/LIFECYCLE.md)
- [Subsystem composition/startup](CORE/SUBSYSTEM_COMPOSITION.md)
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
