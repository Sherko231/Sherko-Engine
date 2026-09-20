# Phase 5R Symbol Inventory

Planning baseline: `master` `262a1d5dfca529979d10c5bd45aad3ce488343a5` (P5R-T01 activation).

This is a planning inventory, not executable permission to rename/move/split/delete code. Every later change must be re-validated against the then-current source and its active Issue.

## Coverage method

A recursive repository-tree audit found **214 Java files total**. P5R-T01 classifies the **107 production/runtime top-level Java files** under current runtime source sets (`src/main/java` plus the standalone renderer `src/visualDemo/java`), excluding tests/fixtures and excluding `feasibility-spikes` because P5R-T20 explicitly owns the experimental spike naming/isolation pass. Empty/deferred modules with no production Java are listed separately below.

Covered production/runtime top-level files by module:

- `engine-core`: 36
- `engine-platform-lwjgl`: 19
- `engine-render-opengl`: 43 production + 1 visual-demo file
- `game-client`: 2
- `game-sandbox`: 5
- `game-server`: 2

Total: **107**.

The inventory also records relevant public/package-level nested types that materially affect later P5R decomposition or public-contract review.

Action vocabulary is defined in [NAMING_STANDARD.md](NAMING_STANDARD.md).

## engine-core

| Symbol | Visibility | Action | Proposed name/location | Current responsibility / rationale | Contract risk |
| --- | --- | --- | --- | --- | --- |
| `Aabb3f` | public class | **KEEP** | — | World-space AABB value is precise and conventional. | Public spatial API; D-041 semantics must not change. |
| `CameraMatrices` | public class | **KEEP** | — | Constructs accepted view/projection matrices; name states matrix responsibility. | Public spatial API; D-045/D-046 sensitive. |
| `ConfigEntry` | public record | **KEEP** | — | Raw config value plus source is accurately described. | Public config contract. |
| `ConfigError` | public record | **KEEP** | — | Represents one validation error precisely. | Public config failure contract. |
| `ConfigKey` | public class | **KEEP** | — | Typed config key is already concise and exact. | Public config contract. |
| `ConfigSource` | public record | **KEEP** | — | Diagnostic source value is explicit. | Public config contract. |
| `ConfigValidationException` | public class | **KEEP** | — | Exception name exactly states failure category. | Public failure contract. |
| `DebugAabb` | public record | **KEEP** | — | Renderer-neutral debug AABB submission. | Public debug/spatial API. |
| `DebugColor` | public record | **KEEP** | — | Linear debug RGB value; domain term is clear. | Public rendering-neutral value. |
| `DebugFrame` | public class | **KEEP** | — | Immutable bounded per-frame debug snapshot; existing name is sufficiently clear. | Public debug-frame contract. |
| `DebugLine` | public record | **KEEP** | — | Debug line primitive is exact. | Public spatial API. |
| `DebugPrimitive` | public sealed interface | **KEEP** | — | Common debug geometry capability is explicit. | Public sealed hierarchy. |
| `DebugRay` | public record | **KEEP** | — | Finite debug ray submission is clear. | Public spatial API. |
| `DebugSphere` | public record | **KEEP** | — | Debug sphere primitive is exact. | Public spatial API. |
| `DebugTextCounter` | public record | **KEEP** | — | Diagnostic label/value counter, not general text; name already captures the limitation. | Public debug contract. |
| `EngineClock` | public class | **KEEP** | — | Monotonic elapsed-time sampler; accepted name is clear in engine-core context. | Public timing API. |
| `EngineConfigLoader` | public class | **KEEP** | — | Loads layered startup configuration and delegates validation; Loader is accurate. | Public config API. |
| `EngineConfigSchema` | public class | **KEEP** | — | Typed startup configuration schema/validation boundary; name is established and clear. | Public config API. |
| `EngineLogger` | public class | **KEEP** | — | Structured engine logging boundary; name is accurate. | Public logging API; nested public types retained. |
| `EngineSubsystem` | public abstract class | **KEEP** | — | Defines subsystem lifecycle contract; name is domain-accurate. | Public lifecycle API. |
| `FatalTerminationCoordinator` | public class | **KEEP** | — | Coordinates orderly fatal shutdown before process termination; T07 applied the responsibility-revealing Coordinator suffix. | Public lifecycle API; renamed in P5R-T07 with semantics unchanged. |
| `FixedStepAccumulator` | public class | **KEEP** | — | Exact fixed-step accumulation responsibility is explicit. | Public timing API. |
| `FixedStepCatchUpPolicy` | public class | **KEEP** | — | Explicit catch-up limiting policy; role suffix is correct. | Public timing policy. |
| `Frustum3f` | public class | **KEEP** | — | Conventional immutable 3D frustum value. | Public spatial API. |
| `InputResponseSettings` | public record | **KEEP** | — | Deterministic response settings are correctly named. | Public input/config semantics. |
| `NativeResourceRegistry` | public class | **KEEP** | — | Tracks explicit native ownership registrations; Registry is accurate rather than vague Manager. | Public ownership/lifecycle API. |
| `Plane3f` | public class | **KEEP** | — | Conventional immutable 3D plane value. | Public spatial API. |
| `PlayerInputCommand` | public class | **KEEP** | — | Simulation-tick device-neutral player command is accurately named. | Public command/protocol-facing semantics. |
| `PlayerInputCommandCodec` | public class | **KEEP** | — | Explicit binary codec role is clear. | Public binary layout/protocol-sensitive API. |
| `Ray3f` | public class | **KEEP** | — | Conventional immutable 3D ray value. | Public spatial API. |
| `ScreenRays` | public class | **KEEP** | — | Builds world rays from screen coordinates; plural utility name is accepted and specific. | Public D-046 spatial API. |
| `Sphere3f` | public class | **KEEP** | — | Conventional immutable 3D sphere value. | Public spatial API. |
| `SubsystemGraph` | public class | **KEEP** | — | Immutable subsystem dependency graph is accurately named. | Public lifecycle/dependency contract. |
| `SubsystemStartupCoordinator` | public class | **KEEP** | — | Coordinates ordered startup plus rollback sequencing; T07 applied the responsibility-revealing Coordinator suffix. | Public lifecycle API; renamed in P5R-T07 with rollback semantics unchanged. |
| `Transform` | public class | **KEEP** | — | Canonical transform domain type; shorter name is clearer than a suffixed alternative. | Public spatial/hierarchy API. |
| `TransformQuantization` | public class | **KEEP** | — | Explicit bounded transform quantization boundary. | Public serialization/quantization contract; D-047 sensitive. |

