# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `b3924cad533d5ed774fc4c92fd2ed22e856e91c2` — P1-T10A PR #57 merged; merged-master CI #137 passed |
| Completed milestone / phase | M1 — Engine Foundation / P1 — Build, modules, and quality gates |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05, P1-T06, P1-T07, P1-T08, P1-T09, P1-T10, P1-T10A |
| Phase 1 live state | Epic #2 closed as completed after the exit gate passed |
| Next planned implementation | Materialize P2-T01 as its own executable Issue, then implement only that Issue on a dedicated branch |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare with remote `master`, and inspect GitHub for activity newer than this snapshot.

## Exact next action

Phase 1 is complete. Before any Phase 2 implementation, create the next executable roadmap Issue for P2-T01 (`EngineSubsystem` lifecycle) and treat that Issue as the sole implementation contract. Do not begin P2-T02 or later work at the same time.

Repository CI currently selects repository-scoped self-hosted Windows x64 runners. At least one matching runner must be online for required PR/push jobs to execute; queued jobs are not verification evidence.

## What is actually implemented

- Gradle Wrapper and Java 25 toolchain configuration.
- The 16 production-target engine/game/support modules from `ENGINE_SCOPE.md` remain intact.
- `feasibility-spikes` is an additional experimental Gradle subproject; it is not a production runtime module.
- The root project is a build/quality/task aggregator with no Java `src/` tree.
- All nine Phase 0 spike Java sources live under `feasibility-spikes/src/main/java/com/samo/spike/**`.
- Spike-only LWJGL/Jolt/Snaploader/OSHI/Steamworks dependencies and native runtime artifacts are owned by `feasibility-spikes`, not the root project.
- Existing historical root Phase 0 task names remain compatibility aliases to identically named tasks in `:feasibility-spikes`; Steam/JFR evidence paths under root `build/spikes/**` are preserved.
- The root dependency lock represents root build/quality configurations; `feasibility-spikes/gradle.lockfile` owns the relocated spike runtime graph.
- Shared group/version/repository/toolchain/JUnit Platform conventions remain centralized at the root.
- `test-support` exports JUnit 5 and AssertJ and owns the repository-wide `ModulePackageBoundaryTest` source.
- D-016 package/API boundary enforcement covers all 17 declared Gradle subprojects, including experimental `feasibility-spikes`, without weakening the cross-module API-only rule.
- Checkstyle 14.1.0 scans production/test sources while explicitly excluding the experimental feasibility source tree; `verifyCheckstyleSourceBoundary` verifies that exclusion.
- JaCoCo 0.8.15 remains configured for the same 12 test-bearing engine modules.
- `.github/workflows/java25.yml` contains five Windows Java 25 jobs and selects `[self-hosted, Windows, X64]`.
- The architecture job targets `:test-support:test`, and native smoke invokes the preserved root aliases.
- `game-client` and `game-server` have separate runnable entry points, reproducible `--version` reporting, and server headless runtime-boundary verification.

## Phase 1 completion evidence

P1-T10A completed the final Phase 1 follow-up:

- PR #57 merged to `master` as `b3924cad533d5ed774fc4c92fd2ed22e856e91c2`.
- Final PR-head CI run #136 passed all five required jobs on exact head `a10f0404868bd6b48c2c3e22ef24748e6955fd28`.
- Merged-`master` push CI run #137 passed all five required jobs on merge commit `b3924cad533d5ed774fc4c92fd2ed22e856e91c2`.
- Issue #56 closed as completed.
- Phase 1 Epic #2 closed as completed.
- The Phase 1 exit gate — repeatable empty client and headless-server build/run commands with the required quality gates — is satisfied.

## What remains skeleton or planned

- Production engine subsystems and `game-sandbox` remain skeletons; no production lifecycle, renderer, asset, world, physics, audio, networking, runtime UI, editor, or gameplay implementation exists yet.
- Client/server entry points remain intentionally minimal foundation composition roots apart from version reporting.
- `feasibility-spikes` remains disposable experimental evidence code. Its presence must not be interpreted as production engine implementation.
- Phase 2 is the next implementation phase and begins with P2-T01 only after that task is materialized as an active Issue.

## Verified feasibility baseline

Phase 1 did not strengthen or replace existing feasibility evidence:

- Java 25 and the selected Windows x64 native stack have been exercised together.
- GLFW/OpenGL 4.6, Jolt JNI, OpenAL, localhost UDP, deterministic impairment, Steam initialization, and Java FFM flat-API access retain their previous evidence classifications.
- P0-T12 remains a 15-second integrated smoke test, not sustained-stability evidence.
- The current self-hosted CI environment does not expand any feasibility claim. Only explicitly executed feasibility Issues may do that.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These follow-up gates remain open after Phase 1. They do not block beginning independent Phase 2 foundation work, but their evidence must not be assumed by later work that depends on it.

## Live-state reconciliation

Before starting the next task, a fresh agent must:

1. read `AGENTS.md` in full and follow its required read order;
2. inspect `git status --short --branch`, `git rev-parse HEAD`, remote `master`, open PRs, and active Issues;
3. confirm Phase 1 Epic #2 and Issue #56 are closed and no newer repository activity supersedes this checkpoint;
4. inspect independent follow-up Issues #42–#44 and preserve their evidence limits;
5. confirm at least one matching self-hosted Windows x64 runner is online before interpreting queued CI;
6. materialize P2-T01 as the next executable Issue before any Phase 2 implementation;
7. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste transient board state here. Update live GitHub workflow state immediately and this checkpoint in the same PR as the durable repository change.
