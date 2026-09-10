# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Pre-P1-T06 verified `master` | `c703510ce0927d50b8da450c12dcbe7e765f3485` — merge of P1-T05 / PR #48 |
| Previous handoff-system merge | Issue #46 / PR #47 on predecessor `b53dd3b2cc4b4b08ece74fdc3998dd9f130e64bc` |
| P1-T06 work | Issue #36 / PR #49; introduced by the containing change |
| Active milestone / phase | M1 — Engine Foundation / P1 — Build, modules, and quality gates |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05, P1-T06 |
| Next executable task after this merge | P1-T07 — Issue #37, enforce module API/package boundaries |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare the checked-out branch with remote `master`, and then check GitHub for activity newer than this snapshot.

## Exact next action

After PR #49 is merged and `master` push CI is confirmed, unless GitHub shows newer merged/in-progress work, start from current `master`, activate Issue #37, create a dedicated P1-T07 branch, and implement only its module-boundary acceptance criteria. Do not fold P1-T08+ or Phase 2 work into that change.

## What is actually implemented

- Gradle Wrapper and Java 25 toolchain configuration.
- Sixteen declared subprojects matching `ENGINE_SCOPE.md`, including `engine-ui` and `test-support`.
- One-way Gradle project dependency graph with runtime UI separated from the OpenGL adapter.
- Central dependency version catalog and committed dependency locks for the current runtime/test/tool dependency graph.
- Shared group/version/repository/toolchain/JUnit Platform configuration at the root.
- `test-support` exporting JUnit 5 and AssertJ.
- A minimal shared-setup smoke test in each of the 12 current test-bearing engine modules.
- Root commands for project discovery, all-module build, lock resolution, Checkstyle quality verification, JaCoCo report verification, and Phase 0 spikes.
- Checkstyle 14.1.0 quality rules for applicable root/module Java production and test sources: wildcard imports and empty catch blocks are errors; a deliberately narrow rule also rejects standalone ignored returns from selected fully-qualified side-effect-free `java.lang.Math` calls.
- `buildAllModules` includes the root `check` quality gate before/alongside all subproject builds.
- Root Phase 0 spike sources remain experimental and are deliberately excluded from the production Checkstyle scan; `verifyCheckstyleSourceBoundary` tests that exclusion.
- An opt-in invalid Checkstyle fixture exists for deterministic negative verification.
- JaCoCo 0.8.15 is configured on the 12 engine modules that currently carry tests.
- Each configured module generates JaCoCo XML and HTML reports; `verifyJacocoReports` fails when either format is missing.
- P1-T06 defines no global or per-module minimum coverage percentage.
- Pull-request and `master` CI runs on Windows with Java 25, verifies JaCoCo reports, and uploads them as the `jacoco-reports` artifact.
- Phase 0 feasibility spike source remains under root `src/main/java/com/samo/spike/`.

## What is only skeleton or planned

- All production engine/game modules are Gradle skeletons; no production lifecycle, renderer, asset, world, physics, audio, networking, runtime UI, editor, or gameplay implementation exists yet.
- Module/package boundary enforcement is next in P1-T07 / #37; current direction is configured but not architecture-tested.
- CI expansion for architecture and selected Windows native smoke gates is planned for P1-T08 / #38.
- Client and headless-server entry points are planned for P1-T09 / #39.
- Root Phase 0 spikes are experimental and have not been moved into the planned feasibility module (P1-T10A).
- A playable local engine begins in later phases; the repository cannot build or run a game yet.

## Verified feasibility baseline

- Java 25 and the selected Windows x64 native stack can run together.
- GLFW/OpenGL 4.6, Jolt JNI, OpenAL, and localhost UDP were exercised independently.
- P0-T11 deterministically reproduced latency, jitter, loss, duplication, and reordering.
- Steamworks4j initialized Steam and callbacks, but does not expose the required modern `ISteamNetworkingSockets` surface.
- Java 25 FFM loaded the official Steam flat API and obtained/used an `ISteamNetworkingSockets` pointer. This proves API access only.
- P0-T12 exercised graphics, physics, audio, and UDP together for 15 seconds under JFR and shut down cleanly. It is a smoke test, not sustained-stability evidence.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These do not block independent Phase 1 foundation tasks unless the active Issue consumes the missing claim.

## Verification at this checkpoint

- PR #48 final PR-head and merged-`master` CI passed on Windows / Java 25 for the P1-T05 quality gate.
- PR #49 run #82 at branch commit `e986b3782f96f8875d3d33f4c0093ab2e950dc24` passed `javaToolchains`, `projects`, `buildAllModules`, and `verifyJacocoReports`; CI also uploaded the `jacoco-reports` artifact. This proves XML and HTML report production for all 12 configured test-bearing engine modules.
- PR #49 run #84 at branch commit `ee244390ab7e7e585280692ae758b26e027f9da5` passed `resolveAndLockAllDependencies --write-locks`, captured the generated dependency locks, then passed `javaToolchains`, `projects`, `buildAllModules`, `verifyJacocoReports`, and JaCoCo artifact upload.
- The generated lock state adds JaCoCo 0.8.15 agent/report dependencies and ASM transitives only to the 12 configured test-bearing engine modules; those generated lockfiles are committed in PR #49.
- The final PR-head CI after all durable lock/documentation changes must pass before merge.
- Canonical commands and report locations are in `docs/BUILD_AND_VERIFY.md`.

## P1-T05 boundary decision

Issue #35 explicitly allowed either replacing wildcard imports in the Phase 0 spikes or deliberately excluding and testing the experimental-source boundary. The implementation takes the latter route so a build-quality task does not rewrite experimental native feasibility code. The excluded tree is `src/main/java/com/samo/spike/**`, and `verifyCheckstyleSourceBoundary` fails if those sources leak into the production scan.

## P1-T06 coverage boundary

Issue #36 requires coverage visibility rather than a coverage target. JaCoCo is therefore applied to the 12 engine modules that currently contain tests, with deterministic XML and HTML report paths and a root verification task. Game modules and `test-support` are not treated as test-bearing coverage targets while they have no tests of their own. No coverage-driven test padding or percentage threshold is introduced.

No product scope, public API, module dependency direction, protocol, native-ownership rule, or durable architecture decision changed in P1-T06; therefore `ENGINE_SCOPE.md`, `docs/ARCHITECTURE.md`, and `docs/DECISIONS.md` do not require changes for this task.

## Live-state reconciliation

Before starting the next task, a fresh agent must:

1. read `AGENTS.md` in full;
2. run `git status --short --branch` and `git rev-parse HEAD`;
3. fetch and compare with remote `master`;
4. inspect Issue #37, the P1 epic #2, open pull requests, and follow-up Issues #42–#44;
5. prefer newer merged code/tests and the active Issue when they legitimately supersede this commit-contained snapshot;
6. stop if the sources conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste board columns or transient in-progress state here. Update the live Issue/Project immediately; update this checkpoint in the same pull request as the durable repository change.
