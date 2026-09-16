# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified starting `master` | `e1801b11a713ce6cc73276c644aa15351ac508a1` |
| Last accepted phase | Phase 3 — Platform and input |
| Last accepted task | P3-T10 / Issue #93 / PR #160 |
| P3-T10 final candidate | `cc5842c37956f67272b62f4071b016523ac86159` |
| P3-T10 heavy verification | workflow #299 / run `35105650405`, all five required jobs passed |
| Accepted merged `master` | `e1801b11a713ce6cc73276c644aa15351ac508a1` |
| Exact-merge verification | workflow #300 / run `35106618677`, lightweight master verification passed |
| Active phase | Phase 4 — Math and spatial conventions |
| Active task | P4-T01 / Issue #94 / branch `p4-t01-spatial-conventions` |
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

## P4-T01 active — canonical spatial conventions

Issue #94 is ACTIVE / executable. The task branch starts exactly from accepted `master` `e1801b11...`.

P4-T01 is documentation/architecture only. It establishes one canonical world-space contract before transform/camera/renderer/physics/assets/audio/network spatial code begins relying on implicit assumptions:

- right-handed Cartesian world;
- +X right;
- +Y up;
- -Z forward (+Z backward);
- meters for world position/distance and meters/second for linear velocity;
- radians internally and radians/second for angular velocity;
- positive rotation follows the right-hand rule around the positive axis;
- transform scale is dimensionless, with `(1,1,1)` as identity.

The normative document is `docs/SPATIAL_CONVENTIONS.md`; D-041 records the durable decision. External library/format differences must be converted at adapter/import/export boundaries rather than redefining engine world space.

P4-T01 deliberately does not choose projection/NDC depth convention, reversed-Z, FOV/near/far policy, Euler storage/order, quaternion canonical sign, glTF/Jolt/OpenAL conversion details, network quantization, or any P4-T02+ implementation.

## Verification / CI state for P4-T01

P4-T01 is authorized as a Markdown-only task. No local Gradle execution is claimed by this connected authoring environment.

Before final PR:

- inspect the complete branch diff and require every changed path to end in `.md` for the Markdown-only exemption;
- search the repository for conflicting authoritative handedness/axis/unit claims;
- verify `ENGINE_SCOPE.md` is unchanged and still selects JOML;
- verify no Java/Gradle/dependency/lock/workflow/module-edge change exists;
- verify D-041 does not pull projection/depth/adapter implementation choices forward;
- reconcile `README.md`, `ROADMAP.md`, architecture, wiki navigation, and this handoff with Phase 3 completion / Phase 4 activation.

If the final complete diff remains Markdown-only, heavy PR CI and post-merge lightweight runtime verification are not required by `AGENTS.md`; their absence is an expected exemption, not a passing test. If any non-Markdown path appears, the exemption is lost and the normal heavy-final-candidate plus exact-merge lifecycle applies.

## Deferred acceptance evidence

The original P4-T01 backlog wording requires renderer, physics, and asset-conversion tests to cite the canonical spatial document. Those production paths do not exist yet and must not be fabricated by P4-T01. Their future executable Issues must cite `docs/SPATIAL_CONVENTIONS.md` when those tests become real.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

None of those gates blocks Phase 4 spatial documentation/math work.

## Exact next action

Finish the bounded P4-T01 Markdown changes, run the complete-diff/contradiction audit, record review provenance and sandbox/wiki impact, then open one final PR linked with `Refs #94`. Merge only after confirming the Markdown-only exemption still applies and `master` has not advanced; close #94 only after merged repository state and documentation are consistent. Do not activate P4-T02 until P4-T01 is accepted.
