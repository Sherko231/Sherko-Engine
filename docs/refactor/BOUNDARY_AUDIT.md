# Phase 5R Public / Internal Boundary Audit

Baseline: `master` `5063a1f638bea9befa003b54cf3d5587178f8777`, activated by P5R-T02 / Issue #262.

This document records the current consumption and implementation boundaries before Phase 5R begins renaming/decomposition. It does **not** authorize a public rename, visibility change, package move, source split, compatibility alias, or feature change.

## Boundary model

Three different concepts must not be conflated:

1. **Java visibility** — whether a declaration uses `public`, package-private, protected, or private.
2. **Repository package/module boundary** — the `.root`, `.api`, and `.internal` roots in `config/architecture/module-boundaries.properties`, enforced for cross-module Java source references by `ModulePackageBoundaryTest`.
3. **Supported engine-consumer API** — the engine types callers are intentionally expected to compile against and that the wiki documents as production API.

A Java `public` modifier alone does not create a supported engine-consumer API.

The supported P1-P5 engine-consumer packages are currently:

- `com.samo.engine.core.api`
- `com.samo.engine.platform.api`
- `com.samo.engine.render.api`

Game executable/sandbox classes, renderer visual-demo classes, feasibility spikes, test/build helpers, and declarations under an `.internal` package are not automatically reusable engine-library API even when Java visibility is `public`.

## Existing enforcement and its limits

### D-016 source boundary

`ModulePackageBoundaryTest`:

- requires one registered module root, API root, and internal root for every declared Gradle subproject;
- validates production package ownership;
- rejects cross-module source references that do not target the target module's declared API root;
- checks both imports and fully qualified source references;
- uses the most-specific module root for overlapping package roots such as network modules.

It is intentionally source-level. It does not inspect compiled bytecode, reflection strings/resources, generated sources outside the scanned trees, or decide which `public` declarations inside one module are intended consumer API.

### Renderer compile artifact boundary

`engine-render-opengl` has stronger artifact-level separation:

- `apiElements` publishes an API-only JAR containing `com/samo/engine/render/api/**`;
- the full-main `classes` secondary artifact is removed from that compile variant;
- `verifyPublicApiBoundary` compiles a consumer against that API artifact and exported signature dependencies;
- the API artifact is checked to contain supported renderer API classes and to contain no `com/samo/engine/render/opengl/internal/**` classes;
- the normal runtime JAR retains renderer implementation and shader resources.

The task's explicit required-class list is a focused assertion, not an exhaustive declaration list; package inclusion is what carries the complete `com.samo.engine.render.api` surface.

### Core/platform artifact boundary

`engine-core` and `engine-platform-lwjgl` currently use normal Gradle Java-library artifacts rather than a separate API-only JAR. Their supported consumer boundary is therefore expressed by the declared `.api` package root, public type visibility, architecture tests for in-repository cross-module usage, and synchronized wiki documentation. There is no JPMS/strong runtime encapsulation claim.

This makes Java visibility discipline important during P5R: implementation helpers must not become `public` merely because an extraction crosses a Java package boundary.

## Supported engine-consumer API

Source has changed through bounded P5R implementation since the T01 inventory baseline. The tables below track the current accepted consumer boundary against the live boundary registry and `wiki/API_INDEX.md`; accepted T07 changes only the two explicitly authorized public lifecycle coordinator names.

### `engine-core` — `com.samo.engine.core.api`

