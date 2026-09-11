# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `3dc684ab2b48ab8255d33a5e353cadea59d76979` — post-P2-T10 handoff maintenance PR #127 merged |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T10 |
| Current executable work | P2-T11 / Issue #81 — sampled JFR allocation metric evidence |
| Current branch | `p2-t11-allocation-metric` |
| Next planned roadmap implementation | P2-T12 / Issue #82 — structured runtime logging; planning-only until P2-T11 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T10 completion evidence

P2-T10 / Issue #80 is complete:

- PR #125 merged to `master` as `0625bf2dc2531e21e894bf69f7414c7e17f945e2`.
- Final exact-head PR workflow #188 / run `34598785022` passed all five required jobs on head `9332f963fa21360b501b7b9074ab40c9f3380b8e`.
- Merged-master workflow #189 / run `34599099291` passed all five required jobs on exact merge commit `0625bf2dc2531e21e894bf69f7414c7e17f945e2`.
- Merged-master `engine-subsystem-tests` artifact: ID `10263238589`, digest `sha256:d6aa6fdc8dabe3e493f9fbaf33d599faf00b03c623458b188cc2074ec39fa5fd`.
- Merged-master `jacoco-reports` artifact: ID `10262719107`, digest `sha256:5659587e28729e29a50a95ab0798a879723bcf6cd64e3114af55cb20eaeaeb2a`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and author self-review were not represented as independent review.

Documentation-only Issue #126 / PR #127 then reconciled the post-P2-T10 handoff and merged as `3dc684ab2b48ab8255d33a5e353cadea59d76979`. Its complete diff contained only `README.md` and this file, so the Markdown-only CI exemption applied.

## P2-T11 executable contract

Issue #81 is activated and is the sole executable roadmap task for this branch.

P2-T11 adds no production public API and no dependency. `AllocationMetricBenchmarkTest` uses Java 25 JFR `jdk.ObjectAllocationSample` events and their sampled `weight` field to estimate Java-heap allocation pressure for two controlled channels: `simulation tick` and `render frame`.

Each channel is measured in its own recording window on a uniquely named dedicated platform thread after warm-up. Only samples attributed to that workload thread contribute to the result. The report records iteration count, sample count, sampled weight, measured duration, and estimated bytes per iteration. The render workload is synthetic/headless and must not be presented as OpenGL-renderer evidence.

The benchmark also runs a deliberately allocating control and a nonallocating arithmetic control. The allocating control must produce usable JFR samples; otherwise verification fails instead of fabricating a zero metric. The nonallocating control may legitimately produce zero sampled bytes, which is not proof of mathematical zero allocation.

The retained report path is:

`engine-core/build/reports/allocation/p2-t11-allocation-metric.txt`

The metric is sampled profiling evidence only. It does not measure native/direct/GPU memory, exact allocation per individual frame/tick, retained heap, or GC pause cost, and it establishes no allocation budget.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` production code remains unchanged by P2-T11; lifecycle, timing, configuration, and native-resource ownership contracts through P2-T10 remain intact.
- P2-T11 is test/evidence-only and uses JDK Flight Recorder already selected by `ENGINE_SCOPE.md`.
- No dependency, lockfile, module edge, native binding, protocol, persisted format, or product-scope change is authorized.
- CI retains five Windows x64 self-hosted jobs; P2-T11 is non-exempt because Java test source and workflow YAML change.

## Exact next action

On `p2-t11-allocation-metric`:

1. keep changes limited to the six paths authorized by Issue #81;
2. run the focused `AllocationMetricBenchmarkTest` and confirm it writes the required report;
3. run the existing engine-core regression set plus normal five-job CI on the exact PR head;
4. retain the allocation report in CI artifacts;
5. audit the complete changed-file list against #81;
6. self-review JFR event selection, sampled-weight interpretation, thread attribution, warm-up separation, report labeling, and claim limits;
7. merge only after exact-head PR CI passes;
8. require separate passing push CI on the exact resulting `master` merge commit;
9. record final evidence and close #81 consistently.

Do not activate or implement P2-T12 inside P2-T11.

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

Active:

- P2-T11 / #81 — sampled JFR allocation-observability benchmark/evidence.

Next:

- P2-T12 / #82 — structured runtime logging; planning-only.
- P2-T13 / #83 — orderly fatal termination; planning-only and depends on P2-T10/P2-T12.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated allocation benchmark/test evidence does not satisfy that integrated gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block P2-T11, but its JFR allocation evidence must not strengthen native-stability or restartability claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, active Issues, and workflow state with this checkpoint;
4. verify #81 remains the sole active executable roadmap task;
5. audit all changed paths against #81;
6. require exact-head PR CI and exact merged-master push CI because this task is non-Markdown;
7. keep P2-T12/P2-T13 and later tasks planning-only until activated one at a time;
8. preserve P2's still-unproven ten-minute integrated exit gate and independent native feasibility gates;
9. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
