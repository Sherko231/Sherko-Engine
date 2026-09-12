# Sherko Engine

Java-first 3D engine scoped for small/medium first-/third-person, physics-heavy, humorous multiplayer co-op games.

## Start here

- AI coding agents: read [`AGENTS.md`](AGENTS.md) first and follow its required order.
- Humans: use this page for orientation, then open the scope, status, and active Issue.
- Engine users: use the in-repository [`wiki/`](wiki/README.md) for public API/library usage and practical examples.
- Owner-facing manual demo: see [`game-sandbox/README.md`](game-sandbox/README.md) and run `.\gradlew.bat :game-sandbox:runEngineDemo` on Windows x64.

## Project documents

- [AI agent contract](AGENTS.md) — mandatory read order, truth hierarchy, work/CI rules, sandbox-maintenance rule, and handoff checklist.
- [Engine scope](ENGINE_SCOPE.md) — product and architecture boundaries.
- [Development status](docs/DEVELOPMENT_STATUS.md) — exact checkpoint represented by the current repository commit.
- [Roadmap](ROADMAP.md) — milestone-level plan and current focus.
- [Architecture](docs/ARCHITECTURE.md) — module responsibilities, dependencies, and implementation maturity.
- [Decision log](docs/DECISIONS.md) — durable accepted/provisional/superseded engineering decisions.
- [Build and verification](docs/BUILD_AND_VERIFY.md) — canonical verification commands and evidence expectations.
- [GitHub execution model](docs/GITHUB_PROJECT_SETUP.md) — Issue/branch/PR lifecycle and CI-efficiency policy.
- [Technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md) — detailed implementation task catalog with stable task IDs; checkboxes are not status.
- [Engineering references](docs/REFERENCES.md) — source map behind the roadmap.
- [Engine API wiki](wiki/README.md) — human/AI consumer guide for implemented public APIs, examples, lifecycle/ownership rules, and current limitations.
- [Engine sandbox](game-sandbox/README.md) — owner-facing manual observation path for currently demonstrable engine behavior.

## Current state

The Java 25 multi-project foundation declares all 16 production-target modules plus the experimental `feasibility-spikes` subproject. It centralizes the toolchain, dependency locking, JUnit 6/AssertJ test support, quality gates, and repeatable client/server entry points. `engine-core` provides the lifecycle, dependency/startup rollback, deterministic timing, configuration, native-resource diagnostics, structured logging, and fatal-termination foundation completed in Phase 2. `engine-platform-lwjgl` contains the production `GlfwWindow` platform boundary from P3-T01 onward: OpenGL 4.6 window ownership, separate logical/framebuffer sizing, in-place window-mode transitions, focus-loss-safe cursor capture/input cleanup, raw/fallback relative mouse acquisition, and the active P3-T06 renderer-frame `InputSnapshot` work.

Phase 0 proved GLFW/OpenGL, Jolt JNI, OpenAL, localhost UDP, Steam initialization, deterministic network impairment, Java FFM access to the Steam flat API, and a 15-second combined native smoke run. End-to-end SteamNetworkingSockets and sustained/repeated native lifecycle evidence remain explicit follow-up gates.

Phase 1 and Phase 2 are complete. P3-T01 / Issue #84 through P3-T05 / Issue #88 are formally complete. P3-T04A / #149 established `game-sandbox` as the canonical owner-facing manual demo while preserving the headless server boundary, and P3-T04B / #151 completed the sandbox logging cleanup.

P1-T08A / Issue #153 is complete through PR #154 / merged `master` `134bd3cc18258325f835f2d704319bc23a6bca47`. The normal CI lifecycle keeps development work on the task branch without heavy Actions runs, runs the complete five-job matrix on the exact final non-draft PR candidate, and runs one lightweight verifier on the exact merged `master` commit instead of routinely repeating the heavy matrix. `workflow_dispatch` remains available when a task explicitly needs stronger exact-merge evidence.

P3-T05 / Issue #88 completed through PR #155 / merged `master` `2f3dcd3d9117db28b994115f0c420b884e5a59b0`; final heavy workflow #282 and exact-merge lightweight workflow #283 passed.

P3-T06 / Issue #89 is the active Phase 3 task. It introduces the first public immutable renderer-frame hardware view: `InputSnapshot`, `InputKey`, `InputMouseButton`, and `GlfwWindow.captureInputSnapshot(long)`. Snapshot capture exposes bounded held keyboard/mouse state, retained raw press/release edges, focus/capture state, and accumulated relative mouse delta without exposing GLFW/LWJGL types or polling GLFW itself. It remains a client/platform hardware view; the headless/replay-friendly `PlayerInputCommand` boundary belongs to P3-T09.

The sandbox now evolves with that public boundary: it captures one `InputSnapshot` after each demo-frame event poll and prints bounded diagnostics for frame identity, focus/capture state, W/A/S/D held state, and public mouse delta. It still uses only authorized public production APIs and remains separate from acceptance/performance evidence.

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

Do not open a PR as a development scratchpad. Corrections after failed CI legitimately create a new candidate and therefore a new heavy run; avoidable cosmetic/status edits after a passing candidate should be completed before the PR is opened. If `master` advances relative to the tested candidate, refresh the candidate and reverify it before merge.

Qualifying Markdown-only changes retain the documented complete-diff CI exemption. Tasks that explicitly require exact-merge native/performance/integration evidence may request a stronger post-merge run through `workflow_dispatch`; ordinary tasks do not repeat the complete five-job matrix on `master`.

When a task adds or changes public engine API or consumer-visible usage behavior, the same pull request must update the relevant [`wiki/`](wiki/README.md) pages. Tasks with no wiki impact should record that explicitly rather than making meaningless documentation churn.
