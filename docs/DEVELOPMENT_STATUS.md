# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `5012235cc0fcc2fc702919cf2e7bf185bf3c3595` — P3-T07 / Issue #90 merged through PR #157; lightweight exact-merge workflow #291 passed |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 and P2 are complete |
| Completed roadmap implementation | P1-T01 through P1-T10A plus P1-T08A maintenance, P2-T01 through P2-T13 plus Phase 2 exit gate, P3-T01 through P3-T07 plus P3-T04A/P3-T04B |
| Active feature implementation | P3-T08 / Issue #91 / branch `p3-t08-action-transitions` — renderer-frame gameplay action evaluation/transitions |
| Next planned task | P3-T09 — planning-only until P3-T08 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect local status/HEAD when available plus live remote `master`, Issues, PRs, and workflow state before continuing.

## Stable completed foundation

Phase 1 and Phase 2 remain complete. The Phase 2 D-030 / Issue #135 integrated exit gate passed on exact merged `master` `88ddfbd64468ba85d37eb32e1f6d40c9083e1326`: at least 60 continuous seconds of integrated fixed 60 Hz simulation ticks, bounded catch-up after the injected stall, orderly lifecycle shutdown, and empty `NativeResourceRegistry` cleanup were verified together.

P0-T09A / #42, P0-T13 / #43, and P0-T14 / #44 remain independent feasibility gates. Phase 1/2 completion and later bounded platform/input tests do not satisfy or strengthen those gates.

## Phase 3 completed work

P3-T01 through P3-T07 are complete, together with P3-T04A/P3-T04B sandbox maintenance.

The production platform/input boundary now provides:

- D-031: explicit GLFW/OpenGL 4.6 lifecycle/native ownership;
- D-032: owner-thread event polling and separated logical/framebuffer dimensions;
- D-033: in-place windowed/borderless/exclusive primary-monitor transitions;
- D-034: focus-loss-safe held-state cleanup and explicit cursor recapture policy;
- D-035: relative mouse acquisition using GLFW raw mode when supported and a disabled-cursor position-delta fallback otherwise;
- D-036: immutable renderer-frame `InputSnapshot` values with engine-owned key/button vocabulary, retained hardware edges, focus/capture state, and consumed relative mouse delta;
- D-037: immutable complete `InputActionBindings` with exactly eleven typed actions and strict JSON schema-v1 loading through implementation-only Jackson 2.21.2.

P3-T06 / Issue #89 completed through PR #156: exact final head `946629bb1135605f7b9f5600b780bbfbfc5a27bc` passed heavy workflow #285 / run `34721031011`; merged `master` `92ad157adb696bd5a9d21933f6d9af60be3634a7` passed lightweight workflow #286 / run `34721328876`.

P3-T07 / Issue #90 completed through PR #157:

- exact final head `b05d5a21e34be8bd19f34e428bd67fcd83ba945f` passed heavy workflow #290 / run `34767716312` with Build/quality, Unit, Architecture, JaCoCo, and Windows native smoke all successful;
- merged `master` `5012235cc0fcc2fc702919cf2e7bf185bf3c3595` passed lightweight workflow #291 / run `34768018171`;
- committed dependency locks, headless server boundary, and exact-merge client/server version reporting passed on the merge commit;
- Issue #90 is closed completed.

`game-server` remains independent of `engine-platform-lwjgl`.

## P1-T08A CI lifecycle

P1-T08A / Issue #153 established the current runner-efficient lifecycle:

1. implementation/tests/docs/wiki/sandbox/self-review happen on the dedicated branch before the final non-draft PR;
2. the exact final PR candidate runs the five-job heavy matrix;
3. merge is allowed only while the tested head/base remain current;
4. the exact merged `master` commit runs one lightweight verifier covering dependency locks, the headless-server runtime boundary, and exact client/server version metadata;
5. a stronger post-merge full matrix is task-specific/manual, not routine duplication.

Markdown-only exemption rules remain governed by `AGENTS.md`, `docs/CI_LIFECYCLE.md`, and `docs/BUILD_AND_VERIFY.md`.

## P3-T08 active implementation — action evaluation and transitions

Issue #91 was freshly audited and activated from verified `master` `5012235cc0fcc2fc702919cf2e7bf185bf3c3595`. Dedicated branch: `p3-t08-action-transitions`.

The bounded implementation introduces D-038 without changing the project/module graph or external dependencies:

- public `InputActionEvaluator` evaluates one immutable `InputSnapshot` against one immutable `InputActionBindings` set;
- immutable `InputActionSnapshot` exposes one complete evaluated renderer-frame action view;
- immutable `InputActionState` exposes `pressed`, `held`, `released`, scalar `value`, and vector `x`/`y` with irrelevant components fixed at zero;
- key/button bindings contribute their signed scale while held; mouse-delta bindings contribute the current snapshot delta multiplied by signed scale;
- simultaneous contributions add deterministically in binding order with no clamp, normalization, sensitivity, inversion, dead-zone, or response curve;
- DIGITAL actions are active when scalar value is non-zero; VECTOR2 actions are active when either component is non-zero; exact signed cancellation is inactive;
- action transitions are derived from the previous successful aggregate activity;
- a complete one-frame tap is preserved only when the same bound key/button reports both press and release in the current hardware snapshot;
- pressing another binding while an action is already active does not duplicate `pressed`; releasing one binding while another keeps the aggregate active does not emit `released`;
- frame IDs must be strictly increasing after the first successful evaluation; gaps are allowed;
- non-finite mouse delta or aggregate values fail before evaluator state advances;
- failed evaluations leave prior action activity and last successful frame identity unchanged;
- evaluator calls are externally serialized; no thread-safe/shared evaluator contract is introduced;
- P3-T09 tick-aligned `PlayerInputCommand`, replay/network codecs, controller/settings, sensitivity/dead zones/curves, UI consumption, and renderer/camera behavior remain out of scope.

