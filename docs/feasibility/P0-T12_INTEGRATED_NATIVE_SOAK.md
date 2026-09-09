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

The repository owner explicitly changed the P0-T12 acceptance duration from **15 minutes** to **15 seconds** on 2026-09-10. GitHub Issue #29 contains the updated live acceptance criteria and supersedes the older 15-minute wording still present in the canonical backlog.

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

## Verified runtime result

The owner ran the integrated soak successfully on the target Windows x64 development machine under Java 25.

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

**Complete.** The owner-approved 15-second integrated native soak passed under JFR with all selected subsystems active together, complete UDP echo traffic, clean process shutdown, and no observed positive Jolt native-allocation growth. This satisfies the final Phase 0 executable feasibility gate.