# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `2f3dcd3d9117db28b994115f0c420b884e5a59b0` — P3-T05 / Issue #88 merged through PR #155; lightweight exact-merge workflow #283 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A plus P1-T08A maintenance, P2-T01 through P2-T13 plus Phase 2 exit gate, P3-T01 through P3-T05 plus P3-T04A/P3-T04B |
| Active feature implementation | P3-T06 / Issue #89 / branch `p3-t06-input-snapshot` — immutable renderer-frame `InputSnapshot` |
| Next planned task | P3-T07 / Issue #90 — planning-only until P3-T06 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect local status/HEAD when available plus live remote `master`, Issues, PRs, and workflow state before continuing.

## Stable completed foundation

Phase 1 and Phase 2 remain complete. The Phase 2 D-030 / Issue #135 integrated exit gate passed on exact merged `master` `88ddfbd64468ba85d37eb32e1f6d40c9083e1326`: at least 60 continuous seconds of integrated fixed 60 Hz simulation ticks, bounded catch-up after the injected stall, orderly lifecycle shutdown, and empty `NativeResourceRegistry` cleanup were verified together.

P0-T09A / #42, P0-T13 / #43, and P0-T14 / #44 remain independent feasibility gates. Phase 1/2 completion and later bounded platform/demo tests do not satisfy or strengthen those gates.

## Phase 3 completed work

P3-T01 through P3-T05 are complete, together with P3-T04A/P3-T04B sandbox maintenance.

The production `GlfwWindow` boundary now provides:

- D-031: explicit GLFW/OpenGL 4.6 lifecycle/native ownership;
- D-032: owner-thread event polling and separated logical/framebuffer dimensions;
- D-033: in-place windowed/borderless/exclusive primary-monitor transitions;
- D-034: focus-loss-safe held-state cleanup and explicit cursor recapture policy;
- D-035: internal relative mouse acquisition using GLFW raw mode when supported and a disabled-cursor position-delta fallback otherwise.

P3-T05 / Issue #88 completed through PR #155:

- exact final head `5348f10e139e7b8dd337e296b90a296130bca614` passed heavy workflow #282 / run `34718462000` with all five jobs;
- retained `p3-t05-mouse-motion` artifact ID `10305972206`, digest `sha256:1d790b2b0854be28763d0e172f0cb7330eaf6531c86f8bb751a1ddf990956ec0`;
- merged `master` `2f3dcd3d9117db28b994115f0c420b884e5a59b0` passed lightweight workflow #283 / run `34718902190`;
- Issue #88 closed completed.

`game-sandbox` remains the canonical owner-facing manual demo. Its demo-only platform runtime remains non-consumable/non-exported so `game-server` stays headless.

Run the demo on Windows x64 with:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

## P1-T08A CI lifecycle

P1-T08A / Issue #153 established the current runner-efficient lifecycle:

1. implementation/tests/docs/wiki/sandbox/self-review happen on the dedicated branch before the final non-draft PR;
2. the exact final PR candidate runs the five-job heavy matrix;
3. merge is allowed only while the tested head/base remain current;
4. the exact merged `master` commit runs one lightweight verifier covering dependency locks, the headless-server runtime boundary, and exact client/server version metadata;
5. a stronger post-merge full matrix is task-specific/manual, not routine duplication.

Markdown-only exemption rules remain governed by `AGENTS.md`, `docs/CI_LIFECYCLE.md`, and `docs/BUILD_AND_VERIFY.md`.

## P3-T06 active implementation — renderer-frame InputSnapshot

Issue #89 was freshly audited and activated from verified `master` `2f3dcd3d9117db28b994115f0c420b884e5a59b0`. Dedicated branch: `p3-t06-input-snapshot`.

The bounded implementation introduces D-036 and the first public hardware-frame read boundary without changing the module graph:

