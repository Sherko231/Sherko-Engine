# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `a6e1da1f083ce65ffed6b156a10bf3ecfae23e5b` — maintenance PR #107 merged; merged-master push CI #172 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01, P2-T02, P2-T03, P2-T04 |
| Current executable work | P2-T05 / Issue #75 — fixed-step simulation accumulator |
| Current branch | `p2-t05-fixed-step-accumulator` |
| Next planned roadmap implementation | P2-T06 — frame-gap clamp and catch-up cap; remains planning-only until P2-T05 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect the live branch/HEAD, compare with remote `master`, and inspect GitHub for newer activity.

## Exact next action

Issue #75 is the one active executable roadmap contract. Its API/architecture choices were finalized in the Issue before coding, as required by `AGENTS.md`. The task branch contains the bounded P2-T05 implementation, tests, D-022, architecture/build/orientation updates, and CI evidence wiring.

The next action is to audit the complete branch diff against Issue #75, record review provenance, open the dedicated linked PR, and require a passing exact-head PR workflow before merge. After merge, require a separate passing `master` push workflow on the exact merge commit. Superseded PR runs are obsolete under the repository concurrency policy.

Do not activate or implement P2-T06 until P2-T05 is merged and merged-master verification passes. Do not use P2-T05 unit tests to claim the Phase 2 ten-minute integrated exit gate.

## Maintenance #106 completion evidence

The CI-run authority maintenance task is complete and no longer active:

- Issue #106 is closed as completed.
- PR #107 merged to `master` as `a6e1da1f083ce65ffed6b156a10bf3ecfae23e5b`.
- Superseded PR workflow #170 / run `34584434102` was automatically cancelled after the PR head advanced, demonstrating same-PR `cancel-in-progress` behavior.
- Final exact-head PR workflow #171 / run `34584496552` passed all five required jobs on head `d03f83c22c169e6f96caad6f1aad756bbd3dbf49`.
- Merged-master workflow #172 / run `34584853136` passed all five required jobs on exact merge commit `a6e1da1f083ce65ffed6b156a10bf3ecfae23e5b`.
- Merged-master artifacts: `engine-subsystem-tests` ID `10193233861`, digest `sha256:c5b8a5e5f85ca7c0f9c026c0257c107695510cd63f65afdc8dd5632e5a8323df`; `jacoco-reports` ID `10193211828`, digest `sha256:e4a313fd615b6129d60032591249e3145a4fc72d4aa664f7282b6d1dc9bba940`.
- The workflow now cancels superseded runs using a PR-number/ref concurrency group while preserving exact-head and merged-master evidence requirements.

## Implemented Phase 2 foundation

### P2-T01 / Issue #64 — subsystem lifecycle

`EngineSubsystem` defines one guarded initialize/start/stop/close lifetime with explicit cleanup and externally serialized calls. P2-T01 merged via PR #65; its exact-head and merged-master CI passed.

### P2-T02 / Issue #72 — subsystem dependency ordering

`SubsystemGraph` snapshots registrations, validates duplicate/missing/cyclic dependencies, and returns deterministic dependency-first ordering without invoking lifecycle hooks. P2-T02 merged via PR #103; its exact-head and merged-master CI passed.

### P2-T03 / Issue #73 — partial-startup rollback

`SubsystemStartup` activates an already resolved order and performs deterministic reverse rollback while preserving the original failure and suppressing cleanup failures. P2-T03 merged via PR #104; its exact-head and merged-master CI passed.

### P2-T04 / Issue #74 — monotonic elapsed-time sampling

`EngineClock` samples monotonic elapsed nanoseconds through `System.nanoTime` or an injected `LongSupplier`, preserves the last accepted baseline on rejected/failing samples, and uses ordinary signed `long` subtraction for supported wraparound semantics. P2-T04 merged via PR #105 as `d7464276ce9bf5cf3ef3fb66fe4e1d5ced1781ea`; exact-head workflow #168 and merged-master workflow #169 passed all five required jobs.

## P2-T05 implementation handoff

`com.samo.engine.core.api.FixedStepAccumulator` is implemented on the task branch with the exact public surface authorized by Issue #75:

```java
public final class FixedStepAccumulator {
    public static final int TICKS_PER_SECOND = 60;
    public FixedStepAccumulator();
    public long advance(long elapsedNanos);
}
```

