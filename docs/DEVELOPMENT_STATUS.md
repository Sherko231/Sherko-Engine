# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `d7464276ce9bf5cf3ef3fb66fe4e1d5ced1781ea` — P2-T04 PR #105 merged; merged-master push CI #169 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01, P2-T02, P2-T03, P2-T04 |
| Current executable work | Maintenance Issue #106 — exact-head CI authority and obsolete PR-run cancellation policy |
| Current branch | `maint-obsolete-pr-ci` |
| Next planned roadmap implementation | P2-T05 / #75 remains planning-only until #106 is merged and its merged-master CI passes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect the live branch/HEAD, compare with remote `master`, and inspect GitHub for newer activity.

## Exact next action

Issue #106 is the only active executable change. It is repository-maintenance work, not a roadmap task. Its bounded goal is to make obsolete PR CI runs cancel automatically and to teach future agents that only the exact current PR-head run is authoritative before merge, while merged-`master` push CI remains separate required evidence.

The authorized file set for #106 is:

- `AGENTS.md`
- `docs/BUILD_AND_VERIFY.md`
- `.github/workflows/java25.yml`
- this `docs/DEVELOPMENT_STATUS.md`

Do not modify production Java, dependencies, lockfiles, module declarations, roadmap/backlog task definitions, architecture decisions, product scope, runner labels, branch protection, or P2-T05 implementation under #106. `README.md` still contains stale P2-T04 live-status wording on `master`; it is intentionally outside #106 and must not be edited silently under this maintenance contract.

## Maintenance implementation — Issue #106

The workflow now uses top-level concurrency:

```yaml
concurrency:
  group: ci-${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true
```

Expected operational semantics:

- commits to the same PR share a PR-number concurrency group, so newer runs supersede older queued/in-progress runs;
- different PRs use different groups and therefore never cancel each other's runs;
- pushes to `master` use the `master` ref group and remain independent from PR groups;
- `workflow_dispatch` uses its ref group;
- the five existing CI jobs, triggers, runner labels, commands, artifact paths, and pass/fail requirements remain unchanged.

`AGENTS.md` and `docs/BUILD_AND_VERIFY.md` now define the evidence policy explicitly: determine the current PR head SHA first; runs for older PR-head SHAs are obsolete; stale runs may be cancelled when tooling allows; cancellation is neither success nor failure for the current head; only a completed passing exact-head PR run permits merge; after merge, a separate successful push run on the exact resulting `master` commit is required.

## P2-T04 completion evidence

P2-T04 / Issue #74 is complete:

- PR #105 merged to `master` as `d7464276ce9bf5cf3ef3fb66fe4e1d5ced1781ea`.
- Final PR-head workflow #168 / run `34582615496` passed all five required jobs on head `5a4455c3b1044325b71875276ec01dc979069465`.
- Merged-master workflow #169 / run `34583617608` passed all five required jobs on exact merge commit `d7464276ce9bf5cf3ef3fb66fe4e1d5ced1781ea`.
- PR artifacts: `engine-subsystem-tests` ID `10192533830`, digest `sha256:c846b8f97b83dae4a2b003256ec49d761700de5350d6d9062635d42beab354db`; `jacoco-reports` ID `10192522333`, digest `sha256:d79b3ecd6184549334e31951a7658e4df75189d7c8611998cbd6d40ff387dc6b`.
- Merged-master artifacts: `engine-subsystem-tests` ID `10192703114`, digest `sha256:4292d576b5096b93e8857d361a153526d6065950a90b6afa62680dbfe295361e`; `jacoco-reports` ID `10192683664`, digest `sha256:3a2969aa3d531f1df663c8b0516ddcf96196d5b8e51b63ba0c1e1f1e8da3cd06`.
- Independent review for P2-T04 was recorded as `not performed`; CI and self-review were not represented as independent review.

`engine-core` now contains the implemented P2 foundation through D-021: `EngineSubsystem`, `SubsystemGraph`, `SubsystemStartup`, and `EngineClock`.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- Shared JUnit 6/AssertJ, Checkstyle, JaCoCo, dependency locking, architecture verification, client/server entry points, and native-smoke tasks remain in place.
- CI still has five Windows x64 self-hosted jobs: build/quality, unit tests, architecture tests, JaCoCo, and Windows native smoke.
- `master` is not platform-protected by required status checks; the agent contract nevertheless forbids merge before passing exact-head PR CI and requires merged-master verification.
- No Phase 2 fixed-step accumulator, catch-up policy, interpolation, configuration system, native-resource registry, allocation metric, structured logging, fatal-shutdown coordination, or phase-exit loop is implemented yet.

## Phase 2 status

Completed:

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock` elapsed-nanosecond sampling.

Next roadmap item after maintenance #106:

- P2-T05 / #75 — fixed-step accumulator at 1/60 second; still planning-only until #106 is complete.

P2 phase completion is not claimed. The existing phase exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block #106 or the independent P2 timing work, but their evidence limits must not be strengthened by unrelated CI success.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local `git status --short --branch` and `git rev-parse HEAD` when a local checkout is used;
3. compare remote `master`, open PRs, and active Issues with this checkpoint;
4. confirm #106 is still the sole active executable change until it merges;
5. identify the current PR head SHA before interpreting workflow results;
6. treat runs for older PR-head SHAs as obsolete evidence and cancel them only when tooling/permissions allow;
7. require exact-head PR CI success before merge and separate exact-merge `master` push CI success afterward;
8. confirm #75 remains planning-only until #106 completes;
9. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.

## Maintenance rule

Update this file when #106 final exact-head verification completes and again when merge changes the current/next-work checkpoint. Live GitHub workflow state remains authoritative for activity newer than the containing commit.
