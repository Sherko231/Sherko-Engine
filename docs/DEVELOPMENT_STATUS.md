# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Pre-P1-T05 verified `master` | `80ea343b9c23423ca2564d92b7d94fccdffe85b1` — standalone `SystemPromptForAI` agent-guideline commit |
| Previous handoff-system merge | Issue #46 / PR #47 on predecessor `b53dd3b2cc4b4b08ece74fdc3998dd9f130e64bc` |
| P1-T05 work | Issue #35 / PR #48; introduced by the containing change |
| Active milestone / phase | M1 — Engine Foundation / P1 — Build, modules, and quality gates |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05 |
| Next executable task after this merge | P1-T06 — Issue #36, add JaCoCo test coverage reporting |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare the checked-out branch with remote `master`, and then check GitHub for activity newer than this snapshot.

The pre-task `80ea343b...` commit added only `SystemPromptForAI`; it did not change implementation state. This checkpoint supersedes the stale wording that previously described PR #47 as the containing commit.

## Exact next action

After PR #48 is merged and `master` push CI is confirmed, unless GitHub shows newer merged/in-progress work, start from current `master`, activate Issue #36, create a dedicated P1-T06 branch, and implement only its JaCoCo acceptance criteria. Do not fold P1-T07+ or Phase 2 work into that change.

## What is actually implemented

- Gradle Wrapper and Java 25 toolchain configuration.
- Sixteen declared subprojects matching `ENGINE_SCOPE.md`, including `engine-ui` and `test-support`.
- One-way Gradle project dependency graph with runtime UI separated from the OpenGL adapter.
- Central dependency version catalog and committed dependency locks for the existing runtime/test dependency graph.
- Shared group/version/repository/toolchain/JUnit Platform configuration at the root.
- `test-support` exporting JUnit 5 and AssertJ.
- A minimal shared-setup smoke test in each engine module.
- Root commands for project discovery, all-module build, lock resolution, Checkstyle quality verification, and Phase 0 spikes.
- Checkstyle 14.1.0 quality rules for applicable root/module Java production and test sources: wildcard imports and empty catch blocks are errors; a deliberately narrow rule also rejects standalone ignored returns from selected fully-qualified side-effect-free `java.lang.Math` calls.
- `buildAllModules` includes the root `check` quality gate before/alongside all subproject builds.
- Root Phase 0 spike sources remain experimental and are deliberately excluded from the production Checkstyle scan; `verifyCheckstyleSourceBoundary` tests that exclusion.
- An opt-in invalid Checkstyle fixture exists for deterministic negative verification.
- Pull-request and `master` CI triggers on Windows with Java 25.
- Phase 0 feasibility spike source under root `src/main/java/com/samo/spike/`.

## What is only skeleton or planned

- All production engine/game modules are Gradle skeletons; no production lifecycle, renderer, asset, world, physics, audio, networking, runtime UI, editor, or gameplay implementation exists yet.
- JaCoCo coverage reporting is next in P1-T06 / #36.
- Module/package boundary enforcement is planned for P1-T07 / #37; current direction is configured but not architecture-tested.
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

- PR #45 CI passed on Windows / Java 25, including the root test suite and `engine-ui` tests.
- PR #47 CI run #70 passed at branch commit `41408ed0f796acaa973559ddff30642350de10b0` on Windows with Temurin 25.0.4.
- PR #48 valid-baseline CI run #74 at `701cb188cc02f3a10cd9bbf5076758d2cc099e20` passed `javaToolchains`, `projects`, `buildAllModules`, and `test` on Windows Server 2025 / Temurin 25.0.4. `buildAllModules` included the new root `check` quality gate.
- PR #48 negative-verification CI run #75 at `c9794ccbe4f34e434b8209f98b70f12b325c2b4e` intentionally enabled `InvalidCheckstyleFixture.java`. `buildAllModules` failed at `:checkstyleMain` with exactly three errors: `AvoidStarImport`, the ignored-return `RegexpSinglelineJava` rule, and `EmptyCatchBlock`. This failure is expected acceptance evidence, not a valid merge candidate.
- The invalid fixture was then restored to opt-in-only behavior; the final PR-head CI must pass before merge.
- Canonical commands and evidence requirements are in `docs/BUILD_AND_VERIFY.md`.

## P1-T05 boundary decision

Issue #35 explicitly allowed either replacing wildcard imports in the Phase 0 spikes or deliberately excluding and testing the experimental-source boundary. This implementation takes the latter route so a build-quality task does not rewrite experimental native feasibility code. The excluded tree is `src/main/java/com/samo/spike/**`, and `verifyCheckstyleSourceBoundary` fails if those sources leak into the production scan.

No product scope, public API, module dependency direction, protocol, native-ownership rule, or durable architecture decision changed in P1-T05; therefore `ENGINE_SCOPE.md`, `docs/ARCHITECTURE.md`, and `docs/DECISIONS.md` do not require changes for this task.

## Live-state reconciliation

Before starting the next task, a fresh agent must:

1. read `AGENTS.md` in full;
2. run `git status --short --branch` and `git rev-parse HEAD`;
3. fetch and compare with remote `master`;
4. inspect Issue #36, the P1 epic #2, open pull requests, and follow-up Issues #42–#44;
5. prefer newer merged code/tests and the active Issue when they legitimately supersede this commit-contained snapshot;
6. stop if the sources conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste board columns or transient in-progress state here. Update the live Issue/Project immediately; update this checkpoint in the same pull request as the durable repository change.
