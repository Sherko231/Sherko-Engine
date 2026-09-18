# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. Live GitHub Issues/PRs/workflows may be newer than this file; a fresh agent must inspect them before continuing.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Active phase | Phase 5 — Rendering foundation |
| Completed milestone | M1 — Engine Foundation (Phases 1–4) |
| P4-T08 accepted | Issue #101 / PR #175; intentionally completed before P4-T07 |
| P4-T07 accepted | Issue #100 / PR #176 |
| P4-T09 accepted | Issue #102 / PR #179 |
| P4-T09 final candidate | `a07a0e8ee9dd0e062a557cebf0259fcfb9b8706c` |
| P4-T09 heavy verification | run `35246722603` (#334), all five required jobs passed after an infrastructure-only retry of the unchanged Windows native job |
| P4-T09 merged `master` | `66a81a418e0c953b8f00e54226265aa7cd226749` |
| P4-T09 exact-merge verification | run `35247709901` (#335), Lightweight master verification passed on the exact merge SHA |
| Phase 4 exit gate | Passed — spatial tests execute in `engine-core` independently of OpenGL and Jolt |
| Phase 4 exit/readiness record | Issue #180 / Markdown-only PR #181 |
| Phase 5 activation baseline | `41988dc60b3f36ea64687e733a9c2d5a594e50e3` |
| Active executable task | None — P5-T07 / Issue #189 is accepted; P5-T08 / Issue #190 remains PLANNED |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

## Phase 4 completion

P4-T01 through P4-T09 are accepted. D-041 fixes right-handed world space with +X right, +Y up, -Z forward, meters, radians internally, right-hand positive rotation, and dimensionless scale. P4-T02 establishes the JOML mutable/preallocated hot-loop policy. D-042 through D-047 cover hierarchical transforms, cycle rejection, geometry primitives, view/projection semantics, screen-to-world mapping, and bounded transform position/quaternion quantization.

P4-T08 / Issue #101 was deliberately completed before P4-T07 so the screen-to-world implementation could consume an accepted projection/depth convention instead of guessing one.

P4-T09 / Issue #102 completed through PR #179. Final candidate `a07a0e8ee9dd0e062a557cebf0259fcfb9b8706c` passed the required five-job heavy workflow in run #334 / `35246722603`; the Windows native job was retried without changing the candidate after its first attempt received an external cancelled conclusion despite successful steps. The final workflow result is `success`. PR #179 merged as `66a81a418e0c953b8f00e54226265aa7cd226749`, and exact merged master passed Lightweight verification in run #335 / `35247709901`.

### Phase 4 exit evidence

The backlog exit gate is: **spatial tests pass independently of OpenGL and Jolt**.

The accepted P4-T09 candidate and merged master share repository tree `c2e290e844cbd4ae0f08f40796b19967cc254b84`. On that tree, heavy run #334's `Unit tests` job passed `Run root and subproject tests`. `engine-core/build.gradle.kts` declares only the scope-selected pure-Java JOML dependency (`api(libs.joml)`), has no project dependency, and targeted source search finds no LWJGL/OpenGL or Jolt usage in `engine-core` tests. The spatial test surface therefore executes independently of those native systems. Issue #180 records the gate and Phase 5 readiness review.

Conclusion: Phase 4 is complete and M1 — Engine Foundation is complete.

## Phase 5 readiness

Phase 5 is the current M2 focus. Its goal is a stable, inspectable 3D room without gameplay or physics dependencies.

Accepted prerequisites already exist:

- production GLFW/OpenGL 4.6 window/context lifecycle from Phase 3;
- canonical D-041 world-space contract;
- D-045 view/perspective convention and D-046 screen-to-world mapping;
- hierarchical transforms and geometry primitives in `engine-core`;
- existing `engine-render-opengl` module boundary;
- Java 25 / Windows x64 / OpenGL 4.6 Core / forward-renderer technology choices in `ENGINE_SCOPE.md`.

The owner removed mandatory hardware-baseline benchmarking from the roadmap on 2026-09-18. The 1080p60 target remains a development performance target, but exact minimum CPU/GPU/driver/RAM/VRAM qualification is not a current Phase 5 task or exit gate and must not be claimed without separate future evidence.

Issue #182 and follow-up #201 are closed as not planned after the owner removed mandatory hardware benchmarking from the roadmap. P5-T01 / #183 through P5-T07 / #189 are accepted. P5-T08 / #190 is the next planned renderer-foundation task and remains PLANNED until deliberately activated against the current repository state.

P5-T01 / #183 is accepted. PR #204 final head `8848b7fe7a9d2f55b14a294e4bdc4c6a3d8cd0d3` passed all five heavy CI jobs in run #352 / `35330036806`, including the Windows native OpenGL debug acceptance. It merged as `754f3ea5f1183c7a719de04776c503a0d00153cf`, and exact-merge Lightweight verification passed in run #353 / `35330526069`.

P5-T02 / #184 is accepted. PR #205 final head `7222a9a40279e65b40d70a36adf86d39165f7409` passed all five heavy CI jobs in run #354 / `35331271228`, merged as `be8263f728ddddcab609b3ad32111f71c3a9db75`, and exact-merge Lightweight verification passed in run #355 / `35331779335`.

P5-T03 / #185 is accepted. PR #206 final head `b00196ed0ac2fed6d831c3b44024d776819499a8` passed all five heavy CI jobs in run #356 / `35333540082`, merged as `0d8c2a623cf8ad53539af827e3d0d897f6d79ca6`, and exact-merge Lightweight verification passed in run #357 / `35333968825`.

P5-T04 / #186 is accepted. PR #207 final head `842c644ac72cb4819c6e0ff6b3172835738c7aba` passed all five heavy CI jobs in run #358 / `35335280661`, merged as `c11131c5d4c5a2be820a6d3f493bc0f09e2eb131`, and exact-merge Lightweight verification passed in run #359 / `35336687979`.

P5-T05 / #187 is accepted. PR #208 final head `a1fa179bc5daab0345ebfb32f3d62ed66d548d4c` passed all five heavy CI jobs in run #360 / `35337827503`, merged as `5dc3ac09a7ffc98ad249d60ad5d165c234fa4198`, and exact-merge Lightweight verification passed in run #361 / `35338981955`.

P5-T06 / #188 is accepted. PR #209 final head `a28d76fbb83df3ec7a80c9b345c67c0f1bce6d8e` passed all five heavy CI jobs in run #362 / `35339837097`, merged as `cd49273608344e1bdf0d23698591ab516d049fc8`, and exact-merge Lightweight verification passed in run #363 / `35340897864`.

P5-T07 / #189 is accepted. PR #210 final head `18e364f1bdf39d2808f7bdef15fe5b866fff2ebc` passed the required five-job final-candidate CI in run #366 / `35349866530`, including the retained Windows indexed-draw primitive-count/PNG evidence. It merged as `617d0b961d9eb84dcc118a49e8a84cf83092927d`, and exact merged `master` passed Lightweight master verification in run #367 / `35350927079`. The accepted task provides public `OpenGlRenderer`, window-owned `GlfwWindow.present()`, one internal indexed triangle with explicit depth/cull state and P5-T06 uniform blocks, deterministic backend tests, and visible integration into the persistent sandbox through public APIs only. It adds no arbitrary mesh/assets/materials/lighting/sRGB/world/ECS/physics behavior.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | End-to-end two-process SteamNetworkingSockets lifecycle |
| P0-T13 / #43 | Claims of sustained native stability | 15-minute combined native run with retained evidence |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported lifecycle cycles or explicit process-global limits |

The P0 follow-up gates do not block Phase 5 renderer-foundation work, but their unproven claims must remain narrow.

## Exact next action

P5-T07 / #189 is accepted and its handoff is reconciled. Treat P5-T08 / #190 as the next planned task only; before activating it, re-verify live GitHub/repository state and refine its executable contract if needed. Do not pull arbitrary mesh/assets, materials, lighting, world/ECS, physics, or later renderer work forward.
