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

The Java 25 multi-project foundation declares all 16 production-target modules plus the experimental `feasibility-spikes` subproject. It centralizes the toolchain, dependency locking, JUnit 6/AssertJ test support, quality gates, and repeatable client/server entry points. `engine-core` provides the lifecycle, dependency/startup rollback, deterministic timing, configuration, native-resource diagnostics, structured logging, and fatal-termination foundation completed in Phase 2.

`engine-platform-lwjgl` contains the production Phase 3 platform/input boundary: OpenGL 4.6 window ownership, separate logical/framebuffer sizing, in-place window-mode transitions, focus-loss-safe cursor capture/input cleanup, raw/fallback relative mouse acquisition, immutable renderer-frame `InputSnapshot`, strict immutable data-driven `InputActionBindings`, and the active P3-T08 renderer-frame action evaluator/transition layer.

Phase 0 proved GLFW/OpenGL, Jolt JNI, OpenAL, localhost UDP, Steam initialization, deterministic network impairment, Java FFM access to the Steam flat API, and a 15-second combined native smoke run. End-to-end SteamNetworkingSockets and sustained/repeated native lifecycle evidence remain explicit follow-up gates.

Phase 1 and Phase 2 are complete. P3-T01 / Issue #84 through P3-T07 / Issue #90 are formally complete. P3-T04A / #149 established `game-sandbox` as the canonical owner-facing manual demo while preserving the headless server boundary, and P3-T04B / #151 completed the sandbox logging cleanup.

P1-T08A / Issue #153 is complete through PR #154 / merged `master` `134bd3cc18258325f835f2d704319bc23a6bca47`. The normal CI lifecycle keeps development work on the task branch without heavy Actions runs, runs the complete five-job matrix on the exact final non-draft PR candidate, and runs one lightweight verifier on the exact merged `master` commit instead of routinely repeating the heavy matrix. `workflow_dispatch` remains available when a task explicitly needs stronger exact-merge evidence.

P3-T06 / Issue #89 completed through PR #156 / merged `master` `92ad157adb696bd5a9d21933f6d9af60be3634a7`; final heavy workflow #285 and exact-merge lightweight workflow #286 passed. The public platform boundary exposes `InputSnapshot`, `InputKey`, `InputMouseButton`, and `GlfwWindow.captureInputSnapshot(long)` without exposing GLFW/LWJGL types.

P3-T07 / Issue #90 completed through PR #157 / merged `master` `5012235cc0fcc2fc702919cf2e7bf185bf3c3595`; final heavy workflow #290 and exact-merge lightweight workflow #291 passed. The public platform API now includes exactly eleven typed gameplay actions, immutable binding descriptors, complete immutable `InputActionBindings`, and strict JSON schema-v1 loading through implementation-only Jackson 2.21.2.

P3-T08 / Issue #91 is the active Phase 3 task on branch `p3-t08-action-transitions`. It adds caller-owned stateful `InputActionEvaluator` plus immutable per-frame `InputActionSnapshot` / `InputActionState`, deterministic additive analog aggregation, action-level pressed/held/released semantics, preserved same-binding one-frame taps, strict increasing frame identity, and atomic failure behavior. It intentionally does not implement P3-T09 tick-aligned `PlayerInputCommand`/replay/network codecs or P3-T10 controller/settings/response curves.

The owner-facing sandbox now evaluates the committed demo binding set through public production APIs and shows MOVE X/Y plus representative JUMP/INTERACT transitions alongside existing raw hardware diagnostics. Its platform dependency remains non-exported so `game-server` stays headless.

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
