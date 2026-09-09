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
| Code change | Implementation + tests/evidence | Direct commit to the owner-selected branch unless a PR is explicitly requested |

### Planning rule

- **NOW:** expand the current phase into executable Issues.
- **NEXT:** keep detailed tasks in the technical backlog; create Issues only when the phase is close to execution.
- **LATER:** detailed tasks may remain in the technical backlog, but they are planning baselines rather than frozen implementation contracts. Do not pre-create hundreds of Issues.
- Task IDs such as `P10-T06` are permanent identifiers. Issue numbers are not.
- Task wording and acceptance criteria may be refined before a task becomes an executable Issue. Once an Issue is created for execution, that Issue is the implementation contract unless it is deliberately updated.
- Live task status belongs only in GitHub Issues/Project; roadmap and backlog documents do not track Ready/In Progress/Done state.
- A phase is complete only when its **exit gate** passes; completing every individual task is necessary but not sufficient.
- Any task that requires an undeclared architectural change stops and produces a decision/update before implementation continues.

## Milestones

| Milestone | Name | Horizon | Phases | Outcome |
| --- | --- | --- | --- | --- |
| M0 | Feasibility | COMPLETE | P0 | Prove the risky native/Steam/networking assumptions before the architecture depends on them. |
| M1 | Engine foundation | NOW | P1-P4 | Establish the build, module boundaries, lifecycle, timing, input, math, and spatial contracts. |
| M2 | Local playable runtime | NEXT | P5-P9 | Render and load a room, build the world/physics stack, then prove a five-minute local co-op-style vertical slice. |
| M3 | Multiplayer core | LATER | P10-P13 | Build transport, replication, prediction/correction, join-in-progress, and Steam session flow. |
| M4 | Genre systems | LATER | P14 | Add network-aware audio, animation, IK, navigation, perception, and one server-authoritative enemy. |
| M5 | Tools | LATER | P15 | Add the minimum editor and diagnostics needed to author and debug the game without source edits. |
| M6 | Production base | LATER | P16 | Profile, fuzz, soak-test, package, and freeze a v1 engine API only after the network slice survives release gates. |

## Completed — M0 / Phase 0

M0 passed its exit gate.

Durable Phase 0 conclusions include:

1. Java 25 and the selected native stack are viable on the target Windows x64 environment.
2. Steamworks4j is sufficient for basic Steam client integration but not for the required modern `ISteamNetworkingSockets` surface.
3. Java 25 FFM can call the official Steam flat API directly and reach `ISteamNetworkingSockets` without authored C/C++ glue.
4. The localhost impairment harness can reproduce latency, jitter, loss, duplication, and reordering.
5. GLFW/OpenGL, Jolt JNI, OpenAL, and UDP can run together cleanly under JFR in the owner-approved 15-second integrated soak.

The conditional dedicated-server fallback P0-T10 was not required because P0-T09 established a viable Java-to-Steam networking path.

## Current focus — M1 / Phase 1

**Goal:** make later engine changes isolated, testable, repeatable, and reversible through a disciplined multi-project build and quality gates.

The current executable task is `P1-T01 — Initialize multi-project Gradle build`.

### Phase 1 work packages

1. **Initial multi-project structure** — P1-T01
2. **Remaining module skeletons** — P1-T02
3. **Dependency/version reproducibility** — P1-T03
4. **Testing and code-quality gates** — P1-T04..P1-T08
5. **Client/server entry points and version reporting** — P1-T09..P1-T10

The exact task definitions and planning acceptance criteria are in the [technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md#phase-1---build-modules-and-quality-gates).

## Milestone exit outcomes

### M0 — Feasibility
**Passed.** The project has a concrete production networking path and the selected native stack runs together cleanly.

### M1 — Engine foundation
Client and headless server run from repeatable commands; fixed-tick simulation, input replay, lifecycle/resource ownership, and spatial conventions are independently tested.

### M2 — Local playable runtime
A five-minute local level proves rendering, cooked assets, component-driven scenes, player movement, physics interactions, one cooperative objective, failure, and restart.

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
6. Implement on the explicitly selected working branch, currently `master`, unless the repository owner explicitly requests another branch or a PR.
7. Close the Issue only after its acceptance criteria and verification evidence pass.
8. Keep `docs/DEVELOPMENT_STATUS.md` synchronized after meaningful repository developments.

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
- the linked Issue is closed consistently with the verified result.

## Status convention

This file uses planning horizons such as **NOW**, **NEXT**, **LATER**, and **COMPLETE** at milestone level. Exact live task state is maintained in GitHub Issues/Project. Dates are deliberately omitted until enough Phase 1 throughput exists to estimate them credibly.