## engine-platform-lwjgl

| Symbol | Visibility | Action | Proposed name/location | Current responsibility / rationale | Contract risk |
| --- | --- | --- | --- | --- | --- |
| `GlfwWindow` | public class | **DECOMPOSE** | `Keep public GlfwWindow facade; extract internal GLFW backend/callback, input/focus/cursor, and mode/size collaborators` | 1837-line facade owns several separable responsibilities; roadmap T03-T05 explicitly require decomposition. | Public API/facade, native ownership, thread affinity, focus/cursor/window-mode behavior must remain unchanged. |
| `InputAction` | public enum | **KEEP** | — | Named gameplay action vocabulary is clear. | Public input/config schema names. |
| `InputActionBindings` | public class | **KEEP** | — | Immutable complete binding set is clear. | Public schema-v1 config API. |
| `InputActionBindingsLoader` | package-private class | **DECOMPOSE** | `Provisional: InputBindingsJsonParser + InputBindingsValidator behind the existing load boundary` | 250-line loader mixes JSON syntax parsing and semantic validation; P5R-T06 explicitly targets this split. | Internal only; schema v1/error meaning must remain compatible. |
| `InputActionComponent` | public enum | **KEEP** | — | Action value component enum is precise. | Public config schema. |
| `InputActionEvaluator` | public class | **KEEP** | — | Evaluates frame snapshots into action state; responsibility is explicit. | Public input semantics. |
| `InputActionSnapshot` | public class | **KEEP** | — | Immutable action-state snapshot for one renderer frame. | Public snapshot contract. |
| `InputActionState` | public class | **KEEP** | — | One action's evaluated state is explicit. | Public input contract. |
| `InputActionValueType` | public enum | **KEEP** | — | Value shape enum is exact. | Public config/input contract. |
| `InputBinding` | public record | **KEEP** | — | Descriptor connecting one control to one action component; concise domain name is sufficient. | Public config schema; nested control hierarchy retained. |
| `InputBindingLoadException` | public class | **KEEP** | — | Precisely names load/validation failure surface. | Public failure contract. |
| `InputKey` | public enum | **KEEP** | — | Device-neutral keyboard key vocabulary. | Public input API. |
| `InputMouseButton` | public enum | **KEEP** | — | Device-neutral mouse-button vocabulary. | Public input API. |
| `InputSnapshot` | public class | **KEEP** | — | Immutable hardware snapshot name is exact. | Public input snapshot API. |
| `OpenGlDebugMode` | public enum | **KEEP** | — | Names production OpenGL debug context mode. | Public platform/render configuration. |
| `OpenGlThreadGuard` | public class | **KEEP** | — | Guard role and OpenGL affinity are explicit. | Public thread-affinity contract. |
| `PlayerInputCommandSampler` | public class | **KEEP** | — | Samples frame action state into simulation-tick commands; name is exact. | Public bridge semantics. |
| `WindowMode` | public enum | **KEEP** | — | Display mode enum is established and unambiguous. | Public window API. |
| `WindowSizeListener` | public interface | **KEEP** | — | Listener receives logical/framebuffer size signals; name is acceptable. | Public callback API. |

## engine-render-opengl public API

| Symbol | Visibility | Action | Proposed name/location | Current responsibility / rationale | Contract risk |
| --- | --- | --- | --- | --- | --- |
| `OpenGlRenderer` | public class | **KEEP** | — | Public OpenGL renderer facade; name is exact and stable. | Public renderer API; ownership/thread affinity. |
| `RenderCullingCounters` | public record | **KEEP** | — | Frame culling diagnostics are accurately named. | Public diagnostic API. |
| `RenderFramePacket` | public class | **KEEP** | — | Immutable renderer-facing frame snapshot; Packet is accepted renderer submission vocabulary here. | Public render submission contract. |
| `RenderLocalLight` | public sealed interface | **KEEP** | — | Renderer-facing local-light hierarchy is explicit. | Public spatial/render API. |
| `RenderPointLight` | public record | **KEEP** | — | Renderer-facing point-light submission is exact. | Public spatial/render API. |
| `RenderSpotLight` | public record | **KEEP** | — | Renderer-facing spot-light submission is exact. | Public spatial/render API. |

## engine-render-opengl internals and visual demo

