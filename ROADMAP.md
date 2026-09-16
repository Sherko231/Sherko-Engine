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
| Owner observation | Human-observable demonstration of implemented public behavior | [`game-sandbox/`](game-sandbox/README.md), synchronized when a task can be demonstrated without pulling future work forward |

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
- When a task adds or materially changes a human-observable engine capability, evaluate `game-sandbox` impact under `AGENTS.md`. Update the sandbox through public production APIs when appropriate, otherwise record `Sandbox impact: none — <reason>` instead of pulling future work forward.

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

## Completed foundation — M1 / Phases 1–3

**Outcome so far:** the build/module foundation plus shared lifecycle, timing, configuration, ownership, allocation-observability, logging, fatal-shutdown, and platform/input contracts are implemented and verified.

Phase 1 is complete. The client and headless server build and run through repeatable commands; module/package boundaries, quality gates, dependency locking, CI, and reproducible version reporting are in place. The final additive follow-up P1-T10A moved disposable Phase 0 spikes into the experimental `feasibility-spikes` module while keeping the 16-module production target unchanged.

P1-T08A / Issue #153 is complete through PR #154 on merged `master` `134bd3cc18258325f835f2d704319bc23a6bca47`. It optimized the repository CI lifecycle without reducing the final-candidate gate: ordinary branch development runs no heavy matrix, the exact final non-draft PR candidate runs the existing five heavy jobs, and ordinary merged `master` commits run one lightweight exact-merge verifier. Full CI remains available deliberately through `workflow_dispatch` when a task requires stronger exact-merge evidence.

Phase 2 is complete. P2-T01 through P2-T13 are merged, and the D-030 Phase 2 exit gate passed on exact merged `master` with more than 60 continuous seconds of integrated fixed 60 Hz ticks, bounded catch-up, orderly lifecycle shutdown, and verified native-resource-registry cleanup. This completion does not replace the independent P0-T09A/P0-T13/P0-T14 feasibility gates.

Phase 3 is also complete. P3-T01 through P3-T10 plus the owner-facing sandbox maintenance established the production platform/input boundary, renderer-frame hardware/action state, deterministic input response, tick-aligned `PlayerInputCommand`, fixed 126-byte replay/storage codec, and deterministic headless input replay. P3-T10 / Issue #93 completed through PR #160 on merged `master` `e1801b11a713ce6cc73276c644aa15351ac508a1`; the final candidate passed the heavy five-job matrix and the exact merge passed the lightweight master verifier. The Phase 4 entry review was recorded before P4 activation.

The exact containing-commit checkpoint and verification evidence are recorded in [`docs/DEVELOPMENT_STATUS.md`](docs/DEVELOPMENT_STATUS.md).

## Current focus — M1 / Phase 4

**Goal:** prevent coordinate-system and transform bugs from spreading across renderer, physics, audio, assets, and networking by fixing one spatial convention and then building deterministic math/spatial primitives against it.

P4-T01 / Issue #94 is accepted. D-041 establishes the canonical world convention in [`docs/SPATIAL_CONVENTIONS.md`](docs/SPATIAL_CONVENTIONS.md): right-handed world, +X right, +Y up, -Z forward, meters for linear world quantities, radians for internal angular quantities, positive rotation by the right-hand rule, and dimensionless transform scale. External library or authoring-format differences are converted at adapter/import/export boundaries instead of redefining engine world space.

P4-T01 deliberately did not choose view/projection matrix details, OpenGL NDC/depth policy, reversed-Z, FOV/near/far rules, Euler/quaternion storage policy, external-format/Jolt/OpenAL conversion details, or network quantization. Those remain later bounded tasks. Renderer, physics, and asset-conversion tests must cite the canonical document once those production paths exist; P4-T01 did not fabricate future implementations to satisfy that forward-looking requirement early.

P4-T02 / Issue #95 is the next planned Phase 4 task. It remains planning-only until a fresh repository audit refines it into an executable contract from current `master`; implementation must read and preserve `docs/SPATIAL_CONVENTIONS.md`. P4-T03 through P4-T09 remain separately planned tasks for transforms/hierarchy, geometry primitives, screen-to-world rays, view/projection construction, and transform quantization. Their executable Issues must likewise be freshly audited against the accepted P4-T01 convention before implementation. The Phase 4 exit remains: spatial tests pass independently of OpenGL and Jolt.

The exact task definitions and planning acceptance criteria are in the [technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md#phase-4---math-and-spatial-conventions). Live workflow status belongs to GitHub Issues/Project, not this roadmap.

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
7. Finish implementation, focused verification where available, required docs/wiki/sandbox updates, self-review, and consistency audit **before** opening the normal non-draft PR. Do not use the PR as a development scratchpad.
8. Open one final PR linked to the Issue only when the branch is intended to be the merge candidate. For non-exempt work use `Refs #...`; draft PRs are optional only for early human/reviewer visibility.
9. Require the heavy five-job CI matrix on the exact final non-exempt PR candidate. If the candidate changes or its tested base becomes stale, refresh it and require another exact-candidate pass.
10. Merge only after required final-candidate verification passes.
11. For ordinary non-exempt work, require the lightweight exact-merge `master` verifier and then close the Issue. Do not routinely repeat the complete heavy matrix after merge.
12. Use `workflow_dispatch` or a task-specific command for stronger post-merge evidence only when the active Issue explicitly requires it.
13. Keep `docs/DEVELOPMENT_STATUS.md` synchronized with the durable checkpoint (completed tasks, next action, blockers, maturity, and evidence) without copying transient board columns.
14. Keep `wiki/` synchronized whenever the public engine API or its consumer-visible use changes.
15. Evaluate and update `game-sandbox` whenever a newly implemented capability is appropriately human-observable through the public engine boundary.

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
- sandbox impact is handled under `AGENTS.md`: update the owner-facing demo when appropriate or record `Sandbox impact: none — <reason>`;
- the exact final PR candidate passes its required verification;
- the linked pull request is merged;
- the exact merged commit passes the applicable lightweight or explicitly stronger post-merge verification;
- the linked Issue is closed consistently with the verified result.

## Status convention

This file uses planning horizons such as **NOW**, **NEXT**, **LATER**, and **COMPLETE** at milestone level. `docs/DEVELOPMENT_STATUS.md` records the checkpoint contained by the current commit; GitHub Issues/Project records newer live workflow state. Dates are deliberately omitted until enough Phase 1 throughput exists to estimate them credibly.
