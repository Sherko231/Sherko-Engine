# Sherko Engine Roadmap

> This file is the **product/engineering roadmap**, not the detailed implementation checklist.
> The canonical detailed task catalog lives in [`docs/roadmap/TECHNICAL_BACKLOG.md`](docs/roadmap/TECHNICAL_BACKLOG.md).

Sherko Engine is a Java-first engine intentionally scoped for small/medium **3D first-/third-person, physics-heavy, humorous multiplayer co-op games**. The roadmap is outcome-based: the technical backlog may stay detailed across all phases, while only work near execution is materialized as GitHub Issues.

## Roadmap model

| Level | Purpose | Source of truth |
| --- | --- | --- |
| Product scope | What the engine is and is not | [`ENGINE_SCOPE.md`](ENGINE_SCOPE.md) |
| Durable architecture | Accepted architecture choices within scope | [`docs/DECISIONS.md`](docs/DECISIONS.md) |
| Canonical spatial contract | Engine world handedness, axes, units, rotation sign, scale, and adapter-boundary rule | [`docs/SPATIAL_CONVENTIONS.md`](docs/SPATIAL_CONVENTIONS.md), under D-041 |
| Milestones | Major outcomes and ordering | this file |
| Technical backlog | Stable task IDs, detailed planning criteria, exit gates | [`docs/roadmap/TECHNICAL_BACKLOG.md`](docs/roadmap/TECHNICAL_BACKLOG.md) |
| Active work and live status | Work that can be picked up now and its current state | GitHub Issues / Project |
| Code change | Implementation + tests/evidence | Dedicated task branch and final pull request linked to one active Issue |
| CI lifecycle | Efficient final-candidate and exact-merge verification | [`docs/CI_LIFECYCLE.md`](docs/CI_LIFECYCLE.md) + `AGENTS.md` |
| Engine consumer usage | How to use implemented public APIs | [`wiki/`](wiki/README.md), synchronized with production API changes |
| Owner observation | Persistent cumulative interaction with implemented public behavior | [`game-sandbox/`](game-sandbox/README.md), extended in-place when a capability is meaningfully usable without pulling future work forward |

### Planning rule

- **NOW:** expand the current phase into executable Issues.
- **NEXT:** keep detailed tasks in the technical backlog; create Issues only when the phase is close to execution.
- **LATER:** detailed tasks may remain in the technical backlog, but they are planning baselines rather than frozen implementation contracts. Do not pre-create hundreds of Issues.
- Task IDs such as `P10-T06` are permanent identifiers. Issue numbers are not.
- Additive task IDs such as `P3-T04A`, `P3-T04B`, and maintenance IDs such as `P1-T08A` may be inserted when newly discovered bounded work is needed without renumbering established tasks.
- Task wording and acceptance criteria may be refined before a task becomes an executable Issue. Once an Issue is created for execution, that Issue is the implementation contract unless it is deliberately updated.
- Live workflow status belongs in GitHub Issues/Project. The commit-contained completed/next-work checkpoint belongs in `docs/DEVELOPMENT_STATUS.md`; roadmap and backlog documents do not track board state.
- A phase is complete only when its **exit gate** passes; completing every individual task is necessary but not sufficient.
- Demonstrate phase exit through the actual integrated runtime/test path and retain scenario, environment, tested SHA, observed results, and evidence using `docs/BUILD_AND_VERIFY.md`. Isolated test success alone does not establish integration.
- Before materializing the next phase, review its assumptions, dependencies, current use cases, and planned acceptance against the completed phase's evidence. Record the review in the closing Issue/PR. Future task details remain planning baselines; refine them deliberately without changing scope, decisions, or exit thresholds implicitly.
- Any task that requires an undeclared architectural change stops and produces a decision/update before implementation continues.
- Any task involving spatial math, transforms, cameras, renderer/physics world-space assumptions, asset conversion, spatial audio, or network-spatial data must consume the accepted canonical contract in `docs/SPATIAL_CONVENTIONS.md` rather than relying on a library/tool default.
- When a task changes how engine consumers use a public API, update the relevant `wiki/` pages in the same pull request; if there is no consumer/wiki impact, record that explicitly.
- `game-sandbox` is a persistent cumulative playground, not a disposable feature demo. When a task adds or materially changes a human-observable capability that is usable through already-authorized public production APIs, integrate it into the existing sandbox experience while preserving already-usable capabilities. Prefer owner-controlled interaction over timed feature tours. If the capability cannot yet be represented honestly without internals or future work, record `Sandbox impact: none — <reason>`.

## Milestones

