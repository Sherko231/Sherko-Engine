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

The repository has a working Java 25 multi-project build with 17 declared Gradle subprojects: the 16 production-target engine/game/support modules defined by `ENGINE_SCOPE.md`, plus the experimental `feasibility-spikes` subproject. Engine subsystem modules and `game-sandbox` remain skeletons, while `game-client` and `game-server` provide minimal executable composition roots for the foundation state. The root project is now a build, quality, and task-aggregation project with no Java source tree and no spike runtime dependencies.

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
| `game-client` | Client composition root | Minimal executable foundation entry point | `game-sandbox`, platform, render, audio, IP, Steam adapters |
| `game-server` | Headless/listen-server composition root | Minimal executable headless foundation entry point | `game-sandbox`, IP and Steam adapters |
| `test-support` | Shared JUnit 5/AssertJ support plus repository architecture verification | Implemented build/test support | None |
| `feasibility-spikes` | Disposable Phase 0 native/network feasibility executables | Experimental | None; external native/library dependencies only |

`feasibility-spikes` is not a seventeenth production engine module. It is deliberately outside the locked 16-module runtime target in `ENGINE_SCOPE.md`, and no production client/server module depends on it.

## Package/API boundary contract

`config/architecture/module-boundaries.properties` is the machine-readable registry for all 17 declared Gradle subprojects. Every declared subproject, including the experimental feasibility module, declares:

- one owned package root;
- one public cross-module API root under that package root;
- one internal implementation root under that package root.

Code inside a module may use its own implementation packages. Cross-module imports must target the other module's declared API root. Imports of another module's internal root are architectural violations even when the Gradle project dependency itself is otherwise valid.

`ModulePackageBoundaryTest` now lives under `test-support/src/test` because the root has no Java source tree. It verifies registry completeness, verifies source packages stay under their owning module root, and scans production/declared-subproject imports for cross-module implementation shortcuts. A deliberate fixture under `config/architecture/fixtures/` represents a forbidden `game-client -> engine-platform-lwjgl.internal` dependency and remains opt-in negative evidence.

These package roots define boundaries, not future subsystem interfaces. P1-T10A does not create a reusable feasibility API or promote spike code into production architecture.

## Dependency rules

- Dependencies point from composition/game layers toward engine APIs and adapters, never from engine modules into game modules.
- `engine-core` is the lowest shared layer and must not depend on LWJGL, OpenGL, Jolt, OpenAL, Steam, or game code.
- `engine-ui` exposes renderer-neutral state/draw data; it must not expose OpenGL or imgui-java types.
- `engine-network-api` contains no socket/Steam implementation details.
- `game-server` must remain runnable without window, renderer, or audio modules.
- `engine-editor` may consume runtime modules, but runtime modules must not depend on the editor.
- Asset authoring/import dependencies belong in offline tooling; shipped gameplay consumes cooked formats.
- Cross-module Java imports target only the destination module's declared API package root; `.internal` packages are never public contracts.
- Production engine/game modules must not depend on `feasibility-spikes`; its external dependencies exist only to reproduce Phase 0 evidence.

## Runtime composition target

The client will compose platform/input, OpenGL rendering, runtime UI, audio, world/physics, game rules, and either IP or Steam networking. The server will compose world/physics, game rules, and networking without graphics/audio. Server authority owns gameplay state and dynamic physics; clients predict/present but do not submit authoritative transforms.

P1-T09 establishes only the runnable composition roots and their Gradle tasks. The current entry points intentionally initialize no production subsystems because those implementations begin in later phases. `game-server` additionally verifies that its runtime classpath contains no platform, renderer, audio, GLFW, OpenGL, or OpenAL dependencies.

## Experimental code boundary

Phase 0 code now lives under `feasibility-spikes/src/main/java/com/samo/spike/` and proves isolated capabilities:

- GLFW/OpenGL initialization;
- Jolt lifecycle;
- OpenAL lifecycle;
- localhost UDP and deterministic impairment;
- Steam initialization and FFM flat-API access;
- combined native smoke/soak executables.

The source was relocated without changing its experimental classification or promoting its behavior into reusable engine layers. Spike-only LWJGL/Jolt/Snaploader/OSHI/Steamworks dependencies are owned by `feasibility-spikes`, while root tasks with the historical names delegate to the matching subproject tasks so existing verification commands remain reproducible.

P0-T09A / Issue #42, P0-T13 / Issue #43, and P0-T14 / Issue #44 remain independent follow-up gates. Moving their supporting code does not satisfy those gates or strengthen prior feasibility claims.

## Architecture verification status

| Property | Status |
| --- | --- |
| 16 production-target modules declared | Implemented |
| Experimental `feasibility-spikes` subproject isolated | Implemented by P1-T10A / Issue #56 |
| Root contains no Java source/runtime spike dependencies | Implemented by P1-T10A / Issue #56 |
| Shared Java 25/test conventions | Implemented |
| Dependency locking/version catalog | Implemented |
| Automated package/module boundary test | Implemented by P1-T07; relocated/extended by P1-T10A |
| Client/server executable composition roots | Implemented by P1-T09 / Issue #39 |
| Production engine subsystems | Planned: Phase 2 onward |
