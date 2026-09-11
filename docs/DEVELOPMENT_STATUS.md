# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `e318a4f3bd6392e1b24c883b7e1bd10532edbfda` — post-P2-T11 handoff maintenance PR #130 merged |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T11 |
| Current executable work | P2-T12 / Issue #82 — structured runtime logging |
| Current branch | `p2-t12-structured-logging` |
| Next planned roadmap implementation | P2-T13 / Issue #83 — orderly fatal termination; planning-only until P2-T12 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T11 completion and handoff evidence

P2-T11 / Issue #81 is complete:

- PR #128 merged to `master` as `a7ef3df4b6b03a10534a600de3e4ca3f945080bb`.
- Final exact-head PR workflow #191 / run `34602466847` passed all five required jobs on head `9b9f73f101955e0e6fa9079478098baf32a634f5`.
- Merged-master workflow #192 / run `34602702350` passed all five required jobs on exact merge commit `a7ef3df4b6b03a10534a600de3e4ca3f945080bb`.
- Merged-master `engine-subsystem-tests` artifact: ID `10265810108`, digest `sha256:11e5ef2dfbe04b3710ee26f2e62ac43ba1253620130844c4e89e1333032babd6`.
- Merged-master `jacoco-reports` artifact: ID `10265375432`, digest `sha256:8f800eeaf57194e4405a4abbf0ff147105f2bfe697bc10efe473d189c2b7a161`.
- P2-T11 remained benchmark/test-only: no production public API, dependency, module edge, durable decision, or native binding was added.
- Independent review was not mandatory under the activated #81 contract because the task remained test/evidence-only; CI was not represented as independent review.

Documentation-only Issue #129 / PR #130 then reconciled the post-P2-T11 handoff and merged as `e318a4f3bd6392e1b24c883b7e1bd10532edbfda`. Its complete diff contained only `README.md` and this file, so the Markdown-only CI exemption applied and no automatic PR-head or merged-master build/test workflow was required.

## P2-T12 executable contract

Issue #82 is activated and is the sole executable roadmap task for this branch.

P2-T12 adds `EngineLogger` in `engine-core` as one JDK-only synchronous structured logging boundary. Every emitted event carries an automatically captured timestamp and caller thread ID/name plus caller-supplied severity/message and immutable optional context fields for frame, simulation tick, subsystem, connection, and entity.

The logger forwards every valid event to one caller-owned `Sink` without severity thresholding. Sink callbacks are serialized across concurrent callers. `flush()` is explicit and synchronous. Sink `RuntimeException`/`Error` values propagate unchanged; the logger does not swallow, wrap, retry, buffer, queue, or move work to a background thread.

P2-T12 defines no file/console/JSON/logfmt format, rotation policy, persistent sink, global singleton, or runtime composition wiring. `FATAL` is only a severity label here; P2-T13 owns orderly fatal termination behavior.

The acceptance use case is structured filtering: interleaved retained events for two connection IDs must be filterable by `event.context().connection()` without parsing message text.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` production contracts through P2-T10 remain intact; P2-T11 remains test/evidence-only.
- P2-T12 adds one public top-level type only: `EngineLogger`, with nested `Level`, `Context`, `Event`, and `Sink`.
- No dependency, Gradle file, lockfile, module edge, native binding, protocol, persisted format, or product-scope change is authorized.
- CI retains five Windows x64 self-hosted jobs; P2-T12 is non-exempt because Java source/test and workflow YAML change.

## Exact next action

On `p2-t12-structured-logging`:

1. keep changes limited to the eight paths authorized by Issue #82;
2. finish `EngineLogger` and `EngineLoggerTest` exactly to the activated contract;
3. record D-028 in `docs/DECISIONS.md` and describe the logging boundary in `docs/ARCHITECTURE.md`;
4. add the focused verification command to `docs/BUILD_AND_VERIFY.md` and retain `EngineLoggerTest` XML in CI evidence;
5. audit the complete changed-file list against #82;
6. self-review field semantics, caller-thread attribution, null/missing representation, sink ownership, synchronization, filtering, flush, failure propagation, and absence of hidden persistence/async behavior;
7. seek independent review because P2-T12 adds a public API and durable decision; if unavailable, record `not performed`, reason, and residual risk honestly;
8. require passing exact-head PR CI on the final PR head;
9. merge only after exact-head CI passes, then require separate passing push CI on the exact resulting `master` merge commit;
10. record final evidence and close #82 consistently.

Do not activate or implement P2-T13 inside P2-T12.

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

Active:

- P2-T12 / #82 — structured runtime logging boundary.

Next:

- P2-T13 / #83 — orderly fatal termination; planning-only and expected to build on P2-T10/P2-T12.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2 task tests do not satisfy that integrated gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block P2-T12, and structured logging tests must not strengthen unrelated native feasibility claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, active Issues, and workflow state with this checkpoint;
4. verify #82 remains the sole active executable roadmap task;
5. audit all changed paths against #82;
6. require exact-head PR CI and exact merged-master push CI because this task is non-Markdown;
7. keep P2-T13 and later tasks planning-only until activated one at a time;
8. preserve P2's still-unproven ten-minute integrated exit gate and independent native feasibility gates;
9. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
