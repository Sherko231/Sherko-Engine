# Sherko Engine

Java-first 3D engine scoped for small/medium first-/third-person, physics-heavy, humorous multiplayer co-op games.

## Start here

- AI coding agents: read [`AGENTS.md`](AGENTS.md) first and follow its required order.
- Humans: use this page for orientation, then open the scope, status, and active Issue.

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

## Current state

The Java 25 multi-project foundation declares all 16 production-target modules plus the experimental `feasibility-spikes` subproject. It centralizes the toolchain, dependency locking, JUnit 6/AssertJ test support, quality gates, and repeatable client/server entry points. `engine-core` provides the single-subsystem lifecycle contract, dependency ordering, bounded rollback for partially failed ordered startup, the monotonic `EngineClock` elapsed-time sampler, exact 60 Hz fixed-step accumulation, bounded frame-gap/catch-up policy, renderer-facing interpolation alpha, typed startup configuration validation, deterministic startup configuration layering, explicit native-resource ownership diagnostics, and the synchronous structured `EngineLogger` boundary. P2-T11 adds test-only JFR allocation-observability evidence. Concrete production subsystems remain architectural skeletons.

Phase 0 proved GLFW/OpenGL, Jolt JNI, OpenAL, localhost UDP, Steam initialization, deterministic network impairment, Java FFM access to the Steam flat API, and a 15-second combined native smoke run. End-to-end SteamNetworkingSockets and sustained/repeated native lifecycle evidence remain explicit follow-up gates.

Phase 1 is complete; M1 continues through Phases 2–4. P2-T01 through P2-T11 are complete and merged. P2-T12 / Issue #82 is the active bounded implementation task. P2-T13 and later P2/P3/P4 Issues remain planning-only until activated one at a time. P2's ten-minute integrated phase exit remains unproven. See [`docs/DEVELOPMENT_STATUS.md`](docs/DEVELOPMENT_STATUS.md) for the commit-contained handoff, then verify current GitHub Issues and Pull Requests before starting.

All agent-authored changes use one active Issue, a dedicated branch, a pull request, and required verification before merge. Qualifying Markdown-only changes use the documented CI exemption.
