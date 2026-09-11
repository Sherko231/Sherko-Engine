# Sherko Engine

Java-first 3D engine scoped for small/medium first-/third-person, physics-heavy, humorous multiplayer co-op games.

## Start here

- AI coding agents: read [`AGENTS.md`](AGENTS.md) first and follow its required order.
- Humans: use this page for orientation, then open the scope, status, and active Issue.
- Engine users: use the in-repository [`wiki/`](wiki/README.md) for public API/library usage and practical examples.

## Project documents

- [AI agent contract](AGENTS.md) — mandatory read order, truth hierarchy, work rules, and handoff checklist.
- [Engine scope](ENGINE_SCOPE.md) — product and architecture boundaries.
- [Development status](docs/DEVELOPMENT_STATUS.md) — exact checkpoint represented by the current repository commit.
- [Roadmap](ROADMAP.md) — milestone-level plan and current focus.
- [Architecture](docs/ARCHITECTURE.md) — module responsibilities, dependencies, and implementation maturity.
- [Decision log](docs/DECISIONS.md) — durable accepted/provisional/superseded engineering decisions.
- [Build and verification](docs/BUILD_AND_VERIFY.md) — canonical commands and evidence expectations.
- [Technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md) — detailed implementation task catalog with stable task IDs; checkboxes are not status.
- [Engineering references](docs/REFERENCES.md) — source map behind the roadmap.
- [Engine API wiki](wiki/README.md) — human/AI consumer guide for implemented public APIs, examples, lifecycle/ownership rules, and current limitations.

## Current state

The Java 25 multi-project foundation declares all 16 production-target modules plus the experimental `feasibility-spikes` subproject. It centralizes the toolchain, dependency locking, JUnit 6/AssertJ test support, quality gates, and repeatable client/server entry points. `engine-core` provides the lifecycle, dependency/startup rollback, deterministic timing, configuration, native-resource diagnostics, structured logging, and fatal-termination foundation completed in Phase 2. `engine-platform-lwjgl` contains the production `GlfwWindow` platform boundary from P3-T01 and the active P3-T02 implementation now separates logical window dimensions from framebuffer pixel dimensions through `WindowSizeListener` and bounded owner-thread `pollEvents()` delivery.

Phase 0 proved GLFW/OpenGL, Jolt JNI, OpenAL, localhost UDP, Steam initialization, deterministic network impairment, Java FFM access to the Steam flat API, and a 15-second combined native smoke run. End-to-end SteamNetworkingSockets and sustained/repeated native lifecycle evidence remain explicit follow-up gates.

Phase 1 and Phase 2 are complete. P3-T01 / Issue #84 is complete and merged through PR #141. Its exact merged `master` commit `a781c34a7959e207b29058803a2fb74a9cf0d662` passed merged-master workflow #218, including the real Windows x64 `GlfwWindowNativeTest`; retained evidence observed OpenGL 4.6 on the production path and verified orderly cleanup with an empty native-resource registry. This does not satisfy the separate P0-T09A/P0-T13/P0-T14 feasibility gates.

Phase 3 remains in progress. P3-T02 / Issue #85 is the current executable task on branch `p3-t02-window-sizing` through draft PR #146. P3-T03 / Issue #86 and later Phase 3 tasks remain planning-only until P3-T02 is merged, verified on exact `master`, and a fresh activation audit is performed.

All agent-authored changes use one active Issue, a dedicated branch, a pull request, and required verification before merge. Non-exempt pull requests reference their Issue without auto-closing it; the Issue is closed only after exact merged-`master` CI passes. Qualifying Markdown-only changes use the documented CI exemption.

When a task adds or changes public engine API or consumer-visible usage behavior, the same pull request must update the relevant [`wiki/`](wiki/README.md) pages. Tasks with no wiki impact should record that explicitly rather than making meaningless documentation churn.