| Symbol | Visibility | Action | Proposed name/location | Current responsibility / rationale | Contract risk |
| --- | --- | --- | --- | --- | --- |
| `BoundedDynamicBufferUploader` | package-private class | **KEEP** | — | Name states bounded dynamic upload role and resource ownership behavior. | Internal native ownership; P5R-T14 may simplify duplication only with equivalent cleanup. |
| `CameraMatricesUniformBlock` | package-private class | **KEEP** | — | T13 canonical name states that this std140 CPU packer carries view/projection matrices specifically. | Internal shader ABI: GLSL `CameraBlock`, binding, offsets, and layout remain unchanged. |
| `CleanupFailureSuppression` | package-private class | **KEEP** | — | T14 canonical name for the narrow helper that preserves a primary failure while adding only distinct cleanup failures as suppressed; it does not own resources. | Internal failure-preservation semantics must remain exact. |
| `CpuFrustumCuller` | package-private class | **KEEP** | — | CPU frustum visibility test responsibility is explicit. | Internal spatial semantics. |
| `DebugLineRenderer` | package-private class | **KEEP** | — | Renders debug lines and owns required GL resources; role is clear. | Internal native ownership. |
| `DebugLineVertexPacker` | package-private class | **KEEP** | — | Packs debug-line vertices; precise role. | Internal packing/layout behavior. |
| `DirectionalLight` | package-private record | **KEEP** | — | Internal directional-light value is exact. | Internal shader/light semantics. |
| `DrawSubmission` | package-private record | **KEEP** | — | Internal draw submission descriptor is precise. | Internal render ordering/state semantics. |
| `DrawSubmissionSorter` | package-private class | **KEEP** | — | Establishes documented draw order; role suffix is correct. | Internal ordering contract. |
| `ReferenceSceneRenderer` | public-in-internal-package class | **DECOMPOSE** | T10 accepted name; T11 accepted frame-orchestration decomposition | Fixed renderer-owned reference-room/world lifecycle/resource owner and public-facade delegate. T11 extracts non-owning frame collaborators while retaining creation/rollback/close ownership. | Implementation type but public modifier for cross-package `OpenGlRenderer` access; excluded from API artifact. |
| `RendererFrameUniformUploader` | package-private class | **KEEP** | — | T11 owner for reusable camera/per-frame/local-light packing and upload; borrows buffer handles without owning them. | Internal upload ordering and shader-block ABI consumers. |
| `ReferenceSceneVisibilityPlanner` | package-private class | **KEEP** | — | T11 owner for frustum extraction, fixed reference-scene visibility, and ordered draw-submission preparation. | Internal culling/sorting semantics; T12 may rename existing subordinate culling/submission/light types only. |
| `ReferenceSceneDrawExecutor` | package-private class | **KEEP** | — | T11 owner for world/debug/view-model frame execution and GL-state restoration; borrows program/VAO/renderers without closing them. | Internal draw/state/order semantics. |
| `RendererFrameDiagnostics` | package-private class | **KEEP** | — | T11 owner for latest-success culling/text-counter publication state. | Public facade exposes snapshots but not this owner type. |
| `LocalLightSelector` | package-private class | **KEEP** | — | T12 canonical name for the stateful bounded first-N local-light selector actor. | Internal selection capacity/logging semantics. |
| `LocalLightUniformBlock` | package-private class | **KEEP** | — | Payload and shader block role are explicit. | Internal shader ABI/layout. |
| `LwjglOpenGlDrawBackend` | package-private class | **KEEP** | — | LWJGL adapter implementing the OpenGL draw backend contract. | Internal native adapter/thread affinity. |
| `LwjglOpenGlResourceBackend` | package-private class | **KEEP** | — | LWJGL adapter implementing resource operations. | Internal native ownership/adapter. |
| `LwjglOpenGlUniformBlockReflectionBackend` | package-private class | **KEEP** | — | LWJGL adapter for uniform-block reflection; long but exact. | Internal shader reflection adapter. |
| `OpenGlMaterialStatePolicy` | package-private record | **KEEP** | — | T12 canonical name makes the concrete OpenGL blend/depth/cull state mapping explicit. | Internal fixed-state mapping; no material semantics change. |
| `OpenGlBuffer` | package-private class | **KEEP** | — | Owned OpenGL buffer wrapper is precise. | Native ownership/thread affinity. |
| `OpenGlDrawBackend` | package-private interface | **KEEP** | — | Replaceable backend boundary for GL draw/state work. | Internal adapter contract. |
| `OpenGlFramebuffer` | package-private class | **KEEP** | — | Owned framebuffer wrapper is precise. | Native ownership/thread affinity. |
| `OpenGlProgram` | package-private class | **KEEP** | — | Owned linked program wrapper is precise. | Native ownership/thread affinity. |
| `OpenGlResourceBackend` | package-private interface | **KEEP** | — | Replaceable GL resource backend boundary is precise. | Internal adapter/ownership contract. |
| `OpenGlSampler` | package-private class | **KEEP** | — | Owned sampler wrapper is precise. | Native ownership/thread affinity. |
| `OpenGlShader` | package-private class | **KEEP** | — | Owned shader wrapper is precise. | Native ownership/thread affinity. |
| `OpenGlTexture` | package-private class | **KEEP** | — | Owned texture wrapper is precise. | Native ownership/thread affinity/color encoding. |
| `OpenGlUniformBlockReflectionBackend` | package-private interface | **KEEP** | — | Reflection backend contract is exact. | Internal shader ABI inspection. |
| `OpenGlVertexArray` | package-private class | **KEEP** | — | Owned VAO wrapper is precise. | Native ownership/thread affinity. |
| `OwnedOpenGlHandle` | package-private class | **KEEP** | — | Generic owned GL-handle lifecycle primitive; ownership is explicit in name. | Native ownership/cleanup. |
| `FramebufferMetricsUniformBlock` | package-private class | **KEEP** | — | T13 canonical name states that the block payload is framebuffer size and inverse size rather than general per-frame state. | Internal shader ABI: GLSL `PerFrameBlock`, binding, offsets, and layout remain unchanged. |
| `SrgbPresentationMode` | package-private enum | **KEEP** | — | T13 canonical name states the hardware-vs-manual sRGB presentation-encoding dimension explicitly. | Internal D-056/D-063 presentation semantics. |
| `RenderMaterialDescriptor` | package-private record | **KEEP** | — | T12 canonical name describes the immutable declarative material value without implying renderer/resource ownership. | Internal material/state behavior; no public resource API. |
| `SrgbTransfer` | package-private class | **KEEP** | — | Exact sRGB transfer-function utility. | Internal color semantics. |
| `TextureColorEncoding` | package-private enum | **KEEP** | — | Distinguishes linear vs sRGB texture storage/interpretation. | Internal D-056 color semantics. |
| `UniformBlockLayoutVerifier` | package-private class | **KEEP** | — | Verifies reflected block layout against expected ABI; precise. | Internal shader ABI validation. |
| `ViewFrustumExtractor` | package-private class | **KEEP** | — | Extracts frustum planes from view/projection matrices; exact. | Internal spatial semantics. |
| `ViewModelProjectionFactory` | package-private class | **KEEP** | — | T15 canonical name for the stateless creator of the dedicated fixed view-model projection matrix. | Internal view-model spatial constants must remain unchanged. |
| `ViewModelFixtureVertexPacker` | package-private class | **KEEP** | — | T15 owner for the fixed six-vertex validation-fixture byte packing; keeps fixture layout/color data out of the GL resource owner. | Internal validation-fixture bytes must remain unchanged. |
| `ViewModelRenderer` | package-private class | **KEEP** | — | Dedicated view-model render path and GL ownership; precise. | Internal native ownership/view-model semantics. |
| `RendererVisualDemo` | visualDemo public class | **KEEP ENTRY POINT / DECOMPOSED** | `RendererVisualDemoApplication`, `RendererVisualDemoLoop`, `RendererVisualDemoFramebufferSize`, `AnimatedDemoLighting`, `MaterialComparisonOverlay` | T18 accepted implementation reduces the entry point to launch-only while giving lifecycle, frame-loop, framebuffer state, animated lighting, and GL overlay ownership explicit package-private homes. | Non-production demo remains isolated; command, behavior, GL ownership, material/shader values, and production/public renderer API must remain unchanged. |

