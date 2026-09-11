# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `4a757207aa1c3eca5286e0b4a6dbafddea349e05` — engine API wiki/synchronization task merged through PR #145 after P3-T01 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, Phase 2 exit gate, and P3-T01; P3-T02 implementation is present in this checkpoint but formal task closure remains live-state gated |
| P3-T02 implementation vehicle | Issue #85 / branch `p3-t02-window-sizing` / PR #146; branch/PR wording becomes historical after merge |
| Formal P3-T02 completion authority | Exact final PR-head CI, PR merge, exact merged-`master` push CI, then Issue #85 closure; inspect live GitHub state |
| Next planned roadmap task | P3-T03 / Issue #86 — planning-only until live GitHub confirms P3-T02 is formally complete and #86 is freshly refined/activated |
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
- The retained merged-master report records `engine.commit=a781c34a7959e207b29058803a2fb74a9cf0d662`, matching the exact merge commit.
- D-031 records the production `GlfwWindow` ownership/thread-affinity boundary.
- The final diff contained exactly the 14 paths authorized by Issue #84 plus its dependency-lock amendment; no later Phase 3/P5 implementation was included.
- Independent review was not performed because no separate reviewer/person/agent identity was available. CI and self-review are not treated as substitutes; residual risk is the absence of a second independent inspection of the public `GlfwWindow` API and native cleanup boundary.

P3-T01 added the first concrete production platform subsystem in `engine-platform-lwjgl`. It requests explicit OpenGL 4.6 Core GLFW hints, verifies actual OpenGL 4.6 support, logs actual version/renderer through D-028, tracks the window through the caller-owned D-027 registry, and keeps native-bearing lifecycle hooks on the initializing thread.

## P3-T02 implementation checkpoint — logical versus framebuffer sizing

Issue #85 was freshly audited against verified starting `master`, the implemented P3-T01 `GlfwWindow`, D-031, the Phase 3 backlog, module boundaries, and the API wiki, then refined into an executable contract before implementation began.

PR #146 implements only the bounded P3-T02 contract:

- keeps the existing five-argument `GlfwWindow` constructor source-compatible;
- adds renderer-neutral `WindowSizeListener` callbacks for logical window dimensions and framebuffer pixel dimensions;
- adds owner-thread `GlfwWindow.pollEvents()` as the bounded event-polling operation while STARTED;
- stages native GLFW size callbacks and delivers consumer callbacks only after polling returns, avoiding direct consumer execution inside native callbacks;
- queries initial logical and framebuffer sizes independently rather than deriving one from the other;
- permits zero framebuffer axes as valid minimized-window state and rejects negative platform dimensions before public delivery;
- owns the per-window size callback lifecycle inside D-031, with callback release treated as a terminal cleanup attempt to avoid unsafe double-free retries after partial native cleanup failure;
- adds deterministic tests with intentionally different logical/pixel pairs plus a real Windows native acceptance test and retained P3-T02 report;
- adds D-032 for the durable public event/threading contract;
- updates the public API wiki in the same PR;
- adds no dependency, lockfile, module edge, renderer, fullscreen, focus/input, raw-handle, content-scale callback API, or later Phase 3 implementation.

A pre-handoff validation run, workflow #230 / run `34634744872`, passed all five required jobs on implementation head `b6e136b7327f3958db9ca3b52f080b14eab5cdcc`, including the real Windows P3-T02 native sizing acceptance and artifact upload. Because handoff/documentation reconciliation followed that validation, workflow #230 is supporting implementation evidence rather than authority for any later final PR head. Final acceptance always follows the live exact-head/merged-master rules below.

This commit-contained checkpoint intentionally does not predict whether PR #146 has already merged by the time it is read. Formal P3-T02 completion is a live workflow fact: require a successful workflow on the exact final PR head, merge PR #146, require the separate push workflow on the exact resulting `master` commit, then close Issue #85 as completed. Until all of those live steps have happened, P3-T03 / #86 remains planning-only.

## Engine API wiki

The repository maintains an in-repo consumer/API guide under [`../wiki/`](../wiki/README.md). It documents how humans and AI consumers use **implemented** production APIs, with practical examples, lifecycle/ownership rules, and explicit current limitations.

P3-T02 changes public API and caller-visible threading/event behavior, so PR #146 synchronizes `WindowSizeListener`, `GlfwWindow.pollEvents()`, logical-vs-framebuffer semantics, minimized framebuffer behavior, examples, and limitations in the same change. The wiki remains lower authority than scope, accepted decisions, active Issues, code/tests/evidence, this checkpoint, live GitHub state, and the roadmap/backlog.

## Phase 3 status

Phase 3 is in progress. P3-T01 is complete. The P3-T02 implementation is contained in this checkpoint; its formal open/merged/completed workflow state must be read from live PR #146 / Issue #85 rather than inferred from this Markdown file.

P3-T03 / Issue #86 and later Phase 3 tasks remain planning-only until live GitHub confirms P3-T02's exact merged-master verification and Issue #85 closure. P3-T02 does not implement fullscreen, focus/input, raw mouse, input snapshots/actions/commands, a renderer loop, buffer swapping, or the Phase 3 exit gate.

## Exact next action

1. Read `AGENTS.md` fully and inspect live `master`, PR #146, Issue #85, and workflow state.
2. If PR #146 is still open, continue only P3-T02: require the exact final PR-head workflow to pass, complete the final diff/review audit, and merge only then.
3. If PR #146 is merged but Issue #85 is still open, verify remote `master` equals the merge result and require the separate push workflow on that exact merged commit to pass before closing #85.
4. Record independent-review provenance honestly; if no separate reviewer was available, record `not performed`, the reason, and residual risk instead of treating CI/self-review as independent review.
5. Preserve P0-T09A/P0-T13/P0-T14 as independent gates; P3-T02 does not satisfy them.
6. Only after live GitHub confirms #85 is completed, freshly audit/refine/activate P3-T03 / #86. Do not infer activation from task numbering.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input work, but P3-T02 evidence must not strengthen those feasibility claims without executing the corresponding gate.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, PR #146, Issue #85, other open Issues/PRs, and workflow state with this checkpoint;
4. treat this file's P3-T02 branch/PR references as implementation provenance, not proof that the task is still open;
5. keep P3-T03 / #86 planning-only unless live GitHub explicitly shows P3-T02 is formally complete and #86 was separately refined/activated;
6. preserve P0-T09A/P0-T13/P0-T14 as independent gates;
7. reconcile relevant `wiki/` pages against the production API whenever a task changes consumer-visible behavior;
8. stop if code, docs, wiki, live GitHub state, or an active Issue conflict instead of guessing.
