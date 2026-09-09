# Sherko Engine

Java-first 3D engine scoped for small/medium first-/third-person, physics-heavy, humorous multiplayer co-op games.

## Project documents

- [Development status](docs/DEVELOPMENT_STATUS.md) — living snapshot of current progress, proven conclusions, temporary spike code, and documentation-maintenance rules.
- [Engine scope](ENGINE_SCOPE.md) — product and architecture boundaries.
- [Roadmap](ROADMAP.md) — milestone-level plan and current focus.
- [Technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md) — detailed implementation task catalog with stable task IDs and acceptance criteria.
- [Engineering references](docs/REFERENCES.md) — source map behind the roadmap.

## Current state

**M0 / Phase 0 — Feasibility is complete.** The risky native/Steam/networking assumptions have been exercised far enough to pass the milestone exit gate: Java can reach the required SteamNetworkingSockets flat API path, the localhost impairment harness works, and GLFW/OpenGL, Jolt JNI, OpenAL, and UDP run together cleanly in the integrated JFR soak.

The repository is now entering **M1 / Phase 1 — Build, modules, and quality gates**. The first executable task is `P1-T01`, which establishes the initial multi-project Gradle structure with `engine-core`, `test-support`, `game-client`, and `game-server`.

For the fastest orientation on what has already been proven, what is temporary, and what should happen next, read [`docs/DEVELOPMENT_STATUS.md`](docs/DEVELOPMENT_STATUS.md).

Implementation work should be taken from GitHub Issues for the active phase, not directly from the entire technical backlog. Exact live task state remains in GitHub Issues/Project; repository-level context and durable conclusions are summarized in the development-status document.