## Game/runtime entry surfaces

| Symbol | Visibility | Action | Proposed name/location | Current responsibility / rationale | Contract risk |
| --- | --- | --- | --- | --- | --- |
| `ClientMain` | public class | **KEEP** | — | Executable client bootstrap entry point is clear. | Executable behavior/version-report invocation. |
| `game-client.internal.ClientVersionReport` | public class in internal package | **KEEP** | — | T19 accepted implementation gives the executable-specific reporter an explicit client role while retaining its internal package and exact output behavior. | Internal executable helper; emitted output must remain byte/meaning compatible. |
| `SandboxCamera` | package-private class | **KEEP** | — | Sandbox-only camera state/control helper; name is clear in sandbox package. | Sandbox behavior/spatial controls. |
| `SandboxControls` | package-private class | **KEEP** | — | Owner control mapping for persistent sandbox; exact. | Sandbox controls must remain unchanged. |
| `SandboxDiagnosticFormatter` | package-private class | **KEEP** | — | Pure owner diagnostic formatter; exact. | Sandbox output wording/meaning may be acceptance-sensitive. |
| `SandboxMain` | public class | **KEEP** | — | T16 canonical bootstrap/lifecycle entry point after extracting per-frame application responsibilities. | Sandbox public entry point and observable controls/behavior must remain stable. |
| `SandboxApplicationLoop` | package-private class | **KEEP** | — | T16 owner for frame/tick sequencing, public input evaluation, camera command application, render/present ordering, and non-exit sleep. | Sandbox ordering/timing behavior. |
| `SandboxControlState` | package-private class | **KEEP** | — | T16 owner for current window mode/input-response settings and application of already-resolved owner controls. | Owner-control side effects/log ordering; T17 still owns nested control type naming. |
| `SandboxSceneSetup` | package-private class | **KEEP** | — | T16 owner for fixed sandbox lights/debug primitives, world projection, and per-frame render-packet construction. | Sandbox spatial/scene values must remain unchanged. |
| `SandboxDiagnostics` | package-private class | **KEEP** | — | T16 owner for periodic diagnostic timing, mouse accumulation, render/debug counter assembly, and publication. | Owner diagnostic wording/meaning/cadence. |
| `SandboxFramebufferSize` | package-private class | **KEEP** | — | T16 top-level owner for mutable current framebuffer pixel dimensions consumed by the application loop. | DPI/framebuffer dimension semantics. |
| `EngineDemoMain` | removed legacy sandbox entry point | **REMOVE** | — | T17 reference audit confirmed no live code/workflow/test consumer; canonical `SandboxMain` / `runSandbox` remains. | Historical documentation may mention it only as removed compatibility provenance. |
| `ServerMain` | public class | **KEEP** | — | Executable headless server bootstrap entry point is clear. | Headless/runtime verification boundary. |
| `game-server.internal.ServerVersionReport` | public class in internal package | **KEEP** | — | T19 accepted implementation gives the executable-specific reporter an explicit server role while retaining its internal package and exact output behavior. | Internal executable helper; emitted output must remain byte/meaning compatible. |

## Experimental feasibility-spike entry/harness surface — P5R-T20

These types remain under the explicit experimental `feasibility-spikes` module and `com.samo.spike.*` package root. They are verification/evidence executables, not production engine APIs.

| Symbol | Action | Current responsibility / rationale | Isolation / compatibility risk |
| --- | --- | --- | --- |
| `OpenGL46Spike` | **KEEP** | P0-T03 OpenGL 4.6 feasibility executable; name states API level and spike purpose. | Preserve `runOpenGL46Spike` and historical evidence. |
| `JoltLifecycleSpike` | **KEEP** | P0-T04 Jolt JNI lifecycle/cleanup feasibility executable. | Preserve lifecycle evidence and native cleanup behavior. |
| `OpenAL3DAudioSpike` | **KEEP** | P0-T05 OpenAL 3D-audio feasibility executable. | Preserve `runOpenAL3DAudioSpike` and audio evidence behavior. |
| `LocalhostUdpSpike` | **KEEP** | P0-T06 localhost UDP feasibility executable serving server/client roles. | Preserve `runUdpSpikeServer` / `runUdpSpikeClient`. |
| `NetworkImpairmentHarness` | **KEEP** | P0-T11 deterministic localhost latency/jitter/loss/duplication/reordering harness. | Harness wording already distinguishes it from production transport. |
| `SteamInitSpike` | **KEEP** | P0-T07 Steam initialization/callback feasibility executable. | Must not imply production Steam transport coverage. |
| `SteamFlatApiFfmSpike` | **KEEP** | P0-T09 Java FFM access to the Steam flat API. | Must retain the narrow “API access, not end-to-end transport” conclusion. |
| `WindowsNativeCiSmoke` | **KEEP** | Headless-safe hosted-Windows GLFW/OpenAL lifecycle smoke used by CI. | Smoke utility remains experimental/build verification, not engine runtime architecture. |
| `IntegratedNativeEvidenceHarness` | **KEEP** | T20 accepted name for the shared integrated GLFW/OpenGL/Jolt/OpenAL/UDP evidence executable backing both P0-T12 smoke and P0-T13 sustained tasks. | Replaced misleading `IntegratedNativeSoakSpike` name only; task names, durations, evidence labels/JFR paths, subsystem behavior, and conclusions remain unchanged. |

