# Sherko Engine

Java-first 3D engine scoped for small/medium first-/third-person, physics-heavy, humorous multiplayer co-op games.

## Project documents

- [Development status](docs/DEVELOPMENT_STATUS.md) — living snapshot of current progress, proven conclusions, temporary spike code, and documentation-maintenance rules.
- [Engine scope](ENGINE_SCOPE.md) — product and architecture boundaries.
- [Roadmap](ROADMAP.md) — milestone-level plan and current focus.
- [Technical backlog](docs/roadmap/TECHNICAL_BACKLOG.md) — detailed implementation task catalog with stable task IDs and acceptance criteria.
- [Engineering references](docs/REFERENCES.md) — source map behind the roadmap.

## Current state

The native Java 25 baseline is proven: GLFW/OpenGL, Jolt JNI, OpenAL, localhost UDP, Steam initialization, deterministic network impairment, and Java FFM access to the Steam flat API have all been exercised.

The 15-second combined native run is classified as an integration smoke test. End-to-end SteamNetworkingSockets connection/message ownership and longer native stability tests remain explicit follow-up gates; they are not treated as already-proven production transport.

M1 / Phase 1 — Build, modules, and quality gates is active. Exact task state belongs only in GitHub Issues/Project so this README does not duplicate a changing task number.

For durable conclusions and implementation notes, read [`docs/DEVELOPMENT_STATUS.md`](docs/DEVELOPMENT_STATUS.md). For executable work, use an active GitHub Issue and complete it through a task branch, pull request, and CI verification.
