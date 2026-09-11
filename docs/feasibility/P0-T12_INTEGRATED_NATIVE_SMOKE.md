# P0-T12 — Integrated Native Feasibility Smoke Test

## Purpose

Run the previously proven native subsystems together in one disposable Java 25 feasibility executable and verify short startup, interaction, and orderly shutdown.

This spike combines:

- GLFW window lifecycle;
- OpenGL 4.6 Core rendering/debug context;
- Jolt JNI physics simulation;
- OpenAL device/context/source/buffer lifecycle;
- localhost UDP send/echo traffic;
- Java Flight Recorder.

It is not production engine architecture.

## Classification and duration

The repository owner changed P0-T12 from 15 minutes to 15 seconds on 2026-09-10. The run therefore proves integrated startup/use/shutdown only and is classified as a smoke test, not a soak or long-duration leak/stability test.

The canonical backlog now records the 15-second contract directly. P0-T13 owns the sustained 15-minute run and P0-T14 owns repeated lifecycle evidence.

## Entry point

`src/main/java/com/samo/spike/integration/IntegratedNativeSoakSpike.java`

## Run command

From the repository root on the target Windows x64 development machine:

```powershell
.\gradlew.bat runIntegratedNativeSmoke
```

Default duration: `15` seconds.

Optional override:

```powershell
.\gradlew.bat runIntegratedNativeSmoke -PnativeEvidenceDurationSeconds=15
```

## JFR output

The Gradle task starts Java Flight Recorder automatically with the `profile` settings and dumps the recording on exit to:

`build/spikes/native-evidence/p0-t12-smoke.jfr`

For the separate sustained test, run:

```powershell
.\gradlew.bat runIntegratedNativeSoak
```

Its default duration is 900 seconds and its JFR output is `build/spikes/native-evidence/p0-t13-soak.jfr`.

## Verified runtime result

The owner ran the integrated smoke test successfully on the target Windows x64 development machine under Java 25.

Observed runtime evidence:

- JFR recording started successfully before the spike began;
- OpenGL initialized as `4.6.0 NVIDIA 592.02`;
- OpenAL initialized as `1.1 ALSOFT 1.25.2`;
- Jolt initialized as `6.0.0`;
- UDP initialized on `127.0.0.1:42120`;
- the 15-second runtime completed;
- UDP sent `58` datagrams and echoed all `58`;
- Jolt debug allocation balance changed from `1` initially to `0` at shutdown (`delta=-1`), so no positive native-allocation growth was observed;
- the executable printed the P0-T12 pass line and Gradle exited successfully.

The LWJGL `sun.misc.Unsafe::objectFieldOffset` warning is a Java deprecation warning from LWJGL's legacy unsafe memory backend. It did not fail the run or invalidate the feasibility result, but it should be re-evaluated when LWJGL/JDK versions are upgraded.

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

**Complete as a smoke test.** The 15-second integrated run passed under JFR with all selected subsystems active together, complete UDP echo traffic, clean process shutdown, and no observed positive Jolt native-allocation growth. It does not claim sustained stability; P0-T13 and P0-T14 must provide that evidence.

## Wiki synchronization

This file remains feasibility evidence and must not be presented as a production API guide. If a future native/platform/render/physics/audio/networking task converts behavior covered here into an implemented public engine API or changes caller-visible usage, update the relevant [`../../wiki/`](../../wiki/README.md) pages in the same PR. Otherwise record `Wiki impact: none — <reason>`.
