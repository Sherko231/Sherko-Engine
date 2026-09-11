# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `b2f8d8971c81234568d7cbce180b8cc7b5e9d0fe` — P2-T09 / PR #122 merged and verified |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T09 |
| Current executable work | Documentation-only maintenance Issue #123 — reconcile post-P2-T09 handoff |
| Current branch | `maint-post-p2-t09-handoff` |
| Next planned roadmap implementation | P2-T10 / Issue #80 — native-resource registry; planning-only until this maintenance completes and #80 is explicitly refined/activated |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T09 completion evidence

P2-T09 / Issue #79 is complete:

- PR #122 merged to `master` as `b2f8d8971c81234568d7cbce180b8cc7b5e9d0fe`.
- Final exact-head PR workflow #186 / run `34596141502` passed all five required jobs on head `0142bb0a0d5af497878b6dc082dadbed646b304f`.
- PR `engine-subsystem-tests` artifact: ID `10262673006`, digest `sha256:dc1ba55b99e1bf9c0723230e9d00eab8c1426aa327b039df7ae83c80f82139e4`.
- PR `jacoco-reports` artifact: ID `10262088284`, digest `sha256:b5f33b7840bdf30b7446dcab29d724d781917a976df910e1f3d1bba476c7ccc1`.
- Merged-master workflow #187 / run `34596379138` passed all five required jobs on exact merge commit `b2f8d8971c81234568d7cbce180b8cc7b5e9d0fe`.
- Merged-master `engine-subsystem-tests` artifact: ID `10262423630`, digest `sha256:ed391cface3cbd9666a633e86fdb381fe03be7cc205206dd9fe0a6379f282ea9`.
- Merged-master `jacoco-reports` artifact: ID `10262088696`, digest `sha256:faa427e126647da4aa6510e8fee0602f08d8a834f8402259391d3e84b33400a0`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and self-review were not represented as independent review.

P2-T09 established deterministic startup configuration layering in `engine-core` through `EngineConfigLoader`: fixed precedence `EngineConfigSchema` defaults < game file < user file < command-line overrides, bounded UTF-8 `key=value` parsing with source line attribution, missing optional files as absent layers, validate-after-merge behavior, and no subsystem lifecycle side effects.

## Workflow-discipline history around P2-T09

- Documentation-only Issue #118 / PR #119 reconciled the post-P2-T08 handoff and merged as `3e3e0b78ae001c55cdcba11dcbd566157b7a51eb`; its complete diff was Markdown-only and correctly produced no build/test workflow.
- During the following P2-T09 startup audit, an accidental README preparation edit was committed directly to `master` as `95b3a0df390794160095f053a1e3dbb68547251e`. Work stopped immediately.
- Maintenance Issue #120 / PR #121 restored the exact pre-accident README through the required branch/PR path and merged as `f7ea7dca6559a0afc2ea9e5c83b2c9b25ca27e41`. Its complete diff was one Markdown file, so the Markdown-only CI exemption applied.
- No P2-T09 code was written directly to `master`; PR #122 started from the repaired master above and completed normally.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` contains lifecycle/order/startup rollback, monotonic elapsed sampling, exact 60 Hz accumulation, bounded catch-up policy, renderer-facing interpolation alpha, typed startup configuration validation, and deterministic layered startup configuration loading.
- P2-T09 added no dependency, lockfile, module edge, native binding, protocol, persisted game format, or scope change.
- CI retains five Windows x64 self-hosted jobs and the Markdown-only path exemption.

## Exact next action

Finish only maintenance Issue #123:

1. keep the diff limited to `README.md` and `docs/DEVELOPMENT_STATUS.md`;
2. verify the post-P2-T09 facts against live GitHub state;
3. audit the complete changed-file list and require both paths to end in `.md`;
4. apply the Markdown-only CI exemption and do not classify the absence of build/test workflows as pass/fail/skipped;
5. merge the linked maintenance PR;
6. confirm #123 closes consistently;
7. only then perform a fresh startup audit for P2-T10 / Issue #80 and refine/activate #80 before any code.

Do not activate or implement P2-T10 inside this maintenance task.

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

Next roadmap task after maintenance:

- P2-T10 / #80 — native-resource registry; planning-only until explicitly activated.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2 task tests do not satisfy that integrated gate.

## Repository maintenance policy

Markdown-only changes are exempt from automatic PR-head and merged-master build/test CI only when the complete non-empty diff contains exclusively `.md` paths. Issue #123 is intended to qualify only if its final complete diff remains exactly the two authorized Markdown files.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block handoff maintenance or P2-T10 activation, but unrelated unit/CI success must not strengthen their claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, active Issues, and workflow state with this checkpoint;
4. verify Issue #123 is the sole current maintenance task until merged;
5. audit all changed paths against #123;
6. apply the Markdown-only exemption only if the complete diff remains non-empty and all paths end in `.md`;
7. keep P2-T10 / #80 and later tasks planning-only until activated one at a time;
8. preserve P2's still-unproven ten-minute integrated exit gate;
9. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