| Type | Consumer role | Later P5R treatment |
| --- | --- | --- |
| `EngineSubsystem` | Base lifecycle contract for one engine subsystem. | KEEP public contract; T07 may refactor lifecycle internals only unless T21 authorizes a public rename. |
| `SubsystemGraph` | Declares named subsystem dependencies and resolves safe order. | KEEP; public nested `Registration` remains part of the contract. |
| `SubsystemStartupCoordinator` | Coordinates ordered startup and rollback on partial failure. | KEEP after the accepted P5R-T07 public rename; behavior remains D-020-compatible. |
| `EngineClock` | Monotonic elapsed-time sampling. | KEEP. |
| `FixedStepAccumulator` | Exact fixed-rate tick accumulation/interpolation progress. | KEEP. |
| `FixedStepCatchUpPolicy` | Bounds elapsed spikes and per-update catch-up work. | KEEP. |
| `Transform` | Canonical mutable local/world transform under D-041/D-042. | KEEP; T09 may audit internal structure without spatial semantic change. |
| `TransformQuantization` | Bounded canonical position/quaternion value quantization under D-047. | KEEP; public nested encoded values remain stable. |
| `CameraMatrices` | D-045 view/perspective matrix construction. | KEEP. |
| `ScreenRays` | D-046 screen/viewport-to-world ray construction. | KEEP. |
| `Ray3f` | Immutable normalized world-space ray/query value. | KEEP. |
| `Plane3f` | Immutable normalized world-space plane. | KEEP. |
| `Sphere3f` | Immutable world-space sphere/query value. | KEEP. |
| `Aabb3f` | Immutable world-space AABB/query value. | KEEP. |
| `Frustum3f` | Immutable six-plane frustum/query value. | KEEP. |
| `DebugColor` | Renderer-neutral finite linear debug color. | KEEP. |
| `DebugPrimitive` | Sealed renderer-neutral debug-geometry family. | KEEP public sealed hierarchy. |
| `DebugLine` | World-space debug line submission. | KEEP. |
| `DebugAabb` | Debug AABB submission. | KEEP. |
| `DebugSphere` | Debug sphere submission. | KEEP. |
| `DebugRay` | Finite debug-ray submission. | KEEP. |
| `DebugTextCounter` | Bounded diagnostic label/value submission. | KEEP. |
| `DebugFrame` | Immutable bounded per-frame debug snapshot. | KEEP. |
| `InputResponseSettings` | Platform-independent deterministic response settings/math. | KEEP. |
| `PlayerInputCommand` | Device-neutral simulation-tick player input command. | KEEP; nested digital types are public command vocabulary. |
| `PlayerInputCommandCodec` | Explicit fixed replay/storage ByteBuffer codec. | KEEP; layout/meaning remain protocol-sensitive. |
| `EngineConfigLoader` | Resolves supported startup config layers. | KEEP. |
| `EngineConfigSchema` | Validates raw startup config into typed values. | KEEP. |
| `ConfigKey` | Canonical typed configuration key (`ConfigKey<T>` in source). | KEEP. |
| `ConfigEntry` | Raw config value plus diagnostic source. | KEEP. |
| `ConfigSource` | Diagnostic config-source identity. | KEEP. |
| `ConfigError` | One source-aware config validation error. | KEEP. |
| `ConfigValidationException` | Aggregates config validation failures. | KEEP. |
| `EngineLogger` | Structured synchronous logging boundary. | KEEP; nested public logging vocabulary remains consumer API. |
| `NativeResourceRegistry` | Explicit native ownership registration/terminal diagnostics. | KEEP; nested `Registration` remains lifecycle-sensitive public API. |
| `FatalTerminationCoordinator` | One-shot orderly fatal shutdown coordination. | KEEP after the accepted P5R-T07 public rename; behavior remains D-029-compatible. |

Public nested engine-core types that remain supported consumer API:

- `EngineLogger.Level`
- `EngineLogger.Context`
- `EngineLogger.Event`
- `EngineLogger.Sink`
- `NativeResourceRegistry.Registration`
- `PlayerInputCommand.DigitalAction`
- `PlayerInputCommand.DigitalState`
- `SubsystemGraph.Registration`
- `TransformQuantization.QuantizedPosition`
- `TransformQuantization.QuantizedRotation`

Beyond the two D-066 lifecycle coordinator renames explicitly authorized by T07, further public engine-core renames remain deferred to the deliberate T21 public naming pass. P5R-T08 explicitly re-audited `EngineConfigLoader`, `EngineConfigSchema`, the `Config*` value/error types, `EngineLogger`, `EngineClock`, `FixedStepAccumulator`, `FixedStepCatchUpPolicy`, `NativeResourceRegistry`, and nested public logging/resource vocabulary and retained them unchanged because the live names already match their responsibilities.

### `engine-platform-lwjgl` — `com.samo.engine.platform.api`

