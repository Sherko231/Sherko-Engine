# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `66ae658fb13901f040093bb486ad0f87443e1b4f` — handoff PR #71 merged; exact-head PR CI #154 and merged-master push CI #155 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05, P1-T06, P1-T07, P1-T08, P1-T09, P1-T10, P1-T10A, P2-T01 |
| Phase 1 live state | Epic #2 closed as completed after the exit gate passed |
| Current implementation | P2-T02 / Issue #72 — graph-only dependency ordering is present in this checkpoint; inspect its PR for final verification and merge state |
| Next planned implementation | After P2-T02 / #72 is verified and merged, refine P2-T03 / #73 for coordinated rollback; do not recreate existing planning Issues |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare with remote `master`, and inspect GitHub for activity newer than this snapshot.

## Exact next action

Phase 1 and P2-T01 / Issue #64 are complete. Documentation maintenance #66/#68/#70 is complete; PR #71 and its merged master passed CI #154/#155. P2-T02 / #72 adds graph-only dependency ordering in this checkpoint. Inspect its linked PR and finish verification/merge if still open, then refine the existing P2-T03 / #73 planning contract. Do not combine rollback with P2-T02.

The owner explicitly requested planning Issues for remaining P2 and all P3/P4 work: P2 #72–#83, P3 #84–#93, P4 #94–#102. This is newer live workflow state than the prior checkpoint, not completed implementation or permission to run phases in bulk. P3/P4 still require the preceding phase exit and planning review. P2 phase exit remains unproven; graph and lifecycle tests do not demonstrate the ten-minute integrated headless loop.

Repository CI currently selects repository-scoped self-hosted Windows x64 runners. At this checkpoint GitHub reports `master` as unprotected with status-check enforcement off, so the platform does not itself block a failing PR merge. The repository agent contract still requires a passing exact-head PR run before merge and a passing merged-`master` push run unless the owner explicitly grants a separate bounded exception. At least one matching runner must be online for those jobs to execute; queued, cancelled, or unstarted jobs are not verification evidence.

## What is actually implemented

- Gradle Wrapper and Java 25 toolchain configuration.
- The 16 production-target engine/game/support modules from `ENGINE_SCOPE.md` remain intact.
- `feasibility-spikes` is an additional experimental Gradle subproject; it is not a production runtime module.
- The root project is a build/quality/task aggregator with no Java `src/` tree.
- All nine Phase 0 spike Java sources live under `feasibility-spikes/src/main/java/com/samo/spike/**`.
- Spike-only LWJGL/Jolt/Snaploader/OSHI/Steamworks dependencies and native runtime artifacts are owned by `feasibility-spikes`, not the root project.
- Existing historical root Phase 0 task names remain compatibility aliases to identically named tasks in `:feasibility-spikes`; Steam/JFR evidence paths under root `build/spikes/**` are preserved.
- The root dependency lock represents root build/quality configurations; `feasibility-spikes/gradle.lockfile` owns the relocated spike runtime graph.
- Shared group/version/repository/toolchain/JUnit 6 Platform conventions remain centralized at the root.
- `test-support` exports JUnit 6 and AssertJ and owns the repository-wide `ModulePackageBoundaryTest` source.
- D-016 source-level verification receives all declared subprojects from Gradle, validates exact registry coverage and production package ownership, and scans imports plus fully qualified references in main/test Java trees. Test package ownership, bytecode, reflection strings/resources, and generated sources outside the conventional trees remain explicitly out of scope.
- Checkstyle 14.1.0 scans production/test sources while explicitly excluding the experimental feasibility source tree; `verifyCheckstyleSourceBoundary` verifies that exclusion.
- JaCoCo 0.8.15 remains configured for the same 12 test-bearing engine modules.
- `.github/workflows/java25.yml` contains five Windows Java 25 jobs and selects `[self-hosted, Windows, X64]`.
- The architecture job targets `:test-support:test`, and native smoke invokes the preserved root aliases.
- `game-client` and `game-server` have separate runnable entry points, reproducible `--version` reporting, and server headless runtime-boundary verification.

## Completed documentation maintenance — Issues #66 and #68

Issue #66 strengthened the agent contract and templates with concrete API/architecture use cases, failure behavior, current design need, observable test intent, honest review provenance, and phase-integration evidence rules. It changed documentation only and did not alter runtime API, architecture, dependencies, workflow definitions, or phase thresholds.

PR #67 merged as `a28fa6d8c1da7c385da129ab19320c0960091fc6`. Its PR-head run #150 and merged-master run #151 did not execute successfully because no matching self-hosted Windows x64 runner was available; the owner explicitly authorized a one-time bypass for Issue #66 only. Those runs are not recorded as passes.

Issue #68 corrected the stale README Phase 2 next-action pointer. PR #69 merged as `cf52a5c4de58bb0ad0f50dbc2a89b81144ca8416` under a separate explicit one-time owner override for that README-only maintenance. The exception did not modify `AGENTS.md`, `docs/BUILD_AND_VERIFY.md`, or future task requirements. Subsequent handoff maintenance #70 / PR #71 passed PR CI #154 and merged-master push CI #155; no exception is assumed for P2-T02.

## P2-T02 implementation and verification handoff

