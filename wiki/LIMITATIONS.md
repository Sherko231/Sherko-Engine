# Current limitations

This page prevents planned roadmap work from being mistaken for an already usable library API.

## Platform/input

Implemented:

- one production `GlfwWindow` lifecycle;
- explicit OpenGL 4.6 Core context request;
- actual OpenGL version/renderer verification and logging;
- explicit native ownership/cleanup.

Not yet exposed as production API:

- event polling abstraction;
- window-size/framebuffer-size events;
- fullscreen transitions;
- focus events;
- keyboard state;
- mouse state;
- raw mouse capture;
- controller input;
- tick-aligned input snapshots/commands.

## Rendering

A production renderer API/loop is not yet available for normal engine consumers. `GlfwWindow` does not expose swap/poll methods or an OpenGL debug callback.

## Assets/world/physics/audio/networking/editor

The target modules exist according to the repository architecture, but a module's existence does not mean its consumer API has been implemented. Do not write wiki examples for planned APIs until the corresponding implementation task is complete and verified.

## Native evidence limits

Current `GlfwWindow` acceptance evidence proves one production window/context lifecycle on Windows x64 with OpenGL 4.6. It does not replace the separate sustained native-soak or repeated native-lifecycle feasibility gates.

## Stability

The engine is pre-v1 and the public API is still being built phase by phase. Treat current documented contracts as real for the corresponding completed tasks, but do not assume a broad API freeze.

For exact status, use [`../docs/DEVELOPMENT_STATUS.md`](../docs/DEVELOPMENT_STATUS.md) and live GitHub Issues/PRs rather than this page.