- public `InputSnapshot`, `InputKey`, and `InputMouseButton` live under `com.samo.engine.platform.api`;
- `GlfwWindow.captureInputSnapshot(long frameId)` is STARTED/owner-thread-only and performs no GLFW polling;
- snapshots are immutable and expose caller-owned frame ID, focus/capture state, bounded keyboard/mouse held levels, retained raw hardware pressed/released edges, and accumulated relative mouse delta;
- successful snapshot capture consumes pending edges and accumulated mouse delta while preserving held levels and the D-035 motion baseline;
- a press and release entirely between snapshots is retained as both edges rather than disappearing;
- GLFW key repeat does not create repeated press edges;
- focus loss converts currently held supported inputs to pending release edges, discards stale pending presses, clears mouse motion, and preserves D-034 explicit-recapture behavior;
- public signatures expose no GLFW/LWJGL classes, handles, or constants;
- `game-server` receives no dependency on `engine-platform-lwjgl`; headless/tick/replay portability remains P3-T09 `PlayerInputCommand` work.

Deterministic acceptance coverage is being added in `InputSnapshotTest` and `GlfwWindowInputSnapshotTest`, alongside the existing P3 focus/mouse-motion regressions.

### Sandbox impact

P3-T06 creates an authorized public observation boundary, so the existing sandbox is updated in the same task:

- one `InputSnapshot` is captured after each demo-frame `pollEvents()`;
- once-per-second bounded diagnostics expose snapshot frame ID, focus/capture state, W/A/S/D held state, and accumulated public snapshot mouse delta since the prior diagnostic;
- no direct GLFW/native/internal call is added;
- the demo remains a diagnostic surface, not a game, renderer benchmark, or performance gate.

`:game-server:verifyHeadlessServerRuntime` remains required to prove the existing demo-only platform classpath is still non-exported.

### Wiki/API impact

P3-T06 adds public consumer API, so the task updates:

- `wiki/API_INDEX.md`;
- `wiki/PLATFORM/GLFW_WINDOW.md`;
- new `wiki/PLATFORM/INPUT.md`;
- `wiki/LIMITATIONS.md`;
- `wiki/README.md` navigation;
- `game-sandbox/README.md`.

P3-T07+ action/controller/command APIs must not be documented as implemented.

## Phase 3 status and exact next action

Phase 3 remains in progress. P3-T01 through P3-T05 are accepted; P3-T06 / #89 is active and is not accepted until its exact final PR candidate and exact merged commit pass the current CI lifecycle.

Exact next action for P3-T06:

1. finish deterministic implementation/tests, D-036, architecture/build/wiki/sandbox handoff, and complete-diff self-review on `p3-t06-input-snapshot`;
2. verify no dependency/version/lockfile/module-edge change and keep `game-server` free of platform/native runtime dependencies;
3. open one final non-draft PR only when the candidate is complete;
4. require the five-job heavy matrix on the exact final PR head; existing Windows native smoke must regress P3-T01 through P3-T05 on that same candidate even though P3-T06 itself adds no native call;
5. if the candidate changes after a pass or `master` advances relative to the tested base, refresh and reverify before merge;
6. merge only the exact verified candidate;
7. require the lightweight exact-merge `master` verifier, record acceptance/review evidence, close #89, then freshly audit P3-T07 / #90.

The Phase 3 exit remains the backlog requirement that an identical recorded input sequence can be replayed into headless simulation. A renderer-frame `InputSnapshot` alone does not satisfy that exit gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input work, but bounded P3 native evidence must not be represented as satisfying them.

## Live-state reconciliation

A fresh agent must read `AGENTS.md` first, inspect local status/HEAD when a checkout exists, inspect live remote `master`, Issue #89, branch `p3-t06-input-snapshot`, any open PR, and workflow state, then compare those facts with this checkpoint. If live GitHub state has moved beyond this document, live GitHub controls workflow status. Stop rather than guess if code, docs, the active Issue, PR, or verification evidence conflict.