T20 also adds a Gradle isolation guard under `:feasibility-spikes:check` that rejects any project dependency from another declared subproject to `:feasibility-spikes`. Existing architecture/source review continues to confirm production Java has no `com.samo.spike.*` references.

## Modules with no production/runtime Java in the current P1-P5 implementation

These modules are present in `settings.gradle.kts` but currently have no `src/main/java` production types to classify for T01: `engine-ui`, `engine-assets`, `engine-world`, `engine-physics-jolt`, `engine-audio-openal`, `engine-network-api`, `engine-network-ip`, `engine-steam`, `engine-editor`, and `test-support`.

They are not permission to add Phase 6+ implementation during P5R.

## Relevant nested/package-level types

| Owner | Nested/package-level type | Visibility | Action | Proposed name/location | Rationale / risk |
| --- | --- | --- | --- | --- | --- |
| `EngineLogger` | `Level` | public enum | KEEP | — | Logging severity vocabulary is clear and public. |
| `EngineLogger` | `Context` | public record | KEEP | — | Structured log context; public contract. |
| `EngineLogger` | `Event` | public record | KEEP | — | Structured emitted event; public contract. |
| `EngineLogger` | `Sink` | public interface | KEEP | — | Caller-owned log sink capability is exact. |
| `NativeResourceRegistry` | `Registration` | public nested class | KEEP | — | Represents registry registration ownership/close token; lifecycle-sensitive. |
| `PlayerInputCommand` | `DigitalAction` | public enum | KEEP | — | Tick-command digital action vocabulary; protocol-sensitive. |
| `PlayerInputCommand` | `DigitalState` | public record | KEEP | — | Immutable digital action state; public command semantics. |
| `SubsystemGraph` | `Registration` | public record | KEEP | — | Named subsystem/dependency registration value; public lifecycle contract. |
| `TransformQuantization` | `QuantizedPosition` | public record | KEEP | — | Encoded position value; D-047/layout-sensitive. |
| `TransformQuantization` | `QuantizedRotation` | public record | KEEP | — | Encoded rotation value; D-047/layout-sensitive. |
| `InputBinding` | `Control` | public sealed interface | KEEP | — | Binding control hierarchy contract. |
| `InputBinding` | `KeyControl` | public record | KEEP | — | Exact binding descriptor. |
| `InputBinding` | `MouseButtonControl` | public record | KEEP | — | Exact binding descriptor. |
| `InputBinding` | `MouseDeltaControl` | public record | KEEP | — | Exact binding descriptor. |
| `InputBinding` | `MouseDeltaAxis` | public enum | KEEP | — | Config schema vocabulary. |
| `GlfwWindow` | `CallbackState` | package-level nested record | DECOMPOSE | Provisional `GlfwCallbackRegistration` | Generic callback state sits inside the oversized facade; T03 should move it with backend callback ownership. |
| `GlfwWindow` | `SizeCallbackState` | package-level nested record | DECOMPOSE | Provisional `GlfwSizeCallbackRegistration` | T03/T05 boundary; callback ownership must remain explicit. |
| `GlfwWindow` | `InputCallbackState` | package-level nested record | DECOMPOSE | Provisional `GlfwInputCallbackRegistration` | T03/T04 boundary; input callback ownership. |
| `GlfwWindow` | `MotionCallbackState` | package-level nested record | DECOMPOSE | Provisional `GlfwMotionCallbackRegistration` | T03/T04 boundary; relative-motion callback ownership. |
| `GlfwWindow` | `DebugCallbackState` | package-level nested record | DECOMPOSE | Provisional `GlfwDebugCallbackRegistration` | T03 backend/debug callback ownership. |
| `GlfwWindow` | `MouseMotion` | package-level nested record | MOVE | Provisional internal input collaborator | Motion state belongs with T04 input/cursor responsibility, not facade orchestration. |
| `GlfwWindow` | `Dimensions`, `Position`, `VideoMode`, `WindowGeometry`, `MonitorTarget`, `TransitionPlan` | package-level nested records | MOVE | Provisional internal window-mode package/group | These values collectively model T05 window-mode/size planning; exact names should be re-evaluated there. |
| `GlfwWindow` | `SizeEventSink` | package-level nested interface | MOVE | Provisional internal size callback sink | T03/T05 extraction; preserve callback delivery ordering. |
| `GlfwWindow` | `InputEventSink`, `MotionEventSink` | package-level nested interfaces | MOVE | Provisional internal input callback sinks | T03/T04 extraction; preserve focus/input edge semantics. |
| `GlfwWindow` | `DebugEventSink` | package-level nested interface | MOVE | Provisional internal GL-debug callback sink | T03 extraction; no public debug behavior change. |
| `GlfwWindow` | `Backend` | package-level nested interface | MOVE | Provisional `GlfwBackend` top-level internal interface | Replaceable native boundary belongs outside the facade; tests already depend on it. |
| `GlfwWindow` | `LwjglBackend` | package-level nested class | MOVE | Provisional `LwjglGlfwBackend` | Concrete LWJGL adapter belongs beside `GlfwBackend`; T03 owns the move. |
| `RenderMaterialDescriptor` | material enums/texture/scalar records | package-level companion types | KEEP | — | T12 re-review retains `MaterialShaderVariant`, `MaterialTextureBinding`, `MaterialScalars`, `MaterialBlendMode`, `MaterialDepthMode`, and `MaterialCullMode`; each is clear in material context. | Preserve all fixed-state and shader semantics. |
| `OpenGlResourceBackend` | `FenceStatus` | package-level nested enum | KEEP | — | Backend-specific sync status is clear in owner context. |
| `OpenGlShader` | `Stage` | package-level nested enum | KEEP | — | Shader stage enum is precise in owner context. |
| `AnimatedDemoLighting` | `LightingFrame` | package-level nested record | KEEP | — | T18 accepted implementation moves animated demo light/debug-marker generation into a dedicated package-private owner; the nested value names one generated frame. |
| `MaterialComparisonOverlay` | top-level visualDemo class | KEEP | — | T18 accepted implementation moves the existing overlay into a package-private visual-demo GL resource owner without changing allocation, draw, rollback, or close semantics. |
| `SandboxControls` | `SandboxAction` | package-level nested enum | KEEP | — | T17 canonical nested name remains explicit when consumed by `SandboxApplicationLoop` / `SandboxControlState`. | Owner-control members/order/semantics unchanged. |
| `SandboxControls` | `SandboxControlInput` | package-level nested record | KEEP | — | T17 canonical nested name states this value is sandbox owner-control input, not platform input generally. | Record fields/order/semantics unchanged. |
| `SandboxDiagnosticFormatter` | `DiagnosticValues` | package-level nested record | KEEP | — | Clear in owner context; no need to churn unless moved. |

