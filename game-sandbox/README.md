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
- `EngineClock`, `FixedStepAccumulator`, and `FixedStepCatchUpPolicy` driving cumulative 60 Hz simulation-tick diagnostics;
- interpolation alpha as a timing diagnostic;
- orderly stop/close plus `NativeResourceRegistry.assertNoOpenResources()`.

Runtime state changes and diagnostics are emitted through the production `EngineLogger` boundary. Direct console output is reserved for owner-facing instructions such as the startup timeline and the Alt+Tab prompt; the logger's caller-owned console sink is the component that formats structured log events onto `System.out`.

The once-per-second line is deliberately a sandbox diagnostic. It is not FPS, a renderer benchmark, a soak test, or performance acceptance evidence.

## Current limitations

The window is intentionally visually empty because the production renderer path has not been implemented yet. The sandbox does not bypass that limitation with direct OpenGL/LWJGL calls.

There is no public gameplay input snapshot/key-command API yet, so the demo is scripted rather than keyboard-driven. It does not add raw mouse motion, renderer presentation, buffer swapping, action mappings, or later roadmap behavior before the corresponding engine tasks exist.

## Maintenance rule

When a future task adds or materially changes an engine capability that can be observed through already-authorized public production APIs, update this sandbox in the same PR so the owner can inspect the new capability.

If a capability cannot yet be demonstrated without exposing internal APIs, importing implementation packages, calling native libraries directly, or implementing a future roadmap task, do not force it into the sandbox. Record:

`Sandbox impact: none — <reason>`

in the task PR/handoff and extend the sandbox when the necessary public engine boundary actually exists.

Prefer evolving this demo over creating unrelated throwaway demos. Additional subsystem-specific demo entry points are acceptable later only when one combined demo would become unclear or impractical.
