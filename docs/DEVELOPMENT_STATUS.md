# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `36f612259d29829971302c6f3df2e0a8c1d1f2bf` — P2-T06 / PR #115 merged; merged-master workflow #180 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T06 |
| Current executable work | P2-T07 / Issue #77 — renderer-facing interpolation alpha |
| Current branch | `p2-t07-interpolation-alpha` |
| Next planned roadmap implementation | P2-T08 — typed configuration keys; remains planning-only until P2-T07 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect the live branch/HEAD, compare with remote `master`, and inspect GitHub for newer activity.

## Exact next action

Issue #77 is the active executable contract. Its public API and durable interpolation semantics were finalized before coding. The task branch contains:

- `FixedStepAccumulator.interpolationAlpha()` as the only new public API;
- `FixedStepInterpolationTest` with deterministic JUnit 6 coverage;
- D-024;
- architecture, build/verification, README, CI-evidence, and status updates.

The next action is to audit the complete branch diff against Issue #77, perform author self-review, record independent-review provenance honestly, open the linked PR, and require passing exact-head PR CI because the branch contains Java/YAML. After merge, require a separate successful `master` push workflow on the exact merge commit.

Do not activate P2-T08 while P2-T07 remains unmerged/unverified. Do not use isolated P2-T07 tests to claim the ten-minute Phase 2 exit gate.

## P2-T07 implementation handoff

`FixedStepAccumulator` still owns exact integer/rational 60 Hz progression. P2-T07 adds only:

```java
public double interpolationAlpha();
```

The method returns the retained fractional progress normalized to `[0.0, 1.0)` using the existing exact remainder divided by one billion. It is read-only and does not consume progress.

Simulation and rendering remain separate:

1. elapsed time is sampled by `EngineClock`;
2. `FixedStepCatchUpPolicy` advances the caller-owned accumulator with bounded elapsed/catch-up work;
3. the caller executes only the returned whole fixed simulation ticks;
4. rendering may query `interpolationAlpha()` separately.

Examples established by the contract/tests:

- fresh or exact whole-tick state -> alpha `0.0`;
- 25,000,000 ns -> one whole tick due plus alpha `0.5`;
- 12,500,000 ns -> alpha `0.75`;
- 125,000,000 ns through the default catch-up policy -> 5 executable ticks, dropped extra whole ticks, retained alpha `0.5`;
- elapsed discarded by a frame-gap clamp does not inflate alpha.

Floating point is presentation-only. `advance(long)`, `FixedStepCatchUpPolicy`, `EngineClock`, tick rate, lifecycle APIs, dependencies, module edges, native bindings, protocols, and persisted formats remain unchanged.

## P2-T06 completion evidence

P2-T06 / Issue #76 is complete:

- PR #115 merged to `master` as `36f612259d29829971302c6f3df2e0a8c1d1f2bf`.
- Final exact-head PR workflow #179 / run `34590118120` passed all five required jobs on head `b4965619daa336b3136ce091c0a1ab127db72d00`.
- Merged-master workflow #180 / run `34590462161` passed all five required jobs on the exact merge commit.
- Merged-master `engine-subsystem-tests` artifact: ID `10195447609`, digest `sha256:c65459c32834207a95841ea8e56b7a82ca11b3fe2c3f831e5d3acbd85ebbeae4`.
- Merged-master `jacoco-reports` artifact: ID `10195430483`, digest `sha256:ce2f23796734528ae5a5532c9cb84cdaaf83b63abf14af6189507632e7f41cab`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and self-review were not represented as independent review.

## Repository maintenance policy

Markdown-only changes are exempt from automatic PR-head and merged-master build/test CI only when the complete non-empty diff contains exclusively paths ending in `.md`. Any Java, YAML, Gradle, lockfile, configuration, script, resource, or other non-Markdown path restores the normal exact-head PR CI and exact merged-master CI requirements.

This P2-T07 task is non-exempt because it changes Java source/tests and workflow YAML.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` contains lifecycle/order/startup rollback, monotonic elapsed sampling, exact 60 Hz accumulation, bounded catch-up policy, and on this task branch renderer-facing interpolation alpha.
- Shared JUnit 6/AssertJ, Checkstyle, JaCoCo, dependency locking, architecture verification, client/server entry points, and native-smoke tasks remain in place.
- CI retains five Windows x64 self-hosted jobs and same-PR concurrency cancellation; the focused engine-core evidence suite includes `FixedStepInterpolationTest` on this branch.

## Phase 2 status

Completed and merged:

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock` elapsed-nanosecond sampling.
- P2-T05 / #75 — exact fixed-step accumulator at 60 Hz.
- P2-T06 / #76 — bounded frame-gap and catch-up policy.

Active:

- P2-T07 / #77 — expose render interpolation alpha separately from simulation progression.

Next after P2-T07:

- P2-T08 — typed configuration keys with defaults, bounds, source location, and validation errors; remains planning-only.

P2 phase completion is not claimed. The existing exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2-T01 through P2-T07 tests do not satisfy that gate.

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
4. verify Issue #77 remains the one active executable roadmap task;
5. audit the complete changed-file list against #77's authorized paths;
6. require exact-head PR CI and exact merged-master CI because P2-T07 contains non-Markdown files;
7. keep P2-T08 and later tasks planning-only until activated one at a time;
8. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.
