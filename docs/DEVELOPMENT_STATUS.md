# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `7876b17140fc5a124dc943642f205c9be73859d6` — P3-T04A merged through PR #150 and verified by merged-master workflow #271 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, Phase 2 exit gate, P3-T01 through P3-T04, P3-T04A |
| Active implementation | P3-T04B / Issue #151 / branch `p3-t04b-sandbox-engine-logger` / PR #152 |
| Paused next task | P3-T05 / Issue #88 — executable contract retained but implementation not started; resume only after P3-T04B formal completion and fresh audit against new `master` |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect local status/HEAD when available plus live remote `master`, Issues, PRs, and workflow state before continuing.

## Stable completed foundation

Phase 1 and Phase 2 remain complete. The Phase 2 D-030 / Issue #135 integrated exit gate passed on exact merged `master` `88ddfbd64468ba85d37eb32e1f6d40c9083e1326` through merged-master workflow #203 / run `34616504486`: at least 60 continuous seconds of integrated fixed 60 Hz simulation ticks, bounded catch-up after the injected stall, orderly lifecycle shutdown, and empty `NativeResourceRegistry` cleanup were verified together.

P0-T09A / #42, P0-T13 / #43, and P0-T14 / #44 remain independent feasibility gates. Phase 1/2 completion and later bounded platform/demo tests do not satisfy or strengthen those gates.

## Phase 3 completed tasks

### P3-T01 — production GLFW/OpenGL window lifecycle

P3-T01 / Issue #84 is complete through PR #141 / merged `master` `a781c34a7959e207b29058803a2fb74a9cf0d662`. D-031 records the production `GlfwWindow` owner-thread/native-ownership boundary and retained Windows x64 evidence verifies one real OpenGL 4.6 lifecycle with cleanup.

### P3-T02 — logical versus framebuffer sizing

P3-T02 / Issue #85 is complete through PR #146 / merged `master` `534853a334ab52fcbd2f44931343e83f1bdaa096`. D-032 records separate logical-window/framebuffer-pixel channels plus owner-thread `pollEvents()`; retained native evidence verifies the production path.

### P3-T03 — window mode transitions

P3-T03 / Issue #86 is complete through PR #147 / merged `master` `497034e21fd988a2d5dcab5d035a5dacf0d635a7`. Merged-master workflow #245 passed all five jobs. The retained Windows native acceptance executed exactly 20 successful `BORDERLESS_FULLSCREEN -> WINDOWED -> EXCLUSIVE_FULLSCREEN -> WINDOWED` transitions while retaining the same OpenGL context and restoring windowed geometry. D-033 records the in-place primary-monitor window-mode contract.

### P3-T04 — focus-loss input safety

P3-T04 / Issue #87 is formally complete through PR #148 / merged `master` `eb82814b9f545dcf04be004912f69694942d604b`.

- Final exact-head CI passed before merge.
- Merged-master workflow #257 / run `34706106972` passed all five jobs on exact merge commit `eb82814b9f545dcf04be004912f69694942d604b`.
- Public owner-thread `GlfwWindow.setCursorCaptured(boolean)` plus explicitly owned focus/key/button callbacks are implemented.
- Focus loss clears internal held state, releases effective cursor capture, and focus regain does not auto-recapture; explicit later capture is required.
- D-034 records the focus-loss/cursor-capture boundary.
- Issue #87 is closed as completed.

### P3-T04A — owner-facing sandbox demo

P3-T04A / Issue #149 is formally complete through PR #150 / merged `master` `7876b17140fc5a124dc943642f205c9be73859d6`.

- Final exact-head PR CI passed all five jobs.
- Merged-master workflow #271 / run `34709719815` passed all five jobs on exact merge commit `7876b17140fc5a124dc943642f205c9be73859d6`.
- `game-sandbox` is now the canonical owner-facing manual engine demo.
- `EngineDemoMain` runs a roughly 38-second scripted scenario through already-public production APIs only.
- `EngineDemoTimeline` / `EngineDemoTimelineTest` keep the scripted window-mode/cursor-capture sequence deterministic without native GLFW.
- The demo-only `engine-platform-lwjgl` runtime is non-consumable and does not contaminate `game-server`; `:game-server:verifyHeadlessServerRuntime` passed.
- `AGENTS.md` requires future tasks to update the sandbox when a capability is meaningfully observable through authorized public APIs, or record `Sandbox impact: none — <reason>`.
- The current demo remains intentionally visually empty until the renderer has a public production presentation path.
- Issue #149 is closed as completed.

Run locally on Windows x64 with:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

The manual run is owner-observation evidence only. Automated correctness remains owned by tests, native acceptance, CI, and phase gates.

## P3-T04B active implementation — sandbox structured logging cleanup

Issue #151 is a bounded owner-facing sandbox cleanup inserted before P3-T05. PR #152 changes only the sandbox consumer surface and its README.

The implementation:

- routes logical-window and framebuffer-size notifications through the existing public `EngineLogger`;
- routes scripted window-mode/cursor-capture transitions and orderly-shutdown status through `EngineLogger`;
- routes once-per-second timing diagnostics through `EngineLogger` with `subsystem=game-sandbox` and the current simulation tick when available;
- keeps direct `System.out` only in the caller-owned console sink and for explicit owner-facing timeline/instruction text;
- does not change `EngineLogger`, any public engine API, dependencies, lockfiles, module edges, workflow behavior, renderer code, or P3-T05+ input behavior;
- preserves the existing ~38-second script and cleanup semantics.

PR-head workflow #272 / run `34710794783` passed all five jobs on pre-documentation head `3357aa85b2a56b8cdbc30b87cae661dc1dbf4ed8`. Because documentation reconciliation changes the PR head afterward, that run becomes obsolete and the final PR head still requires its own exact-head passing workflow before merge.

`Wiki impact: none — no public engine API or consumer lifecycle/configuration semantics changed.`

`Sandbox impact: EngineDemoMain and game-sandbox/README.md are the changed owner-facing surface.`

## Phase 3 status and exact next action

Phase 3 remains in progress. P3-T01 through P3-T04 and P3-T04A are complete. P3-T04B / #151 is active. P3-T05 / #88 remains paused with no implementation started. The Phase 3 exit remains the backlog requirement that an identical recorded input sequence can be replayed into headless simulation; the sandbox does not complete or replace that gate.

Exact next action for P3-T04B:

1. Complete the documentation/status reconciliation required by Issue #151.
2. Require all five CI jobs to pass on the exact final PR #152 head; older passing runs are obsolete after any commit.
3. Merge PR #152 only after exact-head CI passes.
4. Require a separate five-job push workflow on the exact resulting `master` merge commit.
5. Only then close Issue #151 as completed.
6. Freshly audit/reactivate P3-T05 / #88 against the resulting verified `master`, recreating/rebasing its stale no-code branch as appropriate before implementation.

The authoring agent cannot manually observe the interactive demo window in its connected environment. The owner's local `:game-sandbox:runEngineDemo` invocation remains the intended human-observation path and does not replace automated acceptance.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input or owner-facing sandbox work, but bounded sandbox/native evidence must not be represented as satisfying them.

## Live-state reconciliation

A fresh agent must read `AGENTS.md` first, inspect local status/HEAD when a checkout exists, inspect live remote `master`, Issue #151, PR #152, exact-head workflow state, and compare those facts with this checkpoint. P3-T05 / #88 is intentionally paused while #151 is active. If live GitHub state has moved beyond this document, live GitHub controls workflow status. Stop rather than guess if code, docs, the active Issue, PR, or verification evidence conflict.