Private implementation-only enums/records that do not materially affect later Phase 5R planning are intentionally not promoted into the inventory merely to create churn.

## P5R implementation updates

### P5R-T03 accepted implementation

The accepted T03 implementation preserves public `GlfwWindow` and extracts the native test/adapter seam into package-private top-level types in the same `com.samo.engine.platform.api` package:

- `GlfwNativeBackend` replaces nested `GlfwWindow.Backend`;
- `LwjglGlfwNativeBackend` replaces nested `GlfwWindow.LwjglBackend`;
- callback ownership values are named `GlfwErrorCallbackRegistration`, `GlfwSizeCallbackRegistration`, `GlfwInputCallbackRegistration`, `GlfwCursorPositionCallbackRegistration`, and `OpenGlDebugCallbackRegistration`;
- backend event sinks are extracted with GLFW/OpenGL responsibility-bearing names.

The T01 `GlfwWindow` decomposition classification remains active for T04/T05: input/focus/cursor state and window-mode/size transition model types intentionally remain inside the facade for their bounded later tasks. No T03 helper is public and no package reorganization is performed.

### P5R-T04 accepted implementation

The accepted T04 implementation preserves the public `GlfwWindow` facade and resolves the T01 input/focus/cursor decomposition into three package-private top-level responsibility owners in the same package:

- `GlfwInputState` owns focus state, held keyboard/mouse state, frame edges, focus-loss release synthesis, native-to-engine input mapping, and snapshot edge consumption;
- `GlfwMouseMotionTracker` owns cursor baseline/previous samples and accumulated relative delta; its package-private `MouseDelta` replaces the former `GlfwWindow.MouseMotion` test value;
- `GlfwCursorCaptureController` owns requested/effective capture, explicit rearm, raw-motion enable/disable, retryable cursor normalization, capture rollback, focus-loss release, and cleanup retry behavior.

`GlfwWindow` retains lifecycle/thread-affinity orchestration, callback registration ownership from T03, staged callback failure surfacing, and all P5R-T05 window-mode/size model responsibilities. No T04 helper is public and no package reorganization is performed.

### P5R-T10 accepted implementation

T10 applies the inventory's fixed reference-scene decomposition in one bounded step:

- `IndexedStaticMeshPipeline` becomes `ReferenceSceneRenderer`, matching the implementation's current renderer-owned reference-room/world role;
- package-private `ReferenceRoomFixture` owns only fixed CPU-side room geometry/index/texture/bounds/count constants and byte construction;
- `OpenGlRenderer` keeps the same public API and delegates to the renamed internal implementation;
- frame orchestration, culling, sorting, uniform/light upload, draw execution, diagnostics, resource ownership, debug/view-model composition, and cleanup remain in `ReferenceSceneRenderer` for later bounded tasks.

No public renderer/resource/material API is added and no Phase 5 output/state/ownership semantics change.

### P5R-T11 accepted implementation

T11 decomposes only frame-time orchestration while retaining `ReferenceSceneRenderer` as the lifecycle/native-resource owner:

- `RendererFrameUniformUploader` owns reusable camera/per-frame/local-light block packing and upload order;
- `ReferenceSceneVisibilityPlanner` owns frustum extraction, reference-room visibility evaluation, camera-depth calculation, and ordered submission preparation;
- `ReferenceSceneDrawExecutor` owns the existing world -> debug -> view-model draw sequence plus viewport/framebuffer-sRGB restoration;
- `RendererFrameDiagnostics` owns latest-success culling and debug text-counter snapshots.

All four collaborators are package-private and non-owning. Existing material/light/culling/uniform/presentation/resource names and cleanup behavior remain for T12-T15 rather than being pulled into T11.

### P5R-T13 accepted implementation

T13 applies only three justified internal Java type renames:

- `CameraUniformBlock` -> `CameraMatricesUniformBlock`, naming the view/projection matrix payload explicitly;
- `PerFrameUniformBlock` -> `FramebufferMetricsUniformBlock`, naming the actual framebuffer-size/inverse payload instead of general frame state;
- `PresentationMode` -> `SrgbPresentationMode`, naming the exact sRGB presentation dimension.

