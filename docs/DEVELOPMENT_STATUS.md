# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `b9144e9e939cf468a5228f499aac130886d16456` |
| Last accepted feature | P3-T09 / Issue #92 / PR #159; merged commit `b9144e9e939cf468a5228f499aac130886d16456`; heavy workflow #294 / run `34771652425` passed; exact-merge workflow #295 / run `34771935986` passed |
| Active feature implementation | P3-T10 / Issue #93 / branch `p3-t10-input-response-settings` |
| Next planned phase | Phase 4 planning review after P3-T10 completion and Phase 3 exit confirmation |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Accepted Phase 3 foundation through P3-T09

P3-T01 through P3-T09 are accepted, together with P3-T04A/P3-T04B sandbox maintenance. The platform/input boundary provides:

- D-031 through D-035: production GLFW/OpenGL window lifecycle, size delivery, display modes, focus/cursor safety, and relative mouse acquisition;
- D-036: immutable renderer-frame `InputSnapshot` with engine-owned key/button vocabulary and retained hardware edges;
- D-037: immutable complete data-driven action bindings for exactly eleven gameplay actions;
- D-038: caller-owned stateful renderer-frame `InputActionEvaluator`, deterministic aggregation, and action-level pressed/held/released semantics;
- D-039: immutable device-neutral per-tick `PlayerInputCommand`, explicit fixed 126-byte replay/storage codec, renderer-frame-to-tick sampler, and deterministic headless replay evidence.

P3-T09 completed through PR #159. Its exact final PR candidate `0e480de55f43adbd73f96b39b18bd6eaab1e2008` passed heavy five-job workflow #294 / run `34771652425`. Merge commit `b9144e9e939cf468a5228f499aac130886d16456` passed lightweight exact-merge workflow #295 / run `34771935986`. Issue #92 is closed completed.

`game-server` remains independent of `engine-platform-lwjgl`.

## P3-T10 active implementation — deterministic input response settings

Issue #93 is ACTIVE / executable. The branch starts exactly from verified `master` `b9144e9e...`.

Current implementation scope:

- new immutable `engine-core` `InputResponseSettings` with mouse sensitivity, Y inversion, controller dead zone, and controller response exponent;
- neutral defaults `1.0 / false / 0.0 / 1.0`;
- strict finite/range validation;
- deterministic `applyMouseX`, `applyMouseY`, and axis-local `applyControllerAxis` response math;
- `InputActionEvaluator(InputActionBindings)` remains source-compatible and uses neutral defaults;
- new evaluator constructor accepts explicit `InputResponseSettings`;
- `setResponseSettings(...)` changes future evaluator frames only;
- relative mouse delta is shaped before existing binding scale and additive aggregation;
- controller response math is defined and tested without introducing controller discovery, polling, vocabulary, callbacks, or bindings;
- P3-T09 `PlayerInputCommand`, codec, sampler, and replay semantics remain unchanged.

D-040 is the intended durable decision for this bounded response-settings ownership/flow.

## Scope boundaries

P3-T10 does not implement controller/gamepad capture, radial stick dead zones, mouse smoothing/acceleration, per-axis sensitivity, settings persistence/UI, gameplay camera/movement, production networking, packet changes, prediction/replication, or Phase 4 work.

No `ENGINE_SCOPE.md`, external dependency, version catalog, dependency lockfile, workflow, project/module dependency edge, binding JSON schema, `InputSnapshot`, `PlayerInputCommand`, replay codec, or game-server dependency change is authorized.

## Verification state

This authoring environment has connected GitHub access but no local repository checkout/toolchain. No local Gradle pass is claimed.

Focused verification required on the final candidate:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.InputResponseSettingsTest" --rerun-tasks
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.InputActionEvaluatorTest" --tests "com.samo.engine.platform.api.InputActionEvaluatorResponseSettingsTest" --rerun-tasks
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.PlayerInputCommandReplayTest" --rerun-tasks
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

The final non-draft PR must pass the normal heavy five-job matrix on the exact final head. If the branch changes after that pass, the prior run becomes obsolete. Merge only while the tested head and base remain current; then require the lightweight exact-merge `master` verifier.

## Phase 3 exit status

The P3-T09 replay scenario already exercises the backlog exit behavior: a fixed device-neutral command sequence can be encoded, decoded, and replayed into a headless deterministic test consumer with the same independently calculated result.

P3-T10 is the last required Phase 3 task. Phase 3 may be marked complete only after the P3-T10 exact final candidate passes its response tests plus the existing replay evidence, merges, and the exact merged `master` passes the lightweight verifier. Then perform and record the P4 planning review before activating P4-T01.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None of those gates blocks bounded P3-T10 response-settings work.

## Exact next action

1. finish D-040 / architecture / build-verification / wiki synchronization and complete-diff self-review;
2. confirm branch remains based on current `master` and no conflicting PR exists;
3. open one final non-draft PR with `Refs #93`;
4. require all five heavy PR jobs on the exact final head;
5. correct any failures only on the task branch and require a fresh exact-head pass;
6. merge only while the tested head/base remain current;
7. require lightweight exact-merge `master` verification;
8. record final P3-T10 and Phase 3 exit evidence, perform the P4 planning review, and close #93 only when all acceptance is consistent.
