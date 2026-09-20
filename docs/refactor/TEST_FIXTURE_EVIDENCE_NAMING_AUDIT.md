# P5R-T22 Test, Fixture, and Evidence Naming Audit

Baseline: `master` `db4f10cea849ea0949974f5cff416a7c13f3f55f`.

Issue: #302 — synchronize test, fixture, and evidence naming after the P1-P5 refactor sequence.

## Result

The test suite is already synchronized with the accepted production/refactor vocabulary except for one generic negative GLSL fixture name.

- 99 current `*Test.java` classes were reviewed.
- Every current `@Test` method name in those classes was inspected against the behavior it protects.
- Direct tests for accepted P5R renames already use the current production vocabulary.
- One fixture rename is justified: `broken.frag` -> `invalid-syntax.frag`.
- Historical phase/task evidence identifiers are intentionally preserved.

No test assertion, production behavior, coverage intent, evidence semantics, workflow behavior, or public API changes.

## Test-class review

### `engine-assets` — 1

`SharedTestSupportSmokeTest`.

### `engine-audio-openal` — 1

`SharedTestSupportSmokeTest`.

### `engine-core` — 25

`AllocationMetricBenchmarkTest`, `CameraMatricesTest`, `DebugFrameTest`, `EngineClockTest`, `EngineConfigLoaderTest`, `EngineConfigSchemaTest`, `EngineLoggerTest`, `EngineSubsystemTest`, `FatalTerminationCoordinatorTest`, `FixedStepAccumulatorTest`, `FixedStepCatchUpPolicyTest`, `FixedStepInterpolationTest`, `InputResponseSettingsTest`, `JomlHotLoopAllocationTest`, `NativeResourceRegistryTest`, `Phase2IntegratedGateTest`, `PlayerInputCommandCodecTest`, `PlayerInputCommandReplayTest`, `ScreenRaysTest`, `SharedTestSupportSmokeTest`, `SpatialPrimitivesTest`, `SubsystemGraphTest`, `SubsystemStartupCoordinatorTest`, `TransformQuantizationTest`, `TransformTest`.

### `engine-editor` — 1

`SharedTestSupportSmokeTest`.

### `engine-network-api` — 1

`SharedTestSupportSmokeTest`.

### `engine-network-ip` — 1

`SharedTestSupportSmokeTest`.

### `engine-physics-jolt` — 1

`SharedTestSupportSmokeTest`.

### `engine-platform-lwjgl` — 18

`GlfwWindowDebugNativeTest`, `GlfwWindowFocusNativeTest`, `GlfwWindowFocusTest`, `GlfwWindowInputSnapshotTest`, `GlfwWindowModeNativeTest`, `GlfwWindowMouseMotionNativeTest`, `GlfwWindowMouseMotionTest`, `GlfwWindowNativeTest`, `GlfwWindowSizeNativeTest`, `GlfwWindowTest`, `InputActionBindingsTest`, `InputActionEvaluatorEdgeTest`, `InputActionEvaluatorResponseSettingsTest`, `InputActionEvaluatorTest`, `InputSnapshotTest`, `OpenGlThreadGuardTest`, `PlayerInputCommandSamplerTest`, `SharedTestSupportSmokeTest`.

### `engine-render-opengl` — 42

`AnimatedDemoLightingTest`, `BoundedDynamicBufferUploaderNativeTest`, `BoundedDynamicBufferUploaderTest`, `CleanupFailureSuppressionTest`, `CpuFrustumCullerTest`, `DebugGeometryNativeTest`, `DebugLineVertexPackerTest`, `DirectionalLightNativeTest`, `DirectionalLightTest`, `DrawSubmissionSorterTest`, `GlslOfflineValidationTest`, `GlslRuntimeValidationNativeTest`, `LocalLightSelectorTest`, `LocalLightUniformBlockTest`, `LocalLightsNativeTest`, `OpenGlRendererConfigurationTest`, `OpenGlResourceNativeTest`, `OpenGlResourceOwnershipTest`, `OpenGlTextureColorEncodingTest`, `Phase5ExitNativeTest`, `ReferenceSceneRendererNativeTest`, `ReferenceSceneRendererTest`, `RenderCullingCountersTest`, `RenderFramePacketTest`, `RenderLocalLightTest`, `RenderMaterialDescriptorNativeTest`, `RenderMaterialDescriptorTest`, `RendererFrameDiagnosticsTest`, `RendererVisualDemoFramebufferSizeTest`, `RendererVisualDemoLoopTest`, `SharedTestSupportSmokeTest`, `SrgbColorPathNativeTest`, `SrgbPresentationModeTest`, `SrgbPresentationNativeTest`, `SrgbTransferTest`, `UniformBlockLayoutVerifierTest`, `UniformBlockNativeTest`, `UniformBlockPackingTest`, `ViewFrustumExtractorTest`, `ViewModelFixtureVertexPackerTest`, `ViewModelNativeTest`, `ViewModelProjectionFactoryTest`.

### `engine-steam` — 1

`SharedTestSupportSmokeTest`.

### `engine-ui` — 1

`SharedTestSupportSmokeTest`.

### `engine-world` — 1

`SharedTestSupportSmokeTest`.

### `game-sandbox` — 4

`SandboxCameraTest`, `SandboxControlsTest`, `SandboxDiagnosticFormatterTest`, `SandboxFramebufferSizeTest`.

### `test-support` — 1

`ModulePackageBoundaryTest`.

### Test-method result

All current `@Test` method names were inspected directly. The names describe observable behavior/failure boundaries rather than implementation technique, and no stale pre-refactor production name was found.

Examples of already synchronized vocabulary include:

