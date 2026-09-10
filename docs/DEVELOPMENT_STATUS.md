# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Pre-P1-T08 verified `master` | `b74ac96e506813f1ab67a19433078ffcbbc9f419` — merge of P1-T07 / PR #50 |
| P1-T08 work | Issue #38 / PR #51; introduced by the containing change |
| Active milestone / phase | M1 — Engine Foundation / P1 — Build, modules, and quality gates |
| Completed roadmap implementation | P1-T01, P1-T02, P1-T02A, P1-T03, P1-T03A, P1-T04, P1-T05, P1-T06, P1-T07, P1-T08 |
| Next executable task after this merge | P1-T09 — Issue #39, add client and headless-server entry points |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must run `git rev-parse HEAD`, compare the checked-out branch with remote `master`, and then inspect GitHub for activity newer than this snapshot.

## Exact next action

After PR #51 is merged and merged-`master` push CI is confirmed, unless GitHub shows newer merged/in-progress work, start from current `master`, activate Issue #39, create a dedicated P1-T09 branch, and implement only its entry-point acceptance criteria. Do not fold P1-T10+ or Phase 2 work into that change.

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
- Root Phase 0 spike sources remain experimental and outside production architecture.

## What is only skeleton or planned

- All production engine/game modules are still Gradle skeletons; no production lifecycle, renderer, asset, world, physics, audio, networking, runtime UI, editor, or gameplay implementation exists yet.
- P1-T07 defines package boundaries but deliberately does not create placeholder future subsystem APIs merely to fill empty modules.
- Client and headless-server entry points are next in P1-T09 / #39.
- Root Phase 0 spikes are experimental and have not been moved into the planned feasibility module (P1-T10A).
- A playable local engine begins in later phases; the repository cannot build or run a game yet.

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

These do not block independent Phase 1 foundation tasks unless the active Issue consumes the missing claim.

## P1-T08 verification evidence

- PR #51 run #100 proved the new build, unit-test, architecture, and JaCoCo jobs were valid, while the initial native job using `runIntegratedNativeSmoke` failed on GitHub-hosted Windows with `WGL: The driver does not appear to support OpenGL`. This was treated as an environment limitation rather than ignored or converted to a false pass.
- The native CI design was changed to a hosted-Windows-safe smoke: `runWindowsNativeCiSmoke` initializes/terminates GLFW and OpenAL with `ALSOFT_DRIVERS=null`, followed by `runJoltLifecycleSpike -PjoltSpikeCycles=1`.
- PR #51 run #105 at branch commit `338b7311a339f01934b486fc428372eb1a52561a` passed all five required jobs. The native job reported GLFW initialization success, OpenAL 1.1 success, and one Jolt JNI physics cycle with native allocation balance returning to zero.
- PR #51 run #107 at branch commit `8246ca19ecd557e03f03c041cce760bd370710e9` intentionally set `JAVA_TOOL_OPTIONS=-Darchitecture.includeInvalidFixture=true` only for the architecture job. `ModulePackageBoundaryTest.modulesOnlyImportOtherModulesThroughDeclaredApiRoots()` failed and the workflow concluded failure. This is the required controlled negative evidence and is not a merge candidate.
- The controlled-failure environment override was removed immediately afterward.
- Final PR-head CI after this documentation update must pass before merge.
- Merged-`master` push CI must pass after merge before P1-T08 is considered fully complete.
- No dependency was added, so dependency locks require no refresh.
- Canonical commands and CI environment limitations are recorded in `docs/BUILD_AND_VERIFY.md`.

## P1-T08 CI boundary

Issue #38 requires automatic compile/build, unit-test, architecture-test, and Windows native smoke coverage. The hosted runner cannot create the OpenGL 4.6 context required by the existing integrated graphics spike, so the unattended CI gate proves only the native lifecycles it can support reliably: GLFW initialization/cleanup, OpenAL initialization/cleanup through its null backend, and Jolt JNI initialization/use/cleanup. Full OpenGL 4.6 and integrated P0-T12 graphics evidence remains target-machine feasibility evidence.

This does not change `ENGINE_SCOPE.md`, `ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, or `docs/roadmap/TECHNICAL_BACKLOG.md`; P1-T08 implements their existing contract without changing product scope, module ownership, public API, protocol, or milestone order.

## Live-state reconciliation

Before starting the next task, a fresh agent must:

1. read `AGENTS.md` in full;
2. run `git status --short --branch` and `git rev-parse HEAD`;
3. fetch and compare with remote `master`;
4. inspect Issue #39, the P1 epic #2, open pull requests, and follow-up Issues #42–#44;
5. prefer newer merged code/tests and the active Issue when they legitimately supersede this commit-contained snapshot;
6. stop if the sources conflict instead of guessing.

## Maintenance rule

Update this file when a merge changes completed tasks, the exact next action, blockers, implementation maturity, or verified conclusions. Do not paste board columns or transient in-progress state here. Update the live Issue/Project immediately; update this checkpoint in the same pull request as the durable repository change.
