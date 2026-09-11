# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `3bfb4a9cc075c8bc52adee0b11eba937bda4c16f` — P2-T03 PR #104 merged; merged-master push CI #159 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05, P1-T06, P1-T07, P1-T08, P1-T09, P1-T10, P1-T10A, P2-T01, P2-T02, P2-T03 |
| Phase 1 live state | Epic #2 closed as completed after the exit gate passed |
| Current implementation | P2-T04 / Issue #74 — active executable contract for `EngineClock`; no P2-T04 production code is present in this activation checkpoint |
| Next planned implementation | P2-T05 / #75 remains planning-only until P2-T04 is merged and verified |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare with remote `master`, and inspect GitHub for activity newer than this snapshot.

## Exact next action

P2-T01 / #64, P2-T02 / #72, and P2-T03 / #73 are complete. P2-T03 merged through PR #104 as `3bfb4a9cc075c8bc52adee0b11eba937bda4c16f`; exact-head PR workflow #158 and merged-master push workflow #159 passed all five required jobs. Issue #74 is now the one active executable implementation contract.

Before writing P2-T04 code from a local checkout, follow `AGENTS.md`: inspect `git status --short --branch`, `git rev-parse HEAD`, fetch current remote `master`, preserve unrelated/uncommitted user work, and confirm no newer PR or repository activity supersedes this checkpoint. Continue only on the dedicated `p2-t04-engine-clock` branch created from verified master `3bfb4a9cc075c8bc52adee0b11eba937bda4c16f`.

The authorized P2-T04 implementation is limited to the monotonic `EngineClock` contract defined in Issue #74. Do not implement the P2-T05 accumulator, P2-T06 catch-up policy, P2-T07 interpolation, configuration, resource registry, logging, or any later roadmap work. P2 phase exit remains unproven; lifecycle, graph, rollback, and clock unit tests do not by themselves demonstrate the ten-minute integrated headless-loop gate.

## Current repository state

- Gradle Wrapper and Java 25 toolchain configuration are established.
- The repository declares the locked 16 production-target engine/game/support modules plus experimental `feasibility-spikes`.
- The root project remains a build/quality/task aggregator with no Java production source tree.
- Shared JUnit 6/AssertJ support, Checkstyle, JaCoCo, dependency locking, architecture verification, and client/server entry points remain in place.
- Repository CI contains five Windows Java 25 jobs on `[self-hosted, Windows, X64]`: build/quality, unit tests, architecture tests, JaCoCo, and Windows native smoke.
- `master` is currently unprotected with GitHub status-check enforcement off; regardless, `AGENTS.md` still forbids merge before a passing exact-head PR run and requires merged-master push verification unless the owner explicitly grants a separate bounded exception.
- `game-server` remains headless at the dependency/runtime boundary.
- `engine-core` currently contains D-018 `EngineSubsystem`, D-019 `SubsystemGraph`, and D-020 `SubsystemStartup`.
- No `EngineClock` production source exists in this activation checkpoint yet; D-021 must be recorded with the implementation, not pre-claimed by this handoff-only update.

## P2-T03 completion evidence

P2-T03 is complete and no longer active:

- Issue #73 is closed as completed.
- PR #104 merged to `master` as `3bfb4a9cc075c8bc52adee0b11eba937bda4c16f`.
- Final PR-head workflow #158 / run `34580844022` passed all five required jobs.
- Merged-master push workflow #159 / run `34581224884` passed all five required jobs.
- The focused lifecycle/graph/rollback suite passed under Windows x64 / Temurin Java 25.
- PR-head artifacts: `engine-subsystem-tests` ID `10191616722`, digest `sha256:1a7b632233718a4e751bd16d78deec0bceb25fa3acb0cdd38665c359a2f09250`; `jacoco-reports` ID `10191613297`, digest `sha256:8666f82dec7f7cf9ba19e56aa6bda6a4468f5db5fe8f021e041f24599b392578`.
- Merged-master artifacts: `engine-subsystem-tests` ID `10191745792`, digest `sha256:ad8dd85c3b6ccb48955b1e2c5fc2624c5f18da8974903fdcd3f80ef922691e7f`; `jacoco-reports` ID `10191740430`, digest `sha256:983a9158a16b490e39a5854b494a9f0734e28c48e1f2b0d7b6c350da93c69a24`.
- Independent review for P2-T03 was unavailable and was explicitly recorded as `not performed`; author self-review and CI were not represented as substitutes.

