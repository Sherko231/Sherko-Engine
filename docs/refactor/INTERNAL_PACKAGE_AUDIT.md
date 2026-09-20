# P5R-T23 Internal Package Audit

Baseline: `master` `26286cbce9de349825743b3dfe9a73f5bcb68474`.

Issue: #303 — reorganize internal packages only where stable responsibility groups are now evident.

## Result

**KEEP the current package layout.**

The completed P1-P5 refactor sequence has clarified class responsibilities, but the current Java package boundaries are already doing useful encapsulation work. A package move now would either widen implementation visibility, add bridge APIs solely for package access, fragment singleton responsibilities, or create broad path churn without improving ownership clarity.

T23 therefore records no Java package move.

## Explicit internal source roots

Current production `src/main/java` Java under explicit `.internal` roots:

| Module | Internal package | Production types | T23 result |
| --- | --- | ---: | --- |
| `engine-render-opengl` | `com.samo.engine.render.opengl.internal` | 42 | KEEP — one connected implementation dependency component; splitting would require visibility widening/bridges. |
| `game-client` | `com.samo.game.client.internal` | 1 | KEEP — `ClientVersionReport` is already explicit; a one-class nested package would be fragmentation. |
| `game-server` | `com.samo.game.server.internal` | 1 | KEEP — `ServerVersionReport` is already explicit; a one-class nested package would be fragmentation. |

No other production module currently has Java source under an explicit `.internal` package root.

## Renderer internal cohesion

The 42 production types under `com.samo.engine.render.opengl.internal` were reviewed as a source-reference graph. Treating references between internal types as undirected edges produces a single connected component containing all 42 types.

The connected component spans the stable class-level responsibilities established by T10-T15:

- native/resource ownership and upload: `OwnedOpenGlHandle`, `OpenGlBuffer`, `OpenGlTexture`, `OpenGlSampler`, `OpenGlShader`, `OpenGlProgram`, `OpenGlVertexArray`, `OpenGlFramebuffer`, `BoundedDynamicBufferUploader`, resource backends, cleanup suppression;
- draw/material/presentation: `OpenGlDrawBackend`, `LwjglOpenGlDrawBackend`, `RenderMaterialDescriptor`, `OpenGlMaterialStatePolicy`, `DrawSubmission`, `DrawSubmissionSorter`, `SrgbPresentationMode`, `SrgbTransfer`, `TextureColorEncoding`;
- frame/uniform/light/culling: camera/framebuffer/local-light uniform blocks, layout verification/reflection backend, `DirectionalLight`, `LocalLightSelector`, `CpuFrustumCuller`, `ViewFrustumExtractor`, `RendererFrameUniformUploader`, `RendererFrameDiagnostics`;
- reference-scene orchestration: `ReferenceSceneRenderer`, `ReferenceRoomFixture`, `ReferenceSceneVisibilityPlanner`, `ReferenceSceneDrawExecutor`;
- debug/view-model: `DebugLineRenderer`, `DebugLineVertexPacker`, `ViewModelRenderer`, `ViewModelFixtureVertexPacker`, `ViewModelProjectionFactory`.

The groups are responsibility-revealing at class level, but they are not independent Java package islands. Representative cross-group dependencies include:

- `ReferenceSceneRenderer` depends across resource, draw, uniform, light, culling, diagnostics, debug, color, material, and view-model collaborators;
- `ReferenceSceneDrawExecutor` crosses reference-scene, draw/material, debug, light, presentation, and view-model responsibilities;
- `DebugLineRenderer` and `ViewModelRenderer` both consume package-private resource/draw/shader/uniform/presentation infrastructure;
- `RendererFrameUniformUploader` consumes package-private uniform/resource collaborators;
- resource wrappers share package-private backend and ownership helpers.

Most of these types are package-private. Splitting the connected implementation into subpackages such as `.resource`, `.frame`, `.material`, `.lighting`, `.debug`, or `.viewmodel` would therefore require one or more of:

1. making implementation types/members public merely to cross Java package boundaries;
2. adding bridge/facade contracts whose only purpose is package access;
3. duplicating responsibility between packages.

Those outcomes are specifically rejected by the existing P5R public-promotion/boundary rules.

Moving all 42 types wholesale into one deeper package would preserve package-private access but would not create responsibility separation. It would be path/package churn only, so it is also rejected.

## Platform package-private implementation

The platform module intentionally colocates 11 package-private implementation types with its public facades in `com.samo.engine.platform.api`:

