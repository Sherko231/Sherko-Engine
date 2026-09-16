# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `b9144e9e939cf468a5228f499aac130886d16456` |
| Last accepted feature before this candidate | P3-T09 / Issue #92 / PR #159; merged commit `b9144e9e939cf468a5228f499aac130886d16456`; heavy workflow #294 / run `34771652425` passed; exact-merge workflow #295 / run `34771935986` passed |
| Final Phase 3 task represented by this checkpoint | P3-T10 / Issue #93 / PR #160 candidate; consult live GitHub state for final-candidate/merge verification and closure |
| Next planned phase | Phase 4 planning review only after P3-T10 acceptance and Phase 3 exit confirmation |
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

## P3-T10 implementation represented here — deterministic input response settings

Issue #93 is the executable contract for the final required Phase 3 task. This checkpoint contains its bounded implementation; live GitHub Issue #93 / PR #160 state determines whether final-candidate CI, merge, exact-merge verification, the Phase 4 planning review, and closure have occurred after this commit was created.

Implemented scope:

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

D-040 records the durable response-settings ownership/flow.

## Scope boundaries

P3-T10 does not implement controller/gamepad capture, radial stick dead zones, mouse smoothing/acceleration, per-axis sensitivity, settings persistence/UI, gameplay camera/movement, production networking, packet changes, prediction/replication, or Phase 4 work.

No `ENGINE_SCOPE.md`, external dependency, version catalog, dependency lockfile, workflow, project/module dependency edge, binding JSON schema, `InputSnapshot`, `PlayerInputCommand`, replay codec, or game-server dependency change is authorized or present in this candidate.

## Verification state

This authoring environment has connected GitHub access but no local repository checkout/toolchain. No local Gradle pass is claimed.

Focused verification selectors for P3-T10 are:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.InputResponseSettingsTest" --rerun-tasks
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.InputActionEvaluatorTest" --tests "com.samo.engine.platform.api.InputActionEvaluatorResponseSettingsTest" --rerun-tasks
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.PlayerInputCommandReplayTest" --rerun-tasks
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Final acceptance requires the normal heavy five-job matrix on the exact final PR head. If the branch changes after a pass, the prior run is obsolete. Merge only while the tested head and base remain current; then require the lightweight exact-merge `master` verifier. The authoritative current result belongs in live PR #160 / Issue #93 because workflow state can advance after this file's commit.

## Phase 3 exit status

The P3-T09 replay scenario exercises the backlog exit behavior: a fixed device-neutral command sequence can be encoded, decoded, and replayed into a headless deterministic test consumer with the same independently calculated result.

P3-T10 is the last required Phase 3 task. Do not infer Phase 3 completion from this file alone. Confirm live evidence that the exact P3-T10 final candidate passed its response tests plus existing replay evidence, merged with a current base, and the exact merged `master` passed the lightweight verifier. Then perform and record the P4 planning review before activating P4-T01.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None of those gates blocks bounded P3-T10 response-settings work or the subsequent Phase 4 planning review.

## Exact next action

Inspect live Issue #93 and PR #160 before doing anything else.

- If PR #160 has not yet passed heavy CI on its current head, finish that exact-candidate verification; any newer commit invalidates older CI evidence.
- If the exact current candidate passed but is not merged, re-confirm its head and `master` base are unchanged, then merge.
- If merged, require the lightweight exact-merge `master` verifier on the merge SHA.
- Only after that evidence passes, record the Phase 4 planning review against the completed Phase 3 evidence, record final P3-T10 completion evidence, and close #93.
- Do not activate or implement P4-T01 until those live steps are complete.