- `engine-core` contains `SubsystemGraph` and its nested immutable `Registration` record (D-019). The graph validates IDs, instance identity and missing prerequisites; it returns a deterministic unmodifiable dependency-first order or a closed cycle diagnostic.
- Graph construction/resolution never invokes lifecycle hooks, acquires resources, or assumes ownership. Each resolution uses a fresh iterative traversal; lifecycle state, initialization, shutdown and rollback remain caller responsibilities.
- JUnit 6 acceptance tests cover forward references, diamonds/disconnected components, deterministic ordering, self/later-component cycles with printed diagnostics and zero hooks, input validation, identity versus equality, defensive copies, repeated resolution, a 10,000-node chain, and a successful caller-owned synthetic composition using the real D-018 guards.
- Existing CI retains all five jobs and now runs the focused lifecycle and graph suites together. Graph evidence is `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.SubsystemGraphTest.xml` and `engine-core/build/reports/tests/test/index.html`, uploaded alongside lifecycle results in `engine-subsystem-tests`; coverage remains in `jacoco-reports`.
- Local implementation environment: Linux x64, OpenJDK 17.0.20. The current master snapshot was fetched through authenticated GitHub API data; every blob, the complete tree and original signed commit SHA were verified. Direct `git fetch` lacked shell authentication. `./gradlew` was not executable in the existing tracked file mode; invoking `bash gradlew` reached the wrapper but failed before compilation because the Gradle 9.3.0 download was unreachable. No local Java 25 test pass is claimed, and the target/toolchain is not weakened.
- Final review provenance, PR-head CI, merged-master push CI, executed command results and artifact references belong in Issue #72 and its PR after they execute. Do not interpret this checkpoint as a CI pass or completed merge.
- No native, dependency, lockfile, entry-point, protocol, scope, or module-edge changes. P2-T03 rollback and the P2 phase gate remain unimplemented; #42–#44 evidence limits are unchanged.

## P2-T01 implementation and verification handoff

- `engine-core` now contains `com.samo.engine.core.api.EngineSubsystem`, an abstract `AutoCloseable` base with final initialize/start/stop/close guards and protected phase hooks (D-018).
- One instance has one lifecycle. Invalid/reentrant transitions are rejected before hooks; unchecked forward hook failures permit only explicit cleanup. Unstarted instances can close directly; running instances require explicit stop. Close is attempted once even if cleanup throws.
- JUnit 6 acceptance tests exercise the production guards and synthetic resource ownership. They do not prove native restartability or native cleanup safety.
- At the P2-T01 checkpoint, no subsystem graph or other P2 task was implemented. P2-T02 adds graph-only ordering above; coordinated rollback, clock, runtime loop and configuration remain unimplemented. Client/server entry points are unchanged.
- The five existing CI jobs now also execute focused lifecycle acceptance and unchanged dependency-lock resolution. Test evidence: `engine-core/build/test-results/test/TEST-com.samo.engine.core.api.EngineSubsystemTest.xml`, `engine-core/build/reports/tests/test/index.html`, and the `engine-subsystem-tests` CI artifact; coverage remains in `jacoco-reports`.
- The implementation environment is Linux x64 with JDK 17 only. The starting checkout was materialized from authenticated GitHub API data and every blob, tree, and the original commit SHA verified. The local focused Gradle command failed before compilation because the Gradle 9.3.0 download was blocked by network access. Required Java 25/Windows runtime results must come from actual CI execution, not this local environment.
- P2-T01 merged via PR #65 as `f84ca1d87292daf2648c9f6dc9371156facdb945`. Final PR CI #148 and merged-master push CI #149 passed all five jobs. Issue #64 is closed as completed; its body and PR #65 retain exact SHA, command, environment, skipped-check, and artifact evidence.
- Independent #42–#44 remain open and their evidence limits are unchanged.

## Phase 1 completion evidence

P1-T10A completed the final Phase 1 follow-up:

- PR #57 merged to `master` as `b3924cad533d5ed774fc4c92fd2ed22e856e91c2`.
- Final PR-head CI run #136 passed all five required jobs on exact head `a10f0404868bd6b48c2c3e22ef24748e6955fd28`.
- Merged-`master` push CI run #137 passed all five required jobs on merge commit `b3924cad533d5ed774fc4c92fd2ed22e856e91c2`.
- Issue #56 closed as completed.
- Phase 1 Epic #2 closed as completed.
- The Phase 1 exit gate — repeatable empty client and headless-server build/run commands with the required quality gates — is satisfied.
- The existing JUnit 6.0.0 implementation is the accepted shared test baseline; Issue #60 corrects the earlier framework-version wording, stale README phase pointer, and CI-enforcement wording, and adds a mandatory agent consistency audit without changing build behavior.
- Issue #62 hardens D-016 against Gradle-module-list drift, missing production packages, fully qualified shortcuts, and overlapping-root misattribution without changing production code or dependencies.

## What remains skeleton or planned

- Concrete engine subsystems and `game-sandbox` remain skeletons. Single-subsystem lifecycle and graph-only ordering are implemented; renderer, asset, world, physics, audio, networking, runtime UI, editor, and gameplay implementations remain planned.
- Client/server entry points remain intentionally minimal foundation composition roots apart from version reporting.
- `feasibility-spikes` remains disposable experimental evidence code. Its presence must not be interpreted as production engine implementation.
- Phase 2 now contains P2-T01 lifecycle and P2-T02 graph-only ordering. Rollback, clock, configuration, ownership registry, and the phase exit gate remain unimplemented.

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
3. confirm Phase 1 Epic #2 and corrective Issues #60 and #62 are closed and no newer repository activity supersedes this checkpoint;
4. inspect independent follow-up Issues #42–#44 and preserve their evidence limits;
5. confirm at least one matching self-hosted Windows x64 runner is online before interpreting queued CI;
6. confirm #64 and documentation maintenance #66/#68/#70 are complete; inspect #72 and its PR for final CI/merge state, then refine existing P2-T03 / #73 after completion;
7. stop if code, docs, GitHub state, or the active Issue conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste transient board state here. Update live GitHub workflow state immediately and this checkpoint in the same PR as the durable repository change.
