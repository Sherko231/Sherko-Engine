# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `46df2af471915e091061f5b9efeb3b00b100a068` — PR #114 repaired the accidental direct-master README edit through the Markdown-only exemption |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T05 |
| Current executable work | P2-T06 / Issue #76 — bounded frame gaps and catch-up work |
| Current branch | `p2-t06-catch-up-policy` |
| Next planned roadmap implementation | P2-T07 — render interpolation alpha; remains planning-only until P2-T06 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect the live branch/HEAD, compare with remote `master`, and inspect GitHub for newer activity.

## Exact next action

Issue #76 is the active executable contract. Its API and durable policy choices were finalized before coding. The task branch now contains:

- `FixedStepCatchUpPolicy` in `engine-core`;
- focused `FixedStepCatchUpPolicyTest` coverage;
- D-023;
- architecture, build/verification, README, CI-evidence, and status updates.

The next action is to audit the complete branch diff against Issue #76, perform self-review and record independent-review provenance honestly, open the linked PR, and require passing exact-head PR CI because this change contains Java/YAML. After merge, require a separate passing `master` push workflow on the exact merge commit.

Do not activate P2-T07 while P2-T06 remains unmerged/unverified. Do not use isolated P2-T06 tests to claim the ten-minute Phase 2 exit gate.

## P2-T06 implementation handoff

`com.samo.engine.core.api.FixedStepCatchUpPolicy` is implemented with the public surface authorized by Issue #76:

```java
public final class FixedStepCatchUpPolicy {
    public static final long DEFAULT_MAX_FRAME_GAP_NANOS = 250_000_000L;
    public static final int DEFAULT_MAX_STEPS_PER_UPDATE = 5;

    public FixedStepCatchUpPolicy();
    public FixedStepCatchUpPolicy(long maxFrameGapNanos, int maxStepsPerUpdate);

    public long advance(FixedStepAccumulator accumulator, long elapsedNanos);
}
```

The default behavior clamps one elapsed duration to 250 ms and exposes at most 5 whole simulation steps. Elapsed time above the clamp and whole due steps above the cap are deliberately discarded. Fractional sub-tick progress from the accepted elapsed duration remains inside the caller-owned `FixedStepAccumulator`.

A deterministic two-second stall therefore returns exactly 5 steps: the policy clamps 2 seconds to 250 ms, the 60 Hz accumulator makes 15 whole ticks due, and the policy drops 10 instead of carrying them as future backlog.

The explicit constructor accepts strictly positive limits only. Null accumulator and negative elapsed input are rejected before accumulator mutation. Zero elapsed is valid. `FixedStepAccumulator`, `EngineClock`, lifecycle APIs, tick execution, pacing, interpolation, configuration loading, and callback ownership remain unchanged.

`FixedStepCatchUpPolicyTest` covers default constants, frame-gap and step-cap boundaries, the 2-second-stall case, dropped-backlog behavior, clamp discard behavior, fractional remainder preservation, zero input, negative input, null accumulator, and invalid limits using integer-nanosecond expectations only.

D-023 records the durable policy. No dependency, lockfile, module-edge, native binding, protocol, persisted-format, or product-scope change is part of P2-T06.

## Recent maintenance evidence

### Markdown-only CI policy — Issue #111

Issue #111 completed through PR #112 and merged as `3293ad34cbbc6ccade3baa82a350b2af32ea23d2`. PR workflow #177 and merged-master workflow #178 both passed all five jobs because that policy task itself changed workflow YAML. After that merge, future complete diffs containing only `.md` files are exempt from automatic PR-head and merged-master build/test CI after a complete changed-file audit.

### Accidental direct-master README repair — Issue #113

A direct README write accidentally created `baaa675041d2d00b3ace681f8a21c6aacdce2805` on `master`, violating the branch/PR rule. Work stopped immediately. Maintenance #113 restored README exactly through dedicated PR #114. The repair diff contained only `README.md`, so no automatic CI was required under the current Markdown-only policy. The repaired master baseline for P2-T06 is `46df2af471915e091061f5b9efeb3b00b100a068`.

This incident does not authorize direct-master writes; all subsequent P2-T06 work is on the dedicated task branch.

## P2-T05 completion evidence

P2-T05 / Issue #75 is complete:

- PR #108 merged to `master` as `bf6863f327496889c2857f33865d2a9df360fa3d`.
- Final exact-head PR workflow #173 / run `34586031161` passed all five required jobs on head `d5f5ec4c7f65d25a53794a2b647e456aca26c92b`.
- Merged-master workflow #174 / run `34586538003` passed all five required jobs on exact merge commit `bf6863f327496889c2857f33865d2a9df360fa3d`.
- Merged-master artifacts: `engine-subsystem-tests` ID `10193887501`, digest `sha256:0a7ae9cd28e28e261fb8cd0bd438ddf12aab24da149f6b93e1f20b52716ddece`; `jacoco-reports` ID `10193870940`, digest `sha256:f4cb97c85a282947f2a265a673f0f87ece353b69e9e8b922ddb22d81f96007b6`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and self-review were not represented as independent review.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` now contains lifecycle/order/startup rollback, monotonic elapsed sampling, exact 60 Hz accumulation, and the P2-T06 bounded catch-up policy on the task branch.
- Shared JUnit 6/AssertJ, Checkstyle, JaCoCo, dependency locking, architecture verification, client/server entry points, and native-smoke tasks remain in place.
- CI retains five Windows x64 self-hosted jobs and same-PR concurrency cancellation; the focused engine-core evidence suite now includes `FixedStepCatchUpPolicyTest` on this task branch.

## Phase 2 status

Completed and merged:

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock` elapsed-nanosecond sampling.
- P2-T05 / #75 — exact fixed-step accumulator at 60 Hz.

Active:

- P2-T06 / #76 — clamp frame gaps and cap catch-up steps.

Next after P2-T06:

- P2-T07 — expose render interpolation alpha separately from simulation delta; remains planning-only.

P2 phase completion is not claimed. The existing exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2-T01 through P2-T06 tests do not satisfy that gate.

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
4. verify Issue #76 remains the one active executable roadmap task;
5. audit the complete changed-file list against #76's authorized paths;
6. because P2-T06 contains non-Markdown files, require exact-head PR CI and exact merged-master CI;
7. keep P2-T07 and later tasks planning-only until activated one at a time;
8. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.