- `SubsystemStartupCoordinatorTest` and `FatalTerminationCoordinatorTest`;
- `ReferenceSceneRendererTest` / `ReferenceSceneRendererNativeTest`;
- `RenderMaterialDescriptorTest` / `RenderMaterialDescriptorNativeTest`;
- `LocalLightSelectorTest`;
- `SrgbPresentationModeTest`;
- `CleanupFailureSuppressionTest`;
- `ViewModelProjectionFactoryTest`;
- `RendererVisualDemoFramebufferSizeTest` / `RendererVisualDemoLoopTest`;
- `SandboxControlsTest`, whose owner type remains intentionally named `SandboxControls`.

Generic but behavior-oriented names such as `UniformBlockPackingTest`, `OpenGlResourceOwnershipTest`, `Phase2IntegratedGateTest`, and `Phase5ExitNativeTest` remain KEEP because they describe a cross-type contract or historical gate rather than a renamed production type.

## Fixture review

| Fixture | Result | Reason |
| --- | --- | --- |
| `engine-platform-lwjgl/src/test/resources/input/action-bindings-v1.json` | KEEP | Explicit versioned action-binding schema fixture; name states domain and version. |
| `engine-render-opengl/src/test/resources/shaders/p5/broken.frag` | RENAME -> `invalid-syntax.frag` | The file intentionally contains a GLSL syntax error; `broken` is generic and does not identify the negative-input purpose. |

The GLSL fixture bytes remain unchanged. The coupled test method becomes `rejectsInvalidSyntaxFixtureWithDiagnostics`, preserving the same validator failure and diagnostic assertions while naming the fault explicitly.

## Historical evidence naming

Historical CI/evidence identifiers are **not** production class names. They encode accepted roadmap provenance and are retained even when later P5R work renamed implementation types.

### Environment variables kept

`SHERKO_P2_EXIT_GATE`, `SHERKO_P3_T01_NATIVE`, `SHERKO_P3_T02_NATIVE`, `SHERKO_P3_T03_NATIVE`, `SHERKO_P3_T04_NATIVE`, `SHERKO_P3_T05_NATIVE`, `SHERKO_P5_T01_NATIVE`, `SHERKO_P5_T03_NATIVE`, `SHERKO_P5_T04_NATIVE`, `SHERKO_P5_T05_NATIVE`, `SHERKO_P5_T06_NATIVE`, `SHERKO_P5_T07_NATIVE`, `SHERKO_P5_T08_NATIVE`, `SHERKO_P5_T09_NATIVE`, `SHERKO_P5_T13_NATIVE`, `SHERKO_P5_T14_NATIVE`, `SHERKO_P5_T15_NATIVE`, `SHERKO_P5_T16_NATIVE`, `SHERKO_P5_T17_NATIVE`, and `SHERKO_P5_T18_NATIVE`.

### Workflow artifact names kept

`engine-subsystem-tests`, `jacoco-reports`, `p3-t01-glfw-window`, `p3-t02-window-size`, `p3-t03-window-modes`, `p3-t04-focus-loss`, `p3-t05-mouse-motion`, `p5-t01-opengl-debug`, `p5-t03-opengl-resources`, `p5-t04-bounded-upload`, `p5-t05-glsl-runtime`, `p5-t06-uniform-blocks`, `p5-t07-indexed-mesh`, `p5-t08-srgb`, `p5-t09-materials`, `p5-t13-directional-light`, `p5-t14-local-lights`, `p5-t15-srgb-presentation`, `p5-t16-debug-geometry`, `p5-t17-view-model`, and `p5-t18-phase5-exit`.

### Report/capture names kept

Existing report and capture files remain under their accepted task IDs, including examples such as:

- `p2-exit-60-second-gate.txt`;
- `p3-t01-glfw-window.txt` through the accepted P3 native reports;
- `p4-t02-joml-hot-loop-allocation.txt`;
- `p5-t07-indexed-mesh.txt/.png`;
- `p5-t09-materials.txt/.png`;
- `p5-t13-directional-light.txt/.png`;
- `p5-t14-local-lights.txt/.png`;
- `p5-t15-srgb-presentation.txt/.png`;
- `p5-t16-debug-geometry.txt/.png`;
- `p5-t17-view-model.txt/.png`;
- `p5-t18-exit.txt` and the P5-T18 room/camera captures.

Names such as `p5-t07-indexed-mesh` are deliberately historical even though the later production owner is `ReferenceSceneRenderer`. Rewriting them would reduce traceability to the accepted evidence task and would require unnecessary CI/document churn.

## Scope preservation

T22 changes only test vocabulary/resource path plus refactor/handoff documentation.

It does not change:

- production Java;
- test assertions or coverage intent;
- fixture bytes/meaning;
- workflow semantics or artifact names;
- Gradle/dependencies/locks;
- module/package/public API boundaries;
- native ownership/lifecycle;
- spatial/config/wire/persisted semantics;
- wiki consumer guidance;
- sandbox behavior;
- Phase 6 scope.

Wiki impact: none — no public API or consumer usage changes.

Sandbox impact: none — no runtime capability or owner-facing usage changes.


## Acceptance record

P5R-T22 was accepted through PR #349.

- Final candidate: `5bda2120526a40c4176aa161ba1492060118860f`.
- Heavy verification: run #491 / `35530836855`, all five required jobs passed.
- Merge commit: `387549c83ffa6af9d8d69b0373772e84ac23053c`.
- Exact-merge verification: run #492 / `35532808303`, Lightweight master verification passed.
- Fixture identity: old/new resource content shares Git blob `fe6d5138a34d2a8903ab88b73b54110b25b6adeb`.
- Historical CI/evidence identifiers: unchanged.
- Production/wiki/sandbox behavior: unchanged.
