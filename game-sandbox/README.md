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
- public `OpenGlRenderer` production composition drawing one indexed static triangle every visible framebuffer frame, now using the fixed P5-T08 neutral-gray sRGB reference texture and one default-framebuffer sRGB encode;
- P5-T06 camera/per-frame uniform blocks driven by a fixed D-041/D-045 camera and current framebuffer aspect;
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
- interpolation alpha and once-per-second diagnostics;
- structured `EngineLogger` output;
- orderly stop/close plus `NativeResourceRegistry.assertNoOpenResources()` on exit.

The once-per-second line is deliberately an owner diagnostic. It is not FPS, a benchmark, a soak test, replay acceptance evidence, or leak proof.

## Current limitations

The sandbox now shows one neutral-gray indexed triangle on a dark development background through the public production renderer path. P5-T08 makes this fixed reference path sRGB-correct: the renderer samples an internal sRGB texture in linear space and encodes once when writing the sRGB-capable default framebuffer. The sandbox itself still performs no direct OpenGL/LWJGL calls.

This remains bounded renderer-foundation content. It does not provide arbitrary/public textures, materials, lighting, arbitrary mesh loading, HDR/tonemapping/fog/post-processing, world/ECS rendering, gameplay camera ownership, physics gameplay, networking integration, or runtime UI. Those capabilities are added here when their real public production boundaries exist.

## Persistent maintenance rule

For every future task that adds or materially changes an engine capability, evaluate whether the capability is meaningfully usable in this existing playground through already-authorized public production APIs.

If yes, update this same sandbox in the same PR. Preserve the existing usable capabilities unless the active task explicitly replaces or removes them. Do not turn the sandbox back into a timed showcase, a one-feature demo, or a sequence that hides older capabilities while presenting the new one.

If the capability cannot yet be demonstrated without exposing internals, importing implementation packages, calling native libraries directly, or implementing future roadmap work, record:

`Sandbox impact: none — <reason>`

and integrate it later when the required public boundary exists.

A separate subsystem-specific playground entry point is exceptional, not the default. Use one only when coexistence in the main sandbox would be genuinely impractical or confusing and the active Issue explicitly authorizes it.