| Type | Consumer role | Later P5R treatment |
| --- | --- | --- |
| `GlfwWindow` | Public platform facade owning one GLFW/OpenGL window/context lifecycle plus event/presentation/input/window-mode behavior. | **Keep facade and public signatures stable through T03-T05**; extract internal responsibilities only. |
| `OpenGlDebugMode` | Per-window production OpenGL debug-context policy. | KEEP. |
| `OpenGlThreadGuard` | Non-owning public OpenGL thread-affinity guard used by renderer-facing APIs. | KEEP. |
| `WindowSizeListener` | Renderer-neutral logical-window/framebuffer-size callback boundary. | KEEP. |
| `WindowMode` | Public started-window display-mode selection. | KEEP. |
| `InputSnapshot` | Immutable renderer-frame hardware/focus/capture/motion snapshot. | KEEP. |
| `InputKey` | Device-neutral bounded keyboard vocabulary. | KEEP. |
| `InputMouseButton` | Device-neutral bounded mouse-button vocabulary. | KEEP. |
| `InputAction` | Named gameplay action vocabulary. | KEEP; schema/config-sensitive. |
| `InputActionValueType` | DIGITAL/VECTOR2 action shape. | KEEP. |
| `InputActionComponent` | Binding target component vocabulary. | KEEP. |
| `InputBinding` | Immutable device-neutral action-binding descriptor. | KEEP; nested control hierarchy remains consumer API. |
| `InputActionBindings` | Immutable complete strict schema-v1 action-binding set and load entry point. | KEEP public boundary; T06 may split package-private parsing/validation implementation only. |
| `InputBindingLoadException` | Public binding load/schema/validation failure type. | KEEP. |
| `InputActionEvaluator` | Caller-owned renderer-frame action evaluator. | KEEP. |
| `InputActionSnapshot` | Immutable complete action state for one source frame. | KEEP. |
| `InputActionState` | Immutable state/value for one action. | KEEP. |
| `PlayerInputCommandSampler` | Renderer-frame-to-simulation-tick command bridge. | KEEP. |

Public nested `InputBinding` types that remain supported consumer API:

- `InputBinding.Control`
- `InputBinding.KeyControl`
- `InputBinding.MouseButtonControl`
- `InputBinding.MouseDeltaControl`
- `InputBinding.MouseDeltaAxis`

#### T03-T05 extraction constraint

Through P5R-T03 to P5R-T05, `GlfwWindow` implementation collaborators are extracted from the public facade while remaining colocated package-private types in `com.samo.engine.platform.api`.

When T03-T05 extract them:

- do **not** make an implementation collaborator `public` solely so `GlfwWindow` can access it from another Java package;
- a package-private extracted collaborator may remain colocated in `com.samo.engine.platform.api` even though the package name is the consumer API root; Java package-private visibility keeps it out of the consumer type surface;
- moving implementation collaborators into `com.samo.engine.platform.lwjgl.internal` is not itself required by T03-T05 and must not force public visibility;
- T23 is the normal later point to reconsider stable internal package grouping after decomposition has settled.

This constraint applies to the T01 candidates `Backend`, `LwjglBackend`, callback-registration state/sinks, mouse-motion state, and window-mode transition value types.

### `engine-render-opengl` — `com.samo.engine.render.api`

| Type | Consumer role | Later P5R treatment |
| --- | --- | --- |
| `OpenGlRenderer` | Supported bounded production renderer facade. | KEEP public contract through T10-T15. |
| `RenderFramePacket` | Immutable synchronous per-frame renderer submission snapshot. | KEEP. |
| `RenderCullingCounters` | Latest-successful-frame culling diagnostics. | KEEP. |
| `RenderLocalLight` | Sealed bounded local-light submission family. | KEEP. |
| `RenderPointLight` | Immutable point-light submission value. | KEEP. |
| `RenderSpotLight` | Immutable spot-light submission value. | KEEP. |

T10-T15 may rename/decompose renderer internals, but none may expose internal mesh/material/resource/native identities or change these public contracts unless that later Issue explicitly authorizes a public change. T21 remains the deliberate general public naming pass.

### P5R-T09 spatial/math naming audit result

The bounded engine-core spatial/math surface requires no boundary or visibility change. `Transform`, `CameraMatrices`, `ScreenRays`, geometry primitives, and `TransformQuantization` remain supported public API exactly as documented; no helper was promoted, no public type was aliased/wrapped, and no package or ownership boundary moved. Their names remain consistent with `docs/SPATIAL_CONVENTIONS.md` and D-041 through D-047. Broader public naming review remains T21 and stable package regrouping remains T23.

### P5R-T08 core naming audit result

The bounded configuration/logging/timing/native-resource surface required no boundary change. Public visibility and ownership stay exactly as documented above; no helper was promoted, no public type was wrapped or aliased, and no implementation responsibility was moved merely to create a rename. T21 remains the deliberate broad public-name review and T23 remains the stable internal package-grouping task.

## Package-private implementation surfaces

Package-private implementation is allowed inside a declared API package when it is needed to support the public facade without becoming a consumer type.

Current important examples:

