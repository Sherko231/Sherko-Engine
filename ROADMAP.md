# Sherko Engine Roadmap

> This file is the **product/engineering roadmap**, not the detailed implementation checklist.
> The canonical detailed task catalog lives in [`docs/roadmap/TECHNICAL_BACKLOG.md`](docs/roadmap/TECHNICAL_BACKLOG.md).

Sherko Engine is a Java-first engine intentionally scoped for small/medium **3D first-/third-person, physics-heavy, humorous multiplayer co-op games**. The roadmap is outcome-based: the technical backlog may stay detailed across all phases, while only work near execution is materialized as GitHub Issues.

## Roadmap model

| Level | Purpose | Source of truth |
| --- | --- | --- |
| Product scope | What the engine is and is not | [`ENGINE_SCOPE.md`](ENGINE_SCOPE.md) |
| Milestones | Major outcomes and ordering | this file |
| Technical backlog | Stable task IDs, detailed planning criteria, exit gates | [`docs/roadmap/TECHNICAL_BACKLOG.md`](docs/roadmap/TECHNICAL_BACKLOG.md) |
| Active work and live status | Work that can be picked up now and its current state | GitHub Issues / Project |
| Code change | Implementation + tests/evidence | Dedicated task branch and pull request linked to one active Issue |

### Planning rule

- **NOW:** expand the current phase into executable Issues.
- **NEXT:** keep detailed tasks in the technical backlog; create Issues only when the phase is close to execution.
- **LATER:** detailed tasks may remain in the technical backlog, but they are planning baselines rather than frozen implementation contracts. Do not pre-create hundreds of Issues.
- Task IDs such as `P10-T06` are permanent identifiers. Issue numbers are not.
- Task wording and acceptance criteria may be refined before a task becomes an executable Issue. Once an Issue is created for execution, that Issue is the implementation contract unless it is deliberately updated.
- Live workflow status belongs in GitHub Issues/Project. The commit-contained completed/next-work checkpoint belongs in `docs/DEVELOPMENT_STATUS.md`; roadmap and backlog documents do not track board state.
- A phase is complete only when its **exit gate** passes; completing every individual task is necessary but not sufficient.
- Any task that requires an undeclared architectural change stops and produces a decision/update before implementation continues.

## Milestones

| Milestone | Name | Horizon | Phases | Outcome |
| --- | --- | --- | --- | --- |
| M0 | Feasibility | FOLLOW-UP GATES | P0 | Native-stack feasibility is proven; end-to-end Steam transport and sustained-lifecycle evidence remain explicit blockers for the phases that consume them. |
| M1 | Engine foundation | NOW | P1-P4 | Establish the build, module boundaries (including runtime UI), lifecycle, timing, input, math, and spatial contracts. |
| M2 | Local playable runtime | NEXT | P5-P9 | Render and load a room, build world/physics/basic-audio/runtime-UI stacks, then prove a five-minute local co-op-style vertical slice. |
| M3 | Multiplayer core | LATER | P10-P13 | Build transport, replication, prediction/correction, join-in-progress, and Steam session flow. |
| M4 | Genre and presentation systems | LATER | P14 | Add network-aware audio, animation, IK, third-person presentation, visual feedback, navigation, perception, and one server-authoritative enemy. |
| M5 | Tools | LATER | P15 | Add the minimum editor and diagnostics needed to author and debug the game without source edits. |
| M6 | Production base | LATER | P16 | Profile, fuzz, soak-test, package, and freeze a v1 engine API only after the network slice survives release gates. |

## Feasibility baseline — M0 / Phase 0

The original Phase 0 task contracts passed. The review found that two claims must remain narrower than “production proven”: the 15-second integrated run is a smoke test, and the FFM spike proves API access rather than a complete Steam transport.

Durable Phase 0 conclusions include:

1. Java 25 and the selected native stack are viable on the target Windows x64 environment.
2. Steamworks4j is sufficient for basic Steam client integration but not for the required modern `ISteamNetworkingSockets` surface.
3. Java 25 FFM can call the official Steam flat API directly and obtain/use an `ISteamNetworkingSockets` pointer without authored C/C++ glue; an end-to-end connection/message spike is still required.
4. The localhost impairment harness can reproduce latency, jitter, loss, duplication, and reordering.
5. GLFW/OpenGL, Jolt JNI, OpenAL, and UDP can initialize, run together for 15 seconds under JFR, and shut down cleanly in the integrated smoke test.

The conditional dedicated-server fallback P0-T10 is not selected. `P0-T09A` must prove the complete Steam connection/callback/send/receive/release/close path before P10/P13 may treat Steam as a production transport. `P0-T13` and `P0-T14` provide sustained and repeated-lifecycle evidence.

## Completed foundation — M1 / Phase 1

**Outcome:** later engine changes are isolated, testable, repeatable, and reversible through a disciplined multi-project build and quality gates.

