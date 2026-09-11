# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `a781c34a7959e207b29058803a2fb74a9cf0d662` — P3-T01 / #84 complete through PR #141 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, Phase 2 exit gate, and P3-T01 |
| Current executable work | None — no Phase 3 implementation Issue is activated at this checkpoint |
| Current task branch | None for roadmap implementation at this checkpoint |
| Current pull request | None for roadmap implementation at this checkpoint |
| Next planned roadmap task | P3-T02 / Issue #85 — planning-only until separately refined and activated |
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

P3-T01 adds the first concrete production platform subsystem in `engine-platform-lwjgl`. It requests explicit OpenGL 4.6 Core GLFW hints, verifies actual OpenGL 4.6 support, logs actual version/renderer through D-028, tracks the window through the caller-owned D-027 registry, and keeps native-bearing lifecycle hooks on the initializing thread.

P3-T01 deliberately does not implement P3-T02+ size/event/fullscreen/input behavior, a renderer loop, buffer swap/polling API, OpenGL debug callbacks, raw handle exposure, multi-window management, or the Phase 3 exit gate.

## Engine API wiki

The repository now maintains an in-repo consumer/API guide under [`../wiki/`](../wiki/README.md). It documents how humans and AI consumers use **implemented** production APIs, with practical examples, lifecycle/ownership rules, and explicit current limitations.

The wiki is deliberately lower authority than scope, accepted decisions, active Issues, code/tests/evidence, this checkpoint, live GitHub state, and the roadmap/backlog. Future tasks that change a public engine API or consumer-visible usage must update the relevant wiki pages in the same PR; tasks with no wiki impact record `Wiki impact: none — <reason>`.

## Phase 3 status

Phase 3 is in progress but only P3-T01 is complete.

P3-T02 / Issue #85 is the next planned roadmap task and remains planning-only at this checkpoint. Before implementation it must be freshly audited, refined into an executable contract, and activated under the normal one-Issue/one-branch/one-PR workflow.

P3-T03 and later Phase 3 tasks remain planning-only. Completing P3-T01 does not complete Phase 3.

## Exact next action

1. Perform a fresh live-state audit from current `master`.
2. Read `AGENTS.md` fully and follow its required document order.
3. Inspect open Issues/PRs and confirm no competing executable task exists.
4. Review P3-T02 / Issue #85 against the now-implemented `GlfwWindow` public boundary and D-031.
5. Refine and activate #85 only if live code/docs/roadmap remain coherent.
6. Create a dedicated P3-T02 branch before any implementation write.
7. Preserve P0-T09A/P0-T13/P0-T14 as independent gates and do not overclaim P3-T01 evidence.
8. When #85 or any later task changes public API/consumer usage, update the relevant `wiki/` pages before handoff.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input work, but Phase 3 evidence must not strengthen those feasibility claims without executing the corresponding gate.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, open Issues, and workflow state with this checkpoint;
4. treat P3-T02 / #85 as planning-only unless live GitHub state explicitly shows it has been refined and activated;
5. preserve P0-T09A/P0-T13/P0-T14 as independent gates;
6. reconcile relevant `wiki/` pages against current production API whenever a task changes consumer-visible behavior;
7. stop if code, docs, wiki, live GitHub state, or an active Issue conflict instead of guessing.