| Milestone | Name | Horizon | Phases | Outcome |
| --- | --- | --- | --- | --- |
| M0 | Feasibility | FOLLOW-UP GATES | P0 | Native-stack feasibility is proven; end-to-end Steam transport and sustained-lifecycle evidence remain explicit blockers for the phases that consume them. |
| M1 | Engine foundation | COMPLETE | P1-P4 | Build/module boundaries, lifecycle, timing, input, math, and spatial contracts are implemented and verified. |
| M2 | Local playable runtime | NOW | P5, P5R, P6-P9 | Render a room, harden the completed foundation through a mandatory architecture/refactor gate, then load assets and build world/physics/basic-audio/runtime-UI stacks before proving a five-minute local co-op-style vertical slice. |
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

The conditional dedicated-server fallback P0-T10 is not selected. `P0-T09A` must prove the complete Steam connection/callback/send/receive/release/close path before P10/P13 may treat Steam as production transport. `P0-T13` and `P0-T14` provide sustained and repeated-lifecycle evidence.

## Completed foundation — M1 / Phases 1–4

**Outcome:** the build/module foundation plus shared lifecycle, timing, configuration, ownership, allocation-observability, logging, fatal-shutdown, platform/input, math, and spatial contracts are implemented and verified.

Phase 1 is complete. The client and headless server build and run through repeatable commands; module/package boundaries, quality gates, dependency locking, CI, and reproducible version reporting are in place. P1-T10A moved disposable Phase 0 spikes into the experimental `feasibility-spikes` module while keeping the 16-module production target unchanged. P1-T08A / Issue #153 optimized the CI lifecycle without weakening exact-final-candidate verification.

Phase 2 is complete. P2-T01 through P2-T13 are merged, and the D-030 exit gate passed with more than 60 continuous seconds of integrated fixed 60 Hz ticks, bounded catch-up, orderly lifecycle shutdown, and verified native-resource-registry cleanup. This does not replace independent P0-T09A/P0-T13/P0-T14 evidence.

Phase 3 is complete. P3-T01 through P3-T10 established the production platform/input boundary, renderer-frame hardware/action state, deterministic response settings, tick-aligned `PlayerInputCommand`, fixed replay/storage codec, and deterministic headless replay. The persistent `game-sandbox` supersedes the earlier scripted demo presentation while preserving accepted production APIs.

Phase 4 is complete. P4-T01 through P4-T09 established the canonical D-041 world convention, JOML hot-loop policy, hierarchical transforms and cycle/dirty behavior, geometry primitives, D-045 view/perspective semantics, D-046 screen-to-world mapping, and D-047 bounded position/quaternion quantization. P4-T08 was intentionally completed before P4-T07 so ray construction consumed an accepted projection/depth convention.

P4-T09 / Issue #102 / PR #179 completed on merged `master` `66a81a418e0c953b8f00e54226265aa7cd226749`. Its final candidate passed the heavy five-job matrix in run #334 / `35246722603`, and the exact merge passed Lightweight verification in run #335 / `35247709901`.

The Phase 4 exit gate also passes. The accepted candidate and merged master share tree `c2e290e844cbd4ae0f08f40796b19967cc254b84`; root/subproject tests passed on that tree, while `engine-core` depends only on JOML and has no OpenGL/LWJGL or Jolt test dependency/import. The spatial test surface is therefore independently executable from OpenGL and Jolt. Issue #180 records the exit and Phase 5 readiness review.

## Current focus — M2 / Phase 5R architecture & refactor hardening

**Phase 5 outcome:** a stable, inspectable 3D room now renders without gameplay or physics dependencies.

P5-T01 through P5-T18 are accepted. P5-T18 / #251 closed the final integration gaps with one renderer-owned mapped-UV textured-room fixture and a sandbox-owned movable rendered camera using existing public input/camera boundaries. Final candidate `8e61764fda7cd845dc86062ab1d3000067ddf8c5` passed the required five-job CI in run #441 / `35460986757`; PR #253 merged as `96dcd56ac56a3a968b789163e440c7684afbc44d`, and exact-merge Lightweight verification passed in run #442 / `35461897536`.

The phase progresses through OpenGL diagnostics/thread ownership/resource wrappers, bounded upload/shader/uniform infrastructure, the first indexed static mesh, sRGB/material/render-submission/culling/sorting foundations, directional and bounded local lights, correct gamma/sRGB presentation, debug geometry/counters, the first-person view-model layer, and the final P5-T18 integrated textured-room/movable-camera gate closure.

Phase 5 exit review #250 passed the technical backlog contract: **a textured room with depth, camera movement, one directional light, correct sRGB/gamma, and debug geometry renders without gameplay code.** Shadows, fog, tonemapping, and other polish remain deliberately deferred.

Phase 5 is complete. Before any Phase 6 implementation begins, the repository must pass **Phase 5R — Architecture & Refactor Hardening**. Phase 5R is an inserted mandatory engineering gate, not an optional cleanup suggestion and not a renumbering of established P6-P16 work.

