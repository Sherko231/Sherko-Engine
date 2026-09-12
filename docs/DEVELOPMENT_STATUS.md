# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `eb82814b9f545dcf04be004912f69694942d604b` — P3-T04 merged through PR #148 and verified by merged-master workflow #257 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, Phase 2 exit gate, P3-T01 through P3-T04 |
| Active implementation | P3-T04A / Issue #149 / branch `p3-t04a-engine-sandbox-demo` / PR #150 |
| Paused next task | P3-T05 / Issue #88 — executable contract retained but implementation not started; resume only after P3-T04A formal completion and fresh audit against new `master` |
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
- Merged-master artifact `p3-t04-focus-loss` ID `10301895821`, digest `sha256:4f0a306752cefdd53b6caab51320cfc4c69f8a873f87c5be1af2efd4efba750c`.
- Public owner-thread `GlfwWindow.setCursorCaptured(boolean)` plus explicitly owned focus/key/button callbacks are implemented.
- Focus loss clears internal held state, releases effective cursor capture, and focus regain does not auto-recapture; explicit later capture is required.
- D-034 records the focus-loss/cursor-capture boundary.
- Issue #87 is closed as completed.
- Independent review was not performed because no separate reviewer/person/agent identity was available; CI and self-review were not treated as substitutes.

P3-T04 does not implement raw mouse, public `InputSnapshot`, actions/commands, controller mappings, renderer behavior, or later Phase 3 work.

## P3-T04A active implementation — owner-facing sandbox demo

Issue #149 was added at the owner's explicit request before P3-T05 implementation so the owner has one canonical place to observe the engine as it grows. It is the only active implementation task. P3-T05 / #88 is paused and its previously prepared branch contains no implementation.

PR #150 currently builds the owner-facing `game-sandbox` demo around already-public production APIs only:

- `EngineDemoMain` runs a roughly 38-second scripted scenario through `GlfwWindow` plus Phase 2 timing primitives;
- `EngineDemoTimeline` defines deterministic window-mode/cursor-capture timing and `EngineDemoTimelineTest` verifies that script without native GLFW;
- the demo reports logical and framebuffer dimensions, cumulative fixed 60 Hz simulation ticks, and interpolation alpha;
- diagnostics are explicitly not labeled FPS or benchmark evidence;
- the demo exercises `WINDOWED -> BORDERLESS_FULLSCREEN -> WINDOWED -> EXCLUSIVE_FULLSCREEN -> WINDOWED`, then cursor capture with an Alt+Tab observation window, explicit release, orderly stop/close, and `NativeResourceRegistry.assertNoOpenResources()`;
- the current demo is intentionally visually empty until the renderer has a public production presentation path; it does not call OpenGL/LWJGL directly to fake later functionality;
- `AGENTS.md` now requires future agents to update the sandbox in the same PR when a new capability is human-observable through already-authorized public APIs, or record `Sandbox impact: none — <reason>` when it cannot be demonstrated without exposing internals or pulling future work forward;
- `game-sandbox/README.md` is the owner-facing run/limitations/current-capabilities guide;
- no public engine API is added by P3-T04A.

An initial ordinary `game-sandbox -> engine-platform-lwjgl` runtime dependency was rejected during self-review because `game-server` consumes `game-sandbox` and the edge would transitively contaminate the headless server with GLFW/OpenGL. The active Issue was deliberately refined before finalizing the implementation. The current design uses `compileOnly` for sandbox source plus a dedicated resolvable/non-consumable `engineDemoRuntime` used only by `runEngineDemo`. The platform dependency must not be published through `game-sandbox` runtime elements, and `:game-server:verifyHeadlessServerRuntime` remains required acceptance evidence.

Run locally on Windows x64 with:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

The manual run is owner-observation evidence only. Automated correctness remains owned by tests, native acceptance, CI, and phase gates.

## Phase 3 status and exact next action

Phase 3 remains in progress. P3-T01 through P3-T04 are complete; P3-T04A is active; P3-T05 / #88 is paused with no implementation started. The Phase 3 exit remains the backlog requirement that an identical recorded input sequence can be replayed into headless simulation; the sandbox does not complete or replace that gate.

Exact next action for P3-T04A:

1. Finish `BUILD_AND_VERIFY.md` and technical-backlog reconciliation plus any lockfile change actually required by Gradle resolution.
2. Run/inspect the focused `EngineDemoTimelineTest`, full routine build/test/architecture/coverage/headless/version/lock matrix, and complete PR diff.
3. Confirm `:game-server:verifyHeadlessServerRuntime` still proves no platform/render/audio/GLFW/OpenGL/OpenAL contamination.
4. Record `Wiki impact: none — P3-T04A changes no public engine API; game-sandbox/README.md owns demo-specific guidance`.
5. Record independent review honestly; if unavailable, record `not performed`, reason, and residual risk.
6. Require all five CI jobs to pass on the exact final PR #150 head; obsolete runs from documentation commits are neither passes nor failures.
7. Mark PR #150 ready and merge only after the exact-head pass.
8. Verify remote `master` equals the merge result and require a separate passing five-job push workflow on that exact merged commit.
9. Only then close Issue #149 as completed.
10. Freshly audit/reactivate P3-T05 / #88 against the new verified `master`; recreate/rebase its stale no-code branch as appropriate before implementation.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input or owner-facing sandbox work, but bounded sandbox/native evidence must not be represented as satisfying them.

## Live-state reconciliation

A fresh agent must read `AGENTS.md` first, inspect local status/HEAD when a checkout exists, inspect live remote `master`, Issue #149, PR #150, exact-head workflow state, and compare those facts with this checkpoint. P3-T05 / #88 is intentionally paused while #149 is active. If live GitHub state has moved beyond this document, live GitHub controls workflow status. Stop rather than guess if code, docs, the active Issue, PR, or verification evidence conflict.