# Sherko Engine

Java-first 3D engine scoped for small/medium first-/third-person, physics-heavy, humorous multiplayer co-op games.

## Start here

- AI coding agents: read [`AGENTS.md`](AGENTS.md) first and follow its required order.
- Humans: use this page for orientation, then open the scope, status, and live GitHub Issue/PR state.
- Engine users: use the in-repository [`wiki/`](wiki/README.md) for public API/library usage and practical examples.
- Owner-facing persistent playground: see [`game-sandbox/README.md`](game-sandbox/README.md) and run `.\gradlew.bat :game-sandbox:runSandbox` on Windows x64.

## Project documents

- [AI agent contract](AGENTS.md) — mandatory read order, truth hierarchy, work/CI rules, persistent-sandbox maintenance rule, and handoff checklist.
- [Engine scope](ENGINE_SCOPE.md) — product and architecture boundaries.
- [Development status](docs/DEVELOPMENT_STATUS.md) — commit-contained handoff checkpoint; inspect live GitHub state before continuing.
- [Roadmap](ROADMAP.md) — milestone-level plan and current phase outcome.
- [Architecture](docs/ARCHITECTURE.md) — module responsibilities, dependencies, and implementation maturity.
- [Spatial conventions](docs/SPATIAL_CONVENTIONS.md) — canonical world handedness, axes, units, camera view/projection convention, screen-to-world mapping, and boundary-conversion rule.
- [Decision log](docs/DECISIONS.md) — durable accepted/provisional/superseded engineering decisions.
- [Build and verification](docs/BUILD_AND_VERIFY.md) — canonical verification commands and evidence expectations.
- [GitHub execution model](docs/GITHUB_PROJECT_SETUP.md) — Issue/branch/PR lifecycle and CI-efficiency policy.
- [Technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md) — detailed implementation task catalog with stable task IDs; checkboxes are not status.
- [Engine API wiki](wiki/README.md) — consumer guide for implemented public APIs and current limitations.
- [Engine sandbox](game-sandbox/README.md) — cumulative owner-facing interactive playground for implemented public behavior.

## Current state

The Java 25 multi-project foundation declares all 16 production-target modules plus experimental `feasibility-spikes`. M1 — Engine Foundation is complete: Phases 1 through 4 established the build/module boundaries, lifecycle/timing/configuration/resource contracts, production platform/input stack, deterministic tick input/replay, canonical spatial conventions, transforms, geometry primitives, camera/screen math, and bounded transform quantization.

Phase 4 completed through P4-T09 / Issue #102 / PR #179. P4-T08 was intentionally executed before P4-T07 so screen-to-world ray construction consumed an accepted view/projection convention. The Phase 4 exit gate is satisfied: the spatial test surface lives in pure-Java/JOML `engine-core`, with no OpenGL or Jolt dependency, and passed on the exact repository tree merged by PR #179.

Phase 5 — Rendering foundation is complete. P5-T01 through P5-T18 are accepted, and exit review #250 passed the integrated textured-room/camera/depth/lighting/sRGB/debug-geometry gate. The follow-up single-view cleanup and standalone renderer visual demo are also merged on current `master`.

Phase 5R — Architecture & Refactor Hardening is complete. P5R-T01 through P5R-T26 are accepted, and the mandatory exit review recorded PASS. Final candidate `40f2bb91ae3afeeee07d7f8be92dd7b088419fa0` passed all five required jobs in run #495; PR #357 merged as `5e0cac4e7748a66b7c2d19e0cf444eaca0a49fce`, and exact-merge Lightweight verification passed in run #496. The current M2 focus is **Phase 6 — asset pipeline and resource lifetime**. P6-T01 through P6-T13 are accepted. P6-T13 / Issue #399 established strict manifest-backed asynchronous cooked-mesh loading into public `MeshAsset` values plus render-thread-only internal GPU buffer upload, without adding arbitrary mesh drawing or cache/reference-counting policy. P6-T14 / Issue #402 is active and must be freshly materialized/refined before implementation; hot reload is the remaining Phase 6 implementation task.

`game-sandbox` is a persistent cumulative playground rather than a disposable feature demo. Capabilities that are meaningfully usable through already-authorized public production APIs should be integrated into that same sandbox experience as they land; features that cannot yet be exposed honestly remain out until their required public boundary exists.

Phase 0 follow-up gates remain separate: P0-T09A / #42 for end-to-end SteamNetworkingSockets, P0-T13 / #43 for sustained native stability, and P0-T14 / #44 for repeated native lifecycle evidence. They do not block Phase 5R refactor hardening or later Phase 6 asset-pipeline planning, but they still limit the claims they were created to prove.

## Default contribution / CI lifecycle

For ordinary non-Markdown agent-authored work:

```text
implement + focused verification + docs + self-review on task branch
                                ↓
                 open one final non-draft PR
                                ↓
             heavy five-job CI on exact candidate
                                ↓
                            merge
                                ↓
       lightweight exact-merge verification on master
                                ↓
                         close Issue
```

Do not open a PR as a development scratchpad. Corrections after failed CI create a new candidate and require a fresh heavy run. If `master` advances relative to the tested candidate, refresh and reverify before merge.

Qualifying Markdown-only changes retain the documented complete-diff CI exemption. Tasks that explicitly require stronger exact-merge native/performance/integration evidence may use `workflow_dispatch`; ordinary tasks do not repeat the complete heavy matrix on `master`.

When a task changes public engine API or consumer-visible usage, update the relevant [`wiki/`](wiki/README.md) pages in the same pull request.

### Java formatting

Java source is formatted mechanically with Spotless + the pinned Eclipse JDT profile in `config/formatter/sherko-eclipse-java.xml`. Use `.\gradlew.bat spotlessApply` to format and `.\gradlew.bat spotlessCheck` to verify. The normal root `check` task enforces formatting. See `docs/JAVA_STYLE.md` for the style contract.
