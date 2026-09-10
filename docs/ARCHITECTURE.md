# Sherko Engine Architecture

This document describes intended module responsibilities and the architecture actually present in the repository. Product constraints live in `ENGINE_SCOPE.md`; durable decisions live in `docs/DECISIONS.md`; changing progress lives in `docs/DEVELOPMENT_STATUS.md`.

## Maturity vocabulary

| State | Meaning |
| --- | --- |
| Implemented | Production-oriented source and tests exist for the stated responsibility. |
| Skeleton | Gradle module and dependency boundary exist, but production subsystem code does not. |
| Experimental | Disposable feasibility code exists and must not be treated as production architecture. |
| Planned | Roadmap contract exists, but the repository does not yet implement it. |

## Current repository architecture

The repository currently has a working Java 25 multi-project build with 16 declared subprojects. All target engine/game modules are skeletons: their Gradle boundaries and shared smoke-test wiring exist, but production engine subsystem implementations have not started. The root project contains experimental Phase 0 native/network spikes.

| Module | Intended responsibility | Current state | Direct project dependencies |
| --- | --- | --- | --- |
| `engine-core` | Lifecycle, time, IDs, events, math/spatial contracts | Skeleton | None |
| `engine-platform-lwjgl` | GLFW/window/input and platform-native boundary | Skeleton | `engine-core` |
| `engine-assets` | Runtime asset handles/formats and loading contracts | Skeleton | `engine-core` |
| `engine-ui` | Renderer-neutral runtime HUD/menu model and draw data | Skeleton | `engine-core`, `engine-assets` |
| `engine-render-opengl` | OpenGL renderer and runtime-UI draw adapter | Skeleton | `engine-core`, `engine-platform-lwjgl`, `engine-assets`, `engine-ui` |
| `engine-world` | Scene/world/component/prefab runtime | Skeleton | `engine-core`, `engine-assets` |
| `engine-physics-jolt` | Jolt-backed physics adapter and ownership | Skeleton | `engine-core` |
| `engine-audio-openal` | OpenAL-backed positional audio | Skeleton | `engine-core` |
| `engine-network-api` | Transport-neutral session/message contracts | Skeleton | `engine-core` |
| `engine-network-ip` | Direct IP transport adapter | Skeleton | `engine-core`, `engine-network-api` |
| `engine-steam` | Steam social/session and transport integration | Skeleton | `engine-core`, `engine-network-api` |
| `engine-editor` | Internal authoring/debug tooling | Skeleton | `engine-core`, `engine-assets`, `engine-world`, `engine-render-opengl` |
| `game-sandbox` | Game rules and vertical-slice content | Skeleton | `engine-core`, `engine-world`, `engine-physics-jolt`, `engine-network-api`, `engine-ui` |
| `game-client` | Client composition root | Skeleton | `game-sandbox`, platform, render, audio, IP, Steam adapters |
| `game-server` | Headless/listen-server composition root | Skeleton | `game-sandbox`, IP and Steam adapters |
| `test-support` | Shared JUnit 5/AssertJ test dependencies and fixtures | Implemented build support | None |

The table reflects Gradle project dependencies at the checkpoint. `P1-T07` will add automated boundary enforcement; until then, the dependency graph is configured but not architecture-tested.

## Dependency rules

- Dependencies point from composition/game layers toward engine APIs and adapters, never from engine modules into game modules.
- `engine-core` is the lowest shared layer and must not depend on LWJGL, OpenGL, Jolt, OpenAL, Steam, or game code.
- `engine-ui` exposes renderer-neutral state/draw data; it must not expose OpenGL or imgui-java types.
- `engine-network-api` contains no socket/Steam implementation details.
- `game-server` must remain runnable without window, renderer, or audio modules.
- `engine-editor` may consume runtime modules, but runtime modules must not depend on the editor.
- Asset authoring/import dependencies belong in offline tooling; shipped gameplay consumes cooked formats.

## Runtime composition target

The client will compose platform/input, OpenGL rendering, runtime UI, audio, world/physics, game rules, and either IP or Steam networking. The server will compose world/physics, game rules, and networking without graphics/audio. Server authority owns gameplay state and dynamic physics; clients predict/present but do not submit authoritative transforms.

## Experimental code boundary

Phase 0 code under root `src/main/java/com/samo/spike/` proves isolated capabilities:

- GLFW/OpenGL initialization;
- Jolt lifecycle;
- OpenAL lifecycle;
- localhost UDP and deterministic impairment;
- Steam initialization and FFM flat-API access;
- a 15-second combined native smoke run.

It is experimental, not a reusable engine layer. Moving reusable behavior into modules requires the planned `P1-T10A` task or another explicit Issue, with ownership contracts and tests.

## Architecture verification status

| Property | Status |
| --- | --- |
| 16 modules declared | Implemented |
| Shared Java 25/test conventions | Implemented |
| Dependency locking/version catalog | Implemented |
| Automated package/module boundary test | Planned: P1-T07 / Issue #37 |
| Client/server executable composition roots | Planned: P1-T09 / Issue #39 |
| Production engine subsystems | Planned: Phase 2 onward |
