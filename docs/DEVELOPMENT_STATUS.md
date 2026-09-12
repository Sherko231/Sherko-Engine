# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `497034e21fd988a2d5dcab5d035a5dacf0d635a7` — P3-T03 merged through PR #147 and verified by merged-master workflow #245 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, Phase 2 exit gate, P3-T01, P3-T02, and P3-T03 |
| Active implementation | P3-T04 / Issue #87 / branch `p3-t04-focus-loss-input-safety` / PR #148 |
| Formal P3-T04 completion authority | Exact final PR-head CI, PR merge, exact merged-`master` push CI, retained P3-T04 native evidence, then Issue #87 closure |
| Next planned roadmap task | P3-T05 / Issue #88 — planning-only until P3-T04 is formally complete and #88 is freshly audited/refined/activated |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect local status/HEAD when available plus live remote `master`, Issues, PRs, and workflow state before continuing.

## Stable completed foundation

Phase 1 and Phase 2 remain complete. The Phase 2 D-030 / Issue #135 integrated exit gate passed on exact merged `master` `88ddfbd64468ba85d37eb32e1f6d40c9083e1326` through merged-master workflow #203 / run `34616504486`: at least 60 continuous seconds of integrated fixed 60 Hz simulation ticks, bounded catch-up after the injected stall, orderly lifecycle shutdown, and empty `NativeResourceRegistry` cleanup were verified together.

P0-T09A / #42, P0-T13 / #43, and P0-T14 / #44 remain independent feasibility gates. Phase 1/2 completion and later bounded platform tests do not satisfy or strengthen those gates.

## Phase 3 completed tasks

### P3-T01 — production GLFW/OpenGL window lifecycle

P3-T01 / Issue #84 is complete through PR #141 / merged `master` `a781c34a7959e207b29058803a2fb74a9cf0d662`. Exact-head workflow #217 and merged-master workflow #218 passed. D-031 records the production `GlfwWindow` owner-thread/native-ownership boundary. The retained native evidence proved one real Windows x64 OpenGL 4.6 lifecycle with actual version/renderer logging and empty registry cleanup; it is not P0-T13/P0-T14 evidence.

### P3-T02 — logical versus framebuffer sizing

P3-T02 / Issue #85 is complete through PR #146 / merged `master` `534853a334ab52fcbd2f44931343e83f1bdaa096`. Exact-head workflow #232 and merged-master workflow #233 passed all five jobs. D-032 records separate logical-window/framebuffer-pixel channels plus owner-thread `pollEvents()`. Retained native evidence verified the production path and cleanup on Windows x64.

### P3-T03 — window mode transitions

P3-T03 / Issue #86 is formally complete.

- PR #147 merged to `master` as `497034e21fd988a2d5dcab5d035a5dacf0d635a7`.
- Final exact-head CI passed on `d2dbde3d594921fed48ec960c2ed70b9fc6439c5`; an earlier setup failure was infrastructure-only and was rerun on the unchanged head.
- Merged-master workflow #245 / run `34703875566` passed all five jobs on exact merge commit `497034e21fd988a2d5dcab5d035a5dacf0d635a7`.
- Merged-master artifact `p3-t03-window-modes` ID `10301720511`, digest `sha256:d8468aee3ab8837d7b3ed031787e1b50aac9d1cd0ec1e2e3d7eb600e4ff5c90e`.
- The native acceptance executed exactly 20 successful transitions as five `BORDERLESS_FULLSCREEN -> WINDOWED -> EXCLUSIVE_FULLSCREEN -> WINDOWED` cycles while retaining the same OpenGL context, restoring windowed geometry, and finishing with empty production-owned native resources.
- D-033 records the in-place primary-monitor window-mode contract.
- Issue #86 is closed as completed.
- Independent review was not performed because no separate reviewer/person/agent identity was available; CI and self-review were not treated as substitutes.