The implemented contract is:

- the fixed rate is exactly 60 ticks per 1,000,000,000 elapsed nanoseconds;
- accepted elapsed input is non-negative `long` nanoseconds, normally supplied by `EngineClock` through the caller;
- cumulative due ticks equal `floor(totalAcceptedElapsedNanos * 60 / 1_000_000_000)`;
- exact fractional progress is retained in integer rational units rather than rounding a simulation step to integer nanoseconds or using floating-point timing;
- quotient/remainder decomposition avoids intermediate overflow even for `Long.MAX_VALUE` elapsed input;
- zero input is valid and preserves fractional progress;
- negative input throws `IllegalArgumentException` before state mutation;
- the accumulator owns only fractional progress; the caller owns tick execution, tick numbering, and cumulative simulation state;
- calls are externally serialized;
- no clock sampling, frame-gap clamping, catch-up cap/backlog dropping, interpolation exposure, pacing, callback ownership, lifecycle integration, or configurable tick rate is included.

`FixedStepAccumulatorTest` provides independent deterministic acceptance coverage for 30/60/144/irregular one-second partitions, equivalent longer totals, sub-tick carry, boundary remainder preservation, zero input, negative rejection without mutation, and `Long.MAX_VALUE` against a `BigInteger` oracle. It contains no sleeps or scheduler-dependent timing.

D-022 records the durable fixed-step accumulation decision. `docs/ARCHITECTURE.md` and `docs/BUILD_AND_VERIFY.md` document the boundary and focused verification. CI extends the existing engine-core focused suite/artifact to retain `FixedStepAccumulatorTest` XML/HTML evidence without weakening any prior job.

No P2-T05 CI pass is claimed by this checkpoint until the actual PR workflow executes. This task changes a public engine API and durable timing decision, so review provenance must be recorded honestly. If no separate reviewer is available, record `not performed`, the reason, and residual arithmetic/API risk; self-review and CI are not substitutes.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- Shared JUnit 6/AssertJ, Checkstyle, JaCoCo, dependency locking, architecture verification, client/server entry points, and native-smoke tasks remain in place.
- CI has five Windows x64 self-hosted jobs: build/quality, unit tests, architecture tests, JaCoCo, and Windows native smoke.
- CI uses workflow concurrency to cancel superseded same-PR runs; only passing exact-head PR CI and the separate exact merged-master push run count as completion evidence.
- `engine-core` contains D-018 `EngineSubsystem`, D-019 `SubsystemGraph`, D-020 `SubsystemStartup`, D-021 `EngineClock`, and on this task branch D-022 `FixedStepAccumulator`.
- No dependency, lockfile, module-edge, native binding, entry-point, protocol, persisted format, product-scope, or backlog definition change is part of P2-T05.

## What remains planned

- P2-T06 frame-gap clamping and catch-up limiting is not implemented.
- P2-T07 render interpolation alpha is not implemented.
- P2-T08 through P2-T13 configuration/ownership/observability/logging/fatal-shutdown work remains planned.
- Concrete renderer, asset, world, physics, audio, networking, runtime UI, editor, and gameplay subsystems remain skeleton/planned work.
- Phase 2 completion remains unproven: the exit gate requires a deterministic fixed-tick headless loop for ten minutes with bounded catch-up and verified cleanup.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block the independent P2-T05 timing work, but their evidence limits must not be strengthened by unrelated unit/CI success.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local `git status --short --branch` and `git rev-parse HEAD` when a local checkout is used;
3. compare remote `master`, open PRs, and active Issues with this checkpoint;
4. confirm Issue #75 remains the one active executable roadmap task until it completes;
5. compare the exact branch diff with Issue #75's allowed file set and public API contract;
6. identify the current PR head SHA before interpreting CI and disregard/cancel obsolete older runs;
7. require exact-head PR CI success before merge and separate exact-merge `master` push CI success afterward;
8. keep P2-T06 and later tasks planning-only until #75 completes;
9. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.

## Maintenance rule

Update this file when P2-T05 final exact-head verification completes and again when merge changes the completed-task/next-action checkpoint. Live GitHub workflow state remains authoritative for activity newer than the containing commit.
