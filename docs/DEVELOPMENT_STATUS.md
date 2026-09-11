# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `0625bf2dc2531e21e894bf69f7414c7e17f945e2` — P2-T10 PR #125 merged and merged-master CI #189 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T10 |
| Current executable work | Documentation-only maintenance Issue #126 — reconcile post-P2-T10 handoff |
| Current branch | `maint-reconcile-post-p2-t10` |
| Next planned roadmap implementation | P2-T11 / Issue #81 — allocation measurement; planning-only until #126 completes and #81 is explicitly refined/activated |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T10 completion evidence

P2-T10 / Issue #80 is complete:

- PR #125 merged to `master` as `0625bf2dc2531e21e894bf69f7414c7e17f945e2`.
- Final exact-head PR workflow #188 / run `34598785022` passed all five required jobs on head `9332f963fa21360b501b7b9074ab40c9f3380b8e`.
- PR `engine-subsystem-tests` artifact: ID `10263521801`, digest `sha256:5f54a58c83fa959791588f18b04448924f5d81d6c6fb747fe6b8b698d5f41fec`.
- PR `jacoco-reports` artifact: ID `10263796775`, digest `sha256:c0de41f2d67df9b278aa388f68b19ed4badd30b6562ba465672a7cb6a7a7a4f2`.
- Merged-master workflow #189 / run `34599099291` passed all five required jobs on exact merge commit `0625bf2dc2531e21e894bf69f7414c7e17f945e2`.
- Merged-master `engine-subsystem-tests` artifact: ID `10263238589`, digest `sha256:d6aa6fdc8dabe3e493f9fbaf33d599faf00b03c623458b188cc2074ec39fa5fd`.
- Merged-master `jacoco-reports` artifact: ID `10262719107`, digest `sha256:5659587e28729e29a50a95ab0798a879723bcf6cd64e3114af55cb20eaeaeb2a`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and author self-review were not represented as independent review.

P2-T10 established D-027 and `NativeResourceRegistry`: explicit synthetic native-handle ownership tracking, allocation-site diagnostics, deterministic leak inventory, idempotent successful close, terminal close-failure tracking, and non-cleaning shutdown verification. It adds no native binding and does not prove native leak freedom, sustained stability, or repeatable native restartability.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` contains lifecycle/order/startup rollback, monotonic elapsed sampling, exact 60 Hz accumulation, bounded catch-up, interpolation alpha, typed/layered startup configuration, and native-resource ownership diagnostics through P2-T10.
- P2-T10 added no dependency, lockfile, module edge, native binding, protocol, persisted format, or product-scope change.
- CI retains five Windows x64 self-hosted jobs and the Markdown-only path exemption.

## Exact next action

Finish documentation-only Issue #126 / branch `maint-reconcile-post-p2-t10`:

1. keep the complete diff limited to `README.md` and this file;
2. audit live GitHub state and the complete PR file list;
3. apply the Markdown-only CI exemption only if both changed paths remain `.md` and no other path appears;
4. merge the linked maintenance PR;
5. close #126 as completed;
6. perform a fresh startup audit from the resulting `master`;
7. refine P2-T11 / #81 into an executable contract before any P2-T11 code is written.

Do not implement P2-T11 inside maintenance #126.

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

Next planned roadmap work:

- P2-T11 / #81 — allocation measurement; planning-only until explicitly activated after this maintenance.
- P2-T12 / #82 — structured runtime logging; planning-only.
- P2-T13 / #83 — orderly fatal termination; planning-only and depends on P2-T10/P2-T12.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2 task tests do not satisfy that integrated gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block P2-T11 planning/implementation, but ordinary CI or allocation measurements must not strengthen their claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, active Issues, and workflow state with this checkpoint;
4. keep one executable Issue active at a time;
5. refine #81 before implementation because its current planning text is not an executable contract;
6. keep P2-T12/P2-T13 and later tasks planning-only until activated one at a time;
7. preserve P2's still-unproven ten-minute integrated exit gate and independent native feasibility gates;
8. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
