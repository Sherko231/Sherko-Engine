# Sherko Engine

Java-first 3D engine scoped for small/medium first-/third-person, physics-heavy, humorous multiplayer co-op games.

## Start here

- AI coding agents: read [`AGENTS.md`](AGENTS.md) first and follow its required order.
- Humans: use this page for orientation, then open the scope, status, and live GitHub Issue/PR state.
- Engine users: use the in-repository [`wiki/`](wiki/README.md) for public API/library usage and practical examples.
- Owner-facing manual demo: see [`game-sandbox/README.md`](game-sandbox/README.md) and run `.\gradlew.bat :game-sandbox:runEngineDemo` on Windows x64.

## Project documents

- [AI agent contract](AGENTS.md) — mandatory read order, truth hierarchy, work/CI rules, sandbox-maintenance rule, and handoff checklist.
- [Engine scope](ENGINE_SCOPE.md) — product and architecture boundaries.
- [Development status](docs/DEVELOPMENT_STATUS.md) — commit-contained handoff checkpoint; inspect live GitHub state before continuing.
- [Roadmap](ROADMAP.md) — milestone-level plan and current phase outcome.
- [Architecture](docs/ARCHITECTURE.md) — module responsibilities, dependencies, and implementation maturity.
- [Decision log](docs/DECISIONS.md) — durable accepted/provisional/superseded engineering decisions.
- [Build and verification](docs/BUILD_AND_VERIFY.md) — canonical verification commands and evidence expectations.
- [GitHub execution model](docs/GITHUB_PROJECT_SETUP.md) — Issue/branch/PR lifecycle and CI-efficiency policy.
- [Technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md) — detailed implementation task catalog with stable task IDs; checkboxes are not status.
- [Engine API wiki](wiki/README.md) — consumer guide for implemented public APIs and current limitations.
- [Engine sandbox](game-sandbox/README.md) — owner-facing manual observation path for implemented behavior.

## Current state

The Java 25 multi-project foundation declares all 16 production-target modules plus experimental `feasibility-spikes`. Phase 1 and Phase 2 are complete. `engine-core` provides lifecycle, dependency/startup rollback, deterministic fixed-step timing, typed configuration, native-resource diagnostics, structured logging, fatal termination, and the device-neutral per-tick input/replay contract.

P3-T01 through P3-T09 are formally complete. P3-T09 / Issue #92 merged through PR #159 at `b9144e9e939cf468a5228f499aac130886d16456`; heavy PR workflow #294 and exact-merge lightweight workflow #295 passed. The platform/input foundation now includes renderer-frame hardware snapshots, strict data-driven action bindings, action transitions, tick-aligned `PlayerInputCommand`, the fixed 126-byte replay/storage codec, deterministic headless replay evidence, and owner-facing tick-command diagnostics.

P3-T10 / Issue #93 is the final required Phase 3 task. Its implementation adds immutable deterministic `InputResponseSettings` in `engine-core`, applies mouse sensitivity/Y inversion at the existing `InputActionEvaluator` boundary before binding scale/aggregation, and defines controller dead-zone/response-curve shaping as a pure axis-local scalar contract without introducing controller discovery/polling/bindings. Consult live Issue #93 / PR #160 to determine whether its final-candidate, merge, and exact-merge verification lifecycle has completed; this orientation page deliberately does not duplicate that volatile workflow state.

P3-T09 command/replay semantics remain unchanged: `PlayerInputCommandSampler` consumes already-evaluated action state, and `game-server` remains independent of `engine-platform-lwjgl`.

Phase 3 completion additionally requires the P3-T10 acceptance lifecycle and the required Phase 4 planning review. Do not activate P4-T01 from this README alone; inspect the live Issue/PR evidence and the commit-contained handoff first.

Phase 0 follow-up gates remain separate: P0-T09A / #42 for end-to-end SteamNetworkingSockets, P0-T13 / #43 for sustained native stability, and P0-T14 / #44 for repeated native lifecycle evidence.

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
