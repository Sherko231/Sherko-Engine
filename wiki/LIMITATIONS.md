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
- internally tracked keyboard/mouse-button held-state safety for focus-loss cleanup;
- focus loss clears held key/button state and releases effective cursor capture;
- focus regain never recaptures automatically and requires an explicit later capture request.

Not yet exposed as production API:

- choosing a non-primary monitor;
- custom fullscreen resolution or refresh-rate selection;
- raw GLFW window/monitor handles;
- public focus events/listeners;
- public keyboard/mouse held-state polling or snapshots;
- raw mouse motion/deltas;
- controller input;
- data-driven input actions/transitions;
- content-scale callbacks as a consumer API;
- tick-aligned input snapshots/commands.

The internal pre-snapshot held-state tracking added for P3-T04 is deliberately not a consumer API. P3-T06 remains responsible for the public hardware snapshot boundary.

## Rendering

A production renderer API/loop is not yet available for normal engine consumers. `GlfwWindow` exposes platform event polling, display-mode changes, and cursor-capture policy only; it still does not expose buffer swapping, viewport mutation, renderer ownership, or an OpenGL debug callback.

## Assets/world/physics/audio/networking/editor

The target modules exist according to the repository architecture, but a module's existence does not mean its consumer API has been implemented. Do not write wiki examples for planned APIs until the corresponding implementation task is complete and verified.

## Native evidence limits

Current production window acceptance covers the bounded GLFW/OpenGL window lifecycle, P3-T02's logical/framebuffer size path, P3-T03's single 20-transition display-mode scenario, and P3-T04's real Windows focus-transfer/cursor-release scenario. P3-T04 does not prove public gameplay input snapshots because `InputSnapshot` is still a later task. These checks do not establish sustained native stability or repeated native restartability; P0-T13 and P0-T14 remain separate evidence gates.

## Stability

The engine is pre-v1 and the public API is still being built phase by phase. Treat current documented contracts as real for the corresponding completed tasks, but do not assume a broad API freeze.

For exact status, use [`../docs/DEVELOPMENT_STATUS.md`](../docs/DEVELOPMENT_STATUS.md) and live GitHub Issues/PRs rather than this page.
