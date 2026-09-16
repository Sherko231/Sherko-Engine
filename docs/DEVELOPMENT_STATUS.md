# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` for P4-T01 | `e1801b11a713ce6cc73276c644aa15351ac508a1` |
| Last accepted phase | Phase 3 — Platform and input |
| Last accepted Phase 3 task | P3-T10 / Issue #93 / PR #160 |
| P3-T10 final candidate | `cc5842c37956f67272b62f4071b016523ac86159` |
| P3-T10 heavy verification | workflow #299 / run `35105650405`, all five required jobs passed |
| P3-T10 merged `master` | `e1801b11a713ce6cc73276c644aa15351ac508a1` |
| P3-T10 exact-merge verification | workflow #300 / run `35106618677`, lightweight master verification passed |
| Active phase | Phase 4 — Math and spatial conventions |
| Accepted Phase 4 task | P4-T01 / Issue #94 / PR #161 |
| P4-T01 merged commit | `dc00a615a9a7e44dedf6f85a4b2867cd282a6e54` |
| P4-T01 verification | Markdown-only exemption after complete-diff and contradiction audit; no build/runtime CI required by policy |
| Cross-cutting maintenance candidate | Issue #165 — persistent cumulative `game-sandbox` playground; confirm live PR/CI state before treating it as accepted |
| Next planned roadmap task | P4-T02 / Issue #95 — requires fresh audit/activation before implementation |
| Milestone | M1 — Engine Foundation remains in progress through P1-P4 |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Phase 3 accepted

Phase 3 is complete through P3-T10. Its accepted platform/input foundation includes:

- production GLFW/OpenGL window lifecycle, sizing, display-mode, focus/cursor, and relative-mouse ownership under D-031 through D-035;
- immutable renderer-frame hardware snapshots under D-036;
- data-driven gameplay action bindings under D-037;
- deterministic renderer-frame action evaluation/transitions under D-038;
- immutable device-neutral per-tick `PlayerInputCommand`, fixed 126-byte replay/storage codec, tick sampler, and deterministic headless replay under D-039;
- deterministic `InputResponseSettings` for mouse sensitivity/Y inversion plus the bounded axis-local controller response primitive under D-040.

P3-T09's replay scenario satisfied the Phase 3 exit behavior and remained green on the final P3-T10 candidate. P3-T10 merged through PR #160 and exact merged `master` passed the required lightweight verifier. Issue #93 is closed completed. The Phase 4 entry planning review is recorded on #93 and #94.

`game-server` remains independent of `engine-platform-lwjgl`.

## Persistent sandbox maintenance — Issue #165 candidate

P3-T04A originally established `game-sandbox` as the owner-facing observation surface using a scripted approximately 38-second timeline. Issue #165 deliberately replaces that presentation/maintenance model without changing the accepted Phase 3 engine APIs or Phase 4 ordering.

The candidate model is one persistent cumulative owner playground:

- canonical command: `.\gradlew.bat :game-sandbox:runSandbox`;
- no fixed duration, automatic feature tour, or automatic shutdown;
- `F` cycles windowed/borderless/exclusive modes;
- `R` toggles cursor capture;
- `Right Shift + F` cycles public mouse-sensitivity settings;
- `Right Shift + R` toggles public mouse-Y inversion;
- `Ctrl + Q` requests orderly sandbox exit;
- ordinary gameplay/action bindings remain active concurrently;
- fixed-step timing, action snapshots, tick commands, structured logging, and native-resource cleanup continue running together;
- `runEngineDemo` is compatibility-only and delegates to the same playground; it is not a second experience.

The durable maintenance rule is in `AGENTS.md`: when a future capability is meaningfully usable through already-authorized public production APIs, integrate it into this same cumulative playground and preserve existing usable capabilities. Do not replace the sandbox with a temporary timed showcase. If a meaningful sandbox path would require internals, direct native calls, or future roadmap work, record `Sandbox impact: none — <reason>` instead.

