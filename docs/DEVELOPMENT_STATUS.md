# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `1b37910e0e32dd6138920131adc6d9c829deb567` — post-P2-T12 handoff PR #133 merged |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T12 |
| Current executable work | P2-T13 / Issue #83 — orderly fatal termination |
| Current branch | `p2-t13-fatal-termination` |
| Phase 2 exit gate | Still separate and unproven: deterministic headless fixed-tick loop for ten minutes with bounded catch-up and verified cleanup |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T12 completion and handoff evidence

P2-T12 / Issue #82 is complete:

- PR #131 merged to `master` as `9bee2d23efca1f084058864c8db0f707a0ef0c45`.
- Final exact-head PR workflow #193 / run `34605423530` passed all five required jobs on head `ad3d48cf8b22199be643c01130ad1625e3ce8caf`.
- Merged-master workflow #194 / run `34606052497` passed all five required jobs on exact merge commit `9bee2d23efca1f084058864c8db0f707a0ef0c45`.
- Merged-master `engine-subsystem-tests` artifact: ID `10266071766`, digest `sha256:e3d63cbc744be6362abfb6fe14986733b53c4748f3de4cc16243bbeec21df7cd`.
- Merged-master `jacoco-reports` artifact: ID `10266536993`, digest `sha256:e1b4738f157293d6af0d532c04f3a9e53dbaea0cff04be76b09c76c990ae525a`.
- D-028 is accepted: `EngineLogger` is the JDK-only synchronous structured logging boundary in `engine-core`.
- Independent review was not performed because no separate reviewer/agent identity was available; CI and author self-review were not represented as independent review.

Documentation-only Issue #132 / PR #133 then reconciled the post-P2-T12 handoff and merged as `1b37910e0e32dd6138920131adc6d9c829deb567`. Its complete diff contained only `README.md` and this file, so the Markdown-only CI exemption applied and no automatic PR-head or merged-master build/test workflow was required.

## P2-T13 executable contract

Issue #83 is activated and is the sole executable roadmap task for this branch.

P2-T13 adds one `engine-core` public type, `FatalTermination`, as bounded one-shot synchronous fatal-shutdown orchestration above the existing lifecycle/resource/logging contracts. A valid fatal call:

1. emits one structured `FATAL` event;
2. visits the successfully started subsystem initialization order in strict reverse order and attempts `stop()` then `close()` for each subsystem;
3. calls `NativeResourceRegistry.assertNoOpenResources()` after owner cleanup;
4. best-effort emits one structured `ERROR` event per captured pre-report failure;
5. flushes `EngineLogger` once;
6. invokes process termination with exit status `1` only after those attempts.

Unchecked logging, stop, close, registry-verification, failure-reporting, and flush failures are contained so later cleanup and the final termination attempt still occur. The package-private terminator seam is test-only; the public constructor uses `System.exit(1)`. A child-JVM test is required to prove the real public process-exit path without terminating the JUnit runner.

P2-T13 does not add a new logging framework, persisted production log format, force-close registry behavior, shutdown hook, background thread, global fatal singleton, general lifecycle manager, dependency, module edge, native binding, or composition-root wiring.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` production contracts through P2-T12 remain intact; P2-T11 remains benchmark/test evidence only.
- P2-T13 implementation is confined to the API/test/docs/workflow paths authorized by Issue #83.
- D-018 stop-before-close, D-027 non-force-closing native-resource diagnostics, and D-028 synchronous logging remain unchanged.
- P0-T09A/#42, P0-T13/#43, and P0-T14/#44 remain independent feasibility gates and are not strengthened by synthetic P2-T13 tests.

## Exact next action

On `p2-t13-fatal-termination`:

1. finish `FatalTermination`, `FatalTerminationTest`, and the child-JVM harness exactly to Issue #83;
2. record D-029 in `docs/DECISIONS.md` and the fatal-shutdown boundary in `docs/ARCHITECTURE.md`;
3. add the focused P2-T13 command/evidence expectations to `docs/BUILD_AND_VERIFY.md` and CI;
4. audit the complete changed-file list against the nine authorized paths;
5. self-review fatal ordering, reverse cleanup, failure aggregation, registry non-force-close behavior, log flush ordering, one-shot/reentrancy/concurrency, thread affinity, and child-process safety;
6. seek independent review because P2-T13 adds a public API and durable architecture decision; if unavailable, record `not performed`, reason, and residual risk honestly;
7. require passing exact-head PR CI on the final PR head SHA;
8. merge only after exact-head CI passes, then require a separate passing push CI on the exact resulting `master` merge commit;
9. record completion evidence and close #83 consistently;
10. do not mark Phase 2 complete from P2-T13 alone — create/activate separate bounded integration evidence work for the existing ten-minute exit gate.

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

Active:

- P2-T13 / #83 — orderly fatal termination.

After P2-T13 implementation completion, no listed P2 implementation task remains, but the phase is still incomplete until the existing integrated exit gate passes.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |
| P2 exit gate | Phase 2 completion | Integrated deterministic headless loop: fixed ticks for ten minutes, bounded catch-up, verified cleanup. |

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, Issue #83, and workflow state with this checkpoint;
4. verify #83 remains the sole active executable roadmap task;
5. audit all changed paths against #83;
6. require exact-head PR CI and exact merged-master push CI because this task is non-Markdown;
7. preserve the still-unproven ten-minute integrated exit gate and independent native feasibility gates;
8. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
