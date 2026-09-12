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

The Java 25 multi-project foundation declares all 16 production-target modules plus the experimental `feasibility-spikes` subproject. It centralizes the toolchain, dependency locking, JUnit 6/AssertJ test support, quality gates, and repeatable client/server entry points. `engine-core` provides the lifecycle, dependency/startup rollback, deterministic timing, configuration, native-resource diagnostics, structured logging, and fatal-termination foundation completed in Phase 2. `engine-platform-lwjgl` contains the production `GlfwWindow` platform boundary from P3-T01, P3-T02's separate logical-window/framebuffer-size delivery, P3-T03's in-place primary-monitor window-mode transitions, and the active P3-T04 focus-loss/cursor-capture safety work.

Phase 0 proved GLFW/OpenGL, Jolt JNI, OpenAL, localhost UDP, Steam initialization, deterministic network impairment, Java FFM access to the Steam flat API, and a 15-second combined native smoke run. End-to-end SteamNetworkingSockets and sustained/repeated native lifecycle evidence remain explicit follow-up gates.

Phase 1 and Phase 2 are complete. P3-T01 / Issue #84, P3-T02 / Issue #85, and P3-T03 / Issue #86 are formally complete. P3-T03 merged through PR #147 as `497034e21fd988a2d5dcab5d035a5dacf0d635a7`; merged-master workflow #245 passed all five jobs on that exact commit, including the real Windows x64 20-transition window-mode acceptance. These results do not satisfy the separate P0-T09A/P0-T13/P0-T14 feasibility gates.

Phase 3 remains in progress. P3-T04 / Issue #87 is the active bounded task on branch `p3-t04-focus-loss-input-safety` / PR #148. It adds owner-thread cursor capture plus focus-loss cleanup of internally tracked held key/mouse-button state without pulling P3-T05 raw mouse or P3-T06 public `InputSnapshot` behavior forward. Formal completion still requires exact final PR-head CI, merge, exact merged-`master` push CI, and Issue closure. P3-T05 / Issue #88 and later Phase 3 tasks remain planning-only.

All agent-authored changes use one active Issue, a dedicated branch, a pull request, and required verification before merge. Non-exempt pull requests reference their Issue without auto-closing it; the Issue is closed only after exact merged-`master` CI passes. Qualifying Markdown-only changes use the documented CI exemption.

When a task adds or changes public engine API or consumer-visible usage behavior, the same pull request must update the relevant [`wiki/`](wiki/README.md) pages. Tasks with no wiki impact should record that explicitly rather than making meaningless documentation churn.