# Sherko Engine Sandbox

`game-sandbox` is the canonical persistent owner-facing playground for Sherko Engine.

It is intentionally cumulative. It is not a disposable demo for one task and it does not automatically walk through features on a timer. The owner should be able to launch one sandbox, keep it running, and freely exercise the production capabilities that are currently available through public engine APIs. As new usable capabilities land, this same playground evolves instead of being replaced by throwaway demos.

It complements automated tests; it does not replace unit, native, integration, CI, soak, lifecycle, replay, or performance evidence.

## Run

On Windows x64 with Java 25 available through the repository Gradle toolchain:

```powershell
.\gradlew.bat :game-sandbox:runSandbox
```

`runEngineDemo` remains only as a temporary compatibility alias. `runSandbox` is the canonical command.

The sandbox stays open until you explicitly exit with `Ctrl+Q`.

## Owner controls

- `F`: cycle `WINDOWED -> BORDERLESS_FULLSCREEN -> EXCLUSIVE_FULLSCREEN -> WINDOWED`.
- `R`: toggle cursor capture.
- `Right Shift + F`: cycle mouse sensitivity through `0.5`, `1.0`, and `2.0` using public `InputResponseSettings`.
- `Right Shift + R`: toggle mouse Y inversion.
- `Ctrl + Q`: exit the sandbox cleanly.

Normal input bindings remain active at the same time: W/A/S/D, mouse movement, Space, E, mouse buttons, Q push-to-talk, and the rest of the committed Phase 3 action bindings. `Ctrl+Q` is reserved by the sandbox for explicit exit; plain `Q` remains the normal action binding.

## What the current sandbox exposes

The current playground keeps these capabilities active together rather than showing them one by one:

- production `GlfwWindow` / OpenGL 4.6 startup through the public engine API;
- public `RenderFramePacket` construction each visible frame, snapshotting the current camera matrices and framebuffer pixel size before synchronous `OpenGlRenderer.render(frame)` consumption;
- P5-T14 public local-light submission on that packet: one warm point light and one cool spot light, with the renderer configured for a maximum of four local lights through the structured sandbox `EngineLogger`;
- public `OpenGlRenderer` production composition drawing the fixed renderer-owned P5-T18 room fixture through the existing two internal P5-T09 material submissions; the room contains mapped UVs, a non-uniform internal sRGB texture, multiple depth-separated surfaces, and a nearer occlusion panel while preserving one presentation encode;
- P5-T15 finalizes the renderer's sRGB presentation policy used automatically by this same sandbox path, with no new sandbox control or renderer-internal import;
- P5-T16 submits one renderer-neutral debug line, AABB, sphere, and finite ray every visible frame through `DebugFrame`, plus bounded `simulation/tick` and `input/frame` text counters; the geometry is visible in the same scene and the renderer-published counters appear in the existing once-per-second console diagnostic;
- P5-T17 automatically adds one engine-owned orange first-person view-model validation rectangle in the lower-center/right region after world/debug rendering; it uses an internal 55-degree projection and isolated depth layer without adding any sandbox renderer-internal import or gameplay/asset object;
- P5-T06 camera/per-frame uniform blocks driven by the sandbox-owned P5-T18 camera pose and current framebuffer aspect; W/A/S/D moves in D-041 world space and mouse LOOK changes bounded yaw/pitch through tick-aligned `PlayerInputCommand` data;
- explicit depth testing and back-face culling through the production renderer path;
- window-owned `GlfwWindow.present()` presentation without exposing a native window handle;
- explicit `OpenGlDebugMode.FAIL_ON_HIGH_SEVERITY` diagnostics, so a high-severity driver message fails at the owner-thread `pollEvents()` boundary instead of being ignored;
- logical window size versus framebuffer pixel size notifications;
- manual transitions among windowed, borderless fullscreen, and exclusive fullscreen on the same production window/context;
- explicit cursor capture and the focus-loss safety rule: Alt+Tab releases effective capture and focus regain does not silently recapture;
- public renderer-frame `InputSnapshot` hardware state;
- data-driven `InputActionBindings` plus `InputActionEvaluator` action state/transitions;
- live mouse sensitivity and mouse-Y inversion changes through `InputResponseSettings`;
- `PlayerInputCommandSampler` and tick-aligned `PlayerInputCommand` output;
- `EngineClock`, `FixedStepAccumulator`, and `FixedStepCatchUpPolicy` driving the fixed 60 Hz simulation-tick path;
- interpolation alpha and once-per-second diagnostics, including P5-T11 tested/visible/culled/submitted render counters from the latest successful frame;
- structured `EngineLogger` output;
- orderly stop/close plus `NativeResourceRegistry.assertNoOpenResources()` on exit.

The once-per-second line is deliberately an owner diagnostic. P5-T11 appends `renderCull[tested=...,visible=...,culled=...,draws=...]`; with the current fixed visible room candidate set the expected steady state is two tested candidates, two visible candidates, zero culled candidates, and two submitted draws. P5-T16 also appends the renderer-published `debugCounters=[simulation/tick=...,input/frame=...]` snapshot. These diagnostics are not FPS, a benchmark, a soak test, replay acceptance evidence, or leak proof.

## Current limitations

The sandbox submits each visible frame through the public immutable `RenderFramePacket` boundary. P5-T18 updates the packet's view matrix from owner-controlled camera state, while P5-T11 derives the active view frustum and tests the fixed renderer-owned room AABB before both current draw candidates. P5-T12 orders visible scene submissions deterministically. P5-T13/P5-T14 provide the fixed directional plus bounded local-light path. P5-T16 adds a `DebugFrame` containing one line, AABB, sphere, ray, and two counters. P5-T17 then automatically renders the internal camera-relative validation rectangle after those world/debug stages using its own projection and depth reset. The sandbox still performs no direct OpenGL/LWJGL calls and imports no renderer implementation packages.

This remains bounded renderer-foundation content. It does not provide arbitrary/public textures or materials, public view-model mesh/material submission, gameplay hands/weapons/tools, skeletal animation/IK, third-person presentation, more than eight local lights per frame, world/ECS light ownership, directional-light replacement, shadows, arbitrary mesh loading, HDR, tonemapping, fog, bloom, exposure, color grading, general post-processing, offscreen framebuffer pipelines, PBR/IBL, clustered/Forward+ lighting, retained debug scenes or multi-frame debug lifetimes, font/glyph debug text, editor/ImGui runtime HUD, world/ECS rendering, gameplay camera/light ownership, physics gameplay, networking integration, or runtime UI. Those capabilities are added here when their real public production boundaries exist.

## Persistent maintenance rule

For every future task that adds or materially changes an engine capability, evaluate whether the capability is meaningfully usable in this existing playground through already-authorized public production APIs.

If yes, update this same sandbox in the same PR. Preserve the existing usable capabilities unless the active task explicitly replaces or removes them. Do not turn the sandbox back into a timed showcase, a one-feature demo, or a sequence that hides older capabilities while presenting the new one.

If the capability cannot yet be demonstrated without exposing internals, importing implementation packages, calling native libraries directly, or implementing future roadmap work, record:

`Sandbox impact: none — <reason>`

and integrate it later when the required public boundary exists.

A separate subsystem-specific playground entry point is exceptional, not the default. Use one only when coexistence in the main sandbox would be genuinely impractical or confusing and the active Issue explicitly authorizes it.
