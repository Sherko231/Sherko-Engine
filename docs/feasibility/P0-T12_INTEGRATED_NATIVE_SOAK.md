# P0-T12 — Integrated Native Feasibility Soak

## Purpose

Run the previously proven native subsystems together in one disposable Java 25 feasibility executable before Phase 1 begins.

This spike combines:

- GLFW window lifecycle;
- OpenGL 4.6 Core rendering/debug context;
- Jolt JNI physics simulation;
- OpenAL device/context/source/buffer lifecycle;
- localhost UDP send/echo traffic;
- Java Flight Recorder.

It is not production engine architecture.

## Duration decision

The repository owner explicitly changed the P0-T12 acceptance duration from **15 minutes** to **15 seconds** on 2026-09-10. GitHub Issue #29 contains the updated live acceptance criteria and supersedes the older 15-minute wording still present in the canonical backlog until that catalog entry can be edited safely.

## Entry point

`src/main/java/com/samo/spike/integration/IntegratedNativeSoakSpike.java`

## Run command

From the repository root on the target Windows x64 development machine:

```powershell
.\gradlew.bat runIntegratedNativeSoak
```

Default duration: `15` seconds.

Optional override:

```powershell
.\gradlew.bat runIntegratedNativeSoak -PnativeSoakDurationSeconds=15
```

## JFR output

The Gradle task starts Java Flight Recorder automatically with the `profile` settings and dumps the recording on exit to:

`build/spikes/native-soak/p0-t12.jfr`

## Expected runtime evidence

The executable must report successful initialization for OpenGL, OpenAL, Jolt, and UDP, run continuously for the requested 15 seconds, and then report UDP traffic totals.

On the Jolt debug native build, shutdown also prints the initial and final Jolt allocation balance. The final balance must not be greater than the initial balance.

A successful run ends with:

```text
P0-T12 passed: integrated native soak completed and all subsystems shut down cleanly.
```

## Cleanup checks

Shutdown is explicit and ordered:

1. close UDP channels;
2. remove/destroy Jolt bodies;
3. close Jolt settings/shapes/job system/allocator/system/filter objects;
4. unregister Jolt types and destroy the factory;
5. stop/delete OpenAL source and buffer, destroy context, close device;
6. free OpenGL debug callback, destroy GLFW window, terminate GLFW.

The spike fails if it observes a high-severity OpenGL debug message, insufficient UDP echo traffic, a Jolt body that fails to settle, OpenAL errors, or positive Jolt native-allocation growth after cleanup.

## Acceptance state

Implementation is complete. P0-T12 remains **in progress** until a local 15-second run succeeds and the generated JFR file is confirmed present.