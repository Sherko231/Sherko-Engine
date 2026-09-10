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

The Java 25 multi-project foundation declares all 16 target modules and centralizes toolchain, dependency, lock, and shared test configuration. These modules are architectural skeletons, not implemented engine subsystems.

Phase 0 proved GLFW/OpenGL, Jolt JNI, OpenAL, localhost UDP, Steam initialization, deterministic network impairment, Java FFM access to the Steam flat API, and a 15-second combined native smoke run. End-to-end SteamNetworkingSockets and sustained/repeated native lifecycle evidence remain explicit follow-up gates.

M1 / Phase 1 is active. The next executable engine-foundation task at this checkpoint is P1-T05 / Issue #35. See [`docs/DEVELOPMENT_STATUS.md`](docs/DEVELOPMENT_STATUS.md) for the commit-contained handoff, then verify current GitHub Issues/Project state before starting.

All agent-authored changes use one active Issue, a dedicated branch, a pull request, and CI before merge.
