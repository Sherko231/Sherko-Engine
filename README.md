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

Phase 5R — Architecture & Refactor Hardening is complete. P5R-T01 through P5R-T26 are accepted, and the mandatory exit review recorded PASS. Final candidate `40f2bb91ae3afeeee07d7f8be92dd7b088419fa0` passed all five required jobs in run #495; PR #357 merged as `5e0cac4e7748a66b7c2d19e0cf444eaca0a49fce`, and exact-merge Lightweight verification passed in run #496. The current M2 focus has completed **Phase 6 — asset pipeline and resource lifetime**. P6-T01 through P6-T14 are accepted, and P6-EXIT / Issue #405 records PASS for the cooked-runtime exit gate. Final exit candidate `17b57f92963de4e336308c4a1a4315423a1003e9` passed all five heavy jobs in run #606 / `35923546789`; its GitHub PR merge-ref commit `97ae0cad824ae3da8607d35f9fae9eebc2726dea` has the exact same parents and repository tree `c5c5ddc055615896a44127f177ad57598c6927ba` as merged `master` `a8b3211729e75bd0192df1c6d04dc98d79eadd54`. Exact merged master then passed Lightweight verification in run #607 / `35924287931`. The persistent `game-sandbox` also includes the accepted Phase 6 Asset Lab from Issue #408 / PR #409, demonstrating public cooked MESH/MATERIAL loading, handle lifecycle, fallback diagnostics, and MATERIAL hot reload without exposing internals or pulling later Phase 7 work forward. Phase 7 is active. P7-T01 / Issue #414 is accepted through PR #415: final candidate `182ff9ca4ed8ef915fac94e05427d6328f41c78f` passed the required five-job matrix in run #641 / `36872723180`, merged as `e25458acaaa68bcd5f9dc5ab201cfd55f4bfb359`, and exact merged master passed Lightweight verification in run #642 / `36878894604`. P7-T02 / Issue #416 is accepted through PR #417: final candidate `7968e6ececf4a2b5cdd614c4e6848cf703ddb6f1` passed the required five-job matrix in run #645 / `36986058671`, merged as `1b10c7683a231afec49e3fecd085314834ab2a96`, and exact merged master passed Lightweight verification in run #646 / `36986734070`. P7-T03 / Issue #418 is accepted through PR #419: final candidate `c02c4715a77eced7feeb8055d0c7dd01ae9d31a4` passed the required five-job matrix in run #647 / `36993124976`, merged as `e97dceb477d9492d28aa2a924e5c5adc93628ba8`, and exact merged master passed Lightweight verification in run #648 / `36994718637`. P7-T04 / Issue #420 is accepted through PR #421: final candidate `7bb6f7f69f8979b4f803dda9ea33d7490f55c616` passed the required five-job matrix in run #649 / `36997114403`, merged as `2098866d1ae01d6d80e66777e1830cb08b6c0bdd`, and exact merged master passed Lightweight verification in run #650 / `36997764861`. P7-T05 / Issue #422 is accepted through PR #423: final candidate `4ad2a804c18920212dea6b76f033ef2dd876411a` passed the required five-job matrix in run #651 / `37004765576`, merged as `8275f5e86647b2629d3cb1b4d3ae973a647043ca`, and exact merged master passed Lightweight verification in run #652 / `37005330891`. P7-T06 / Issue #424 is accepted through PR #425: final candidate `9c73700f5d5a2cfe87b1ba5606c8c900851202e7` passed the required five-job matrix in run #653 / `37009404880`, merged as `82827a7367fca11ec8e3beba6e92fac8ae5457ca`, and exact merged master passed Lightweight verification in run #654 / `37010058098`. P7-T07 / Issue #426 is accepted through PR #427: final candidate `d88d3b48a2d1dbb8ad335fa26901b5456d14a37d` passed the required five-job matrix in run #656 / `37017131128`, merged as `c0c6ecafdfc4047b6c156b91b835fed261cee53b`, and exact merged master passed Lightweight verification in run #657 / `37017937181`. P7-T08 / Issue #429 is accepted through PR #430: final candidate `06de60e1a61f15354f80ca559b1b3b1eb22857a1` passed the required five-job matrix in run #658 / `37658335858`, merged as `55dc0d426fb11049b104450f5bcc419003c23c50`, and exact merged master passed Lightweight verification in run #659 / `37659069575`. P7-T09 / Issue #432 is accepted through PR #433: final candidate `e007711e4ff8bd74e1a155c0402faf5f810695b3` passed all five required jobs in run #660 / `37975398592`, merged as `ebea9dd7a69e14b5b6a71c99229916c7b97516b8`, and exact merged-master Lightweight verification passed in run #661 / `37976046324`. P7-T10 is the next backlog task and is not yet materialized as an executable Issue. P7-T10's prefab circular-reference validation is merged; P7-T11 / Issue #439 now stages atomic scene-world activation internally.

`game-sandbox` is a persistent cumulative playground rather than a disposable feature demo. Capabilities that are meaningfully usable through already-authorized public production APIs should be integrated into that same sandbox experience as they land; features that cannot yet be exposed honestly remain out until their required public boundary exists.

Phase 0 follow-up gates remain separate: P0-T09A / #42 for end-to-end SteamNetworkingSockets, P0-T13 / #43 for sustained native stability, and P0-T14 / #44 for repeated native lifecycle evidence. They do not block current Phase 7 world/scene work, but they still limit the claims they were created to prove.

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