## Implemented Phase 2 foundation

### P2-T01 / Issue #64 — single-subsystem lifecycle

`com.samo.engine.core.api.EngineSubsystem` is an abstract `AutoCloseable` base with final guarded `initialize`, `start`, `stop`, and `close` methods around protected hooks. One instance has one lifetime. Invalid/reentrant transitions fail before hooks; unchecked forward-hook failures permit only explicit cleanup. Successful running instances require explicit stop before close. Close is attempted at most once even if its hook throws.

P2-T01 merged via PR #65 as `f84ca1d87292daf2648c9f6dc9371156facdb945`; exact-head PR CI #148 and merged-master push CI #149 passed.

### P2-T02 / Issue #72 — dependency ordering

`SubsystemGraph` snapshots non-owning registrations and returns deterministic dependency-first ordering without invoking lifecycle hooks. It rejects duplicate IDs/instances, missing prerequisites, and cycles; iterative traversal handles deep graphs. The caller retains lifecycle ownership.

P2-T02 merged via PR #103 as `a1b442b0a3e6cb9475d36b0bed28cbb9f60bc2c0`; exact-head PR workflow #156 and merged-master push workflow #157 passed all five jobs.

### P2-T03 / Issue #73 — coordinated startup rollback

`SubsystemStartup.start(List<EngineSubsystem>)` activates an already resolved order one subsystem at a time. On initialize/start failure it preserves the original throwable, closes the failing subsystem, then stops/closes previously started subsystems in reverse order. Cleanup failures are suppressed on the original failure; successful startup retains caller ownership and normal-shutdown responsibility.

This is synthetic Java lifecycle coordination only. It does not establish native restartability, native leak freedom, sustained stability, or P2 phase completion.

## Active P2-T04 contract summary

Issue #74 is the authoritative executable contract. In summary:

- Add final `com.samo.engine.core.api.EngineClock` in `engine-core`.
- Public surface: no-arg constructor, `LongSupplier` constructor, and `long sampleElapsedNanos()`.
- Default source: `System::nanoTime`.
- Construction does not sample. First successful sample establishes the baseline and returns `0`.
- Later samples return incremental nanosecond deltas from the last accepted reading.
- Equal readings are valid; raw readings may be negative because only differences matter.
- A negative signed elapsed difference throws `IllegalStateException` and does not advance the accepted baseline.
- Ordinary Java `long` subtraction intentionally supports forward `System.nanoTime` wraparound for real intervals below `2^63` ns.
- Source `RuntimeException`/`Error` propagates unchanged and does not advance state.
- Each sample reads the source exactly once.
- No floating-point time, fixed-step accumulator, frame clamp, catch-up cap, interpolation, sleep/pacing, lifecycle integration, or later timing task is included.

The implementation must derive tests from the hand-written sequences and failure cases in Issue #74, record D-021 with the actual implementation, update applicable architecture/build/status docs, and pass exact-head PR plus merged-master CI.

## What remains skeleton or planned

- Concrete renderer, asset, world, physics, audio, networking, runtime UI, editor, and gameplay subsystems remain skeleton/planned work.
- Client/server entry points remain intentionally minimal foundation composition roots and do not instantiate production subsystems yet.
- `feasibility-spikes` remains disposable experimental evidence code.
- Phase 2 has completed lifecycle, dependency ordering, and startup rollback. `EngineClock` is active but not yet implemented in this checkpoint. Fixed-step loop/catch-up/interpolation, configuration, ownership registry, allocation metrics, logging/fatal-shutdown work, and the phase exit gate remain unimplemented.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block independent P2-T04 clock work, but their evidence must not be assumed or strengthened by it.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local `git status --short --branch` and `git rev-parse HEAD`, remote `master`, open PRs, and active Issues;
3. verify `master` has not advanced beyond `3bfb4a9cc075c8bc52adee0b11eba937bda4c16f` without reconciling this branch;
4. confirm Issue #74 remains the one active executable P2 contract and #75 remains planning-only;
5. preserve P0 #42–#44 evidence limits;
6. confirm a matching Windows x64 runner is available before interpreting queued CI as execution evidence;
7. stop if code, docs, GitHub state, or Issue #74 conflict instead of guessing.

## Maintenance rule

Update this file when P2-T04 implementation changes the actual repository maturity, when verification completes, and again when merge changes the completed-task/next-action checkpoint. Live GitHub workflow state remains authoritative for activity newer than the containing commit.