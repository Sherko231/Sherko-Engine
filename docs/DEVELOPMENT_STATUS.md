# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `b9c98ec3bf494122c849fa5a9c670923d7cff191` — P2-T07 / PR #116 merged; merged-master workflow #182 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T07 |
| Current executable work | P2-T08 / Issue #78 — typed startup configuration validation |
| Current branch | `p2-t08-typed-config` |
| Next planned roadmap implementation | P2-T09 — layered configuration loading; remains planning-only until P2-T08 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T07 completion evidence

P2-T07 / Issue #77 is complete:

- PR #116 merged to `master` as `b9c98ec3bf494122c849fa5a9c670923d7cff191`.
- Final exact-head PR workflow #181 / run `34591718600` passed all five jobs on head `c417a4716ff532bfa9a98486a28a505526bd6d6c`.
- PR `engine-subsystem-tests` artifact: ID `10195974227`, digest `sha256:84fe900ca2228a7b978c402e916555e9832615e6be6468cc876a76102d97011a`.
- PR `jacoco-reports` artifact: ID `10195952652`, digest `sha256:f262542e6ddfea5f33efe1102d577c5a9948f1a438b152efe89b5967e53e8cd9`.
- Merged-master workflow #182 / run `34592107752` passed all five jobs on the exact merge commit.
- Merged-master `engine-subsystem-tests` artifact: ID `10196104080`, digest `sha256:70b428c85256123d34ec4c2136a0898853e182b5bbf64901a219c364d6ac9e7e`.
- Merged-master `jacoco-reports` artifact: ID `10196104719`, digest `sha256:e76cfa6566622cca179cb1a4d02d8e71292e02aaeb349caf0b74b1244253720a`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and self-review were not represented as independent review.

## P2-T08 executable contract

Issue #78 is activated and is the sole executable roadmap task for this branch. It adds a fixed startup schema in `engine-core` for one already-resolved raw configuration map.

Authorized public behavior:

- `ConfigSource` preserves opaque diagnostic source text.
- `ConfigEntry` pairs one raw value with its source.
- canonical typed `ConfigKey<Integer>` values define `fullscreen.width`, `fullscreen.height`, and `simulation.tickRate`.
- defaults are width `1920`, height `1080`, tick rate `60`.
- width bounds are inclusive `320..16384`.
- height bounds are inclusive `200..16384`.
- tick rate accepts exactly `60`; P2-T08 does not make simulation rate configurable.
- `EngineConfigSchema.validate(...)` aggregates user errors in supplied-map iteration order, rejects unknown keys, preserves sources, returns all typed defaults/values on success, and performs no lifecycle call.
- `ConfigValidationException` exposes an immutable ordered list of `ConfigError` values.

P2-T09 retains source loading/layering/precedence. P2-T08 does not add file I/O, CLI/environment parsing, hot reload, arbitrary extensible keys, persistence, or a mutable runtime settings service.

## Exact next action

On the active branch:

1. finish the authorized implementation/tests/docs only;
2. audit the complete changed-file list against Issue #78;
3. perform author self-review and record independent-review provenance honestly;
4. open one linked PR;
5. require passing exact-head PR CI because Java/YAML files change;
6. merge only after the current-head workflow passes;
7. require separate passing push CI on the exact resulting `master` merge commit;
8. record final artifacts/evidence and confirm Issue #78 closed consistently.

Do not activate or implement P2-T09 until P2-T08 is merged and verified.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` contains lifecycle/order/startup rollback, monotonic elapsed sampling, exact 60 Hz accumulation, bounded catch-up policy, renderer-facing interpolation alpha, and on this branch typed startup configuration validation.
- No dependency, lockfile, module-edge, native-binding, protocol, persistence-format, or scope change is authorized by P2-T08.
- CI retains five Windows x64 self-hosted jobs. The focused engine-core evidence suite is extended on this branch with `EngineConfigSchemaTest`.

## Phase 2 status

Completed and merged:

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock` elapsed-nanosecond sampling.
- P2-T05 / #75 — exact fixed-step accumulator at 60 Hz.
- P2-T06 / #76 — bounded frame-gap and catch-up policy.
- P2-T07 / #77 — renderer-facing interpolation alpha separated from whole simulation ticks.

Active:

- P2-T08 / #78 — typed configuration keys/defaults/bounds/source-aware validation.

Next after P2-T08:

- P2-T09 — layered config loading and exact precedence; planning-only until activated.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2 task tests do not satisfy that integrated gate.

## Repository maintenance policy

Markdown-only changes are exempt from automatic PR-head and merged-master build/test CI only when the complete non-empty diff contains exclusively `.md` paths. P2-T08 is non-exempt because it changes Java source/tests and workflow YAML.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block P2-T08, but unrelated unit/CI success must not strengthen their claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, and active Issues with this checkpoint;
4. verify #78 remains the sole active executable roadmap task;
5. audit all changed paths against #78;
6. require exact-head PR CI and exact merged-master CI because this task is non-Markdown;
7. keep P2-T09 and later tasks planning-only until activated one at a time;
8. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
