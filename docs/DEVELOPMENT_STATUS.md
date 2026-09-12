# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `61f576f6e23fb6b5518fdeb075db3871fcc5d76e` — P3-T04B / Issue #151 merged through PR #152 and verified by merged-master workflow #276 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, Phase 2 exit gate, P3-T01 through P3-T04B |
| Active maintenance implementation | P1-T08A / Issue #153 / branch `p1-t08a-ci-efficiency` — CI efficiency/process-policy migration |
| Paused next feature task | P3-T05 / Issue #88 — executable contract retained; resume only after #153 completes and a fresh audit against the new verified `master` |
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

The manual run is owner-observation evidence only. Automated correctness remains owned by tests, native acceptance, CI, and phase gates.

### P3-T04B — sandbox structured logging cleanup

P3-T04B / Issue #151 completed through PR #152 / merged `master` `61f576f6e23fb6b5518fdeb075db3871fcc5d76e`.

- Runtime state transitions, logical/framebuffer-size notifications, once-per-second timing diagnostics, cursor transitions, and orderly shutdown status now pass through the existing public `EngineLogger` boundary.
- Direct `System.out` remains only in the caller-owned console sink and explicit owner-facing instruction/timeline text.
- No `EngineLogger` API, dependency, lockfile, module edge, renderer behavior, or P3-T05+ input behavior changed.
- Final PR-head workflow #275 passed all five jobs.
- Historical merged-master workflow #276 passed all five jobs on exact merge SHA `61f576f6e23fb6b5518fdeb075db3871fcc5d76e` under the previous routine post-merge policy.
- Issue #151 is closed as completed.

`Wiki impact: none — no public engine API or consumer lifecycle/configuration semantics changed.`

`Sandbox impact: EngineDemoMain and game-sandbox/README.md were the changed owner-facing surface.`

## P1-T08A active maintenance — CI efficiency without reduced merge confidence

Issue #153 is the active bounded maintenance task inserted before P3-T05. It does not change engine/runtime behavior; it changes repository execution policy and GitHub Actions routing to eliminate duplicate heavy CI work.

Target lifecycle for ordinary non-Markdown work:

```text
branch development      -> 0 heavy CI runs
final non-draft PR      -> 1 heavy five-job CI run
merge to master         -> 1 lightweight exact-merge verification job
```

The heavy PR matrix remains unchanged in scope: build/quality gates, unit tests, architecture tests, JaCoCo, and Windows native smoke still run on the exact final PR candidate. Ordinary pushes to `master` no longer need to repeat that complete matrix. Instead, the exact merged `master` SHA gets one lightweight verifier that checks committed dependency locks, the headless-server runtime boundary, and client/server version metadata including exact `engineCommit == github.sha`.

`workflow_dispatch` remains the explicit full-matrix escape hatch for phase/release/evidence tasks or investigations that genuinely require another complete run.

The AI/process policy is also being changed so an agent must finish implementation, focused verification, required docs/wiki/sandbox impact, self-review, and consistency audit on the dedicated branch before opening the final non-draft PR. A correction after failed CI legitimately creates a new candidate; avoidable cosmetic/status commits after a passing candidate are prohibited because they create unnecessary heavy reruns.

Markdown-only complete-diff exemption remains unchanged.

`Wiki impact: none — CI/agent execution policy does not change public engine API usage.`

`Sandbox impact: none — no engine capability changes.`

## Phase 3 status and exact next action

Phase 3 remains in progress. P3-T01 through P3-T04B are complete. P3-T05 / #88 remains paused with no implementation started. The Phase 3 exit remains the backlog requirement that an identical recorded input sequence can be replayed into headless simulation; CI-process work does not complete or replace that gate.

Exact next action for P1-T08A / #153:

1. Finish reconciling workflow/process policy across `AGENTS.md`, GitHub templates, build/verification docs, roadmap/status/orientation docs, and live Issues that hard-code the obsolete routine five-job post-merge requirement.
2. Complete a final consistency/self-review on branch `p1-t08a-ci-efficiency` before opening any PR.
3. Open one final non-draft PR only after the branch is complete.
4. Require the new heavy five-job PR path to pass on the exact final candidate.
5. Merge only while that tested candidate/base relationship is still current.
6. Require the new lightweight `master` verifier to pass on the exact merge SHA; do not rerun the complete five-job matrix by default.
7. Close Issue #153 only after that exact-merge lightweight verification passes.
8. Freshly audit/reactivate P3-T05 / #88 against the resulting verified `master`, recreating/rebasing its stale no-code branch as appropriate before implementation.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input or CI-policy maintenance, but bounded native evidence must not be represented as satisfying them.

## Live-state reconciliation

A fresh agent must read `AGENTS.md` first, inspect local status/HEAD when a checkout exists, inspect live remote `master`, Issue #153, branch `p1-t08a-ci-efficiency`, any open PR, and workflow state, then compare those facts with this checkpoint. P3-T05 / #88 is intentionally paused while #153 is active. If live GitHub state has moved beyond this document, live GitHub controls workflow status. Stop rather than guess if code, docs, the active Issue, PR, or verification evidence conflict.
