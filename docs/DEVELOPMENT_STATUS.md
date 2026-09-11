# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `f7ea7dca6559a0afc2ea9e5c83b2c9b25ca27e41` — post-P2-T08 handoff reconciled; accidental direct README preparation commit repaired through maintenance PR #121 |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T08 |
| Current executable work | P2-T09 / Issue #79 — layered startup configuration loading |
| Current branch | `p2-t09-layered-config` |
| Next planned roadmap implementation | P2-T10 — native-resource registry; planning-only until P2-T09 completes |
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

P2-T08 established the typed startup schema in `engine-core`: source-aware raw entries, canonical typed keys for fullscreen width/height and the locked 60 Hz simulation rate, defaults, bounds, aggregated ordered validation errors, unknown-key rejection, immutable validated output, and no lifecycle side effects.

## Handoff maintenance before P2-T09

- Documentation-only Issue #118 / PR #119 reconciled the post-P2-T08 handoff and merged as `3e3e0b78ae001c55cdcba11dcbd566157b7a51eb`; its complete diff was Markdown-only and correctly produced no build/test workflow.
- During the following P2-T09 startup audit, an accidental README preparation edit was committed directly to `master` as `95b3a0df390794160095f053a1e3dbb68547251e`. Work stopped immediately.
- Maintenance Issue #120 / PR #121 restored the exact pre-accident README through the required branch/PR path and merged as `f7ea7dca6559a0afc2ea9e5c83b2c9b25ca27e41`. Its complete diff was one Markdown file, so the Markdown-only CI exemption applied.
- No P2-T09 code was written to `master`; the active task branch starts from the repaired master commit above.

## P2-T09 executable contract

Issue #79 is activated and is the sole executable roadmap task for this branch.

The new `EngineConfigLoader` composes startup configuration in the fixed order:

`EngineConfigSchema` defaults < game file < user file < command-line overrides.

Game and user layers are optional caller-supplied UTF-8 `key=value` files. Blank/comment lines are ignored; malformed lines, blank keys, and duplicate keys in one file fail deterministically with path/line diagnostics. Missing files are absent layers; existing unreadable paths propagate `IOException`. Command-line values are supplied as an already-parsed map and use source `command line`.

Only the final effective raw map is validated through the existing `EngineConfigSchema`. Consequently, an invalid lower-precedence value replaced by a valid higher-precedence value does not fail startup, while an invalid winning value reports the winning source. Loading/validation invokes no subsystem lifecycle method.

P2-T09 does not add environment variables, raw argv parsing, automatic OS path discovery, persistence, Java `Properties` escaping semantics, hot reload, mutable settings, arbitrary config keys, configurable tick rate, or P2-T10 work.

## Exact next action

On `p2-t09-layered-config`:

1. finish only the files authorized by Issue #79;
2. run/audit `EngineConfigLoaderTest` through exact-head CI and preserve the existing P2-T08 regression suite;
3. audit the complete changed-file list against Issue #79;
4. perform author self-review and record independent-review provenance honestly;
5. open one linked PR;
6. require passing exact-head PR CI because Java/YAML files change;
7. merge only after the current-head workflow passes;
8. require separate passing push CI on the exact resulting `master` merge commit;
9. record final artifacts/evidence and confirm Issue #79 closes consistently.

Do not activate or implement P2-T10 until P2-T09 is merged and verified.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` contains lifecycle/order/startup rollback, monotonic elapsed sampling, exact 60 Hz accumulation, bounded catch-up policy, renderer-facing interpolation alpha, typed startup configuration validation, and on this branch layered startup configuration loading.
- P2-T09 adds no dependency, lockfile, module edge, native binding, protocol, persisted game format, or scope change.
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

Active:

- P2-T09 / #79 — fixed-precedence layered config loading.

Next after P2-T09:

- P2-T10 — native-resource registry; planning-only until explicitly activated.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2 task tests do not satisfy that integrated gate.

## Repository maintenance policy

Markdown-only changes are exempt from automatic PR-head and merged-master build/test CI only when the complete non-empty diff contains exclusively `.md` paths. P2-T09 is non-exempt because it changes Java source/tests and workflow YAML.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block P2-T09, but unrelated unit/CI success must not strengthen their claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, active Issues, and workflow state with this checkpoint;
4. verify #79 remains the sole active executable roadmap task;
5. audit all changed paths against #79;
6. require exact-head PR CI and exact merged-master CI because this task is non-Markdown;
7. keep P2-T10 and later tasks planning-only until activated one at a time;
8. preserve P2's still-unproven ten-minute integrated exit gate;
9. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
