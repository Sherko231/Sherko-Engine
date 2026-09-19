# Current limitations

This page prevents planned roadmap work from being mistaken for an already usable library API.

## Math / transforms

Implemented:

- public `engine-core` `Transform` with local position, normalized quaternion rotation, local scale, optional parent, and cached world matrix;
- JOML is the public math type family for engine-core spatial APIs under D-042;
- local composition `T * R * S` and world composition `parentWorld * local` under the D-041 world convention;
- caller-owned input/destination values are copied rather than retained as internal mutable aliases;
- self-parenting and indirect transform-parent cycles are rejected atomically before hierarchy mutation;
- successful local/reparent mutations explicitly dirty only the changed transform and its descendants, while unrelated branches remain cached;
- private child membership exists only to support invalidation and is not a public hierarchy-enumeration API;
- immutable `Ray3f`, `Plane3f`, `Sphere3f`, `Aabb3f`, and `Frustum3f` world-geometry primitives;
- normalized ray directions and plane equations, inclusive contact/containment semantics, ray plane/sphere/AABB queries, and frustum point/sphere/AABB classification;
- geometry queries use exact production comparisons without an implicit epsilon; ray misses return `Float.NaN`;
- public `CameraMatrices` construction for right-handed world-to-view matrices and conventional finite perspective projection;
- vertical FOV in radians, positive aspect/near with `far > near`, camera forward mapped to view `-Z`, and OpenGL NDC depth `[-1,+1]` with near/far at `-1/+1` under D-045;
- public `ScreenRays.worldRay(...)` construction from read-only view/projection matrices;
- top-left/Y-down screen mapping, same-domain screen/viewport coordinates, raster pixel centers at `index + 0.5`, closed viewport boundaries, inverse `projection * view` homogeneous unprojection, near-plane ray origin, and normalized near-to-far direction under D-046;
- public `TransformQuantization` value helpers under D-047 for bounded canonical position and quaternion encode/decode;
- position quantization at `1/64 m` over `[-512.0, 511.984375] m` with maximum `1/128 m` per-axis round-trip error and no clamping;
- deterministic smallest-three quaternion quantization with lowest-index tie breaking, `q`/`-q` sign canonicalization, reserved malformed `Short.MIN_VALUE`, and maximum `0.0002 rad` valid round-trip angular error.

Current limitations:

- `Frustum3f` accepts six inward-facing planes directly; there is no view/projection-matrix frustum extraction yet;
- `ScreenRays` does not convert between GLFW logical-window coordinates and framebuffer pixels; callers must supply screen and viewport values in the same domain;
- `ScreenRays` returns one immutable ray per call and does not expose a cached projector or zero-allocation destination API;
- reversed-Z, infinite-far, orthographic, and jittered/TAA projection variants are not implemented;
- no public camera component/object or Transform-to-camera decomposition API exists;
- no public child enumeration or scene-graph API exists; hierarchy ownership beyond `parent()`/`setParent(...)` remains internal;
- no inverse/world-to-local transform API, world-TRS decomposition, Euler API, transform interpolation, entity/component storage, renderer integration, or physics/Jolt adapter exists yet;
- D-047 quantizes position and rotation values only: there is no transform-object codec, scale quantization, byte serialization, packet layout, origin/rebasing scheme, entity/tick association, replication authority, delta compression, transport integration, or persisted transform format;
- D-047's fixed position range is not an open-world/world-partition scheme; later networking/storage work must explicitly define any origin-relative strategy it requires;
- no broad-phase structure, generic collision dispatcher, renderer-culling integration, or physics query adapter is part of the geometry primitive API;
- zero and negative scale are allowed for forward composition, so consumers must not assume a transform is invertible;
- `Transform` is mutable and externally serialized; concurrent mutation/read guarantees are not provided.

Usage: [Transforms](CORE/TRANSFORMS.md), [Spatial primitives](CORE/SPATIAL_PRIMITIVES.md), and [Spatial conventions](CORE/SPATIAL_CONVENTIONS.md).

## Platform/input

Implemented:

- one production `GlfwWindow` lifecycle;
- explicit OpenGL 4.6 Core context request;
- actual OpenGL version/renderer verification and logging;
- explicit native ownership/cleanup;
- bounded owner-thread GLFW event polling through `GlfwWindow.pollEvents()`;
- separate renderer-neutral logical-window and framebuffer-pixel size delivery through `WindowSizeListener`;
- independent initial-size queries plus later GLFW size callbacks, with zero-sized framebuffer states preserved for minimized windows;
- owner-thread in-place transitions among windowed, primary-monitor borderless fullscreen, and primary-monitor exclusive fullscreen through `WindowMode` / `GlfwWindow.setWindowMode(...)`, retaining the same GLFW window/OpenGL context and restoring captured windowed geometry;
- owner-thread cursor capture through `GlfwWindow.setCursorCaptured(boolean)`;
- focus loss clears held key/button state and releases effective cursor/raw capture;
- focus regain never recaptures or re-enables raw mouse mode automatically and requires an explicit later capture request;
- relative mouse-motion acquisition while effective capture is active: GLFW raw mouse mode when supported and a disabled-cursor position-delta fallback otherwise;
- focus/capture/lifecycle transitions clear pending relative motion and invalidate its baseline to avoid re-entry spikes;
- immutable renderer-frame `InputSnapshot` capture through `GlfwWindow.captureInputSnapshot(long)`, exposing bounded `InputKey` / `InputMouseButton` held state, retained raw press/release edges, focus/capture state, and consumed per-snapshot relative mouse delta without exposing GLFW constants/types;
- exactly eleven named Phase 3 `InputAction` values, with MOVE/LOOK as `VECTOR2` and the remaining actions as `DIGITAL`;
- immutable `InputBinding` descriptors over engine key/button vocabulary and relative mouse X/Y controls;
- immutable complete `InputActionBindings` loaded atomically from strict JSON schema version 1;
- explicit `InputBindingLoadException` for file/schema/validation failures while Jackson remains absent from public signatures;
- caller-owned stateful `InputActionEvaluator` producing immutable `InputActionSnapshot` / `InputActionState` for one renderer-frame hardware snapshot;
- deterministic additive scalar/vector action aggregation with exact signed cancellation, no general implicit clamp/normalization, and action-level pressed/held/released transitions;
- preservation of a complete same-binding key/button press+release between hardware snapshots as one-frame `pressed=true`, `held=false`, `released=true` action state;
- overlapping bindings keep transitions action-level: an extra binding does not duplicate press and releasing one binding does not release while the aggregate remains active;
- strictly increasing evaluator frame identity after the first successful frame plus atomic failure on invalid frame order or non-finite input/aggregate values;
- immutable `engine-core` `InputResponseSettings` with deterministic mouse sensitivity/Y-inversion and axis-local controller dead-zone/curve response math;
- `InputActionEvaluator` applies mouse response before binding scale/aggregation, while the legacy constructor uses neutral response defaults and runtime settings replacement affects future frames only;
- immutable `engine-core` `PlayerInputCommand` values containing one simulation tick's MOVE/LOOK plus nine digital scalar/pressed/held/released states without platform/native types;
- explicit fixed-size version-1 `PlayerInputCommandCodec` using caller-supplied `ByteBuffer` for replay/storage round trips without Java serialization;
- caller-owned `PlayerInputCommandSampler` bridging renderer-frame action snapshots to simulation ticks while retaining pending edges and LOOK across zero-tick frames and consuming them exactly once on the next emitted tick command;
- deterministic headless replay coverage that encodes/decodes a fixed command sequence and replays decoded commands through a platform-independent test consumer.

Not yet exposed as production API:

- choosing a non-primary monitor;
- custom fullscreen resolution or refresh-rate selection;
- raw GLFW window/monitor handles;
- public focus event listeners (focus is available as snapshot state only);
- arbitrary keyboard keys or mouse buttons outside the current `InputKey` / `InputMouseButton` vocabulary;
- controller discovery, polling, callbacks, connection lifecycle, button/axis vocabulary, or action bindings;
- radial two-axis stick dead zones;
- mouse smoothing/acceleration or per-axis sensitivity;
- settings persistence/UI;
- configurable general action clamping/normalization beyond the explicit P3-T10 response mapping;
- live remapping UI or binding hot reload;
- content-scale callbacks as a consumer API;
- production networking integration of `PlayerInputCommand`;
- a production packet layout for tick input commands;
- gameplay/UI input-consumption or focus-routing policy.

`InputResponseSettings`, `PlayerInputCommand`, and its codec live in `engine-core`. `InputSnapshot`, `InputActionBindings`, and `InputActionEvaluator` remain client/platform renderer-frame APIs, while `PlayerInputCommandSampler` is the client/platform bridge. `game-server` therefore remains independent of `engine-platform-lwjgl` and can consume/replay core tick commands without GLFW/LWJGL.