Fresh review explicitly keeps `LocalLightUniformBlock`, `UniformBlockLayoutVerifier`, `SrgbTransfer`, and `TextureColorEncoding` unchanged. GLSL names `CameraBlock`, `PerFrameBlock`, `LocalLightBlock`, bindings 0/1/2, byte layouts, and `SHERKO_MANUAL_SRGB_ENCODE` remain ABI/behavior contracts rather than Java rename targets.

### P5R-T12 accepted implementation

T12 applies only three justified internal renames from the T01 inventory:

- `RendererMaterial` -> `RenderMaterialDescriptor`, clarifying that the value is declarative and non-owning;
- `MaterialStatePolicy` -> `OpenGlMaterialStatePolicy`, clarifying that the policy stores concrete OpenGL state decisions;
- `LocalLightSelection` -> `LocalLightSelector`, naming the bounded first-N selector actor rather than a result.

Fresh source review explicitly keeps `DrawSubmission`, `DrawSubmissionSorter`, `CpuFrustumCuller`, `DirectionalLight`, and the companion material enums/records unchanged because their current names are already precise. T12 adds no compatibility aliases, public rename, package move, resource ownership change, ordering/state/limit change, or shader semantic change.


### P5R-T09 accepted KEEP audit

T09 re-audited the live engine-core spatial/math vocabulary against the canonical spatial conventions and D-041 through D-047 and intentionally retained every reviewed public name:

- `Transform` remains the canonical local/world hierarchy transform;
- `CameraMatrices` remains the view/perspective construction boundary;
- `ScreenRays` remains the screen/viewport-to-world ray boundary;
- `Ray3f`, `Plane3f`, `Sphere3f`, `Aabb3f`, and `Frustum3f` remain conventional geometry value names;
- `TransformQuantization`, `QuantizedPosition`, and `QuantizedRotation` remain bounded value-level quantization names without packet/replication claims.

Internal local/world/cache, hierarchy invalidation, camera basis, viewport/NDC/unprojection, geometry/frustum, and quantization encode/decode vocabulary was also reviewed and retained. No ambiguity or mixed responsibility justified a rename/decomposition, and no coordinate/unit/tolerance/allocation/serialization semantics changed.

### P5R-T08 accepted KEEP audit

T08 re-audited the live `engine-core` configuration, logging, timing, and native-resource ownership vocabulary and intentionally retained every reviewed name. The result matches the T01 inventory rather than creating rename churn:

- configuration: `EngineConfigLoader`, `EngineConfigSchema`, `ConfigKey`, `ConfigEntry`, `ConfigSource`, `ConfigError`, and `ConfigValidationException` remain exact to their layered-loading/schema/value/source/failure roles;
- logging: `EngineLogger` plus nested `Level`, `Context`, `Event`, and `Sink` remain explicit for a synchronous structured logging boundary;
- timing: `EngineClock`, `FixedStepAccumulator`, and `FixedStepCatchUpPolicy` remain responsibility-revealing and aligned with D-021 through D-024;
- native ownership: `NativeResourceRegistry` and nested `Registration` remain explicit ownership/close-tracking names aligned with D-027.

Implementation-level merge/parse, sink-lock, sampling/baseline, fixed-step progress/catch-up, resource-key/allocation-site/registration-state vocabulary was also reviewed and retained. No ambiguous Manager/Helper/Util-style owner, mixed responsibility, or naming-driven abstraction was found. No Java/public API/wiki/sandbox change is required; broader public naming still belongs to T21 and stable package regrouping to T23.

### P5R-T07 accepted implementation

The accepted T07 implementation resolves the two T01 lifecycle public-name findings under D-066:

- `SubsystemStartupCoordinator` replaces `SubsystemStartup` and retains the same stateless `start(...)` coordination, partial-start rollback, caller ownership, failure identity, suppression order, and self-suppression avoidance;
- `FatalTerminationCoordinator` replaces `FatalTermination` and retains the same synchronous one-shot `terminate(...)` sequence, cleanup/reporting containment, logger flush, and exit-status behavior;
- `EngineSubsystem` and `SubsystemGraph` remain unchanged because their current names already communicate their roles.

No compatibility aliases, package moves, lifecycle behavior changes, module/dependency changes, or sandbox changes are introduced. Public wiki usage and direct test/CI names are synchronized in the same candidate; historical D-020/D-029 decision text remains unchanged.


### P5R-T06 accepted implementation

The accepted T06 implementation resolves the T01 `InputActionBindingsLoader` decomposition without changing the supported public input API or schema:

- `InputActionBindingsLoader` is retained and narrowed to readable-file checks, UTF-8 reader lifetime, and I/O failure wrapping;
- `InputActionBindingsJsonParser` owns strict Jackson parsing, schema-v1 shape/version/field/control decoding, document-level duplicates, scale validation, and path/context load diagnostics;
- `InputActionBindingsValidator` owns complete action coverage, non-empty/non-null/duplicate binding checks, action/component compatibility, unsupported-entry rejection, and defensive immutable copying;
- `InputActionBindings` retains its public constructor/load/query signatures and delegates only internal responsibilities.

All new collaborators remain package-private in `com.samo.engine.platform.api`; no schema fixture, dependency, lockfile, module edge, package, evaluator/sampler behavior, wiki contract, or sandbox behavior changes.

### P5R-T05 accepted implementation

The accepted T05 implementation completes the bounded P3/T03-T05 `GlfwWindow` decomposition while preserving the public facade:

- `GlfwWindowModeController` owns current mode, windowed restore geometry, primary-monitor/current-video-mode validation, transition planning/application, state commit, and one-attempt rollback;
- `GlfwDeferredSizeDelivery` owns independent logical/framebuffer staging, latest-value coalescing, validation, post-poll delivery order, and lifecycle clearing;
- former nested `Dimensions`, `Position`, `VideoMode`, `WindowGeometry`, `MonitorTarget`, and `TransitionPlan` become package-private `GlfwDimensions`, `GlfwPosition`, `GlfwVideoMode`, `GlfwWindowGeometry`, `GlfwMonitorTarget`, and `GlfwWindowTransitionPlan`.