| Surface | Status | Later owner |
| --- | --- | --- |
| `InputActionBindingsLoader`, `InputActionBindingsJsonParser`, and `InputActionBindingsValidator` in `com.samo.engine.platform.api` | Package-private implementation behind `InputActionBindings.load(...)` / constructor validation. Accepted T06 separates file loading, JSON/schema parsing, and domain validation/copying into named responsibilities without public promotion. | T06 owns this bounded split; T23 may later reconsider stable internal package grouping without widening consumer API. |
| `GlfwNativeBackend`, `LwjglGlfwNativeBackend`, callback registration/sink types, `GlfwInputState`, `GlfwMouseMotionTracker`, `GlfwCursorCaptureController`, `GlfwWindowModeController`, `GlfwDeferredSizeDelivery`, and GLFW window value records | Package-private implementation/testing surface colocated with the public facade; T03-T05 extract native/backend, input/focus/cursor, and mode/size responsibilities without public promotion. | T03-T05 own the bounded decomposition; T23 may later reconsider stable internal package grouping without widening consumer API. |
| Renderer material/submission/culling/light/uniform/color/view-model helpers | Package-private under `com.samo.engine.render.opengl.internal`. | T10-T15. |
| Sandbox application-loop/control-state/scene/diagnostic/framebuffer helpers | Package-private game composition behind public `SandboxMain`, not engine-library API. T16 extracts `SandboxApplicationLoop`, `SandboxControlState`, `SandboxSceneSetup`, `SandboxDiagnostics`, and `SandboxFramebufferSize`; T17 retains those boundaries and clarifies nested owner-control names. | T16 owns decomposition; T17 removes only obsolete sandbox compatibility surface and normalizes nested control names. |

A package named `.api` does not make a package-private type public API; Java visibility still matters. Conversely, a `public` type under an `.internal` package is not automatically supported consumer API.

## Internal adapter/backend boundaries

### Platform native adapter

The accepted P5R-T03 implementation extracts the replaceable/testing boundary from `GlfwWindow` into package-private top-level types in the same API package:

- `GlfwNativeBackend`
- `LwjglGlfwNativeBackend`
- responsibility-specific callback registration values and event sinks for GLFW error, size, input, cursor-position, and OpenGL debug callbacks.

The extracted seam remains implementation-only despite being colocated with the public facade. `GlfwWindow` keeps its public signatures and lifecycle/thread-affinity orchestration; the adapter keeps native callback installation/release and LWJGL/GLFW/OpenGL platform calls. Accepted T04 collaborators own input/focus/cursor state, while the accepted T05 collaborators own window-mode/restore/monitor/rollback state plus deferred logical/framebuffer size delivery. All remain package-private in the same package, and package reorganization remains deferred to T23.

### Renderer OpenGL adapters

The internal renderer has explicit adapter/backend contracts:

- `OpenGlDrawBackend` -> `LwjglOpenGlDrawBackend`
- `OpenGlResourceBackend` -> `LwjglOpenGlResourceBackend`
- `OpenGlUniformBlockReflectionBackend` -> `LwjglOpenGlUniformBlockReflectionBackend`

Native ownership wrappers include `OwnedOpenGlHandle`, `OpenGlBuffer`, `OpenGlVertexArray`, `OpenGlTexture`, `OpenGlSampler`, `OpenGlShader`, `OpenGlProgram`, and `OpenGlFramebuffer`.

These are implementation boundaries, not consumer APIs. T14 may normalize their names/ownership internals but must not publish them.

## Java-public declarations that are not supported engine-consumer API

The following public visibility is intentional/current implementation or executable accessibility, not permission for engine consumers to depend on the type:

| Type | Why it is not engine-consumer API | Later owner |
| --- | --- | --- |
| `com.samo.engine.render.opengl.internal.ReferenceSceneRenderer` | Public modifier allows cross-package use from `OpenGlRenderer`, but the type remains in the declared internal root and excluded from the renderer API artifact. | T10 established the responsibility-bearing name/fixture split; accepted T11 keeps this type as lifecycle/native-resource owner while extracting package-private non-owning frame collaborators. |
| `com.samo.engine.render.opengl.internal.RendererVisualDemo` (visualDemo source set) | Owner-facing standalone demo entry point; not in the production consumer API surface. | T18. |
| `com.samo.game.client.ClientMain` | Executable composition entry point, not reusable engine API. | T19. |
| `com.samo.game.client.internal.VersionReport` | Executable helper; public modifier supports cross-package call from `ClientMain`, not engine consumption. | T19 may rename/restructure while preserving output. |
| `com.samo.game.server.ServerMain` | Headless executable composition entry point, not reusable engine API. | T19. |
| `com.samo.game.server.internal.VersionReport` | Executable helper; public modifier supports cross-package call from `ServerMain`, not engine consumption. | T19 may rename/restructure while preserving output. |
| `com.samo.game.sandbox.SandboxMain` | Persistent owner-facing playground entry point, not an engine-library API. | T16 keeps the entry point while decomposing internals. |
| `com.samo.game.sandbox.demo.EngineDemoMain` | Removed legacy compatibility entry point; T17 reference audit found no live code/workflow/test consumer. | Removed in T17; `SandboxMain` / `runSandbox` remain canonical. |