Phase 1 is complete. The client and headless server build and run through repeatable commands; module/package boundaries, quality gates, dependency locking, CI, and reproducible version reporting are in place. The final additive follow-up P1-T10A moved disposable Phase 0 spikes into the experimental `feasibility-spikes` module while keeping the 16-module production target unchanged.

The exact containing-commit checkpoint and verification evidence are recorded in [`docs/DEVELOPMENT_STATUS.md`](docs/DEVELOPMENT_STATUS.md).

## Current focus — M1 / Phase 2

**Goal:** establish the lifecycle, timing, configuration, cleanup, allocation-observability, logging, and fatal-shutdown contracts that every production subsystem will follow.

Materialize only the next bounded task as an executable Issue. The lifecycle foundation is `P2-T01`, defining `EngineSubsystem` with explicit `initialize`, `start`, `stop`, and `close` phases and unit-tested lifecycle order. Consult [`docs/DEVELOPMENT_STATUS.md`](docs/DEVELOPMENT_STATUS.md) and live GitHub state for completed work and the exact next action. Do not treat the remaining Phase 2 catalog as permission to implement it in bulk.

The exact task definitions and planning acceptance criteria are in the [technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md#phase-2---core-lifecycle-time-configuration-and-native-ownership).

## Milestone exit outcomes

### M0 — Feasibility
The base stack passed its smoke gates. P0-T09A blocks production Steam transport claims; P0-T13/P0-T14 block long-duration native-stability claims.

### M1 — Engine foundation
Client and headless server run from repeatable commands; the runtime UI module boundary, fixed-tick simulation, input replay, lifecycle/resource ownership, and spatial conventions are independently tested.

### M2 — Local playable runtime
A five-minute local level proves rendering, cooked assets, component-driven scenes, player movement, physics interactions, basic audio, runtime HUD/menu flows, one cooperative objective, failure, and restart.

### M3 — Multiplayer core
Four players can connect, join in progress, move responsively, interact with shared physics props, and complete the vertical slice under realistic network impairment and through Steam.

### M4 — Genre systems
Remote animation/audio and one server-authoritative AI actor operate correctly in the multiplayer test map.

### M5 — Tools
A developer can author the test level and diagnose render/physics/network failures without hand-editing scene JSON or adding temporary source code.

### M6 — Production base
The engine passes performance, decoder fuzzing, 4-client soak, GPU compatibility, packaging, and runtime-distribution gates.

## Scope-change policy

A request belongs in v1 only if it is required to pass an existing milestone exit outcome. Otherwise it goes to the deferred list in [`ENGINE_SCOPE.md`](ENGINE_SCOPE.md#deferred-until-a-real-game-proves-the-need).

Examples intentionally deferred: Vulkan/multiple render backends, open-world streaming, a general scripting language, render graph, Forward+, broad job-system/ECS parallelism, host migration, cross-play, mod SDK, ray tracing, and a custom JVM allocator.

## GitHub execution policy

For work near execution:

1. Create one Issue per independently testable roadmap task.
2. Copy the permanent task ID into the Issue title, e.g. `[P1-T01] Initialize multi-project Gradle build`.
3. Use the repository's engine-task template as a starting point, but include only the sections needed to make the task unambiguous.
4. Link real dependencies/blockers explicitly.
5. Keep only a bounded active queue; do not convert the entire technical backlog into Issues.
6. Create a dedicated task branch from current `master`; never implement agent-generated work directly on `master`.
7. Open a pull request linked to the Issue and require CI/verification before merge.
8. Close the Issue only after merge and acceptance evidence pass.
9. Keep `docs/DEVELOPMENT_STATUS.md` synchronized with the merged commit checkpoint (completed tasks, next action, blockers, maturity, and evidence) without copying transient board columns.

## Definition of Ready

Every executable Issue should have:

- one roadmap task ID;
- a bounded goal;
- acceptance criteria;
- dependencies/blockers, or an explicit `None`;
- a verification method or command.

Add non-goals, required tests, allowed files/modules, interface restrictions, and an architecture stop condition **when they materially reduce ambiguity or architectural risk**. Small documentation, configuration, or isolated spike tasks do not need ceremonial fields that add no useful constraint.

## Definition of Done

A task is done only when:

- acceptance criteria pass;
- appropriate tests or verification evidence are recorded;
- no undeclared scope was implemented;
- native/resource ownership remains leak-free where applicable;
- docs/contracts changed by the task are updated;
- the linked pull request is merged after CI/verification;
- the linked Issue is closed consistently with the verified result.

## Status convention

This file uses planning horizons such as **NOW**, **NEXT**, **LATER**, and **COMPLETE** at milestone level. `docs/DEVELOPMENT_STATUS.md` records the checkpoint contained by the current commit; GitHub Issues/Project records newer live workflow state. Dates are deliberately omitted until enough Phase 1 throughput exists to estimate them credibly.
