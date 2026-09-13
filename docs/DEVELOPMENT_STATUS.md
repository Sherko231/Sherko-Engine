# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `92ad157adb696bd5a9d21933f6d9af60be3634a7` — P3-T06 / Issue #89 merged through PR #156; lightweight exact-merge workflow #286 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A plus P1-T08A maintenance, P2-T01 through P2-T13 plus Phase 2 exit gate, P3-T01 through P3-T06 plus P3-T04A/P3-T04B |
| Active feature implementation | P3-T07 / Issue #90 / branch `p3-t07-action-bindings` — strict versioned gameplay action-binding loader |
| Next planned task | P3-T08 / Issue #91 — planning-only until P3-T07 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect local status/HEAD when available plus live remote `master`, Issues, PRs, and workflow state before continuing.

## Stable completed foundation

Phase 1 and Phase 2 remain complete. The Phase 2 D-030 / Issue #135 integrated exit gate passed on exact merged `master` `88ddfbd64468ba85d37eb32e1f6d40c9083e1326`: at least 60 continuous seconds of integrated fixed 60 Hz simulation ticks, bounded catch-up after the injected stall, orderly lifecycle shutdown, and empty `NativeResourceRegistry` cleanup were verified together.

P0-T09A / #42, P0-T13 / #43, and P0-T14 / #44 remain independent feasibility gates. Phase 1/2 completion and later bounded platform/input tests do not satisfy or strengthen those gates.

## Phase 3 completed work

P3-T01 through P3-T06 are complete, together with P3-T04A/P3-T04B sandbox maintenance.

The production platform/input boundary now provides:

- D-031: explicit GLFW/OpenGL 4.6 lifecycle/native ownership;
- D-032: owner-thread event polling and separated logical/framebuffer dimensions;
- D-033: in-place windowed/borderless/exclusive primary-monitor transitions;
- D-034: focus-loss-safe held-state cleanup and explicit cursor recapture policy;
- D-035: relative mouse acquisition using GLFW raw mode when supported and a disabled-cursor position-delta fallback otherwise;
- D-036: immutable renderer-frame `InputSnapshot` values with engine-owned key/button vocabulary, retained hardware edges, focus/capture state, and consumed relative mouse delta.

P3-T05 / Issue #88 completed through PR #155:

- exact final head `5348f10e139e7b8dd337e296b90a296130bca614` passed heavy workflow #282 / run `34718462000` with all five jobs;
- retained `p3-t05-mouse-motion` artifact ID `10305972206`, digest `sha256:1d790b2b0854be28763d0e172f0cb7330eaf6531c86f8bb751a1ddf990956ec0`;
- merged `master` `2f3dcd3d9117db28b994115f0c420b884e5a59b0` passed lightweight workflow #283 / run `34718902190`;
- Issue #88 closed completed.

P3-T06 / Issue #89 completed through PR #156:

- final PR head `946629bb1135605f7b9f5600b780bbfbfc5a27bc` passed heavy workflow #285 / run `34721031011` with all five jobs;
- merged `master` `92ad157adb696bd5a9d21933f6d9af60be3634a7` passed lightweight workflow #286 / run `34721328876`;
- public `InputSnapshot`, `InputKey`, `InputMouseButton`, and `GlfwWindow.captureInputSnapshot(long)` are merged;
- sandbox input diagnostics use the public snapshot API only;
- `game-server` remains independent of `engine-platform-lwjgl`;
- Issue #89 closed completed.

`game-sandbox` remains the canonical owner-facing manual demo. Its demo-only platform runtime remains non-consumable/non-exported so `game-server` stays headless.

Run the demo on Windows x64 with:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

## P1-T08A CI lifecycle

P1-T08A / Issue #153 established the current runner-efficient lifecycle:

1. implementation/tests/docs/wiki/sandbox/self-review happen on the dedicated branch before the final non-draft PR;
2. the exact final PR candidate runs the five-job heavy matrix;
3. merge is allowed only while the tested head/base remain current;
4. the exact merged `master` commit runs one lightweight verifier covering dependency locks, the headless-server runtime boundary, and exact client/server version metadata;
5. a stronger post-merge full matrix is task-specific/manual, not routine duplication.

Markdown-only exemption rules remain governed by `AGENTS.md`, `docs/CI_LIFECYCLE.md`, and `docs/BUILD_AND_VERIFY.md`.

