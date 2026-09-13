# Sherko Engine Sandbox

`game-sandbox` is the canonical owner-facing manual demo for the engine.

It exists so the owner can run one small application and observe what the current production engine APIs actually do. It complements automated tests; it does not replace unit, native, integration, CI, soak, or lifecycle evidence.

## Run

On Windows x64 with Java 25 available through the repository Gradle toolchain:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

The current scripted demo runs for about 38 seconds and prints its timeline before starting.

## What the current demo shows

- production `GlfwWindow` / OpenGL 4.6 startup through the public engine API;
- logical window size versus framebuffer pixel size notifications;
- windowed -> borderless fullscreen -> windowed -> exclusive fullscreen -> windowed transitions on the same production window/context;
- explicit cursor capture and the P3-T04 focus-loss rule: Alt+Tab should release effective capture and focus regain must not auto-recapture;
- one public `InputSnapshot` captured after each demo-frame event poll;
- one public `InputActionSnapshot` evaluated from that hardware snapshot through `InputActionEvaluator` and the committed demo `InputActionBindings`;
- once-per-second bounded diagnostics showing raw W/A/S/D hardware state, MOVE X/Y, JUMP and INTERACT pressed/held/released state, plus accumulated public mouse delta;
- `EngineClock`, `FixedStepAccumulator`, and `FixedStepCatchUpPolicy` driving cumulative 60 Hz simulation-tick diagnostics;
- interpolation alpha as a timing diagnostic;
- orderly stop/close plus `NativeResourceRegistry.assertNoOpenResources()`.

During the cursor-capture observation interval, move the mouse, hold/release W/A/S/D, tap SPACE or E, and optionally Alt+Tab away/back. The diagnostics use only production public APIs: `GlfwWindow.captureInputSnapshot(...)`, `InputSnapshot`, `InputActionBindings`, `InputActionEvaluator`, `InputActionSnapshot`, and `InputActionState`. The sandbox does not import GLFW or platform internals.

The committed sandbox binding resource uses the same strict JSON schema-v1 public loader contract as normal consumers. It maps W/A/S/D to MOVE, mouse delta to LOOK, SPACE to JUMP, E to INTERACT, and the remaining Phase 3 actions to representative existing keys/buttons. This file is demo configuration, not a frozen gameplay-default policy.

Runtime state changes and diagnostics are emitted through the production `EngineLogger` boundary. Direct console output is reserved for owner-facing instructions such as the startup timeline and the Alt+Tab/input prompt; the logger's caller-owned console sink formats structured log events onto `System.out`.

The once-per-second line is deliberately a sandbox diagnostic. It is not FPS, a renderer benchmark, a soak test, or performance acceptance evidence. MOVE/JUMP/INTERACT values are renderer-frame action observations, not P3-T09 simulation-tick commands or replay evidence. `mouseDeltaSinceLastDiagnostic` is an owner-observation sum of the per-frame public snapshot deltas, not a new engine API or performance metric.

## Current limitations

The window is intentionally visually empty because the production renderer path has not been implemented yet. The sandbox does not bypass that limitation with direct OpenGL/LWJGL calls.

The public hardware snapshot, data-driven binding metadata, and renderer-frame action evaluation/transitions now exist. The demo still does not implement controller input/settings, sensitivity/Y inversion/dead zones/curves, tick-aligned `PlayerInputCommand`, replay/network input codecs, gameplay camera behavior, or UI input consumption.

## Maintenance rule

When a future task adds or materially changes an engine capability that can be observed through already-authorized public production APIs, update this sandbox in the same PR so the owner can inspect the new capability.

If a capability cannot yet be demonstrated without exposing internal APIs, importing implementation packages, calling native libraries directly, or implementing a future roadmap task, do not force it into the sandbox. Record:

`Sandbox impact: none — <reason>`

in the task PR/handoff and extend the sandbox when the necessary public engine boundary actually exists.

Prefer evolving this demo over creating unrelated throwaway demos. Additional subsystem-specific demo entry points are acceptable later only when one combined demo would become unclear or impractical.
