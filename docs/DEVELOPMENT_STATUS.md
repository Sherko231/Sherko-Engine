# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `534853a334ab52fcbd2f44931343e83f1bdaa096` — P3-T02 merged through PR #146 and verified by merged-master workflow #233 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, Phase 2 exit gate, P3-T01, and P3-T02 |
| Active implementation | P3-T03 / Issue #86 / branch `p3-t03-window-modes` / PR #147 |
| Formal P3-T03 completion authority | Exact final PR-head CI, PR merge, exact merged-`master` push CI, then Issue #86 closure; inspect live GitHub state |
| Next planned roadmap task | P3-T04 / Issue #87 — planning-only until P3-T03 is formally complete and #87 is freshly audited/refined/activated |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## Phase 2 completion

Phase 2 remains complete. P2-T01 through P2-T13 are merged and the D-030 integrated 60-second headless exit gate passed on exact merged `master` with deterministic fixed 60 Hz simulation ticks, bounded catch-up, orderly shutdown, and an empty `NativeResourceRegistry` after cleanup.

The Phase 2 completion evidence remains anchored by P2-EXIT / Issue #135, PR #136, merged `master` `88ddfbd64468ba85d37eb32e1f6d40c9083e1326`, and merged-master workflow #203 / run `34616504486`.

P0-T09A / #42, P0-T13 / #43, and P0-T14 / #44 remain independent feasibility gates; completing Phase 2 did not satisfy them.

## P3-T01 completion — production GLFW/OpenGL window lifecycle

P3-T01 / Issue #84 is complete.

- PR #141 merged to `master` as `a781c34a7959e207b29058803a2fb74a9cf0d662`.
- Final exact-head PR workflow #217 / run `34625988425` passed all five required jobs on head `44cc71c43d2765acafdb5d6bd65460dd5170f260`.
- Merged-master workflow #218 / run `34627138929` passed all five required jobs on exact merge commit `a781c34a7959e207b29058803a2fb74a9cf0d662`.
- Merged-master P3-T01 artifact ID `10274699577`, digest `sha256:2e97e9b595ed9b6b87e42bf31805123c254e76f5e334f485b7c3283440492acc`.
- Native JUnit evidence: one test, zero skipped, zero failures, zero errors.
- Retained report recorded `result=PASS`, requested OpenGL `4.6 Core`, actual OpenGL `4.6`, `GL_VERSION=4.6.0 NVIDIA 592.02`, renderer `NVIDIA GeForce RTX 5090 Laptop GPU/PCIe/SSE2`, Java 25.0.4.1 on Windows 11 amd64, lifecycle cleanup PASS, and an empty native-resource registry after cleanup.
- D-031 records the production `GlfwWindow` ownership/thread-affinity boundary.
- Independent review was not performed because no separate reviewer/person/agent identity was available. CI and self-review are not treated as substitutes; residual risk is the absence of a second independent inspection of the public `GlfwWindow` API and native cleanup boundary.

## P3-T02 completion — logical versus framebuffer sizing

P3-T02 / Issue #85 is complete.

- PR #146 merged to `master` as `534853a334ab52fcbd2f44931343e83f1bdaa096`.
- Final exact-head workflow #232 passed on final PR head `d667fe33544a0d8ec647e19d5fec84526baedd7c` after retrying infrastructure setup failures on the unchanged head.
- Merged-master workflow #233 / run `34637165258` passed all five required jobs on exact merge commit `534853a334ab52fcbd2f44931343e83f1bdaa096`.
- Merged-master P3-T02 artifact ID `10278678473`, digest `sha256:754be3f8d047c12df3587de7666badee0d03740fed6794b1fb8ec2b6506cb707`.
- Native JUnit evidence: one test, zero skipped, zero failures, zero errors.
- Retained report recorded logical `800x600`, framebuffer `800x600`, content scale `1.0/1.0`, exact engine commit `534853a334ab52fcbd2f44931343e83f1bdaa096`, and an empty native-resource registry after cleanup. Equality was the honest observation on the tested 100% DPI environment; deterministic tests separately prove unequal logical/pixel channels.
- D-032 records the renderer-neutral logical/framebuffer event and owner-thread polling contract.
- Issue #85 closed as completed after merged-master verification.
- Independent review was not performed because no separate reviewer/person/agent identity was available; the residual risk was the absence of a second independent inspection of the public API/native callback cleanup boundary.

P3-T02 preserves the original constructor, adds `WindowSizeListener`, adds owner-thread `pollEvents()`, independently queries/stages logical and framebuffer sizes, accepts zero framebuffer axes for minimized state, rejects negative platform dimensions, and explicitly owns callback cleanup. It adds no renderer, fullscreen, input/focus, raw-handle, or later Phase 3 behavior.

## P3-T03 implementation checkpoint — window mode transitions

Issue #86 was freshly audited against live verified `master` `534853a334ab52fcbd2f44931343e83f1bdaa096`, completed P3-T01/P3-T02 behavior, D-031/D-032, the Phase 3 backlog, and the API wiki, then refined from planning-only into an executable contract before code changes began.

