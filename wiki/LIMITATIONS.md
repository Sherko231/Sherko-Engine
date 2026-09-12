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
- immutable renderer-frame `InputSnapshot` capture through `GlfwWindow.captureInputSnapshot(long)`, exposing bounded `InputKey` / `InputMouseButton` held state, retained raw press/release edges, focus/capture state, and consumed per-snapshot relative mouse delta without exposing GLFW constants/types.

Not yet exposed as production API:

- choosing a non-primary monitor;
- custom fullscreen resolution or refresh-rate selection;
- raw GLFW window/monitor handles;
- public focus event listeners (focus is available as snapshot state only);
- arbitrary keyboard keys or mouse buttons outside the current `InputKey` / `InputMouseButton` vocabulary;
- controller input;
- data-driven input actions and action-level transitions;
- sensitivity/Y inversion/dead-zone/controller-curve settings;
- content-scale callbacks as a consumer API;
- tick-aligned `PlayerInputCommand` records;
- input replay/network codecs.

`InputSnapshot` is a client/platform renderer-frame hardware view, not a headless simulation input format. P3-T07/P3-T08 own the action layer and P3-T09 owns tick-aligned `PlayerInputCommand` / replay-friendly device-neutral commands. `game-server` therefore remains independent of `engine-platform-lwjgl`.

On systems where GLFW raw mouse motion is unavailable, the relative-motion fallback remains independent of cursor screen bounds but does **not** claim to bypass operating-system pointer acceleration.

## Rendering

A production renderer API/loop is not yet available for normal engine consumers. `GlfwWindow` exposes platform event polling, display-mode changes, cursor-capture policy, and renderer-frame input snapshots, but it still does not expose buffer swapping, viewport mutation, renderer ownership, or an OpenGL debug callback.

## Assets/world/physics/audio/networking/editor

The target modules exist according to the repository architecture, but a module's existence does not mean its consumer API has been implemented. Do not write wiki examples for planned APIs until the corresponding implementation task is complete and verified.

## Native evidence limits

Current production window acceptance covers the bounded GLFW/OpenGL window lifecycle, P3-T02's logical/framebuffer size path, P3-T03's single 20-transition display-mode scenario, P3-T04's real Windows focus-transfer/cursor-release scenario, and P3-T05's bounded raw-mode/relative-motion acceptance. P3-T06 adds a pure-Java snapshot boundary over that already-tested hardware ingestion path and does not by itself add a new native evidence claim. These checks do not establish sustained native stability or repeated native restartability; P0-T13 and P0-T14 remain separate evidence gates.

## Stability

The engine is pre-v1 and the public API is still being built phase by phase. Treat current documented contracts as real for the corresponding completed tasks, but do not assume a broad API freeze.

For exact status, use [`../docs/DEVELOPMENT_STATUS.md`](../docs/DEVELOPMENT_STATUS.md) and live GitHub Issues/PRs rather than this page.
