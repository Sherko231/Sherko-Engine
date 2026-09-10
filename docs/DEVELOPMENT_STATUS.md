# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Pre-P1-T07 verified `master` | `4b217208fb614d77dc74828d52d9fbb936c50711` — merge of P1-T06 / PR #49 |
| Previous handoff-system merge | Issue #46 / PR #47 on predecessor `b53dd3b2cc4b4b08ece74fdc3998dd9f130e64bc` |
| P1-T07 work | Issue #37 / PR #50; introduced by the containing change |
| Active milestone / phase | M1 — Engine Foundation / P1 — Build, modules, and quality gates |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05, P1-T06, P1-T07 |
| Next executable task after this merge | P1-T08 — Issue #38, expand CI for compile/tests/architecture/Windows native smoke |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare the checked-out branch with remote `master`, and then check GitHub for activity newer than this snapshot.

## Exact next action

After PR #50 is merged and `master` push CI is confirmed, unless GitHub shows newer merged/in-progress work, start from current `master`, activate Issue #38, create a dedicated P1-T08 branch, and implement only its CI acceptance criteria. Do not fold P1-T09+ or Phase 2 work into that change.

## What is actually implemented

- Gradle Wrapper and Java 25 toolchain configuration.
- Sixteen declared subprojects matching `ENGINE_SCOPE.md`, including `engine-ui` and `test-support`.
- One-way Gradle project dependency graph with runtime UI separated from the OpenGL adapter.
- Central dependency version catalog and committed dependency locks for the current runtime/test/tool dependency graph.
- Shared group/version/repository/toolchain/JUnit Platform configuration at the root.
- `test-support` exporting JUnit 5 and AssertJ.
- A minimal shared-setup smoke test in each of the 12 current test-bearing engine modules.
- Root commands for project discovery, all-module build, lock resolution, Checkstyle quality verification, JaCoCo report verification, and Phase 0 spikes.
- Checkstyle 14.1.0 quality rules for applicable root/module Java production and test sources.
- JaCoCo 0.8.15 reporting for the 12 current test-bearing engine modules, with XML/HTML verification and CI artifacts.
- `config/architecture/module-boundaries.properties` declares one owned package root, one public API root, and one internal implementation root for every one of the 16 Gradle subprojects.
- `ModulePackageBoundaryTest` verifies registry completeness, verifies production source packages stay under their owning module root, and rejects cross-module imports outside the destination module's declared API root.
- A deliberate negative fixture represents a forbidden `game-client -> engine-platform-lwjgl.internal` shortcut and is disabled during normal builds.
- Package/API boundary decision D-016 is recorded in `docs/DECISIONS.md` and reflected in `docs/ARCHITECTURE.md`.
- Root Phase 0 spike sources remain experimental and outside production architecture.

## What is only skeleton or planned

- All production engine/game modules are still Gradle skeletons; no production lifecycle, renderer, asset, world, physics, audio, networking, runtime UI, editor, or gameplay implementation exists yet.
- P1-T07 defines package boundaries but deliberately does not create placeholder future subsystem APIs merely to fill empty modules.
- Dedicated CI jobs for compile, unit tests, architecture tests, and selected Windows native smoke checks are next in P1-T08 / #38.
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

- PR #49 final PR-head and merged-`master` CI passed on Windows / Java 25 for P1-T06, including JaCoCo report verification.
- PR #50 run #94 exposed an implementation error before acceptance: the first architecture-test draft referenced AssertJ from the root test classpath where it was unavailable. This was corrected by using the root's existing JUnit assertions; no dependency or lockfile change was added.
- PR #50 valid run #95 at branch commit `7cdc4de8576adce7293e8d1a149df975b2c5d235` passed `javaToolchains`, `projects`, `buildAllModules`, `verifyJacocoReports`, and JaCoCo artifact upload on Windows / Java 25. The normal architecture tests passed as part of the root test/check flow.
- PR #50 negative run #96 at branch commit `8e717869e431a7be5421a0e0385fe969bfc87991` intentionally set `JAVA_TOOL_OPTIONS=-Darchitecture.includeInvalidFixture=true`. Compilation succeeded; `ModulePackageBoundaryTest.modulesOnlyImportOtherModulesThroughDeclaredApiRoots()` then failed at `:test`, proving the representative forbidden game-to-platform implementation shortcut is rejected. This is expected negative evidence and is not a merge candidate.
- The workflow was restored immediately after the negative run. The final PR-head CI after this documentation change must pass before merge.
- P1-T07 adds no dependency, so dependency locks require no refresh.
- Canonical valid and negative commands are in `docs/BUILD_AND_VERIFY.md`.

## P1-T05 boundary decision

Issue #35 explicitly allowed either replacing wildcard imports in the Phase 0 spikes or deliberately excluding and testing the experimental-source boundary. The implementation takes the latter route so a build-quality task does not rewrite experimental native feasibility code. The excluded tree is `src/main/java/com/samo/spike/**`, and `verifyCheckstyleSourceBoundary` fails if those sources leak into the production scan.

## P1-T06 coverage boundary

Issue #36 requires coverage visibility rather than a coverage target. JaCoCo is applied to the 12 engine modules that currently contain tests, with deterministic XML and HTML report paths and a root verification task. No coverage-driven test padding or percentage threshold is introduced.

## P1-T07 package/API boundary

Issue #37 explicitly authorizes defining module API/package boundaries. D-016 records the durable rule: every Gradle subproject owns an explicit package root; other modules may consume only its declared API root, while `.internal` roots are implementation details. The architecture test enforces this on production package declarations and cross-module imports without changing the Gradle dependency graph or inventing future subsystem interfaces.

`ENGINE_SCOPE.md`, `ROADMAP.md`, and `docs/roadmap/TECHNICAL_BACKLOG.md` do not change because P1-T07 implements their existing contract rather than changing product scope, milestone ordering, or task acceptance criteria. No feasibility evidence changes.

## Live-state reconciliation

Before starting the next task, a fresh agent must:

1. read `AGENTS.md` in full;
2. run `git status --short --branch` and `git rev-parse HEAD`;
3. fetch and compare with remote `master`;
4. inspect Issue #38, the P1 epic #2, open pull requests, and follow-up Issues #42–#44;
5. prefer newer merged code/tests and the active Issue when they legitimately supersede this commit-contained snapshot;
6. stop if the sources conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste board columns or transient in-progress state here. Update the live Issue/Project immediately; update this checkpoint in the same pull request as the durable repository change.
