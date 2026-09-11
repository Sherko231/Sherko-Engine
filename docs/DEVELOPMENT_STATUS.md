# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `88ddfbd64468ba85d37eb32e1f6d40c9083e1326` — P2-EXIT / PR #136 merged |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, plus the Phase 2 exit gate |
| Current executable work | None; post-Phase-2 handoff only |
| Checkpoint source branch | `maint-phase2-complete-phase3-handoff` |
| Next planned roadmap task | P3-T01 — GLFW window creation with explicit OpenGL version/profile hints; planning-only until separately refined/activated |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing. Do not create recursive handoff maintenance solely because this Markdown checkpoint cannot name its own merge SHA.

## Phase 2 completion evidence

Phase 2 is complete.

P2-EXIT / Issue #135 closed the final integration gate after P2-T01 through P2-T13 were already merged:

- PR #136 merged to `master` as `88ddfbd64468ba85d37eb32e1f6d40c9083e1326`.
- Final exact-head PR workflow #202 / run `34614929190` passed all five required jobs on head `ac5c1ea9e53cf61f8546889af3e03c9633ad1e1c`.
- PR `engine-subsystem-tests` artifact: ID `10270088955`, digest `sha256:2234f307325b808ed4eea92efc8f43b9a036d5eb9458f7fca128b100529bf801`.
- Merged-master workflow #203 / run `34616504486` passed all five required jobs on exact merge commit `88ddfbd64468ba85d37eb32e1f6d40c9083e1326`.
- Merged-master `engine-subsystem-tests` artifact: ID `10271245995`, digest `sha256:22bd320a1b0e5d6e3311b54ab600b050ab82fa05f009440fec3b43148407c1dd`.
- Merged-master `jacoco-reports` artifact: ID `10270416180`, digest `sha256:80ae054da125441c283d268ee229fbf0513f0c5ea8f86c968bd7c45c68ceadaa`.
- Retained merged-master report `engine-core/build/reports/phase2/p2-exit-60-second-gate.txt` recorded `result=PASS` on the exact merge SHA.
- Configured duration: 60 s; observed duration: 60.010 s.
- Fixed rate: 60 Hz, integer simulation steps only.
- Executed fixed ticks: 3484; loop updates: 3708.
- Catch-up cap configured/observed: 5 / 5.
- Injected stall: 2000 ms requested, `2014316400` ns observed, 5 steps exposed.
- Lifecycle trace: `initialize,start,stop,close,resource-close`.
- `NativeResourceRegistry` empty after owner cleanup: true.
- Merged-master JUnit evidence: 1 test, 0 skipped, 0 failures, 0 errors; 60.019 s.
- Environment recorded by the retained report: Java `25.0.4.1`, Windows 11 amd64.
- D-030 remains scoped to Phase 2 integration correctness and does not replace P0-T13/P0-T14.
- Independent review was not performed because no separate reviewer/agent identity was available; CI and author self-review were not represented as independent review.

## Completed Phase 2 implementation

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock` elapsed-nanosecond sampling.
- P2-T05 / #75 — exact fixed-step accumulator at 60 Hz.
- P2-T06 / #76 — bounded frame-gap and catch-up policy.
- P2-T07 / #77 — renderer-facing interpolation alpha separated from whole simulation ticks.
- P2-T08 / #78 — typed configuration keys/defaults/bounds/source-aware validation.
- P2-T09 / #79 — fixed-precedence layered configuration loading.
- P2-T10 / #80 — explicit native-resource registry and shutdown leak diagnostics.
- P2-T11 / #81 — sampled JFR allocation-observability benchmark/evidence.
- P2-T12 / #82 — synchronous structured logging boundary.
- P2-T13 / #83 — orderly fatal termination.
- P2 exit / #135 — D-030 60-second integrated headless gate passed on exact merged `master`.

## Post-Phase-2 planning review

Phase 3 assumptions remain compatible with the completed Phase 2 foundation:

- the 60 Hz timing/lifecycle/resource contracts needed by later platform/input integration are available and verified;
- `engine-platform-lwjgl` already exists as the intended platform adapter module and still depends only on `engine-core`;
- Phase 3 remains bounded to stable platform events and tick-aligned player commands; no Phase 2 result requires changing its scope or exit gate;
- the existing P3-T01 backlog acceptance still makes sense as the first bounded implementation step: create `GlfwWindow` with explicit OpenGL version/profile hints and verify the actual GL version/renderer string is logged;
- P3-T01 is not activated by this maintenance. It must receive its own executable Issue/contract, dedicated branch, PR, verification, and any required architecture/review work before coding.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- Phase 1 and Phase 2 are complete.
- M1 remains incomplete because Phase 3 and Phase 4 are still ahead.
- P0-T09A/#42, P0-T13/#43, and P0-T14/#44 remain independent feasibility gates. In particular, Phase 2 completion is not evidence of sustained native stability, repeated native restartability, or end-to-end Steam transport.

## Exact next action

1. Fresh-audit `master`, open Issues/PRs, and current documents.
2. Refine/activate only P3-T01 as the next executable roadmap Issue; do not materialize all of Phase 3 at once.
3. Resolve its concrete API/ownership/failure/test contract before coding, including GLFW/OpenGL context hints, actual-version logging evidence, and cleanup/thread-affinity expectations.
4. Create its dedicated branch from exact current `master` before any repository write.
5. Keep P3-T02 and later Phase 3 tasks planning-only until P3-T01 completes.

This maintenance itself requires no further handoff-only maintenance solely to record its eventual merge SHA.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks beginning ordinary Phase 3 platform/input work, but they continue to limit later claims and phases exactly as documented.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, live Issues, and workflow state with this checkpoint;
4. verify there is no already-active executable Phase 3 Issue before creating another;
5. preserve P0-T09A/P0-T13/P0-T14 as independent gates;
6. treat P3-T01 as planning-only unless live GitHub state explicitly activates/refines it;
7. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
