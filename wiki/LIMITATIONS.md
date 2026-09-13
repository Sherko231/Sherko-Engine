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
- explicit `InputBindingLoadException` for file/schema/validation failures while Jackson remains absent from public signatures.

Not yet exposed as production API:

- choosing a non-primary monitor;
- custom fullscreen resolution or refresh-rate selection;
- raw GLFW window/monitor handles;
- public focus event listeners (focus is available as snapshot state only);
- arbitrary keyboard keys or mouse buttons outside the current `InputKey` / `InputMouseButton` vocabulary;
- controller input;
- action-level pressed/held/released transitions;
- action-value evaluation or simultaneous-binding aggregation/conflict policy;
- sensitivity/Y inversion/dead-zone/controller-curve settings;
- content-scale callbacks as a consumer API;
- tick-aligned `PlayerInputCommand` records;
- input replay/network codecs.

`InputSnapshot` is a client/platform renderer-frame hardware view. P3-T07 binding metadata describes how hardware controls target gameplay action components but does not evaluate a snapshot. P3-T08 owns action-level transition/aggregation semantics and P3-T09 owns tick-aligned `PlayerInputCommand` / replay-friendly device-neutral commands. `game-server` therefore remains independent of `engine-platform-lwjgl`.

The current JSON binding schema is intentionally strict and versioned at `schemaVersion: 1`. Every required action must appear exactly once. Unknown/duplicate fields, invalid enum/control names, empty action binding lists, duplicate binding descriptors, invalid action components, unsupported schema versions, malformed JSON, and unreadable paths fail rather than being ignored or partially applied. This task does not yet define runtime remapping UI, config hot reload, controller bindings, or migration between future schema versions.

On systems where GLFW raw mouse motion is unavailable, the relative-motion fallback remains independent of cursor screen bounds but does **not** claim to bypass operating-system pointer acceleration.

## Rendering

A production renderer API/loop is not yet available for normal engine consumers. `GlfwWindow` exposes platform event polling, display-mode changes, cursor-capture policy, renderer-frame input snapshots, and input-binding metadata/loading, but it still does not expose buffer swapping, viewport mutation, renderer ownership, or an OpenGL debug callback.

## Assets/world/physics/audio/networking/editor

The target modules exist according to the repository architecture, but a module's existence does not mean its consumer API has been implemented. Do not write wiki examples for planned APIs until the corresponding implementation task is complete and verified.

## Native evidence limits

Current production window acceptance covers the bounded GLFW/OpenGL window lifecycle, P3-T02's logical/framebuffer size path, P3-T03's single 20-transition display-mode scenario, P3-T04's real Windows focus-transfer/cursor-release scenario, and P3-T05's bounded raw-mode/relative-motion acceptance. P3-T06 adds a pure-Java snapshot boundary over that already-tested hardware ingestion path, and P3-T07 adds pure-Java binding metadata/JSON loading over the existing engine input vocabulary. Neither task by itself adds a new native evidence claim. These checks do not establish sustained native stability or repeated native restartability; P0-T13 and P0-T14 remain separate evidence gates.

## Stability

The engine is pre-v1 and the public API is still being built phase by phase. Treat current documented contracts as real for the corresponding completed/active bounded tasks, but do not assume a broad API freeze.

For exact status, use [`../docs/DEVELOPMENT_STATUS.md`](../docs/DEVELOPMENT_STATUS.md) and live GitHub Issues/PRs rather than this page.
