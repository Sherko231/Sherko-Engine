# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `134bd3cc18258325f835f2d704319bc23a6bca47` — P1-T08A / Issue #153 merged through PR #154 and verified by lightweight exact-merge workflow #278 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A plus P1-T08A maintenance, P2-T01 through P2-T13, Phase 2 exit gate, P3-T01 through P3-T04B |
| Active feature implementation | P3-T05 / Issue #88 / branch `p3-t05-raw-mouse-motion` — raw relative mouse motion with fallback |
| Next planned task | P3-T06 / Issue #89 — remains planning-only until P3-T05 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect local status/HEAD when available plus live remote `master`, Issues, PRs, and workflow state before continuing.

## Stable completed foundation

Phase 1 and Phase 2 remain complete. The Phase 2 D-030 / Issue #135 integrated exit gate passed on exact merged `master` `88ddfbd64468ba85d37eb32e1f6d40c9083e1326` through merged-master workflow #203 / run `34616504486`: at least 60 continuous seconds of integrated fixed 60 Hz simulation ticks, bounded catch-up after the injected stall, orderly lifecycle shutdown, and empty `NativeResourceRegistry` cleanup were verified together.

P0-T09A / #42, P0-T13 / #43, and P0-T14 / #44 remain independent feasibility gates. Phase 1/2 completion and later bounded platform/demo tests do not satisfy or strengthen those gates.

## Phase 3 completed work

### P3-T01 through P3-T04 — production platform foundation

P3-T01 / #84 through P3-T04 / #87 are complete. The production `GlfwWindow` lifecycle, separate logical/framebuffer sizing, in-place windowed/borderless/exclusive transitions, and focus-loss-safe cursor capture/input cleanup are merged and verified. D-031 through D-034 record the durable platform contracts.

P3-T04 / Issue #87 completed through PR #148 / merged `master` `eb82814b9f545dcf04be004912f69694942d604b`; historical merged-master workflow #257 / run `34706106972` passed all five jobs under the CI policy in force at that time.

### P3-T04A — owner-facing sandbox demo

P3-T04A / Issue #149 completed through PR #150 / merged `master` `7876b17140fc5a124dc943642f205c9be73859d6`.

- `game-sandbox` is the canonical owner-facing manual engine demo.
- `EngineDemoMain` runs a roughly 38-second scripted scenario through already-public production APIs only.
- `EngineDemoTimeline` / `EngineDemoTimelineTest` keep the scripted window-mode/cursor-capture sequence deterministic without native GLFW.
- The demo-only `engine-platform-lwjgl` runtime is non-consumable and does not contaminate `game-server`; `:game-server:verifyHeadlessServerRuntime` passed.
- Future tasks must update the sandbox when a capability is meaningfully observable through authorized public APIs, or record `Sandbox impact: none — <reason>`.
- The current demo remains intentionally visually empty until the renderer has a public production presentation path.
- Historical merged-master workflow #271 / run `34709719815` passed all five jobs under the previous routine post-merge policy.

Run locally on Windows x64 with:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

### P3-T04B — sandbox structured logging cleanup

P3-T04B / Issue #151 completed through PR #152 / merged `master` `61f576f6e23fb6b5518fdeb075db3871fcc5d76e`. Runtime state transitions and diagnostics now use `EngineLogger`; direct console output remains limited to the caller-owned sink and explicit owner instructions. Historical workflow #276 passed all five jobs under the previous post-merge policy.

## P1-T08A completed maintenance — CI efficiency

P1-T08A / Issue #153 completed through PR #154 / merged `master` `134bd3cc18258325f835f2d704319bc23a6bca47`.

- Exact final PR candidate `b8f13a1fc5c0c6f1ecab9a2f823730972a9ed63f` passed workflow #277 with all five heavy jobs.
- Exact merged `master` `134bd3cc18258325f835f2d704319bc23a6bca47` passed workflow #278 with the single lightweight master verifier; the five heavy jobs were intentionally skipped.
- Normal development now stays on the task branch before the final non-draft PR, eliminating routine heavy CI churn during implementation.
- The post-merge verifier checks committed lock state, the headless-server runtime boundary, and exact client/server version metadata. Stronger full post-merge evidence remains opt-in through `workflow_dispatch` or a task-specific command.

## P3-T05 active implementation — relative mouse motion

Issue #88 was freshly re-audited and activated from verified `master` `134bd3cc18258325f835f2d704319bc23a6bca47`; the stale no-code `p3-t05-raw-mouse-motion` branch was fast-forwarded to that baseline before implementation.

The current candidate adds D-035 without creating a new public consumer API:

- `GlfwWindow` owns a cursor-position callback while STARTED.
- Effective cursor capture uses GLFW raw mouse mode when `glfwRawMouseMotionSupported()` is true.
- Unsupported systems use successive disabled-cursor positions as a relative-delta fallback; this fallback does not claim to bypass OS pointer acceleration.
- The first eligible sample after capture/re-capture/baseline invalidation contributes zero delta; later samples accumulate signed X/Y differences.
- Capture release, focus loss, stop/close, and start-failure cleanup clear accumulated motion and invalidate the baseline.
- Focus regain never automatically restores cursor capture or raw mode; explicit recapture remains required under D-034.
- Raw-enable failure preserves the original throwable and performs best-effort raw/cursor rollback; focus-loss native failures remain inside the callback and propagate once from `pollEvents()`.
- Relative motion remains package-private/internal until P3-T06 defines `InputSnapshot` and frame-consumption semantics.

Deterministic coverage is added in `GlfwWindowMouseMotionTest`. Opt-in Windows native acceptance is added in `GlfwWindowMouseMotionNativeTest` with retained report `engine-platform-lwjgl/build/reports/p3/p3-t05-mouse-motion.txt`; the final non-draft PR's Windows-native-smoke job is expected to execute it under `SHERKO_P3_T05_NATIVE=true`.

`Sandbox impact: none — P3-T05 motion state remains intentionally internal until P3-T06; a visible delta demo would require a premature public API solely for the sandbox.`

## Phase 3 status and exact next action

Phase 3 remains in progress. P3-T01 through P3-T04B are complete; P3-T05 / #88 is active and not yet accepted until its exact final PR candidate and exact merged commit pass the required current CI lifecycle.

Exact next action for P3-T05:

1. Finish architecture/build/wiki reconciliation and self-review on `p3-t05-raw-mouse-motion` before opening a PR.
2. Open one final non-draft PR only when the candidate is complete.
3. Require the full five-job heavy matrix on the exact final PR candidate, including P3-T05 native acceptance in `Windows native smoke`.
4. If the candidate changes after that pass or `master` advances relative to the tested base, refresh and reverify before merge.
5. Merge only the exact verified candidate.
6. Require the lightweight exact-merge `master` verifier on the resulting merge SHA; do not repeat the full matrix unless task-specific evidence genuinely requires it.
7. Record review/evidence, close #88 as completed, then freshly audit P3-T06 / #89 before implementation.

The Phase 3 exit remains the backlog requirement that an identical recorded input sequence can be replayed into headless simulation; P3-T05 alone does not complete the phase.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input work, but bounded P3 native evidence must not be represented as satisfying them.

## Live-state reconciliation

A fresh agent must read `AGENTS.md` first, inspect local status/HEAD when a checkout exists, inspect live remote `master`, Issue #88, branch `p3-t05-raw-mouse-motion`, any open PR, and workflow state, then compare those facts with this checkpoint. If live GitHub state has moved beyond this document, live GitHub controls workflow status. Stop rather than guess if code, docs, the active Issue, PR, or verification evidence conflict.
