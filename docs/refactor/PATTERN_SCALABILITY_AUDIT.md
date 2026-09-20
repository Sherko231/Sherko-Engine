# P5R-T24 Pattern and Scalability Audit

Baseline: `master` `90bc1a5e64eac8422050f4cfc2811c58e872d11a`.

Issue: #304 — evidence-backed pattern/scalability hardening for completed P1-P5 code.

## Result

T24 strengthens one existing pattern only: the renderer's Adapter + composition/DI seam.

The current renderer has three distinct package-private adapter contracts:

- `OpenGlResourceBackend`;
- `OpenGlDrawBackend`;
- `OpenGlUniformBlockReflectionBackend`.

Those interfaces remain separate because they model different native responsibilities and support focused fakes. The concrete scalability problem was not the interfaces themselves; it was the repeated positional **parameter cluster** of all three adapters through renderer construction.

T24 introduces package-private immutable `OpenGlBackendSet` to compose the three adapters as one renderer construction dependency while preserving each responsibility-specific interface.

## Concrete problem solved

Before T24, the same triple was passed through:

- `ReferenceSceneRenderer.create(...)`;
- production construction inside `ReferenceSceneRenderer.createProduction(...)`;
- `DebugLineRenderer.create(...)`;
- `ViewModelRenderer.create(...)`;
- all directly coupled `ReferenceSceneRendererTest` construction call sites.

That means adding one new renderer-wide backend concern would require extending several method signatures and every construction test seam independently. The adapter interfaces were already testable, but the composition boundary scaled poorly.

After T24:

- `OpenGlBackendSet` owns exactly the resource/draw/reflection adapter references;
- its compact constructor rejects null members;
- `OpenGlBackendSet.production()` constructs exactly the existing LWJGL adapters;
- `ReferenceSceneRenderer`, `DebugLineRenderer`, and `ViewModelRenderer` accept one backend set instead of three positional backend parameters;
- tests continue injecting the same fake backend implementations through one set;
- renderer collaborators unpack only the adapter they actually use.

No backend interface is merged or broadened.

## Existing patterns reviewed and kept

| Pattern / boundary | Current implementation | T24 result |
| --- | --- | --- |
| Facade | `GlfwWindow`, `OpenGlRenderer` | KEEP — already separates caller-facing API from native internals. |
| Adapter | GLFW and OpenGL backend interfaces with LWJGL implementations | KEEP + strengthen composition of the renderer trio. |
| Policy / strategy-like object | `FixedStepCatchUpPolicy`, `OpenGlMaterialStatePolicy`, input-response settings, sRGB presentation selection | KEEP — each solves a current bounded policy decision. |
| Coordinator | `SubsystemStartupCoordinator`, `FatalTerminationCoordinator` | KEEP — explicit lifecycle orchestration. |
| Composition / DI | package-private constructor/factory injection of native seams | KEEP + reduce renderer backend parameter clustering with `OpenGlBackendSet`. |
| Typed ownership wrappers | `OwnedOpenGlHandle` and specific OpenGL wrappers plus `NativeResourceRegistry` | KEEP — explicit lifetime and cleanup behavior. |

## Patterns explicitly rejected

### Singleton

Rejected. No current production service requires global singleton access. Native/window/renderer ownership is explicit and thread/lifecycle sensitive; hiding it behind a singleton would make cleanup and tests worse.

### Service Locator

Rejected. Dependencies are currently visible in constructors/factories and tests. A locator would hide dependency direction and weaken module/package boundaries.

### Object Pool

Rejected. No current allocation/throughput evidence shows a pool is required. OpenGL/native resources already have explicit lifetime contracts, while hot-loop allocation concerns are handled through preallocation/reuse where evidenced.

### ECS

Rejected. ECS/world architecture belongs to later world/game phases. P1-P5 contains no current entity/component scalability problem that justifies pulling that architecture forward.

### Job system / command bus

Rejected. No current P1-P5 concurrency/work scheduling problem requires one. Future phases own broader simulation/world concurrency work.

### Public DI container / backend registry

Rejected. The renderer adapters are internal implementation seams. There is no current plugin/backend-selection requirement and no justification to widen public API.

### One mega `OpenGlBackend` interface

Rejected. Resource ownership, draw/state submission, and uniform reflection are separate responsibilities with separate fakes/tests. Combining them would increase interface breadth and coupling.

## Behavior and boundary preservation

T24 does not change:

- public `OpenGlRenderer` signatures;
- the individual OpenGL backend interfaces;
- LWJGL native calls;
- resource creation/registration/deletion order;
- shader source/ABI/uniform bindings;
- draw order or render state;
- culling, lighting, diagnostics, debug, view-model, or sRGB behavior;
- thread affinity;
- module/package/public boundaries;
- dependencies/lockfiles/workflows;
- wiki or sandbox usage.

`OpenGlBackendSet` is package-private and remains inside the existing renderer internal package retained by T23.

## Verification intent

Focused tests must prove:

- injected adapter identity is preserved;
- null adapters are rejected;
- `production()` returns the current LWJGL resource/draw/reflection adapters;
- all existing `ReferenceSceneRendererTest` behavior still passes.

The final non-Markdown candidate requires the repository five-job CI matrix on its exact head and Lightweight master verification on the exact merge SHA.

Wiki impact: none — no supported public API/usage changes.

Sandbox impact: none — no owner-facing capability or usage changes.

Durable decision impact: none — this is an internal composition refinement of already accepted Adapter boundaries.