Phase 5R exists to make the completed P1-P5 codebase easier to read, maintain, test, and scale before the asset/world layers multiply dependencies. It emphasizes responsibility-driven class decomposition, comprehensive naming review, descriptive class/method/field/package names, explicit public/internal boundaries, removal of stale compatibility/dead code where safe, and adoption of proven patterns only when they solve an observed coupling/responsibility/testability problem. Behavior, module direction, native ownership, spatial conventions, persisted formats, protocol layouts, and accepted public semantics must remain unchanged unless a bounded P5R Issue explicitly authorizes a contract change.

P5R-T01 through P5R-T03 are accepted, establishing the Phase 5R naming/refactor standard, repository-wide symbol inventory, public/internal/package boundary audit, and the first bounded `GlfwWindow` native/backend decomposition. **P5R-T04** is the active bounded input/focus/cursor decomposition candidate; P5R-T05 must not begin until T04 is accepted. P6-T01 remains blocked. Only after the Phase 5R exit review passes may P6-T01 be materialized. Exact task definitions and acceptance criteria remain in the [technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md).

## Milestone exit outcomes

### M0 — Feasibility
The base stack passed its smoke gates. P0-T09A blocks production Steam transport work; P0-T13/P0-T14 block long-duration native-stability claims.

### M1 — Engine foundation
Complete. Client and headless server run from repeatable commands; runtime UI module boundary, fixed-tick simulation, input replay, lifecycle/resource ownership, and spatial conventions are independently tested.

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
7. Finish implementation, focused verification where available, required docs/wiki/sandbox updates, self-review, and consistency audit **before** opening the normal non-draft PR. Do not use the PR as a development scratchpad.
8. Open one final PR linked to the Issue only when the branch is intended to be the merge candidate. For non-exempt work use `Refs #...`; draft PRs are optional only for early human/reviewer visibility.
9. Require the heavy five-job CI matrix on the exact final non-exempt PR candidate. If the candidate changes or its tested base becomes stale, refresh it and require another exact-candidate pass.
10. Merge only after required final-candidate verification passes.
11. For ordinary non-exempt work, require the lightweight exact-merge `master` verifier and then close the Issue. Do not routinely repeat the complete heavy matrix after merge.
12. Use `workflow_dispatch` or a task-specific command for stronger post-merge evidence only when the active Issue explicitly requires it.
13. Keep `docs/DEVELOPMENT_STATUS.md` synchronized with the durable checkpoint (completed tasks, next action, blockers, maturity, and evidence) without copying transient board columns.
14. Keep `wiki/` synchronized whenever the public engine API or its consumer-visible use changes.
15. Keep the persistent `game-sandbox` cumulative: integrate each newly usable public capability into the existing owner-controlled playground when appropriate, preserve existing usable capabilities, and avoid temporary scripted showcases.

See [`docs/CI_LIFECYCLE.md`](docs/CI_LIFECYCLE.md) for the exact runner-efficiency model and exception rules.

## Definition of Ready

Every executable Issue should have:

- one roadmap task ID;
- a bounded goal;
- acceptance criteria;
- dependencies/blockers, or an explicit `None`;
- a verification method or command.

For public API or durable architecture work, also include a realistic usage example, relevant failure behavior, and why the design is needed now, including a simpler alternative considered. Derive test expectations from that contract and identify the plausible faults they detect. `AGENTS.md` and the Issue template define the required detail.

Add non-goals, required tests, allowed files/modules, interface restrictions, and an architecture stop condition **when they materially reduce ambiguity or architectural risk**. Small documentation, configuration, or isolated spike tasks do not need ceremonial fields that add no useful constraint.

## Definition of Done

A task is done only when:

- acceptance criteria pass;
- appropriate tests or verification evidence are recorded;
- review provenance, findings/dispositions, and unavailable independent review are explicitly recorded as required by `AGENTS.md`; applicable Issue/repository review gates are satisfied;
- no undeclared scope was implemented;
- native/resource ownership remains leak-free where applicable;
- docs/contracts changed by the task are updated;
- relevant `wiki/` usage/API pages are updated when public engine API or consumer-visible behavior changes, or `Wiki impact: none — <reason>` is recorded when no update is applicable;
- sandbox impact is handled under `AGENTS.md`: integrate usable capability into the persistent playground or record `Sandbox impact: none — <reason>`;
- the exact final PR candidate passes its required verification;
- the linked pull request is merged;
- the exact merged commit passes the applicable lightweight or explicitly stronger post-merge verification;
- the linked Issue is closed consistently with the verified result.

## Status convention

This file uses planning horizons such as **NOW**, **NEXT**, **LATER**, and **COMPLETE** at milestone level. `docs/DEVELOPMENT_STATUS.md` records the checkpoint contained by the current commit; GitHub Issues/Project records newer live workflow state. Dates are deliberately omitted until enough implementation throughput exists to estimate them credibly.