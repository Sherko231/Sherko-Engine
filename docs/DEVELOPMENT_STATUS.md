# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `6f242aab4cd9c031bf1992c8cc46de45f41f41c1` — P2-T08 / PR #117 merged; merged-master workflow #185 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T08 |
| Current executable work | Maintenance Issue #118 — reconcile post-P2-T08 handoff status |
| Current branch | `maint-post-p2-t08-handoff` |
| Next planned roadmap implementation | P2-T09 / Issue #79 — layered configuration loading; planning-only until explicitly refined and activated after #118 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T08 completion evidence

P2-T08 / Issue #78 is complete:

- PR #117 merged to `master` as `6f242aab4cd9c031bf1992c8cc46de45f41f41c1`.
- Initial PR workflow #183 failed because the synthetic `CountingSubsystem` test double omitted required no-op lifecycle hooks; that failure was corrected only in the authorized test file and is obsolete evidence.
- Final exact-head PR workflow #184 / run `34593760659` passed all five required jobs on head `efd9c56003d6816791d1c02be2bcefdfa7cf061c`.
- PR `engine-subsystem-tests` artifact: ID `10261530242`, digest `sha256:bb3ee30be572587eb057f0d65579c50204ef54f5000b4d144b0a8123213e26a8`.
- PR `jacoco-reports` artifact: ID `10261470207`, digest `sha256:a8f3001a2fe5377d9bc0fceb3c7baea680e58304712e6ded6157edb61bfbd10e`.
- Merged-master workflow #185 / run `34594010089` passed all five required jobs on exact merge commit `6f242aab4cd9c031bf1992c8cc46de45f41f41c1`.
- Merged-master `engine-subsystem-tests` artifact: ID `10261775449`, digest `sha256:d792274497de7e52b122d36c8ae632321c9bf2e9f8b8abefb90547528f7da6e1`.
- Merged-master `jacoco-reports` artifact: ID `10261470658`, digest `sha256:d56620d4b0e0c0b2fce11435fe61d361894885ac119a8d08a4d10ef21f18c98d`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and self-review were not represented as independent review.

P2-T08 established the typed startup schema in `engine-core`: source-aware raw entries, canonical typed keys for fullscreen width/height and the locked 60 Hz simulation rate, defaults, bounds, aggregated ordered validation errors, unknown-key rejection, immutable validated output, and no lifecycle side effects. P2-T09 retains configuration source layering and precedence.

## Exact next action

Finish maintenance Issue #118 only:

1. keep the complete diff restricted to `README.md` and `docs/DEVELOPMENT_STATUS.md`;
2. verify both documents against live GitHub state;
3. open one linked PR;
4. audit the complete PR changed-file list and require exactly those two `.md` paths;
5. apply the Markdown-only CI exemption — no PR-head or merged-`master` build/test CI is required for this maintenance change;
6. merge the maintenance PR and confirm #118 closes consistently.

After #118 completes, perform a fresh startup audit and then refine/activate P2-T09 / Issue #79 before any P2-T09 code is written. Do not implement #79 from its current planning body without first finalizing its executable contract.

A future agent must not create another handoff-only maintenance task merely because this document correctly describes its containing maintenance checkpoint. After #118 is merged, reconcile live GitHub state with this checkpoint and proceed to the fresh #79 activation audit unless another real conflict exists.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` contains lifecycle/order/startup rollback, monotonic elapsed sampling, exact 60 Hz accumulation, bounded catch-up policy, renderer-facing interpolation alpha, and typed startup configuration validation.
- P2-T08 added no dependency, lockfile, module-edge, native-binding, protocol, persistence-format, or scope change.
- CI retains five Windows x64 self-hosted jobs and the Markdown-only path exemption.

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

Current maintenance:

- #118 — reconcile post-P2-T08 handoff status; documentation-only.

Next roadmap task after maintenance:

- P2-T09 / #79 — layered config loading and exact precedence; planning-only until explicitly activated.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2 task tests do not satisfy that integrated gate.

## Repository maintenance policy

Markdown-only changes are exempt from automatic PR-head and merged-master build/test CI only when the complete non-empty diff contains exclusively `.md` paths. Issue #118 is intended to qualify because only `README.md` and `docs/DEVELOPMENT_STATUS.md` are authorized; verify the complete PR file list before relying on the exemption.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block the current Phase 2 configuration work, but unrelated unit/CI success must not strengthen their claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, active Issues, and workflow state with this checkpoint;
4. while #118 is open, verify it remains the sole active maintenance task and that its diff is exactly the two authorized Markdown files;
5. after #118 merges, treat #79 as the next planned roadmap task, not as executable code authorization until it is refined and activated;
6. preserve P2's still-unproven ten-minute integrated exit gate;
7. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
