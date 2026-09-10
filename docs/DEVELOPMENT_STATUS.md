# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-P1-T10A `master` | `db822c3a0ebd833717d79d44cbee8bf3428fe909` — cleanup PR #55 merged; merged-master CI #125 passed |
| P1-T10A work | Issue #56; introduced by the containing change |
| Active milestone / phase | M1 — Engine Foundation / P1 — Build, modules, and quality gates |
| Completed roadmap implementation in this checkpoint | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05, P1-T06, P1-T07, P1-T08, P1-T09, P1-T10, P1-T10A |
| Next planned work after verified P1-T10A merge | Reconcile/close Phase 1 epic #2, then materialize the next roadmap Issue before implementation; do not begin Phase 2 without its own active Issue |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare with remote `master`, and inspect GitHub for activity newer than this snapshot.

## Exact next action

Complete P1-T10A only after final PR-head CI passes on the exact candidate head, PR #57 is merged, and merged-`master` push CI passes. The repository CI now selects a repository-scoped self-hosted Windows x64 runner, so that runner must be online for the required workflow jobs to execute. Then mark Issue #56 complete, reconcile the Phase 1 exit gate/epic, and create the next executable roadmap Issue before doing more implementation.

## What is actually implemented

- Gradle Wrapper and Java 25 toolchain configuration.
- The 16 production-target engine/game/support modules from `ENGINE_SCOPE.md` remain intact.
- `feasibility-spikes` is an additional experimental Gradle subproject; it is not a production runtime module.
- The root project is a build/quality/task aggregator with no Java `src/` tree.
- All nine Phase 0 spike Java sources were relocated unchanged from root `src/main/java/com/samo/spike/**` to `feasibility-spikes/src/main/java/com/samo/spike/**`.
- Spike-only LWJGL/Jolt/Snaploader/OSHI/Steamworks dependencies and native runtime artifacts are owned by `feasibility-spikes`, not the root project.
- Existing historical root Phase 0 task names remain as compatibility aliases to identically named tasks in `:feasibility-spikes`; Steam/JFR evidence paths under root `build/spikes/**` are preserved.
- The root dependency lock now represents only root build/quality configurations; `feasibility-spikes/gradle.lockfile` owns the relocated spike runtime graph. Dependency versions are unchanged.
- Shared group/version/repository/toolchain/JUnit Platform conventions remain centralized at the root.
- `test-support` still exports JUnit 5 and AssertJ and now also owns the repository-wide `ModulePackageBoundaryTest` source.
- D-016 package/API boundary enforcement now covers all 17 declared Gradle subprojects, including experimental `feasibility-spikes`, without weakening the existing cross-module API-only rule.
- Checkstyle 14.1.0 continues scanning production/test sources while explicitly excluding the experimental feasibility source tree; `verifyCheckstyleSourceBoundary` verifies the exclusion.
- JaCoCo 0.8.15 remains configured for the same 12 test-bearing engine modules.
- `.github/workflows/java25.yml` keeps the five required Windows Java 25 jobs; all five now select `[self-hosted, Windows, X64]`. The runner is manually operated and must be online before CI can execute.
- The architecture job targets `:test-support:test`, and native smoke invokes the preserved root aliases.
- `game-client` and `game-server` retain their separate runnable entry points, P1-T10 `--version` reporting, and server headless runtime boundary verification.
- Cleanup Issue #54 previously removed the unused root `Launcher.java`; P1-T10A removes the remaining root Java source tree by relocating spikes and architecture verification.

## What remains skeleton or planned

- Production engine subsystems and `game-sandbox` remain skeletons; no production lifecycle, renderer, asset, world, physics, audio, networking, runtime UI, editor, or gameplay implementation exists yet.
- Client/server entry points remain intentionally minimal foundation composition roots apart from version reporting.
- `feasibility-spikes` remains disposable experimental evidence code. Its presence must not be interpreted as production engine implementation.
- A playable local engine begins in later phases; Phase 1 establishes foundation/build boundaries rather than a playable game.

## Verified feasibility baseline

P1-T10A changes source/dependency ownership only; it does not strengthen or replace existing feasibility evidence:

- Java 25 and the selected Windows x64 native stack have been exercised together.
- GLFW/OpenGL 4.6, Jolt JNI, OpenAL, localhost UDP, deterministic impairment, Steam initialization, and Java FFM flat-API access retain their previous evidence classifications.
- P0-T12 remains a 15-second integrated smoke test, not sustained-stability evidence.
- The current self-hosted CI runner may have different graphics/native capabilities than previous GitHub-hosted Windows runners, but P1-T10A does not expand any feasibility claim. Only explicitly executed feasibility Issues may do that.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These remain open after P1-T10A.

## P1-T10A verification requirements

Before merge, run and record:

```powershell
.\gradlew.bat projects
.\gradlew.bat resolveAndLockAllDependencies --write-locks
.\gradlew.bat buildAllModules
.\gradlew.bat test
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat verifyJacocoReports
.\gradlew.bat :game-client:runClient
.\gradlew.bat :game-server:runServer
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat :game-client:runClient --args="--version"
.\gradlew.bat :game-server:runServer --args="--version"
```

The current candidate has already passed those local Windows/Java 25 checks, including a clean dependency-lock refresh with no resulting `git status` or lockfile diff. Final PR-head CI and merged-`master` CI still remain mandatory and must actually execute on the configured self-hosted runner.

Also verify the root has no tracked Java `src/` files, production modules have no project dependency on `:feasibility-spikes`, root aliases still execute the relocated native-safe smoke, and lock refresh produces only the expected ownership changes.

No dependency version, product scope, production module direction, public engine API, protocol/asset format, authority, coordinate convention, native ownership contract, or feasibility conclusion is changed by P1-T10A.

## Live-state reconciliation

Before starting the next task, a fresh agent must:

1. read `AGENTS.md` in full;
2. inspect `git status --short --branch`, `git rev-parse HEAD`, remote `master`, open PRs, and active Issues;
3. inspect Issue #56 and P1 epic #2;
4. inspect independent follow-up Issues #42–#44;
5. confirm the repository self-hosted Windows x64 runner is online before interpreting queued CI as evidence;
6. if P1-T10A has merged and merged-master CI passed, reconcile/close Phase 1 before materializing the next roadmap Issue;
7. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste transient board state here. Update live GitHub workflow state immediately and this checkpoint in the same PR as the durable repository change.