This maintenance task adds no renderer, gameplay camera, controller-capture API, world/physics gameplay, networking integration, UI/editor surface, or new engine public API merely to make the sandbox richer. The window therefore remains visually empty until the production renderer boundary exists.

Because Issue #165 changes Java/Gradle files, it is not eligible for the Markdown-only exemption. Before accepting it, require the exact final PR head to pass the normal five-job heavy matrix and require the exact merged `master` commit to pass the ordinary lightweight verifier. Live GitHub state is authoritative for whether those steps are complete.

## P4-T01 accepted — canonical spatial conventions

P4-T01 / Issue #94 established the canonical spatial documentation/architecture contract through PR #161, merged as `dc00a615a9a7e44dedf6f85a4b2867cd282a6e54`.

The accepted world-space convention is:

- right-handed Cartesian world;
- +X right;
- +Y up;
- -Z forward (+Z backward);
- meters for world position/distance and meters per second for linear velocity;
- radians internally and radians per second for angular velocity;
- positive rotation follows the right-hand rule around the positive axis;
- transform scale is dimensionless, with `(1,1,1)` as identity.

The normative document is `docs/SPATIAL_CONVENTIONS.md`; D-041 records the durable decision. External library/format differences must be converted at adapter/import/export boundaries rather than redefining engine world space.

P4-T01 deliberately does not choose projection/NDC depth convention, reversed-Z, FOV/near/far policy, Euler storage/order, quaternion canonical sign, glTF/Jolt/OpenAL conversion details, network quantization, or any P4-T02+ runtime implementation.

## P4-T01 verification and evidence

P4-T01 was documentation/architecture only. The final PR changed exactly eight Markdown files:

- `README.md`;
- `ROADMAP.md`;
- `docs/ARCHITECTURE.md`;
- `docs/DECISIONS.md`;
- `docs/DEVELOPMENT_STATUS.md`;
- `docs/SPATIAL_CONVENTIONS.md`;
- `wiki/CORE/SPATIAL_CONVENTIONS.md`;
- `wiki/README.md`.

The complete changed-file audit found no Java, Gradle, dependency, lockfile, workflow, resource, sandbox source, module-edge, or `ENGINE_SCOPE.md` change. Repository searches found no conflicting authoritative `left-handed` or degree-based world-space contract; existing backlog references to meters/radians agreed with D-041. `ENGINE_SCOPE.md` remained unchanged and continues to select JOML.

Because every changed path ended in `.md`, PR #161 qualified for the repository Markdown-only exemption. Heavy five-job CI and post-merge lightweight runtime verification were not required; no passing runtime/build result is claimed for this documentation-only task. No workflow run was generated for the final PR head or merge commit, as expected under the exemption.

Independent review was not performed because no separate reviewer identity was available in the authoring environment. Residual risk is documentation-level ambiguity in the frozen spatial convention; mitigation is the executable Issue contract, D-041, normative document, architecture/wiki cross-check, contradiction search, and complete-diff audit.

Sandbox impact: none — P4-T01 added no executable/human-observable runtime capability.

## Deferred P4-T01 acceptance evidence

The original backlog wording requires renderer, physics, and asset-conversion tests to cite the canonical spatial document. Those production paths do not exist yet and were not fabricated by P4-T01. Their future executable Issues must cite `docs/SPATIAL_CONVENTIONS.md` when those tests become real.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None of those gates blocks Phase 4 spatial documentation/math work or the sandbox maintenance task.

## Exact next action

Inspect live GitHub state first. If Issue #165 is not yet accepted, finish only that bounded maintenance contract through exact final-head heavy CI, merge, exact-merge lightweight verification, and issue closure. Once #165 is accepted, audit P4-T02 / Issue #95 against current `master`, the accepted D-041 spatial convention, JOML scope, hot-loop allocation evidence requirements, and current architecture before converting #95 from planning to an executable contract. Do not implement P4-T02 from its planning body without that fresh activation.