PR #147 implements only the bounded P3-T03 contract:

- adds public `WindowMode` values `WINDOWED`, `BORDERLESS_FULLSCREEN`, and `EXCLUSIVE_FULLSCREEN`;
- adds owner-thread `GlfwWindow.setWindowMode(WindowMode)` while STARTED;
- retains the same owned GLFW window/OpenGL context for transitions rather than recreating either;
- captures the current windowed position/logical size before first fullscreen entry and restores it on return to windowed;
- implements primary-monitor borderless fullscreen as an undecorated monitor-detached window positioned/sized to the monitor's current mode;
- implements primary-monitor exclusive fullscreen by attaching the same window to the monitor's current mode/refresh rate;
- preserves captured restore geometry across direct borderless/exclusive transitions and recaptures after a completed return to windowed;
- propagates transition `RuntimeException`/`Error` failures unchanged and performs one best-effort rollback, suppressing rollback failure on the original throwable;
- preserves P3-T02 staged logical/framebuffer delivery through `pollEvents()`;
- adds deterministic transition/failure tests and an opt-in Windows x64 `GlfwWindowModeNativeTest` that executes exactly 20 transitions on the production path;
- adds D-033 and synchronizes the consumer wiki;
- adds no dependency, lockfile/module edge, renderer, monitor-selection/custom-mode API, focus/input behavior, raw handle, or P3-T04+ implementation.

The native acceptance sequence is five repetitions of `BORDERLESS_FULLSCREEN -> WINDOWED -> EXCLUSIVE_FULLSCREEN -> WINDOWED`. After each transition it verifies the original current context remains current, OpenGL remains usable, the native monitor/window state matches the requested mode, logical dimensions remain valid, and every return to windowed restores the captured geometry. The retained report path is `engine-platform-lwjgl/build/reports/p3/p3-t03-window-modes.txt`.

This checkpoint intentionally does not predict whether PR #147 has already merged by the time it is read. Formal P3-T03 completion remains a live workflow fact: require a successful workflow on the exact final PR head, merge PR #147, require the separate push workflow on the exact resulting `master` commit, inspect retained native evidence, then close Issue #86 as completed.

Independent review must be recorded honestly in PR #147. If no separate reviewer/person/agent is available, record `not performed`, the reason, and the residual risk; CI and self-review are not substitutes.

## Engine API wiki

The repository maintains an in-repo consumer/API guide under [`../wiki/`](../wiki/README.md). It documents how humans and AI consumers use implemented production APIs, with practical examples, lifecycle/ownership rules, and explicit current limitations.

P3-T03 changes public API and caller-visible threading/window-mode/failure behavior, so PR #147 synchronizes `WindowMode`, `GlfwWindow.setWindowMode(...)`, primary-monitor policy, geometry restoration, limitations, and examples in the same change. The wiki remains lower authority than scope, accepted decisions, the active Issue, code/tests/evidence, this checkpoint, live GitHub state, and the roadmap/backlog.

## Phase 3 status

Phase 3 is in progress. P3-T01 and P3-T02 are formally complete. P3-T03 is the active bounded task represented by Issue #86 / branch `p3-t03-window-modes` / PR #147 until live completion evidence says otherwise.

P3-T04 / Issue #87 and later Phase 3 tasks remain planning-only. P3-T03 does not implement focus-loss input cleanup, raw mouse, input snapshots/actions/commands, a renderer loop, buffer swapping, or the Phase 3 replay exit gate.

## Exact next action

1. Read `AGENTS.md` fully and inspect live remote `master`, PR #147, Issue #86, and exact-head workflow state.
2. If PR #147 is still open, continue only P3-T03: complete the authorized implementation/docs/wiki audit, require the exact final PR-head workflow to pass, and merge only then.
3. If PR #147 is merged but Issue #86 remains open, verify remote `master` equals the merge result and require the separate push workflow on that exact merged commit to pass before closing #86.
4. Inspect the P3-T03 native artifact/report and record exact tested SHA, transition count, mode sequence, context preservation, geometry restore, environment, and registry cleanup.
5. Record independent-review provenance honestly; if unavailable, record `not performed` plus residual risk.
6. Preserve P0-T09A/P0-T13/P0-T14 as independent gates; P3-T03 does not satisfy or strengthen them.
7. Only after live GitHub confirms #86 is completed, freshly audit/refine/activate P3-T04 / #87. Do not infer activation from task numbering.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input work, but P3-T03 evidence must not strengthen those feasibility claims without executing the corresponding gate.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, PR #147, Issue #86, other open Issues/PRs, and workflow state with this checkpoint;
4. treat this file's P3-T03 branch/PR references as implementation provenance, not proof that the task is still open;
5. keep P3-T04 / #87 planning-only unless live GitHub explicitly shows P3-T03 is formally complete and #87 was separately refined/activated;
6. preserve P0-T09A/P0-T13/P0-T14 as independent gates;
7. reconcile relevant `wiki/` pages against production API whenever a task changes consumer-visible behavior;
8. stop if code, docs, wiki, live GitHub state, or an active Issue conflict instead of guessing.
