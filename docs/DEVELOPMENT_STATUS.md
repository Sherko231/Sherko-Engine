# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `10721da73553ee03afdce60e6eea1bb642a57880` — P2-T13 / PR #134 merged |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T13 |
| Current executable work | P2-EXIT / Issue #135 — integrated Phase 2 gate |
| Current branch | `p2-exit-60-second-gate` |
| Phase 2 exit gate | 60 continuous seconds of integrated headless fixed 60 Hz ticks, bounded catch-up, orderly shutdown, and verified cleanup; not yet claimed complete in this checkpoint |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T13 completion evidence

P2-T13 / Issue #83 is complete:

- PR #134 merged to `master` as `10721da73553ee03afdce60e6eea1bb642a57880`.
- Final exact-head PR workflow #195 / run `34610085446` passed all five required jobs on head `10d30867d4ae024851c22201253a3782b08129e2`.
- Merged-master workflow #196 / run `34610606050` passed all five required jobs on exact merge commit `10721da73553ee03afdce60e6eea1bb642a57880`.
- Merged-master `engine-subsystem-tests` artifact: ID `10269040322`, digest `sha256:1a2ae917ec7cd203659b7f8c581895b2e98bcaa6e8a58ea7da6e472511946be1`.
- Merged-master `jacoco-reports` artifact: ID `10268376050`, digest `sha256:2ddb014b391505eb5ec8df655997d80070c8e4586922b54688e97135de2659ee`.
- D-029 is accepted: `FatalTermination` performs one-shot synchronous fatal logging, reverse stop/close cleanup, native-resource verification, best-effort cleanup diagnostics, log flush, and then exit status 1.
- Independent review was not performed because no separate reviewer/agent identity was available; CI and author self-review were not represented as independent review.

P2-T01 through P2-T13 are therefore all implemented and merged. Task completion alone does not establish Phase 2 completion.

## P2-EXIT / Issue #135 executable contract

Issue #135 is the sole active Phase 2 executable task. The repository owner deliberately refined the original ten-minute Phase 2 integration duration to **60 continuous seconds**. D-030 records this duration change; all behavioral parts of the gate remain unchanged.

The integrated gate must demonstrate together:

1. the real `EngineClock` sampling elapsed monotonic time;
2. the real `FixedStepAccumulator` executing whole fixed 60 Hz simulation ticks;
3. the real `FixedStepCatchUpPolicy` bounding a deliberately injected long frame/stall;
4. subsystem startup/lifecycle followed by orderly reverse shutdown;
5. owner cleanup closing a registered `NativeResourceRegistry` resource;
6. `NativeResourceRegistry.assertNoOpenResources()` succeeding after cleanup;
7. at least 60 continuous seconds of observed runtime with retained evidence.

The implementation is test/evidence-only in `engine-core`; it does not add a new production API, dependency, module edge, force-close behavior, production thread, or Phase 3 feature. The long-running test is opt-in for ordinary Gradle test runs and is explicitly enabled once in the CI evidence step so routine aggregate/coverage runs do not duplicate the 60-second gate.

The retained report path is:

`engine-core/build/reports/phase2/p2-exit-60-second-gate.txt`

The report must record configured/observed duration, fixed tick rate, executed tick count, loop update count, catch-up cap/observed maximum, injected-stall observation, lifecycle trace, cleanup success, tested commit/environment, and an explicit statement that this is Java headless integration evidence rather than native soak/stability evidence.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- All listed P2 production contracts through P2-T13 are implemented; P2-T11 remains test-only allocation-observability evidence.
- Issue #135 adds only integrated test/evidence, CI retention, and the documentation needed to define the 60-second gate.
- P0-T09A/#42, P0-T13/#43, and P0-T14/#44 remain independent feasibility gates. In particular, P0-T13 still owns the 15-minute native sustained-stability run and P0-T14 still owns 100-cycle/native restartability evidence.

## Exact next action

On `p2-exit-60-second-gate`:

1. finish the opt-in `Phase2IntegratedGateTest` and CI evidence retention exactly to Issue #135;
2. reconcile the canonical Phase 2 exit-gate duration to 60 seconds in backlog/build/architecture/decision/orientation docs without changing the independent P0 gates;
3. audit the complete changed-file set for unrelated scope;
4. open one linked PR and require exact-head CI because Java/workflow files changed;
5. inspect the retained `p2-exit-60-second-gate.txt` evidence and require the observed runtime to be at least 60 seconds, the stall to be bounded to at most five exposed steps, and registry cleanup to pass;
6. merge only after exact-head CI passes, then require a separate passing push CI on the exact merged `master` commit;
7. only after the merged-master gate evidence passes may Issue #135 be closed and Phase 2 be marked complete;
8. then perform a bounded post-Phase-2 handoff/planning review before activating P3-T01.

## Phase 2 status

Completed and merged:

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

Active integration gate:

- P2-EXIT / #135 — 60-second integrated headless gate.

Phase 2 remains **incomplete** in this checkpoint until #135 passes on the exact merged-master commit and its evidence is recorded.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |
| P2-EXIT / #135 | Phase 2 completion | One integrated 60-second headless run proving fixed ticks, bounded catch-up, orderly shutdown, and empty native-resource registry after cleanup. |

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, Issue #135, and workflow state with this checkpoint;
4. verify #135 remains the sole active Phase 2 executable task;
5. require exact-head PR CI and exact merged-master push CI because this task is non-Markdown;
6. inspect the retained Phase 2 gate report rather than inferring success from workflow configuration;
7. preserve P0-T09A/P0-T13/P0-T14 as independent gates;
8. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
