# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Last verified predecessor merge | PR #45 — architecture/roadmap coherence |
| Predecessor `master` commit | `22a99053c3fec74495e2c870c2898d4e85630177` |
| Handoff-system work | Issue #46; introduced by the commit/PR containing this document |
| Active milestone / phase | M1 — Engine Foundation / P1 — Build, modules, and quality gates |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04 |
| Next executable task | P1-T05 — Issue #35, add Checkstyle quality rules |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare the checked-out branch with remote `master`, and then check GitHub for activity newer than this snapshot.

## Exact next action

Unless GitHub shows newer merged/in-progress work, start from current `master`, activate Issue #35, create a dedicated P1-T05 branch, and implement only its Checkstyle acceptance criteria. Do not begin Phase 2 or fold P1-T06+ work into that change.

## What is actually implemented

- Gradle Wrapper and Java 25 toolchain configuration.
- Sixteen declared subprojects matching `ENGINE_SCOPE.md`, including `engine-ui` and `test-support`.
- One-way Gradle project dependency graph with runtime UI separated from the OpenGL adapter.
- Central dependency version catalog and committed dependency locks.
- Shared group/version/repository/toolchain/JUnit Platform configuration at the root.
- `test-support` exporting JUnit 5 and AssertJ.
- A minimal shared-setup smoke test in each engine module.
- Root commands for project discovery, all-module build, lock resolution, and Phase 0 spikes.
- Pull-request and `master` CI triggers on Windows with Java 25; current CI runs toolchain reporting and the Gradle test suite.
- Phase 0 feasibility spike source under root `src/main/java/com/samo/spike/`.

## What is only skeleton or planned

- All production engine/game modules are Gradle skeletons; no production lifecycle, renderer, asset, world, physics, audio, networking, runtime UI, editor, or gameplay implementation exists yet.
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
- The current build declares 16 subprojects in `settings.gradle.kts`.
- `buildAllModules` is defined to depend on every subproject build, but the predecessor handoff did not preserve direct execution evidence for the post-`engine-ui` graph. Do not repeat the earlier overclaim; run and record it for this handoff change/CI expansion.
- Canonical commands and evidence requirements are in `docs/BUILD_AND_VERIFY.md`.

## Live-state reconciliation

Before starting work, a fresh agent must:

1. read `AGENTS.md` in full;
2. run `git status --short --branch` and `git rev-parse HEAD`;
3. fetch and compare with remote `master`;
4. inspect Issue #35, the P1 epic #2, open pull requests, and follow-up Issues #42–#44;
5. prefer newer merged code/tests and the active Issue when they legitimately supersede this commit-contained snapshot;
6. stop if the sources conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste board columns or transient in-progress state here. Update the live Issue/Project immediately; update this checkpoint in the same pull request as the durable repository change.
