# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `ac1284bdff39dd7816402fb4ee0560721f5dc295` — Phase 3 readiness maintenance #139 / PR #140 complete |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13, plus the Phase 2 exit gate |
| Current executable work | P3-T01 / Issue #84 — production GLFW window with explicit OpenGL 4.6 Core hints |
| Current task branch | `p3-t01-glfw-window` |
| Current pull request | PR #141 — draft/non-closing while implementation and verification continue |
| Next planned roadmap task | P3-T02 / Issue #85 — planning-only until P3-T01 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## Phase 2 completion evidence

Phase 2 is complete. P2-EXIT / Issue #135 closed the final integration gate after P2-T01 through P2-T13 were already merged:

- PR #136 merged to `master` as `88ddfbd64468ba85d37eb32e1f6d40c9083e1326`.
- Final exact-head PR workflow #202 / run `34614929190` passed all five required jobs on head `ac5c1ea9e53cf61f8546889af3e03c9633ad1e1c`.
- Merged-master workflow #203 / run `34616504486` passed all five required jobs on exact merge commit `88ddfbd64468ba85d37eb32e1f6d40c9083e1326`.
- Retained merged-master report `engine-core/build/reports/phase2/p2-exit-60-second-gate.txt` recorded `result=PASS` for 60.010 seconds with fixed 60 Hz ticks, catch-up bounded to five steps, orderly lifecycle shutdown, and an empty `NativeResourceRegistry` after cleanup.
- D-030 remains scoped to Phase 2 integration correctness and does not replace P0-T13/P0-T14.

Completed Phase 2 implementation remains:

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock`.
- P2-T05 / #75 — exact fixed-step accumulator at 60 Hz.
- P2-T06 / #76 — bounded frame-gap/catch-up policy.
- P2-T07 / #77 — renderer-facing interpolation alpha.
- P2-T08 / #78 — typed startup configuration.
- P2-T09 / #79 — layered configuration loading.
- P2-T10 / #80 — explicit native-resource registry.
- P2-T11 / #81 — sampled JFR allocation-observability evidence.
- P2-T12 / #82 — synchronous structured logging.
- P2-T13 / #83 — orderly fatal termination.
- P2 exit / #135 — D-030 integrated 60-second gate.

## Phase 3 activation

The post-Phase-2 planning review remains valid: the completed lifecycle/timing/resource contracts support Phase 3, `engine-platform-lwjgl` is the intended platform adapter, and none of P0-T09A/P0-T13/P0-T14 blocks ordinary platform/input work.

P3-T01 / Issue #84 is now explicitly activated as the sole executable Phase 3 task. Its verified activation baseline is `master` `ac1284bdff39dd7816402fb4ee0560721f5dc295`, with no competing open pull request at activation.

The bounded P3-T01 implementation contract adds exactly one public production type, `com.samo.engine.platform.api.GlfwWindow`, as a D-018 subsystem. It requests OpenGL 4.6 Core through explicit GLFW hints, verifies actual OpenGL 4.6 support, logs actual version/renderer through D-028, tracks the GLFW window through the caller-owned D-027 registry, and keeps all native lifecycle work on the initializing thread.

The already-selected LWJGL 3.4.3 core/GLFW/OpenGL dependencies are being placed in `engine-platform-lwjgl`; no new technology choice, LWJGL version, or repository project edge is introduced. D-031 records the production platform ownership/thread-affinity boundary.

P3-T01 deliberately does not implement P3-T02+ window-size/events/fullscreen/input behavior, a renderer loop, buffer swap/polling API, GL debug callbacks, raw handle exposure, multi-window management, or the Phase 3 replay exit gate.

## Current verification state

The implementation PR is non-exempt because Java, Gradle, lockfile, and workflow files change. Intermediate PR CI may run while the branch is still changing; only a passing workflow on the final exact PR head can authorize merge.

Required final evidence includes:

- deterministic `GlfwWindowTest` on Java 25;
- real Windows x64 `GlfwWindowNativeTest` using the public production constructor;
- retained `engine-platform-lwjgl/build/reports/p3/p3-t01-glfw-window.txt` proving requested OpenGL 4.6 Core, independently observed actual version/renderer, structured-log equality, orderly cleanup, no current context after stop/close, and an empty native-resource registry;
- routine build/test/architecture/coverage/dependency-lock checks;
- exact-head PR CI, then a separate exact merged-`master` push CI;
- independent review of the public API/D-031 if a separate reviewer is available, otherwise honest `not performed` provenance and residual risk.

The native acceptance is one production window/context lifecycle run. It does not satisfy P0-T13 sustained native stability or P0-T14 repeated native restartability.

## Exact next action

On `p3-t01-glfw-window` / PR #141:

1. finish the eleven paths authorized by Issue #84 only;
2. run/inspect deterministic and real Windows native acceptance evidence;
3. audit the complete PR changed-file set against #84 and remove any unrelated drift;
4. self-review lifecycle/thread affinity, partial-failure cleanup, callback ownership, registry ownership, actual-version logging, dependency locking, and absence of later-phase behavior;
5. record independent-review provenance honestly;
6. mark the PR ready only when the final exact-head five-job CI passes and the retained P3-T01 report is valid;
7. merge using `Refs #84`, not an auto-closing keyword;
8. require a separate passing push CI on the exact merge commit and inspect the retained native report;
9. manually close Issue #84 only after merged-master evidence passes;
10. keep P3-T02 and later tasks planning-only.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks P3-T01, but P3-T01 evidence must not strengthen those claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, branch head, PR #141, Issue #84, and workflow state with this checkpoint;
4. verify #84 remains the sole executable Phase 3 task;
5. preserve P0-T09A/P0-T13/P0-T14 as independent gates;
6. require exact-head and exact merged-master CI because P3-T01 is non-exempt;
7. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
