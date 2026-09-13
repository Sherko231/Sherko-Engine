# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `cd6b2ae5a01e44b27647e4b5230bfd766afc630b` |
| Last accepted feature | P3-T08 / Issue #91 / PR #158; merged feature commit `ab42c30d4b186e0ecb1aab1f53a5819ac8d0e097`; exact-merge workflow #293 / run `34769991670` attempt 2 passed |
| Active feature implementation | P3-T09 / Issue #92 / branch `p3-t09-player-input-commands` |
| Next planned task | P3-T10 / Issue #93; planning-only until P3-T09 completes |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

Immediately after P3-T08, two Markdown-only commits (`ec552900...` then `cd6b2ae5...`) resulted from an authoring-tool mistake followed by an exact restoration of this file. P3-T09 starts from the restored current `master` and all implementation work is confined to its dedicated branch.

## Completed Phase 3 foundation

P3-T01 through P3-T08 are accepted, together with P3-T04A/P3-T04B sandbox maintenance. The platform/input boundary provides:

- D-031 through D-035: production GLFW/OpenGL window lifecycle, size delivery, display modes, focus/cursor safety, and relative mouse acquisition;
- D-036: immutable renderer-frame `InputSnapshot` with device-neutral key/button vocabulary and retained hardware edges;
- D-037: immutable complete data-driven action bindings for exactly eleven gameplay actions;
- D-038: caller-owned stateful renderer-frame `InputActionEvaluator`, immutable action snapshots/states, deterministic aggregation, and action-level pressed/held/released semantics.

`game-server` remains independent of `engine-platform-lwjgl`.

## P3-T09 active implementation — tick-aligned replayable commands

Issue #92 is ACTIVE / executable. The task adds a device-neutral simulation-tick command boundary without adding a project/module dependency edge or production dependency.

Current branch implementation includes:

- `engine-core` public immutable `PlayerInputCommand` with MOVE, LOOK, and the existing nine DIGITAL gameplay actions;
- `engine-core` `PlayerInputCommandCodec` using explicit fixed binary schema v1, magic `SPIC`, big-endian encoding, and exact encoded size 126 bytes;
- `engine-platform-lwjgl` public caller-owned `PlayerInputCommandSampler` converting renderer-frame `InputActionSnapshot` values into per-tick commands;
- zero-tick frames retain pending LOOK delta and digital edges; the next emitted command consumes those one-shot values exactly once;
- multiple ticks without another renderer snapshot repeat latest MOVE / digital scalar+held state while LOOK and pressed/released edges become zero/false after the first tick;
- strictly increasing successful frame/tick identities with atomic failure behavior;
- non-finite values and non-finite LOOK accumulation are rejected before sampler state advances;
- a deterministic encode/decode/headless replay test with an independently calculated expected final state;
- canonical sandbox diagnostics now emit `PlayerInputCommand` only for actually due fixed simulation ticks;
- wiki API/input/limitations guidance updated for the new public boundary.

The original activation text incorrectly totaled the codec layout as 124 bytes; the declared fields total 126 bytes (`4 + 2 + 2 + 8 + 32 + 72 + 6`). The correction is recorded on Issue #92 before final candidate verification.

## Scope boundaries

P3-T09 does not implement production networking, transport packet layout, prediction/replication, controller input, sensitivity, Y inversion, dead zones, response curves, gameplay movement/camera behavior, or P3-T10 work. The codec is a replay/storage command format, not a production network packet contract.

No `ENGINE_SCOPE.md`, external dependency, version catalog, dependency lockfile, or project/module dependency change is expected.

## Verification state

This authoring environment has connected GitHub access but no local checkout/toolchain. No local Gradle pass is claimed.

Required focused commands when executed by CI/Windows Java 25 environment:

```powershell
.\gradlew.bat :engine-core:test --tests "com.samo.engine.core.api.PlayerInputCommandCodecTest" --tests "com.samo.engine.core.api.PlayerInputCommandReplayTest" --rerun-tasks
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.PlayerInputCommandSamplerTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

Final acceptance remains the normal repository lifecycle: complete diff/docs/self-review first, one final non-draft PR, heavy five-job CI on the exact PR head, merge only while head/base remain current, then lightweight exact-merge `master` verification before closing #92.

## Phase 3 exit status

P3-T09 makes the Phase 3 backlog exit behavior executable: a fixed recorded `PlayerInputCommand` sequence can be encoded, decoded, and replayed into a headless deterministic test consumer with the same independently expected result. This evidence is limited to the input-command boundary and does not claim deterministic native physics or production network replay.

Phase 3 is **not complete** after P3-T09 because P3-T10 remains a required Phase 3 task. Do not start P3-T10 until #92 completes its full PR/merge verification lifecycle.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None of those gates blocks bounded P3-T09 input-command work.

## Exact next action

1. finish documentation/decision/build-verification synchronization and final complete-diff self-review;
2. confirm branch is based on current `master` and no open PR conflicts exist;
3. open one final non-draft PR with `Refs #92`;
4. require all five heavy PR jobs on the exact final head;
5. correct any failures on the branch and require a fresh exact-head pass;
6. merge only while the tested head/base remain current;
7. require lightweight exact-merge `master` verification;
8. record final evidence and close #92; only then freshly audit P3-T10.