- `GlfwCursorCaptureController`
- `GlfwDeferredSizeDelivery`
- `GlfwInputState`
- `GlfwMouseMotionTracker`
- `GlfwNativeBackend`
- `GlfwPlatformWindowValues`
- `GlfwWindowModeController`
- `LwjglGlfwNativeBackend`
- `InputActionBindingsJsonParser`
- `InputActionBindingsLoader`
- `InputActionBindingsValidator`

Fresh declaration review confirms all 11 remain package-private.

`GlfwWindow` directly depends on the GLFW implementation collaborators, and `InputActionBindings` directly depends on the binding loading/validation collaborators. The remaining package-private helpers support those same groups.

T03-T06 deliberately kept these helpers beside the public facade to avoid widening implementation visibility. Moving them to `com.samo.engine.platform.internal` now would reproduce the exact problem the earlier tasks deferred: public/package-access expansion or extra bridge types solely to cross a Java package boundary.

Result: KEEP the current co-location.

## Renderer visual demo

The standalone renderer visual demo is a separate Gradle source set but currently uses the same Java internal package.

T18 established these demo responsibilities:

- `RendererVisualDemo`
- `RendererVisualDemoApplication`
- `RendererVisualDemoLoop`
- `RendererVisualDemoFramebufferSize`
- `AnimatedDemoLighting`
- `MaterialComparisonOverlay`

A clean move to `com.samo.engine.render.opengl.internal.demo` is not currently possible without widening visibility. `MaterialComparisonOverlay` intentionally uses package-private renderer resource/draw/material types such as `OpenGlBuffer`, `OpenGlProgram`, `OpenGlTexture`, `OpenGlDrawBackend`, `RenderMaterialDescriptor`, and `SrgbPresentationMode`.

Promoting those production internals for demo package access would invert the intended boundary. Duplicating the overlay infrastructure would also be unjustified.

Result: KEEP the current package.

## Client/server executable internals

`game-client` and `game-server` each have one internal helper after T19:

- `com.samo.game.client.internal.ClientVersionReport`
- `com.samo.game.server.internal.ServerVersionReport`

Both names already express their executable responsibility. Creating `.internal.version` for a single type in each module would produce one-class package fragmentation and no clearer ownership boundary.

Result: KEEP both packages.

## Rejected alternatives

| Alternative | Disposition |
| --- | --- |
| Split renderer internals by resource/frame/material/light/debug/view-model responsibility | Rejected — package-private cross-group dependencies would require visibility widening or bridge contracts. |
| Move all renderer internals wholesale to one deeper package | Rejected — broad churn with no responsibility separation. |
| Move platform helpers to `com.samo.engine.platform.internal` | Rejected — public facades would lose package-private access; public promotion for convenience is forbidden. |
| Move visual-demo classes to `.internal.demo` | Rejected — `MaterialComparisonOverlay` consumes package-private production renderer internals. |
| Create client/server `.internal.version` packages | Rejected — one-class package fragmentation. |

## Boundary outcome

T23 makes no change to:

- supported public packages;
- Java visibility;
- Gradle module direction or dependencies;
- renderer artifact separation;
- native ownership/lifetime;
- spatial conventions;
- config/persisted/wire/protocol formats;
- renderer behavior;
- client/server output;
- wiki consumer guidance;
- sandbox behavior.

The current package layout is retained because it preserves stronger encapsulation than the available package splits would.

## Verification record

- Baseline: `26286cbce9de349825743b3dfe9a73f5bcb68474`.
- Explicit production `.internal` roots were enumerated from the recursive repository tree.
- Renderer: 42 production internal types; source-reference graph is one connected component.
- Platform: 11 deferred implementation collaborators were rechecked and remain package-private.
- Public facade references were rechecked for `GlfwWindow` and `InputActionBindings`.
- Renderer visual-demo source was inspected for package-private production-internal coupling.
- Client/server internal roots each contain one helper.
- No source contradiction requiring a package move was found.

If the final T23 PR remains Markdown-only, the repository Markdown-only CI exemption applies. No unrun Gradle/runtime verification is claimed.

Wiki impact: none — no supported package/type/signature/consumer usage changes.

Sandbox impact: none — no runtime capability or owner-facing usage changes.

Durable decision impact: none — this audit confirms the current package-private cohesion rather than changing architecture.


## Acceptance record

P5R-T23 was accepted through PR #351.

- Final audit head: `d6ce4a096c47ec4226fc070ff65f2a76eba4486e`.
- Merge commit: `36352d874c04c383ce53527001e279f7634f973b`.
- Complete PR diff: 9 Markdown files only.
- CI policy: Markdown-only exemption applied; no heavy PR matrix or post-merge Lightweight verifier was required.
- Package result: KEEP the current layout.
- Java/package/API/wiki/sandbox behavior: unchanged.
