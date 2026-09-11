# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `bf6863f327496889c2857f33865d2a9df360fa3d` — P2-T05 PR #108 merged; merged-master push CI #174 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01, P2-T02, P2-T03, P2-T04, P2-T05 |
| Current executable work | Maintenance Issue #109 — reconcile post-P2-T05 handoff status |
| Current branch | `maint-post-p2-t05-handoff` |
| Next planned roadmap implementation | P2-T06 / #76 — frame-gap clamp and catch-up cap; refine and activate only after #109 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect the live branch/HEAD, compare with remote `master`, and inspect GitHub for newer activity.

## Exact next action

Issue #109 is the only active executable change in this checkpoint. It is documentation-only maintenance to reconcile stale repository orientation after P2-T05 completed. Its authorized file set is only `README.md` and this `docs/DEVELOPMENT_STATUS.md`.

Do not activate or implement P2-T06 under #109. After this maintenance PR merges and its exact merged-`master` CI passes, P2-T06 / #76 is the next roadmap task to refine into an executable contract, following the normal `AGENTS.md` startup procedure.

## P2-T05 completion evidence

P2-T05 / Issue #75 is complete:

- PR #108 merged to `master` as `bf6863f327496889c2857f33865d2a9df360fa3d`.
- Final exact-head PR workflow #173 / run `34586031161` passed all five required jobs on head `d5f5ec4c7f65d25a53794a2b647e456aca26c92b`.
- Merged-master workflow #174 / run `34586538003` passed all five required jobs on exact merge commit `bf6863f327496889c2857f33865d2a9df360fa3d`.
- Merged-master artifacts: `engine-subsystem-tests` ID `10193887501`, digest `sha256:0a7ae9cd28e28e261fb8cd0bd438ddf12aab24da149f6b93e1f20b52716ddece`; `jacoco-reports` ID `10193870940`, digest `sha256:f4cb97c85a282947f2a265a673f0f87ece353b69e9e8b922ddb22d81f96007b6`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and self-review were not represented as independent review.
- No required check was skipped.

`engine-core` now contains the implemented Phase 2 foundation through D-022: `EngineSubsystem`, `SubsystemGraph`, `SubsystemStartup`, `EngineClock`, and `FixedStepAccumulator`.

## Implemented Phase 2 foundation

### P2-T01 / Issue #64 — subsystem lifecycle

`EngineSubsystem` defines one guarded initialize/start/stop/close lifetime with explicit cleanup and externally serialized calls.

### P2-T02 / Issue #72 — subsystem dependency ordering

`SubsystemGraph` snapshots registrations, validates duplicate/missing/cyclic dependencies, and returns deterministic dependency-first ordering without invoking lifecycle hooks.

### P2-T03 / Issue #73 — partial-startup rollback

`SubsystemStartup` activates an already resolved order and performs deterministic reverse rollback while preserving the original failure and suppressing cleanup failures.

### P2-T04 / Issue #74 — monotonic elapsed-time sampling

`EngineClock` samples monotonic elapsed nanoseconds through `System.nanoTime` or an injected `LongSupplier`, preserving deterministic elapsed-time semantics without owning fixed-step or pacing policy.

### P2-T05 / Issue #75 — exact 60 Hz fixed-step accumulation

`FixedStepAccumulator` converts non-negative elapsed nanoseconds into newly due whole 60 Hz simulation ticks while retaining exact fractional progress with integer/rational arithmetic. Cadence-partition tests cover 30/60/144/irregular frame sequences, edge cases, rejected negative input, and `Long.MAX_VALUE` overflow safety.

P2-T05 intentionally does not implement frame-gap clamping, catch-up limits/backlog policy, render interpolation, pacing, or the integrated runtime loop. Those remain later tasks.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- Shared JUnit 6/AssertJ, Checkstyle, JaCoCo, dependency locking, architecture verification, client/server entry points, and native-smoke tasks remain in place.
- CI has five Windows x64 self-hosted jobs: build/quality, unit tests, architecture tests, JaCoCo, and Windows native smoke.
- CI uses workflow concurrency to cancel superseded same-PR runs; only passing exact-head PR CI and the separate exact merged-master push run count as completion evidence.
- No dependency, lockfile, module-edge, native binding, entry-point, protocol, persisted-format, product-scope, roadmap, or architecture-decision change is part of maintenance #109.

## Phase 2 status

Completed:

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock` elapsed-nanosecond sampling.
- P2-T05 / #75 — exact fixed-step accumulator at 60 Hz.

Next planned roadmap item after maintenance #109:

- P2-T06 / #76 — bound frame gaps and catch-up work. Its existing Issue is still a planning contract and must be refined/activated before coding.

Later P2 timing/configuration/ownership/observability/logging/fatal-shutdown work remains planned.

P2 phase completion is not claimed. The existing exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2-T01 through P2-T05 tests do not satisfy that gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block the independent Phase 2 timing work, but their evidence limits must not be strengthened by unrelated unit/CI success.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local `git status --short --branch` and `git rev-parse HEAD` when a local checkout is used;
3. compare remote `master`, open PRs, and active Issues with this checkpoint;
4. confirm maintenance #109 has completed before activating P2-T06 / #76;
5. if #76 is selected next, refine its unresolved clamp/catch-up API and behavior choices in the Issue before coding;
6. identify the current PR head SHA before interpreting CI and disregard/cancel obsolete older runs;
7. require exact-head PR CI success before merge and separate exact-merge `master` push CI success afterward;
8. keep P2-T07 and later tasks planning-only until activated one at a time;
9. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.

## Maintenance rule

After maintenance #109 merges and merged-master CI passes, live GitHub state will be newer than this containing commit. The next agent must reconcile that live state before activating P2-T06; do not create another handoff-only maintenance task merely because this document correctly describes its containing maintenance checkpoint.