Feasibility-spike entry points are experimental by scope and are handled separately by T20.

## T01 proposal classification and deferral

| T01 proposal | Boundary classification | Earliest allowed implementation |
| --- | --- | --- |
| `FatalTerminationCoordinator` | Supported public lifecycle coordinator name accepted by T07. | **KEEP**; broader public naming review remains T21. |
| `SubsystemStartupCoordinator` | Supported public lifecycle coordinator name accepted by T07. | **KEEP**; broader public naming review remains T21. |
| `GlfwWindow` decomposition | Public facade retained; extracted collaborators are implementation. | T03-T05, with no public signature/name change. |
| `InputActionBindingsLoader` decomposition | Package-private implementation; T06 candidate splits file loading, strict JSON/schema parsing, and domain validation/copying into named package-private collaborators. | T06. |
| Renderer internal renames/decompositions | Internal implementation; public renderer API remains unchanged. T10 renamed the fixed room/world owner and extracted `ReferenceRoomFixture`; T11 extracts package-private `RendererFrameUniformUploader`, `ReferenceSceneVisibilityPlanner`, `ReferenceSceneDrawExecutor`, and `RendererFrameDiagnostics` without promoting or transferring ownership. | T10-T15. |
| `SandboxMain` decomposition | Game/playground entry point, not engine consumer API; entry point retained. | T16. |
| `EngineDemoMain` removal | Legacy executable compatibility surface, not engine consumer API. | T17 removes it after repository-wide reference verification; no replacement alias is introduced. |
| `RendererVisualDemo` decomposition | Non-production visual-demo entry point remains public only within the dedicated source-set executable surface; extracted collaborators are package-private and not present in the production API artifact. | T18 candidate keeps this boundary unchanged. |
| client/server `VersionReport` renames | Internal executable helpers, not engine consumer API. | T19 while preserving exact compatibility output. |
| feasibility-spike naming/isolation | Experimental surface. | T20. |
| broad public engine naming review | Supported engine consumer API. | T21. |
| internal package moves | Implementation package organization after responsibilities stabilize. | T23. |

## Public-promotion rule for later P5R tasks

A later P5R task must not solve an extraction/package-access problem by promoting an implementation class/interface/record/enum to `public` unless its active Issue explicitly defines a new supported public contract and satisfies the repository's public-API review/wiki requirements.

Preferred order:

1. retain/package-private implementation beside the facade when that preserves encapsulation;
2. decompose ownership/responsibility without changing package first;
3. use existing internal adapter contracts where already accepted;
4. defer stable package reorganization to T23;
5. introduce a new public contract only through an Issue that explicitly requires one.

Existing public modifiers in internal/executable surfaces are implementation debt/entry-point mechanics to be handled by their bounded later tasks, not precedent.

## Architecture / documentation consequences

- No new architecture decision is created by T02; this audit documents the already accepted D-016/D-055 boundaries and current source state.
- `docs/DECISIONS.md` therefore requires no new decision row.
- `docs/BUILD_AND_VERIFY.md` requires no new command.
- Wiki content remains unchanged because no supported API, signature, lifecycle, failure/config semantics, or usage changed.
- Sandbox source/README remain unchanged because no runtime capability changed.

## Verification record

- Fresh baseline: `master` `5063a1f638bea9befa003b54cf3d5587178f8777`.
- P5R-T01 source inventory baseline: `262a1d5dfca529979d10c5bd45aad3ce488343a5`.
- Comparison from the T01 source baseline through the T02 activation baseline contains only Markdown files; no Java/Gradle/resource/workflow source changed after the T01 inventory.
- Boundary registry reviewed: `config/architecture/module-boundaries.properties`.
- Boundary implementation reviewed: `ModulePackageBoundaryTest`.
- Renderer artifact boundary reviewed: `engine-render-opengl/build.gradle.kts` and `verifyPublicApiBoundary`.
- Consumer-role documentation reviewed: `wiki/API_INDEX.md` and `wiki/LIMITATIONS.md`.
- T01 naming/inventory reviewed: `docs/refactor/NAMING_STANDARD.md` and `docs/refactor/SYMBOL_INVENTORY.md`.
- No boundary-test source change is required by the findings above.

Wiki impact: none — existing public consumption boundaries were audited but not changed.

Sandbox impact: none — documentation/boundary audit only.
