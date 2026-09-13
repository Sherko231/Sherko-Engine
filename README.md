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
- [Engine API wiki](wiki/README.md) — consumer guide for implemented public APIs and current limitations.
- [Engine sandbox](game-sandbox/README.md) — owner-facing manual observation path for implemented behavior.

## Current state

The Java 25 multi-project foundation declares all 16 production-target modules plus experimental `feasibility-spikes`. Phase 1 and Phase 2 are complete. `engine-core` provides lifecycle, dependency/startup rollback, deterministic fixed-step timing, typed configuration, native-resource diagnostics, structured logging, and fatal-termination foundations.

P3-T01 through P3-T08 are formally complete. The `engine-platform-lwjgl` boundary now owns production GLFW/OpenGL window lifecycle, logical/framebuffer sizing, display-mode transitions, focus-safe cursor capture and relative mouse acquisition, immutable renderer-frame `InputSnapshot`, strict immutable `InputActionBindings`, and renderer-frame `InputActionEvaluator` action state.

P3-T08 / Issue #91 completed through PR #158. Its exact final PR candidate passed the heavy five-job matrix, and merged feature commit `ab42c30d4b186e0ecb1aab1f53a5819ac8d0e097` passed exact-merge workflow #293 / run `34769991670` on attempt 2. The first attempt failed at runner setup before repository execution and the identical merge commit passed on rerun.

P3-T09 / Issue #92 is the active Phase 3 task on branch `p3-t09-player-input-commands`. It adds the device-neutral simulation-tick `PlayerInputCommand` contract in `engine-core`, an explicit fixed-size ByteBuffer replay codec v1, and a caller-owned `PlayerInputCommandSampler` in `engine-platform-lwjgl` that bridges renderer-frame action snapshots to fixed simulation ticks without creating a server-to-platform dependency.

P3-T09 preserves one-shot input correctly across mismatched render/simulation cadence: LOOK delta and digital pressed/released edges accumulate across zero-tick frames and are consumed exactly once by the next emitted command; latest MOVE and digital scalar/held state repeat when multiple ticks occur without a new renderer snapshot. The codec uses magic `SPIC`, version 1, big-endian fields, exactly nine digital actions, and an exact 126-byte layout. It is a replay/storage command format, not a production network packet layout.

A deterministic headless replay test records commands, encodes/decodes them, and replays the decoded sequence into a platform-independent test consumer against an independently calculated final result. This makes the Phase 3 replay exit behavior testable at the input-command boundary, but Phase 3 remains incomplete until P3-T10 finishes.

The owner-facing sandbox now emits tick commands only when fixed-step timing reports due simulation ticks and displays bounded tick-command diagnostics alongside renderer-frame action diagnostics. Its platform dependency remains non-exported so `game-server` stays headless.

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
