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

The repository has a working Java 25 multi-project build with 17 declared Gradle subprojects: the 16 production-target engine/game/support modules defined by `ENGINE_SCOPE.md`, plus the experimental `feasibility-spikes` subproject. `engine-core` implements the single-subsystem lifecycle contract and graph-only dependency ordering; concrete engine subsystems and `game-sandbox` remain skeletons, while `game-client` and `game-server` provide minimal executable composition roots for the foundation state. The root project is now a build, quality, and task-aggregation project with no Java source tree and no spike runtime dependencies.

| Module | Intended responsibility | Current state | Direct project dependencies |
| --- | --- | --- | --- |
| `engine-core` | Lifecycle, time, IDs, events, math/spatial contracts | Single-subsystem lifecycle (P2-T01) and graph-only dependency ordering (P2-T02); other responsibilities planned | None |
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
| `test-support` | Shared JUnit 6/AssertJ support plus repository architecture verification | Implemented build/test support | None |
| `feasibility-spikes` | Disposable Phase 0 native/network feasibility executables | Experimental | None; external native/library dependencies only |

`feasibility-spikes` is not a seventeenth production engine module. It is deliberately outside the locked 16-module runtime target in `ENGINE_SCOPE.md`, and no production client/server module depends on it.

## Package/API boundary contract

`config/architecture/module-boundaries.properties` is the machine-readable registry for all 17 declared Gradle subprojects. Every declared subproject, including the experimental feasibility module, declares:

- one owned package root;
- one public cross-module API root under that package root;
- one internal implementation root under that package root.

Code inside a module may use its own implementation packages. Cross-module source references, whether imports or fully qualified names, must target the other module's declared API root. References to another module's internal root are architectural violations even when the Gradle project dependency itself is otherwise valid.

`ModulePackageBoundaryTest` lives under `test-support/src/test` because the root has no Java source tree. The `test-support` Gradle test task supplies the complete sorted `rootProject.subprojects` inventory; the test validates the registry against that live list rather than maintaining a second hard-coded list. It requires every scanned production Java file to declare a package under its owning module root, then uses the Java 25 compiler-tree API to inspect imports and fully qualified references in both `src/main/java` and `src/test/java`. Overlapping roots are assigned to the most-specific owner before the source module is excluded.

Test-source package ownership is intentionally not enforced because the Phase 1 module smoke tests share the `com.samo.testing` package from `test-support`; their cross-module references are still scanned. This remains source-level verification: it does not inspect compiled bytecode, reflective class names in strings/resources, or generated sources outside the conventional main/test Java trees. A deliberate fixture under `config/architecture/fixtures/` represents a forbidden `game-client -> engine-platform-lwjgl.internal` import and remains opt-in negative evidence; in-suite regressions cover a fully qualified shortcut, a missing production package, and overlapping network roots.

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

## Single-subsystem lifecycle — P2-T01 / Issue #64

`com.samo.engine.core.api.EngineSubsystem` is an abstract `AutoCloseable` base class in `engine-core`. Final public `initialize`, `start`, `stop`, and `close` methods enforce ordering around protected `onInitialize`, `onStart`, `onStop`, and `onClose` hooks. No dependency or native library is added.

| Operation | Permitted entry state | Successful result |
| --- | --- | --- |
| `initialize()` | NEW | INITIALIZED; resources acquired |
| `start()` | INITIALIZED | STARTED; work active |
| `stop()` | STARTED | STOPPED; work quiescent, resources still owned |
| `close()` | NEW, INITIALIZED, STOPPED, FAILED | CLOSED; release hook attempted |
| repeated `close()` | CLOSED | No-op, including after a failed release hook |

Invalid calls fail before invoking a hook. Transient states reject reentrant lifecycle calls. The owner must externally serialize calls on its lifecycle thread; this is not a concurrent lifecycle manager.

An unchecked initialize/start/stop failure propagates unchanged and leaves FAILED, allowing only explicit close. The close hook must tolerate no setup, partial setup, or failed activation/stopping; it must quiesce remaining activity before releasing resources. A close attempt is terminal even if it throws, avoiding automatic repeated cleanup of potentially invalid native handles. CLOSED means the attempt ended, not that every resource was successfully released; cleanup failures must be reported by the owner.

Each instance has one lifetime, with no restart promise. This does not establish whether a native process-global subsystem supports reinitialization; P0-T14 remains the evidence gate. A successful running instance requires explicit stop before close. Coordinated rollback (P2-T03), the clock and runtime loop remain unimplemented. Client/server composition roots do not yet instantiate a subsystem.

## Subsystem dependency ordering — P2-T02 / Issue #72

`com.samo.engine.core.api.SubsystemGraph` snapshots a list of nested `Registration` records: an exact case-sensitive nonblank ID, an `EngineSubsystem` reference, and ordered prerequisite IDs. Lists are defensively copied. The constructor rejects duplicate IDs, repeated subsystem instances by identity, and missing dependencies after indexing the full list; forward references are valid. A registration rejects null/blank IDs and duplicate prerequisite IDs.

`initializationOrder()` returns an unmodifiable list of the original subsystem references in depth-first postorder, visiting roots in registration order and prerequisites in declaration order. Shared prerequisites appear once. An explicit heap traversal stack handles deep graphs without recursive call-stack growth. Each call uses fresh traversal state.

A cycle anywhere, including a self-cycle or later disconnected component, fails with `IllegalStateException` before any order is returned. Its message contains the closed dependent-to-dependency path, excluding any incoming noncyclic tail, for example `Subsystem dependency cycle: A -> B -> C -> A`. The caller can print/log that diagnostic; no logging framework is introduced.

The graph never calls lifecycle methods, checks subsystem state, owns resources, or performs cleanup. The caller must resolve the entire graph before invoking any hooks and retains explicit lifecycle ownership under D-018. Structural immutability does not make the referenced subsystems immutable. Synthetic composition tests exercise the real lifecycle guards but do not prove native safety, rollback, or the ten-minute P2 phase gate.

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
| Automated package/module boundary test | Implemented by P1-T07; relocated by P1-T10A; hardened by Issue #62 |
| Client/server executable composition roots | Implemented by P1-T09 / Issue #39 |
| Single-subsystem lifecycle order | Implemented by P2-T01 / Issue #64 with JUnit 6 tests |
| Subsystem dependency ordering without lifecycle side effects | Implemented by P2-T02 / Issue #72 with JUnit 6 tests |
| Concrete production engine subsystems / orchestration | Planned: Phase 2 onward |
