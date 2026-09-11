# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `883e20539a16dc6509e163b22a1b6c744ca6ab1e` — maintenance PR #110 merged; merged-master push CI #176 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01, P2-T02, P2-T03, P2-T04, P2-T05 |
| Current executable work | Maintenance Issue #111 — skip full CI for Markdown-only changes |
| Current branch | `maint-markdown-only-ci` |
| Next planned roadmap implementation | P2-T06 / #76 — frame-gap clamp and catch-up cap; refine and activate only after #111 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect the live branch/HEAD, compare with remote `master`, and inspect GitHub for newer activity.

## Exact next action

Issue #111 is the only active executable change in this checkpoint. It is repository-maintenance work that changes CI policy and workflow path filtering; it does not activate P2-T06.

The authorized file set is:

- `AGENTS.md`
- `docs/BUILD_AND_VERIFY.md`
- `.github/workflows/java25.yml`
- this `docs/DEVELOPMENT_STATUS.md`

The task must preserve all existing non-Markdown CI jobs, commands, concurrency, runner labels, artifacts, and manual dispatch. Because this task itself changes workflow YAML, the pre-change CI contract still applies to #111: final exact-head PR CI must pass before merge, followed by merged-`master` CI on the exact merge commit.

After #111 completes, a future change whose complete diff contains only `.md` files will not require automatic PR-head or merged-`master` build/test CI. The complete changed-file list must be audited first; any non-Markdown path makes the normal full CI contract apply.

## Maintenance #109 completion evidence

Maintenance #109 is complete:

- PR #110 merged to `master` as `883e20539a16dc6509e163b22a1b6c744ca6ab1e`.
- Final exact-head PR workflow #175 / run `34587556379` passed all five required jobs on head `201047b75043d630421f9db85c366fee4e91f466`.
- Merged-master workflow #176 / run `34587883984` passed all five required jobs on exact merge commit `883e20539a16dc6509e163b22a1b6c744ca6ab1e`.
- The diff was exactly `README.md` and `docs/DEVELOPMENT_STATUS.md`.

## P2-T05 completion evidence

P2-T05 / Issue #75 is complete:

- PR #108 merged to `master` as `bf6863f327496889c2857f33865d2a9df360fa3d`.
- Final exact-head PR workflow #173 / run `34586031161` passed all five required jobs on head `d5f5ec4c7f65d25a53794a2b647e456aca26c92b`.
- Merged-master workflow #174 / run `34586538003` passed all five required jobs on exact merge commit `bf6863f327496889c2857f33865d2a9df360fa3d`.
- Merged-master artifacts: `engine-subsystem-tests` ID `10193887501`, digest `sha256:0a7ae9cd28e28e261fb8cd0bd438ddf12aab24da149f6b93e1f20b52716ddece`; `jacoco-reports` ID `10193870940`, digest `sha256:f4cb97c85a282947f2a265a673f0f87ece353b69e9e8b922ddb22d81f96007b6`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and self-review were not represented as independent review.

`engine-core` contains the implemented Phase 2 foundation through D-022: `EngineSubsystem`, `SubsystemGraph`, `SubsystemStartup`, `EngineClock`, and `FixedStepAccumulator`.

## Markdown-only CI policy — Issue #111

The task branch changes the repository policy so that a pull request is exempt from automatic build/test CI only when the complete changed-file set is non-empty and every path ends in `.md`.

Expected post-merge behavior:

- automatic `pull_request` and `push` runs targeting `master` are skipped for Markdown-only changes through workflow `paths-ignore`;
- `workflow_dispatch` remains available regardless of file type;
- any non-Markdown file anywhere in the complete PR diff makes the normal exact-head PR CI and exact merged-`master` CI requirements apply;
- a later commit that adds a non-Markdown path makes a previously exempt PR non-exempt;
- the exemption affects build/runtime execution only and does not waive Issue scope, truth hierarchy, review rules, architecture/decision rules, documentation consistency, branch/PR discipline, or explicit manual verification.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- Shared JUnit 6/AssertJ, Checkstyle, JaCoCo, dependency locking, architecture verification, client/server entry points, and native-smoke tasks remain in place.
- CI has five Windows x64 self-hosted jobs: build/quality, unit tests, architecture tests, JaCoCo, and Windows native smoke.
- Existing workflow concurrency still cancels superseded same-PR runs.
- No Java source, test, dependency, lockfile, module-edge, native binding, entry-point, protocol, persisted-format, product-scope, roadmap, backlog, or architecture-decision change is part of maintenance #111.

## Phase 2 status

Completed:

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock` elapsed-nanosecond sampling.
- P2-T05 / #75 — exact fixed-step accumulator at 60 Hz.

Next planned roadmap item after maintenance #111:

- P2-T06 / #76 — bound frame gaps and catch-up work. Its existing Issue is still a planning contract and must be refined/activated before coding.

P2 phase completion is not claimed. The existing exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2-T01 through P2-T05 tests do not satisfy that gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block the independent Phase 2 timing work, but their evidence limits must not be strengthened by unrelated unit/CI success.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local `git status --short --branch` and `git rev-parse HEAD` when a local checkout is used;
3. compare remote `master`, open PRs, and active Issues with this checkpoint;
4. confirm maintenance #111 has completed before activating P2-T06 / #76;
5. audit the complete changed-file list before applying any Markdown-only CI exemption;
6. for a non-exempt PR, identify the current PR head SHA before interpreting CI and require exact-head PR CI plus exact merged-`master` CI;
7. if #76 is selected next, refine its unresolved clamp/catch-up API and behavior choices in the Issue before coding;
8. keep P2-T07 and later tasks planning-only until activated one at a time;
9. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.

## Maintenance rule

Update this file when #111 completes so the next handoff records the merged policy and returns P2-T06 / #76 to the exact-next-action position. Live GitHub workflow state remains authoritative for activity newer than the containing commit.
