# Current limitations

This page prevents planned roadmap work from being mistaken for an already usable library API.

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
- deterministic additive scalar/vector action aggregation with exact signed cancellation, no implicit clamp/normalization, and action-level pressed/held/released transitions;
- preservation of a complete same-binding key/button press+release between hardware snapshots as one-frame `pressed=true`, `held=false`, `released=true` action state;
- overlapping bindings keep transitions action-level: an extra binding does not duplicate press and releasing one binding does not release while the aggregate remains active;
- strictly increasing evaluator frame identity after the first successful frame plus atomic failure on invalid frame order or non-finite input/aggregate values;
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
- controller input;
- sensitivity/Y inversion/dead-zone/controller-curve settings;
- configurable action clamping/normalization/response processing;
- live remapping UI or binding hot reload;
- content-scale callbacks as a consumer API;
- production networking integration of `PlayerInputCommand`;
- a production packet layout for tick input commands;
- gameplay/UI input-consumption or focus-routing policy.

`InputSnapshot`, `InputActionBindings`, and `InputActionEvaluator` remain client/platform renderer-frame APIs. `PlayerInputCommand` and its codec live in `engine-core`, while `PlayerInputCommandSampler` is the client/platform bridge. `game-server` therefore remains independent of `engine-platform-lwjgl` and can consume/replay core tick commands without GLFW/LWJGL.

The current JSON binding schema is intentionally strict and versioned at `schemaVersion: 1`. Every required action must appear exactly once. Unknown/duplicate fields, invalid enum/control names, empty action binding lists, duplicate binding descriptors, invalid action components, unsupported schema versions, malformed JSON, and unreadable paths fail rather than being ignored or partially applied. P3-T09 does not change that schema or define migration between future schema versions.

Action evaluation adds binding contributions in declared binding order using ordinary finite Java `double` arithmetic. Exact cancellation is inactive. Mouse-delta controls have renderer-frame activity semantics: a non-zero delta may press/hold LOOK for that frame and a later zero-delta frame may release it. The evaluator does not infer event order across different physical bindings; only one bound key/button carrying both hardware edges qualifies for the retained one-frame-tap exception.

Tick sampling is deliberately asymmetric across level and one-shot data. Latest MOVE and digital scalar/held state repeat across emitted ticks. LOOK deltas and digital pressed/released edges accumulate across renderer frames until the next emitted tick command, then are consumed once. A second tick emitted without another submitted renderer frame receives zero LOOK and no repeated edges.

`PlayerInputCommandCodec` version 1 is exactly 126 bytes in fixed big-endian order. It is a replay/storage command encoding and is not declared the production gameplay network packet layout. P10+ networking may wrap/version transport independently.

On systems where GLFW raw mouse motion is unavailable, the relative-motion fallback remains independent of cursor screen bounds but does **not** claim to bypass operating-system pointer acceleration.

## Rendering

A production renderer API/loop is not yet available for normal engine consumers. `GlfwWindow` exposes platform event polling, display-mode changes, cursor-capture policy, renderer-frame input snapshots, binding metadata/loading, renderer-frame action evaluation, and the platform side of tick-command sampling, but it still does not expose buffer swapping, viewport mutation, renderer ownership, or an OpenGL debug callback.

## Assets/world/physics/audio/networking/editor

The target modules exist according to the repository architecture, but a module's existence does not mean its consumer API has been implemented. Do not write wiki examples for planned APIs until the corresponding implementation task is complete and verified.

## Native evidence limits

Current production window acceptance covers the bounded GLFW/OpenGL window lifecycle, P3-T02's logical/framebuffer size path, P3-T03's single 20-transition display-mode scenario, P3-T04's real Windows focus-transfer/cursor-release scenario, and P3-T05's bounded raw-mode/relative-motion acceptance. P3-T06 adds a pure-Java snapshot boundary over that already-tested hardware ingestion path, P3-T07 adds pure-Java binding metadata/JSON loading, P3-T08 adds pure-Java action evaluation, and P3-T09 adds pure-Java tick-command sampling/codec/replay over those existing boundaries. These tasks do not establish sustained native stability or repeated native restartability; P0-T13 and P0-T14 remain separate evidence gates.

## Stability

The engine is pre-v1 and the public API is still being built phase by phase. Treat current documented contracts as real for the corresponding completed/active bounded tasks, but do not assume a broad API freeze.

For exact status, use [`../docs/DEVELOPMENT_STATUS.md`](../docs/DEVELOPMENT_STATUS.md) and live GitHub Issues/PRs rather than this page.