## P3-T04 active implementation — focus-loss input safety

Issue #87 was freshly audited against verified `master` `497034e21fd988a2d5dcab5d035a5dacf0d635a7`, the completed D-031/D-032/D-033 platform behavior, the Phase 3 backlog, and current public wiki. It was refined from planning-only into the active executable contract before implementation.

PR #148 is the only active bounded implementation and currently adds:

- public owner-thread `GlfwWindow.setCursorCaptured(boolean)` while STARTED;
- explicitly owned GLFW window-focus, key, and mouse-button callbacks;
- bounded internal held-key/button state used only for pre-P3-T06 focus-loss safety;
- focus loss clearing all tracked held keys/buttons before cursor release;
- focus loss restoring `GLFW_CURSOR_NORMAL` when capture is effectively active;
- no automatic cursor recapture on focus regain; an explicit later `setCursorCaptured(true)` is required;
- staged propagation of a cursor-release failure from `pollEvents()` rather than allowing an unchecked exception to escape through the native focus callback boundary;
- deterministic `GlfwWindowFocusTest` coverage while retaining the existing `GlfwWindowTest` regression suite;
- opt-in Windows x64 `GlfwWindowFocusNativeTest` using a test-only second GLFW helper window for real focus transfer;
- retained report path `engine-platform-lwjgl/build/reports/p3/p3-t04-focus-loss.txt`;
- D-034 plus synchronized public API/wiki guidance;
- no dependency, lockfile/module edge, renderer, raw-mouse, public `InputSnapshot`, action/command, controller, or P3-T05+ implementation.

The native acceptance requires: capture while focused -> real focus transfer away -> normal cursor after focus loss -> focus return with no automatic recapture -> explicit recapture -> orderly cleanup. The report also retains the manual Alt+Tab scenario: hold a movement key while captured, Alt+Tab away, release while unfocused, return; the future P3-T06 public snapshot path must start released rather than stuck. P3-T04 does not claim that public snapshot result exists yet.

During draft verification, the Windows native job on an implementation head passed the existing P3-T01/P3-T02/P3-T03 native checks and the new P3-T04 focus-loss acceptance plus artifact upload. Subsequent documentation commits supersede that run; only a completed passing workflow on the exact final PR head may authorize merge.

## Phase 3 status and next action

Phase 3 remains in progress. P3-T01 through P3-T03 are complete; P3-T04 is active. P3-T05 / Issue #88 and later Phase 3 tasks remain planning-only. The Phase 3 exit remains the backlog requirement that an identical recorded input sequence can be replayed into headless simulation; isolated P3-T04 tests do not complete the phase.

Exact next action:

1. Finish the P3-T04 authorized docs/wiki consistency audit and complete-diff audit.
2. Record independent review honestly. If no separate reviewer/person/agent is available, record `not performed`, the reason, and residual risk; CI/self-review are not substitutes.
3. Require all five CI jobs to pass on the exact final PR #148 head. Ignore/cancel only obsolete runs caused by superseding commits.
4. Inspect the retained `p3-t04-focus-loss` artifact/report and require the tested SHA to match the final PR head.
5. Merge PR #148 only after that exact-head pass.
6. Verify remote `master` equals the merge result and require a separate all-five-job push workflow on that exact merged commit, including P3-T04 native artifact/evidence.
7. Only then close Issue #87 as completed.
8. Freshly audit/refine/activate P3-T05 / #88; do not infer activation from task numbering.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input work, but P3-T04 evidence must not be represented as satisfying them.

## Live-state reconciliation

A fresh agent must read `AGENTS.md` first, inspect local status/HEAD when a checkout exists, inspect live remote `master`, Issue #87, PR #148, exact-head workflow state, and compare those facts with this checkpoint. If PR #148 or #87 have moved beyond this document, live GitHub state controls workflow status. Stop rather than guess if code, docs, wiki, the active Issue, or live GitHub state conflict.