## P3-T07 active implementation — data-driven gameplay action bindings

Issue #90 was freshly audited and activated from verified `master` `92ad157adb696bd5a9d21933f6d9af60be3634a7`. Dedicated branch: `p3-t07-action-bindings`.

The bounded implementation introduces D-037 without changing the project/module graph:

- public `InputAction` defines exactly MOVE, LOOK, JUMP, CROUCH, SPRINT, INTERACT, GRAB, THROW, PRIMARY_USE, PAUSE, and PUSH_TO_TALK;
- MOVE and LOOK are `VECTOR2`; the remaining actions are `DIGITAL`;
- immutable `InputBinding` descriptors reuse P3-T06 `InputKey` / `InputMouseButton` plus relative mouse X/Y controls and signed finite non-zero scale;
- immutable `InputActionBindings` stores one complete binding set and loads strict JSON schema version 1 atomically;
- every required action must appear exactly once with at least one binding;
- unknown/duplicate fields, actions, controls, binding types, invalid action components, malformed JSON, invalid schema version, missing/unreadable files, and duplicate binding descriptors fail explicitly;
- Jackson is an implementation-only parser dependency: `jackson-databind`/`jackson-core` 2.21.2 with the matching 2.21 annotations line; public signatures expose no Jackson type;
- no action evaluation, pressed/held/released aggregation, controller support, input-response settings, `PlayerInputCommand`, replay/network encoding, renderer/camera behavior, or P3-T08+ implementation is included;
- no project/module dependency edge is added and `game-server` remains headless/platform-independent.

Deterministic acceptance coverage is in `InputActionBindingsTest` with a committed complete v1 JSON fixture. Expected values are handwritten rather than derived from the production parser.

### Sandbox impact

Sandbox impact: none — P3-T07 adds configuration metadata/loading only. A meaningful human-observable action demo would require P3-T08 action evaluation, so the current sandbox remains unchanged rather than pulling future work forward.

### Wiki/API impact

P3-T07 adds public consumer API and a persisted JSON configuration schema, so this task updates:

- `wiki/API_INDEX.md`;
- `wiki/PLATFORM/INPUT.md`;
- `wiki/LIMITATIONS.md`.

P3-T08 action-state evaluation and P3-T09 command/replay APIs remain documented as unavailable.

## Verification state for P3-T07

The current execution environment can inspect and modify the connected GitHub repository but does not provide the repository checkout/toolchain needed to execute Gradle locally. Therefore no local Gradle command is represented as passing in this handoff.

Required focused verification before acceptance:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.InputActionBindingsTest" --rerun-tasks
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

The task intentionally changes the platform-module dependency lock for Jackson. The committed lock state records `jackson-databind:2.21.2`, `jackson-core:2.21.2`, and `jackson-annotations:2.21`. Final acceptance still requires the repository heavy five-job PR matrix on the exact final candidate; CI execution is evidence only when the configured runner actually runs the jobs.

## Phase 3 status and exact next action

Phase 3 remains in progress. P3-T01 through P3-T06 are accepted; P3-T07 / #90 is active and is not accepted until its exact final PR candidate and exact merged commit pass the current CI lifecycle.

Exact next action for P3-T07:

1. complete documentation/wiki synchronization and complete-diff self-review on `p3-t07-action-bindings`;
2. open one final non-draft PR linked with `Refs #90` only after the candidate is complete;
3. require the five-job heavy matrix on the exact final PR head, including architecture, unit, coverage, build/quality, and existing Windows native regression jobs;
4. if the candidate changes after a pass or `master` advances relative to the tested base, refresh and reverify before merge;
5. merge only the exact verified candidate;
6. require the lightweight exact-merge `master` verifier, record acceptance/review evidence, close #90, then freshly audit P3-T08 / #91.

The Phase 3 exit remains the backlog requirement that an identical recorded input sequence can be replayed into headless simulation. Loading action metadata alone does not satisfy that exit gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input work, but bounded P3 evidence must not be represented as satisfying them.

## Live-state reconciliation

A fresh agent must read `AGENTS.md` first, inspect local status/HEAD when a checkout exists, inspect live remote `master`, Issue #90, branch `p3-t07-action-bindings`, any open PR, and workflow state, then compare those facts with this checkpoint. If live GitHub state has moved beyond this document, live GitHub controls workflow status. Stop rather than guess if code, docs, the active Issue, PR, or verification evidence conflict.