`GlfwWindow` retains lifecycle/thread-affinity checks, callback/native ownership, public methods, and orchestration. T04 input/focus/cursor collaborators are unchanged. No T05 helper/value type is public and no package reorganization is performed.


## Cross-check against P5R-T02 through P5R-T25

This inventory deliberately defers execution:

- T02 owns public/internal/package-boundary audit before renames.
- T03-T05 own `GlfwWindow` decomposition.
- T06 owns input-binding parse/validation responsibility cleanup.
- T07-T09 own core lifecycle/config/timing/spatial naming implementation.
- T10-T15 own renderer reference-scene/orchestration/material/uniform/resource/debug/view-model refactors.
- T16-T18 own sandbox and standalone demo decomposition/compatibility cleanup.
- T19 owns client/server bootstrap/report vocabulary.
- T20 separately inventories/refines feasibility spikes.
- T21 owns deliberate public API renames after internals stabilize.
- T22 owns test/fixture naming synchronization.
- T23 owns stable internal package reorganization.
- T24 owns pattern/scalability hardening only where observed problems justify it.
- T25 owns final stale-name/dead-code/compatibility cleanup.

Therefore T01 performs no Java rename, move, split, removal, or behavior change.

## Risk summary

Highest-risk future proposals are intentionally deferred:

1. Public lifecycle names `FatalTerminationCoordinator` and `SubsystemStartupCoordinator` — renamed by P5R-T07; broader public API review remains P5R-T21.
2. `GlfwWindow` decomposition — native callback ownership, focus/cursor safety, thread affinity, and window-mode restoration must remain exact.
3. Spatial/quantization types — retained by default to avoid semantic churn around D-041/D-045/D-046/D-047.
4. Renderer uniform/presentation/resource names — any vocabulary cleanup must preserve GLSL ABI, binding/layout, sRGB encoding, draw ordering, and native cleanup.
5. Client/server version-report names — output consumed by CI/runtime compatibility verification must remain unchanged.
6. Legacy `EngineDemoMain` — T17 reference audit found no required executable consumer; the compatibility class/task are removed while `runSandbox` remains canonical.

## Verification record for T01

- Repository tree used: `master` `262a1d5dfca529979d10c5bd45aad3ce488343a5`.
- Recursive tree response was not truncated.
- 214 Java files were enumerated repository-wide.
- 107 current production/runtime top-level Java files were classified here.
- Relevant public/package-level nested types were reviewed separately above.
- Feasibility-spike Java was intentionally deferred to P5R-T20, matching the roadmap ordering rather than silently omitted.
- No Java/source/resource/Gradle/workflow behavior is changed by this document.

Wiki impact: none — proposals only; no public API changed.

Sandbox impact: none — proposals only; no owner-facing behavior changed.


### P5R-T14 accepted implementation

T14 keeps every existing OpenGL resource-wrapper/backend name unchanged after fresh review. It renames only `CleanupFailures` -> `CleanupFailureSuppression` and centralizes equivalent rollback suppression through `runAndSuppress(...)`; native ownership, registration/deletion order, thread affinity, idempotent close, dynamic-buffer fence behavior, and multi-resource close ordering remain unchanged.


### P5R-T15 accepted implementation

T15 keeps `DebugLineVertexPacker`, `DebugLineRenderer`, and `ViewModelRenderer` unchanged after fresh review. It renames only `ViewModelProjection` -> `ViewModelProjectionFactory` and extracts fixed six-vertex fixture packing into `ViewModelFixtureVertexPacker`; D-041/D-045/D-064/D-065 spatial/rendering/order/ownership behavior remains unchanged.


### P5R-T16 accepted implementation

T16 keeps public `SandboxMain` as the bootstrap/lifecycle entry point and extracts package-private `SandboxApplicationLoop`, `SandboxControlState`, `SandboxSceneSetup`, `SandboxDiagnostics`, and top-level `SandboxFramebufferSize`. `SandboxCamera`, `SandboxControls`, and `SandboxDiagnosticFormatter` remain unchanged; T17-owned control/helper naming and `EngineDemoMain` / `runEngineDemo` compatibility cleanup remain deferred. Owner controls, spatial/camera semantics, fixed scene values, diagnostics, renderer/present ordering, and shutdown behavior remain unchanged.


### P5R-T17 accepted implementation

T17 removes the verified-obsolete `EngineDemoMain` source and `runEngineDemo` Gradle task, leaving `SandboxMain` / `runSandbox` canonical. It renames only nested `SandboxControls.Action` -> `SandboxAction` and `SandboxControls.Input` -> `SandboxControlInput`. T16 helper names, owner controls, diagnostics, scene/camera behavior, resources, and shutdown semantics remain unchanged.


## P5R-T21 public API naming audit candidate

Fresh review against `master` `8783cd441b1b126f2bc1858a6706adb3c7bdee66` confirms the current supported P1-P5 public vocabulary is already responsibility-revealing after the earlier bounded refactors.

The durable member-by-member audit lives in `PUBLIC_API_NAMING_AUDIT.md` and covers:

- 36 `engine-core` public top-level types;
- 18 `engine-platform-lwjgl` public top-level types;
- 6 `engine-render-opengl` public top-level types;
- 15 supported nested public types;
- declared public constructors/factories/operations/queries/constants, record components/accessors, interface/default methods, and enum constants.

T21 selects **KEEP** for every currently supported public type/member. No additional rename, alias, wrapper, package move, visibility widening, or compatibility shim is justified. The prior T07 lifecycle coordinator renames remain the only public-name corrections introduced by Phase 5R so far.

Wiki impact: none — no supported public name or usage changes.
Sandbox impact: none — no capability or owner-facing usage changes.