Focused tests cover digital transitions, one-frame taps, mouse buttons, overlapping bindings, exact cancellation, vector MOVE, mouse-delta LOOK, focus-loss-style release, frame-order rejection, non-finite failures, immutability, and the cross-binding false-tap regression.

### Sandbox impact

P3-T08 is meaningfully observable through existing public APIs, so the canonical `game-sandbox` is updated in the same branch. It loads a committed demo schema-v1 binding set through public `InputActionBindings`, evaluates every renderer-frame hardware snapshot through `InputActionEvaluator`, and extends the bounded diagnostic line with MOVE X/Y plus JUMP and INTERACT transition state while retaining raw W/A/S/D and mouse-delta observation.

The sandbox remains a diagnostic/manual-observation surface, not gameplay, a renderer benchmark, P3-T09 tick input, or acceptance authority. Its platform dependency remains demo-only/non-consumable so the headless server boundary is unchanged.

Run the demo on Windows x64 with:

```powershell
.\gradlew.bat :game-sandbox:runEngineDemo
```

### Wiki/API impact

P3-T08 adds public action-evaluation API and caller-visible transition/aggregation/failure semantics. Update in this task:

- `wiki/API_INDEX.md`;
- `wiki/PLATFORM/INPUT.md`;
- `wiki/LIMITATIONS.md`.

P3-T09 tick-aligned commands/replay and P3-T10 controller/settings remain documented as unavailable.

## Verification state for P3-T08

The current authoring environment can inspect and modify the connected GitHub repository but has no repository checkout/toolchain, so no local Gradle command is represented as passing.

Required focused verification before acceptance:

```powershell
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.InputActionEvaluatorTest" --tests "com.samo.engine.platform.api.InputActionEvaluatorEdgeTest" --rerun-tasks
.\gradlew.bat :engine-platform-lwjgl:test --tests "com.samo.engine.platform.api.InputActionBindingsTest" --tests "com.samo.engine.platform.api.InputActionEvaluatorTest" --tests "com.samo.engine.platform.api.InputActionEvaluatorEdgeTest" --rerun-tasks
.\gradlew.bat :game-sandbox:test --rerun-tasks
.\gradlew.bat :game-server:verifyHeadlessServerRuntime
.\gradlew.bat :test-support:test --tests "com.samo.architecture.ModulePackageBoundaryTest" --rerun-tasks
.\gradlew.bat resolveAndLockAllDependencies
```

No dependency or lockfile change is expected. The canonical routine verification matrix and CI lifecycle in `docs/BUILD_AND_VERIFY.md` are unchanged; Issue #91 is the task-specific focused-command contract.

Final acceptance still requires the heavy five-job PR matrix on the exact final candidate, merge only while candidate head/base remain current, then one passing lightweight verifier on the exact merged `master` commit.

## Phase 3 status and exact next action

Phase 3 remains in progress. P3-T01 through P3-T07 are accepted; P3-T08 / #91 is active and is not accepted until its exact final PR candidate and exact merged commit pass the current CI lifecycle.

Exact next action for P3-T08:

1. finish documentation/wiki synchronization and complete-diff self-review on `p3-t08-action-transitions`;
2. inspect the complete diff against `5012235cc0fcc2fc702919cf2e7bf185bf3c3595`, confirming no dependency/lock/module/schema/P3-T09 scope drift;
3. open one final non-draft PR linked with `Refs #91` only after the candidate is complete;
4. require the five-job heavy matrix on the exact final PR head;
5. if the candidate changes after a pass or `master` advances relative to the tested base, refresh and reverify before merge;
6. merge only the exact verified candidate;
7. require the lightweight exact-merge `master` verifier, record acceptance/review evidence, close #91, then freshly audit P3-T09.

The Phase 3 exit remains the backlog requirement that an identical recorded input sequence can be replayed into headless simulation. Renderer-frame action evaluation alone does not satisfy that exit gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connection/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined native execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

None of these gates blocks ordinary P3 platform/input work, but bounded P3 evidence must not be represented as satisfying them.

## Live-state reconciliation

A fresh agent must read `AGENTS.md` first, inspect local status/HEAD when a checkout exists, inspect live remote `master`, Issue #91, branch `p3-t08-action-transitions`, any open PR, and workflow state, then compare those facts with this checkpoint. If live GitHub state has moved beyond this document, live GitHub controls workflow status. Stop rather than guess if code, docs, the active Issue, PR, or verification evidence conflict.
