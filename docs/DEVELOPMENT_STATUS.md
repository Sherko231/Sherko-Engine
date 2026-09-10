# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-cleanup `master` | `835573704d3e80dc3b00dc309de5661558aa4f27` — merge of P1-T10 / PR #53; merged-master CI #123 passed |
| Current cleanup | Issue #54; removes only the unused root `Launcher.java` placeholder |
| Active milestone / phase | M1 — Engine Foundation / P1 — Build, modules, and quality gates |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05, P1-T06, P1-T07, P1-T08, P1-T09, P1-T10 |
| Next planned Phase 1 item | P1-T10A in `docs/roadmap/TECHNICAL_BACKLOG.md`; no executable Issue exists yet, so it must be materialized before implementation |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare the checked-out branch with remote `master`, and then inspect GitHub for activity newer than this snapshot.

## Exact next action

Complete cleanup Issue #54 only after its pull request CI passes and merged-`master` push CI passes. After that, the next planned Phase 1 item remains P1-T10A. It is not executable until a dedicated Issue exists; do not implement P1-T10A or Phase 2 work without that Issue.

## What is actually implemented

- Gradle Wrapper and Java 25 toolchain configuration.
- Sixteen declared subprojects matching `ENGINE_SCOPE.md`, including `engine-ui` and `test-support`.
- One-way Gradle project dependency graph with runtime UI separated from the OpenGL adapter.
- Central dependency version catalog and committed dependency locks for the current runtime/test/tool dependency graph.
- Shared group/version/repository/toolchain/JUnit Platform configuration at the root.
- `test-support` exporting JUnit 5 and AssertJ.
- A minimal shared-setup smoke test in each of the 12 current test-bearing engine modules.
- Root commands for project discovery, all-module build, lock resolution, Checkstyle quality verification, JaCoCo report verification, architecture verification, and Phase 0 spikes.
- Checkstyle 14.1.0 quality rules for applicable root/module Java production and test sources.
- JaCoCo 0.8.15 reporting for the 12 current test-bearing engine modules, with XML/HTML verification and CI artifacts.
- `config/architecture/module-boundaries.properties` declares one owned package root, one public API root, and one internal implementation root for every one of the 16 Gradle subprojects.
- `ModulePackageBoundaryTest` verifies registry completeness, verifies production source packages stay under their owning module root, and rejects cross-module imports outside the destination module's declared API root.
- A deliberate negative fixture represents a forbidden `game-client -> engine-platform-lwjgl.internal` shortcut and is disabled during normal builds.
- Package/API boundary decision D-016 is recorded in `docs/DECISIONS.md` and reflected in `docs/ARCHITECTURE.md`.
- `.github/workflows/java25.yml` has explicit Windows Java 25 jobs for build/quality, unit tests, architecture tests, JaCoCo coverage, and hosted-Windows native smoke.
- The hosted native smoke initializes/terminates GLFW, initializes/cleans up OpenAL through the null backend, and runs one real Jolt JNI physics lifecycle cycle.
- `game-client` has `com.samo.game.client.ClientMain` plus repeatable Gradle task `:game-client:runClient`.
- `game-server` has `com.samo.game.server.ServerMain` plus repeatable Gradle task `:game-server:runServer`.
- `game-server:check` depends on `verifyHeadlessServerRuntime`, which rejects platform/render/audio engine projects and GLFW/OpenGL/OpenAL artifacts from the server runtime classpath.
- `config/version.properties` centrally defines protocol and asset compatibility identifiers.
- Both runnable entry points accept `--version` and report executable identity, exact engine Git commit, protocol version, asset version, Java runtime version, and native-library versions resolved for that executable.
- Version metadata is generated during the build from the current Git checkout plus each executable runtime classpath; an executable with no matching native artifacts reports `nativeLibraries=none`.
- CI runs both version reports from the same checkout, requires the shared identifiers to match, and requires the reported engine commit to equal the exact workflow SHA.
- The unused root `src/main/java/com/samo/Launcher.java` placeholder is removed by cleanup Issue #54; the real runnable entry points remain `ClientMain` and `ServerMain`.
- Root Phase 0 spike sources remain experimental and outside production architecture.

## What is only skeleton or planned

- Production engine subsystems and `game-sandbox` remain Gradle skeletons; no production lifecycle, renderer, asset, world, physics, audio, networking, runtime UI, editor, or gameplay implementation exists yet.
- The client/server entry points remain intentionally empty foundation composition roots apart from version reporting. They do not initialize later production subsystems.
- P1-T10 version identifiers are reporting/build metadata only; protocol negotiation and asset migration are not implemented.
- Root Phase 0 spikes are experimental and have not been moved into the planned feasibility module (P1-T10A).
- A playable local engine begins in later phases; Phase 1 proves foundation/build contracts rather than a playable game.

## Verified feasibility baseline

- Java 25 and the selected Windows x64 native stack can run together.
- GLFW/OpenGL 4.6, Jolt JNI, OpenAL, and localhost UDP were exercised independently on a suitable Windows target machine.
- P0-T11 deterministically reproduced latency, jitter, loss, duplication, and reordering.
- Steamworks4j initialized Steam and callbacks, but does not expose the required modern `ISteamNetworkingSockets` surface.
- Java 25 FFM loaded the official Steam flat API and obtained/used an `ISteamNetworkingSockets` pointer. This proves API access only.
- P0-T12 exercised graphics, physics, audio, and UDP together for 15 seconds under JFR and shut down cleanly. It is a smoke test, not sustained-stability evidence.
- GitHub-hosted Windows can execute headless-safe GLFW/OpenAL/Jolt lifecycle smoke, but its runner image does not provide an OpenGL 4.6 driver/context suitable for the full P0-T12 integrated graphics smoke.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These do not block independent foundation tasks unless the active Issue consumes the missing claim.

## Cleanup #54 verification requirements

Before merge, run and record:

```powershell
.\gradlew.bat buildAllModules
.\gradlew.bat test
```

Acceptance requires `src/main/java/com/samo/Launcher.java` to be absent, no `com.samo.Launcher` references to remain, existing client/server entry points to remain unchanged, and the final PR-head CI plus merged-`master` push CI to pass.

This cleanup does not change dependencies, module direction, public engine APIs, protocols, asset formats, ownership, feasibility conclusions, or the planned P1-T10A migration of Phase 0 spikes.

## Live-state reconciliation

Before starting the next task, a fresh agent must:

1. read `AGENTS.md` in full;
2. run `git status --short --branch` and `git rev-parse HEAD`;
3. fetch and compare with remote `master`;
4. inspect the P1 epic #2, open pull requests, and follow-up Issues #42–#44;
5. confirm whether a P1-T10A executable Issue has been created before attempting that planned backlog item;
6. prefer newer merged code/tests and the active Issue when they legitimately supersede this commit-contained snapshot;
7. stop if the sources conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste board columns or transient in-progress state here. Update the live Issue/Project immediately; update this checkpoint in the same pull request as the durable repository change.
