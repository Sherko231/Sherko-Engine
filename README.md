# Sherko Engine

Java-first 3D engine scoped for small/medium first-/third-person, physics-heavy, humorous multiplayer co-op games.

## Start here

- AI coding agents: read [`AGENTS.md`](AGENTS.md) first and follow its required order.
- Humans: use this page for orientation, then open the scope, status, and active Issue.
- Engine users: use the in-repository [`wiki/`](wiki/README.md) for public API/library usage and practical examples.
- Owner-facing manual demo: see [`game-sandbox/README.md`](game-sandbox/README.md) and run `\.\gradlew.bat :game-sandbox:runEngineDemo` on Windows x64.

## Project documents

- [AI agent contract](AGENTS.md) — mandatory read order, truth hierarchy, work rules, sandbox-maintenance rule, and handoff checklist.
- [Engine scope](ENGINE_SCOPE.md) — product and architecture boundaries.
- [Development status](docs/DEVELOPMENT_STATUS.md) — exact checkpoint represented by the current repository commit.
- [Roadmap](ROADMAP.md) — milestone-level plan and current focus.
- [Architecture](docs/ARCHITECTURE.md) — module responsibilities, dependencies, and implementation maturity.
- [Decision log](docs/DECISIONS.md) — durable accepted/provisional/superseded engineering decisions.
- [Build and verification](docs/BUILD_AND_VERIFY.md) — canonical verification commands and evidence expectations.
- [Technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md) — detailed implementation task catalog with stable task IDs; checkboxes are not status.
- [Engineering references](docs/REFERENCES.md) — source map behind the roadmap.
- [Engine API wiki](wiki/README.md) — human/AI consumer guide for implemented public APIs, examples, lifecycle/ownership rules, and current limitations.
- [Engine sandbox](game-sandbox/README.md) — owner-facing manual observation path for currently demonstrable engine behavior.

## Current state

The Java 25 multi-project foundation declares all 16 production-target modules plus the experimental `feasibility-spikes` subproject. It centralizes the toolchain, dependency locking, JUnit 6/AssertJ test support, quality gates, and repeatable client/server entry points. `engine-core` provides the lifecycle, dependency/startup rollback, deterministic timing, configuration, native-resource diagnostics, structured logging, and fatal-termination foundation completed in Phase 2. `engine-platform-lwjgl` contains the production `GlfwWindow` platform boundary from P3-T01 through P3-T04: OpenGL 4.6 window ownership, separate logical/framebuffer sizing, in-place window-mode transitions, and focus-loss-safe cursor capture/input cleanup.

Phase 0 proved GLFW/OpenGL, Jolt JNI, OpenAL, localhost UDP, Steam initialization, deterministic network impairment, Java FFM access to the Steam flat API, and a 15-second combined native smoke run. End-to-end SteamNetworkingSockets and sustained/repeated native lifecycle evidence remain explicit follow-up gates.

Phase 1 and Phase 2 are complete. P3-T01 / Issue #84 through P3-T04 / Issue #87 are formally complete. P3-T04 merged through PR #148 as `eb82814b9f545dcf04be004912f69694942d604b`; merged-master workflow #257 passed all five jobs on that exact commit, including the real Windows x64 focus-loss acceptance and retained P3-T04 evidence. These results do not satisfy the separate P0-T09A/P0-T13/P0-T14 feasibility gates.

Phase 3 remains in progress. P3-T04A / Issue #149 is the active bounded task on branch `p3-t04a-engine-sandbox-demo` / PR #150. It turns the existing `game-sandbox` skeleton into the canonical owner-facing manual demo while keeping automated tests/CI authoritative and preserving the headless server boundary through a non-exported demo-only platform runtime. P3-T05 / Issue #88 is paused until #149 is merged, exact merged-`master` CI passes, and the task is freshly audited against the new master.

The sandbox is expected to evolve with future human-observable engine capabilities when they can be demonstrated through already-authorized public production APIs. If a task cannot update the sandbox without exposing internals or pulling future roadmap work forward, its PR/handoff must record `Sandbox impact: none — <reason>`.

All agent-authored changes use one active Issue, a dedicated branch, a pull request, and required verification before merge. Non-exempt pull requests reference their Issue without auto-closing it; the Issue is closed only after exact merged-`master` CI passes. Qualifying Markdown-only changes use the documented CI exemption.

When a task adds or changes public engine API or consumer-visible usage behavior, the same pull request must update the relevant [`wiki/`](wiki/README.md) pages. Tasks with no wiki impact should record that explicitly rather than making meaningless documentation churn.