# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `a7ef3df4b6b03a10534a600de3e4ca3f945080bb` — P2-T11 / PR #128 merged |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T11 |
| Current executable work | none; post-P2-T11 documentation maintenance only |
| Current branch | `maint-reconcile-post-p2-t11` |
| Next planned roadmap implementation | P2-T12 / Issue #82 — structured runtime logging; planning-only until refined and activated |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T11 completion evidence

P2-T11 / Issue #81 is complete:

- PR #128 merged to `master` as `a7ef3df4b6b03a10534a600de3e4ca3f945080bb`.
- Final exact-head PR workflow #191 / run `34602466847` passed all five required jobs on head `9b9f73f101955e0e6fa9079478098baf32a634f5`.
- Merged-master workflow #192 / run `34602702350` passed all five required jobs on exact merge commit `a7ef3df4b6b03a10534a600de3e4ca3f945080bb`.
- Merged-master `engine-subsystem-tests` artifact: ID `10265810108`, digest `sha256:11e5ef2dfbe04b3710ee26f2e62ac43ba1253620130844c4e89e1333032babd6`.
- Merged-master `jacoco-reports` artifact: ID `10265375432`, digest `sha256:8f800eeaf57194e4405a4abbf0ff147105f2bfe697bc10efe473d189c2b7a161`.
- P2-T11 remained benchmark/test-only: no production public API, dependency, module edge, durable decision, or native binding was added.
- Independent review was not mandatory under the activated #81 contract because the task remained test/evidence-only; CI was not represented as independent review.

P2-T11 established a repeatable Java 25 JFR `jdk.ObjectAllocationSample` evidence path and retained report `engine-core/build/reports/allocation/p2-t11-allocation-metric.txt`. The metric is sampled Java-heap allocation pressure only; it is not exact per-call allocation, native/direct/GPU memory evidence, retained-heap evidence, or a product budget.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` production contracts through P2-T10 remain intact; P2-T11 added only test/evidence code and CI/report retention.
- No dependency, lockfile, module edge, native binding, protocol, persisted format, or product-scope change was introduced by P2-T11.
- CI retains five Windows x64 self-hosted jobs.

## Exact next action

After this documentation-only maintenance is merged:

1. perform a fresh startup audit from the resulting `master`;
2. verify no open PR or conflicting active roadmap Issue exists;
3. read Issue #82 plus the required scope/backlog/architecture/decision/build documents;
4. refine #82 into one executable P2-T12 contract before any implementation write;
5. explicitly define the logging API/schema, required/optional fields, missing-field representation, severity model, sink behavior, filtering semantics, flush/error behavior, ownership/threading expectations, authorized files, tests, and dependency policy;
6. keep P2-T13 / #83 planning-only while P2-T12 is activated;
7. create a dedicated P2-T12 branch from verified `master` before any repository write;
8. preserve the still-unproven ten-minute Phase 2 exit gate and independent P0 feasibility gates.

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

Next:

- P2-T12 / #82 — structured runtime logging; planning-only until activated.
- P2-T13 / #83 — orderly fatal termination; planning-only and expected to build on P2-T10/P2-T12.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated task tests/evidence do not satisfy that integrated gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not prevent refining P2-T12, but future logging work must not strengthen unrelated native feasibility claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, active Issues, and workflow state with this checkpoint;
4. verify #82 is the next selected roadmap task and no other executable roadmap task conflicts;
5. activate #82 only after resolving its currently open API/format/failure-policy choices;
6. require normal exact-head PR CI and exact merged-master push CI for any non-Markdown P2-T12 implementation;
7. keep P2-T13 and later tasks planning-only until selected one at a time;
8. preserve P2's still-unproven ten-minute integrated exit gate and independent native feasibility gates;
9. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