The current JSON binding schema is intentionally strict and versioned at `schemaVersion: 1`. Every required action must appear exactly once. Unknown/duplicate fields, invalid enum/control names, empty action binding lists, duplicate binding descriptors, invalid action components, unsupported schema versions, malformed JSON, and unreadable paths fail rather than being ignored or partially applied. P3-T09/P3-T10 do not change that schema or define migration between future schema versions.

Action evaluation adds binding contributions in declared binding order using ordinary finite Java `double` arithmetic. Mouse-delta controls are first shaped by the evaluator's current `InputResponseSettings`, then multiplied by their existing binding scale. Exact cancellation is inactive. Mouse-delta controls still have renderer-frame activity semantics: a non-zero resulting delta may press/hold LOOK for that frame and a later zero resulting delta may release it. The evaluator does not infer event order across different physical bindings; only one bound key/button carrying both hardware edges qualifies for the retained one-frame-tap exception.

`InputResponseSettings.applyControllerAxis(...)` is deliberately only an axis-local pure scalar primitive. It validates `[-1,1]`, maps magnitudes at/below the configured dead zone to zero, renormalizes the remaining magnitude into `[0,1]`, applies the positive curve exponent, and restores sign. This does not mean the engine can currently discover or read a controller.

Tick sampling is deliberately asymmetric across level and one-shot data. Latest MOVE and digital scalar/held state repeat across emitted ticks. LOOK deltas and digital pressed/released edges accumulate across renderer frames until the next emitted tick command, then are consumed once. A second tick emitted without another submitted renderer frame receives zero LOOK and no repeated edges.

`PlayerInputCommandCodec` version 1 is exactly 126 bytes in fixed big-endian order. It is a replay/storage command encoding and is not declared the production gameplay network packet layout. P10+ networking may wrap/version transport independently.

On systems where GLFW raw mouse motion is unavailable, the relative-motion fallback remains independent of cursor screen bounds but does **not** claim to bypass operating-system pointer acceleration.

## Rendering

P5-T07 provides the first bounded public production renderer path through `OpenGlRenderer` plus window-owned `GlfwWindow.present()`. P5-T08 adds one internal sRGB reference texture, explicit sRGB-versus-linear storage rules, and one default-framebuffer sRGB encode boundary. P5-T09 reuses the same engine-owned indexed position-only mesh twice through two internal immutable material values carrying explicit shader/textures/scalars/blend/depth/cull policy. P5-T10 adds public immutable `RenderFramePacket`, which snapshots finite camera matrices and framebuffer pixel dimensions before synchronous renderer consumption without retaining mutable caller/world objects.

This is intentionally not a general renderer API yet. The P5-T09 material value is package-internal because P6 has not defined stable runtime asset/resource references. The packet currently carries frame/camera data only; it does not invent mesh/material/asset/native resource identities before P6. There is still no arbitrary mesh submission, public/arbitrary texture creation, public material creation, asset loading/import, lighting, HDR/tonemapping/fog/post-processing, world/ECS integration, batching, frustum culling/sorting, render graph, render worker/command queue, or public native resource handle surface. The persistent sandbox demonstrates the fixed baseline/tinted two-material reference scene while preserving the existing input/window/timing playground.

## Assets/world/physics/audio/networking/editor

The target modules exist according to the repository architecture, but a module's existence does not mean its consumer API has been implemented. Do not write wiki examples for planned APIs until the corresponding implementation task is complete and verified.

## Native evidence limits

Current production window acceptance covers the bounded GLFW/OpenGL window lifecycle, P3-T02's logical/framebuffer size path, P3-T03's single 20-transition display-mode scenario, P3-T04's real Windows focus-transfer/cursor-release scenario, P3-T05's bounded raw-mode/relative-motion acceptance, and P5-T01's single intentional high-severity OpenGL debug-message path. P3-T06 adds a pure-Java snapshot boundary over that already-tested hardware ingestion path, P3-T07 adds pure-Java binding metadata/JSON loading, P3-T08 adds pure-Java action evaluation, P3-T09 adds pure-Java tick-command sampling/codec/replay, and P3-T10 adds pure-Java response math/evaluator integration over those existing boundaries. These tasks do not establish sustained native stability or repeated native restartability; P0-T13 and P0-T14 remain separate evidence gates.

## Stability

The engine is pre-v1 and the public API is still being built phase by phase. Treat current documented contracts as real for the corresponding completed/active bounded tasks, but do not assume a broad API freeze.

For exact status, use [`../docs/DEVELOPMENT_STATUS.md`](../docs/DEVELOPMENT_STATUS.md) and live GitHub Issues/PRs rather than this page.
