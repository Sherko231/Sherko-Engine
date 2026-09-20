# Sherko Engine Architecture

This document describes intended module responsibilities and the architecture actually present in the repository. Product constraints live in `ENGINE_SCOPE.md`; durable decisions live in `docs/DECISIONS.md`; changing progress lives in `docs/DEVELOPMENT_STATUS.md`.

## Maturity vocabulary

| State | Meaning |
| --- | --- |
| Implemented | Production-oriented source and tests exist for the stated responsibility. |
| Skeleton | Gradle module and dependency boundary exist, but production subsystem code does not. |
| Experimental | Disposable feasibility code exists and must not be treated as production architecture. |
| Planned | Roadmap contract exists, but the repository does not yet implement it. |

## Current repository architecture

The repository has a working Java 25 multi-project build with 17 declared Gradle subprojects: the 16 production-target engine/game/support modules defined by `ENGINE_SCOPE.md`, plus the experimental `feasibility-spikes` subproject. `engine-core` implements the shared lifecycle, dependency-ordering/startup-rollback, timing, configuration, native-resource diagnostics, structured logging, fatal-termination foundation, P3-T09 device-neutral tick-command/replay contract, P3-T10 deterministic input-response settings, P4-T02 JOML math dependency/allocation evidence, and P4-T03 cached hierarchical `Transform` under D-041/D-042. `engine-platform-lwjgl` contains the P3-T01 production `GlfwWindow` lifecycle boundary, P3-T02 renderer-neutral logical-window/framebuffer-size delivery, P3-T03 in-place windowed/borderless/exclusive primary-monitor mode transitions, P3-T04 focus-loss-safe cursor capture plus bounded held-key/button safety state, P3-T05 raw/fallback relative mouse acquisition, P3-T06 public immutable renderer-frame `InputSnapshot`, P3-T07 immutable data-driven gameplay action-binding metadata/strict JSON loader, P3-T08 caller-owned renderer-frame action evaluator, P3-T09 renderer-frame-to-tick `PlayerInputCommandSampler`, and P3-T10 response-aware mouse-delta evaluation over the selected LWJGL 3.4.3 GLFW/OpenGL stack. P2-T11 and Issue #135 remain test/evidence-only paths. `engine-render-opengl` now contains the P5-T03 through P5-T12 renderer foundation, including the bounded public `OpenGlRenderer`, explicit D-056 sRGB color path, internal material/state policy, immutable public frame-submission snapshots, bounded CPU frustum culling over accepted P4 geometry, and deterministic renderer-owned draw ordering; other not-yet-implemented engine subsystems remain skeletons. `game-sandbox` owns the persistent cumulative owner-facing playground introduced by P3-T04A and reshaped by Issue #165; it keeps the public renderer together with window/input/timing/action/tick-command capabilities live through non-exported platform/renderer runtime composition, with owner-controlled window mode, cursor capture, mouse sensitivity, and mouse-Y inversion. `game-client` and `game-server` provide minimal executable composition roots for the foundation state.

## Phase 5R public/internal boundary audit — P5R-T02 / Issue #262

The accepted P1-P5 consumer boundary is package- and contract-based, not equivalent to every Java `public` declaration in the repository. Supported engine-consumer packages are `com.samo.engine.core.api`, `com.samo.engine.platform.api`, and `com.samo.engine.render.api`. `config/architecture/module-boundaries.properties` plus D-016 source checks prevent in-repository cross-module Java references outside a target module's declared API root. The renderer adds D-055 artifact-level separation: its compile artifact contains only `com.samo.engine.render.api`, while runtime resolution retains implementation classes/resources.

Java package-private implementation may remain colocated in an API package when that is necessary to support a facade without making the helper a consumer type. Conversely, Java `public` visibility under a declared internal package or executable/demo surface does not by itself create supported engine API. Current examples include renderer-internal `ReferenceSceneRenderer`, the standalone renderer visual-demo entry point, client/server version-report helpers, and game/sandbox entry points. These are bounded implementation or executable surfaces and must not be used as precedent for new public internals.

P5R-T03 through P5R-T05 must preserve `GlfwWindow` as the public platform facade and must not promote extracted backend/callback/input/window-mode collaborators to `public` merely to cross a Java package boundary. Package reorganization is normally deferred until P5R-T23 after responsibility groups stabilize. P5R-T10 through P5R-T15 similarly preserve the public `OpenGlRenderer` / render-submission contracts while refactoring renderer internals. P5R-T07 explicitly authorizes the lifecycle public-name corrections `SubsystemStartupCoordinator` and `FatalTerminationCoordinator`; broader public API naming remains deferred to P5R-T21.

The complete type/consumer-role map and later-task deferrals are recorded in `docs/refactor/BOUNDARY_AUDIT.md`. This audit documents existing D-016/D-055 architecture and current source state; it introduces no new dependency/module edge, public contract, visibility rule, or durable decision.

## Phase 5R GLFW native backend decomposition — P5R-T03 / Issue #263

The accepted P5R-T03 implementation keeps `com.samo.engine.platform.api.GlfwWindow` as the public platform facade while extracting its native test/adapter plumbing into package-private top-level collaborators in the same package. `GlfwNativeBackend` is the replaceable deterministic-test/native-operation boundary; `LwjglGlfwNativeBackend` is the production LWJGL/GLFW/OpenGL adapter. Responsibility-specific callback registration records and event-sink interfaces carry error, size, input, cursor-position, and OpenGL-debug callback ownership without becoming public API.

The extraction deliberately leaves input/focus/cursor state in `GlfwWindow` for P5R-T04 and window-mode/size transition model responsibilities in `GlfwWindow` for P5R-T05. It also deliberately avoids moving the helpers to `com.samo.engine.platform.lwjgl.internal` in this task, because doing so would either widen visibility or require additional facade/package restructuring; stable package reorganization remains P5R-T23 work. Public `GlfwWindow` constructors/methods, native ownership/cleanup order, thread-affinity, callback semantics, input vocabulary, window-mode behavior, and module edges remain unchanged.

## Phase 5R GLFW input/focus/cursor decomposition — P5R-T04 / Issue #264

The accepted P5R-T04 implementation keeps `GlfwWindow` as the public lifecycle/thread-affinity facade while extracting three package-private responsibility owners in `com.samo.engine.platform.api`. `GlfwInputState` owns the current focus flag, GLFW keyboard/mouse held state, frame press/release edges, focus-loss release synthesis, GLFW-to-engine input mapping, and immutable `InputSnapshot` edge consumption. `GlfwMouseMotionTracker` owns the cursor-position baseline and accumulated relative delta. `GlfwCursorCaptureController` owns requested/effective capture, explicit-rearm state, retryable cursor normalization, raw-motion selection, rollback, focus-loss release, and cleanup retry state.

The facade still installs/releases the accepted T03 native callbacks, enforces owner-thread/lifecycle preconditions, stages callback-side failures for `pollEvents()`, and orchestrates these collaborators. Focus regain does not recapture automatically; failed raw-disable or cursor-normalization work remains retryable through explicit release/stop/close. Public input types/signatures and P5R-T05 logical/framebuffer/window-mode/restore/monitor/transition responsibilities remain unchanged. No helper is public and no package/module/dependency edge changes.

## Phase 5R GLFW window-mode/size decomposition — P5R-T05 / Issue #265

The accepted P5R-T05 implementation keeps `GlfwWindow` as the public lifecycle/thread-affinity facade while extracting the remaining Phase 3 window-mode and size-delivery state into package-private collaborators in `com.samo.engine.platform.api`. `GlfwWindowModeController` owns current mode, captured windowed restore geometry, primary-monitor/current-video-mode targeting, transition-plan construction, native transition application, successful state commit, and the existing single best-effort rollback with distinct-failure suppression. `GlfwDeferredSizeDelivery` owns independent logical/framebuffer staging, latest-value coalescing, negative-dimension validation, logical-before-framebuffer post-poll delivery, and lifecycle clearing.

The former nested `Dimensions`, `Position`, `VideoMode`, `WindowGeometry`, `MonitorTarget`, and `TransitionPlan` values become package-private GLFW-specific records: `GlfwDimensions`, `GlfwPosition`, `GlfwVideoMode`, `GlfwWindowGeometry`, `GlfwMonitorTarget`, and `GlfwWindowTransitionPlan`. The T03 native backend and production LWJGL adapter consume these internal values without changing public API or module edges.

`GlfwWindow.setWindowMode(...)` still performs null, lifecycle, and owner-thread checks before delegating. Same-mode requests remain no-ops; direct borderless/exclusive switches preserve the original windowed restore geometry; returning to windowed restores and clears it; transition failures retain the original throwable and make one rollback attempt. Size callbacks still stage only, and public size-listener delivery remains after native polling returns. Accepted T04 input/focus/cursor collaborators are unchanged. No helper/value type is public and no package/module/dependency edge changes.

## Phase 5R input-binding loading/parsing/validation decomposition — P5R-T06 / Issue #266

The accepted P5R-T06 implementation preserves the public `InputActionBindings`, `InputBinding`, `InputBindingLoadException`, action/component/value/control vocabulary, and strict schema-v1 contract while separating three package-private responsibilities in `com.samo.engine.platform.api`. `InputActionBindingsLoader` owns readable-regular-file checks, UTF-8 reader lifetime, and I/O-to-load-exception wrapping. `InputActionBindingsJsonParser` owns Jackson strict duplicate-field parsing, schema shape/version checks, field/control decoding, numeric scale validation, document-level duplicate detection, and path/context diagnostics. `InputActionBindingsValidator` owns complete-set domain validation plus defensive immutable copying and supplies the shared action/component compatibility check.

The split preserves the existing failure precedence and text for accepted P3-T07 scenarios: malformed/duplicate JSON object fields still fail as read/parse errors; schema/type/enum/context failures remain `InputBindingLoadException`; public-constructor programmer errors remain `IllegalArgumentException` or `NullPointerException` with the existing messages. The committed v1 fixture, Jackson version, Gradle/lockfiles, public signatures, evaluator/sampler behavior, module edges, wiki usage, and sandbox behavior remain unchanged. No package reorganization occurs; T23 remains the package-grouping task.


## Phase 5R core lifecycle naming normalization — P5R-T07 / Issue #267

The accepted P5R-T07 implementation changes only two supported public type names under D-066: `SubsystemStartupCoordinator` names the stateless coordinator that starts an already-resolved dependency-first subsystem order and rolls back partial startup, while `FatalTerminationCoordinator` names the one-shot synchronous fatal-shutdown coordinator. The public operations remain `start(...)` and `terminate(...)`; D-020 and D-029 ordering, ownership, failure identity/suppression, cleanup/reporting/flush sequence, and exit status remain unchanged.

`EngineSubsystem` and `SubsystemGraph` are retained because their names already match their responsibilities. No compatibility alias, package move, dependency/module edge, lifecycle state-machine change, shutdown manager, restart contract, or background lifecycle thread is introduced. Historical D-020/D-029 decision rows retain the original accepted type names; D-066 records the current public naming contract. Relevant wiki usage is updated in the same candidate because this is a supported public API rename.


P5-T01 extends the existing `GlfwWindow` ownership boundary with optional OpenGL debug-context diagnostics under D-048. `OpenGlDebugMode.DISABLED` preserves the existing release-safe constructors. `FAIL_ON_HIGH_SEVERITY` requests and verifies a debug context, installs one context-bound callback after capabilities exist, reports normalized source/type/severity/message fields through `EngineLogger`, stages callback/logging failures instead of throwing through native code, and surfaces high-severity failures once from owner-thread `pollEvents()`. The callback is released before context detachment and capability clearing during stop/start-failure/close. No raw OpenGL handle, renderer resource API, draw path, dependency, or module edge is introduced.
 The root project is a build, quality, and task-aggregation project with no Java source tree and no spike runtime dependencies.

P5-T02 / D-049 adds one stable public `OpenGlThreadGuard` to that same window/context boundary. `GlfwWindow.initialize()` binds the guard exactly once to the lifecycle/context owner thread, and existing `GlfwWindow` owner-thread checks delegate to it. Future renderer/OpenGL wrappers must invoke the same guard immediately before native OpenGL entry. The guard exposes no raw `Thread`, native context, handle, executor, queue, or transfer operation; stop/close never move ownership. P5-T02 therefore establishes affinity only and deliberately leaves buffer/texture/shader/program/framebuffer ownership to P5-T03.

P5-T03 / D-050 begins `engine-render-opengl` production implementation with package-internal explicit wrappers for buffers, vertex arrays, textures, samplers, shaders, programs, and framebuffers. Each wrapper asserts D-049 thread affinity before native work, registers successful ownership with the existing native-resource registry, and closes deterministically. Shader/program compile/link failure paths clean only handles created by the failed operation; caller-owned shader wrappers remain independently owned. The renderer module directly reuses the scope-selected LWJGL 3.4.3 OpenGL binding. No public renderer resource API, draw submission, upload strategy, texture/material semantics, resource manager/cache, world/gameplay dependency, or new project edge is introduced.

P5-T04 / D-051 adds one internal fixed-slot dynamic-buffer uploader on top of the P5-T03 owned-buffer and P5-T02 thread-affinity contracts. Capacity is explicit and immutable; uploads are bounded sub-data writes into whole slots, submitted slots carry registered OpenGL sync fences, and strict round-robin reuse occurs only after a non-blocking signaled check. Timeout/wait failure or uploaded-but-unsubmitted wrap rejects before data mutation or ring advancement. This remains an internal correctness path only: no persistent mapping, general streaming allocator, public upload API, draw submission, frame graph, job system, or performance claim is introduced.

P5-T05 / D-052 adds a repository-controlled shader validation path without changing the public renderer surface. The first committed runtime GLSL pair lives under `engine-render-opengl/src/main/resources/shaders/p5/`; a build/test-only LWJGL Shaderc validator compiles those sources offline during verification, and a deliberately broken test fixture proves deterministic failure with diagnostics. The same committed pair is then compiled and linked on the production OpenGL 4.6 context in Windows native acceptance. Shaderc remains test/build-only, while runtime `OpenGlShader`/`OpenGlProgram` diagnostics preserve internal source/program identity and driver logs. No material system, shader variants, reflection, hot reload, asset pipeline, or draw path is introduced.

P5-T06 / D-053 establishes the first fixed renderer uniform-block ABI without adding a draw path. `CameraBlock` uses std140 binding 0 with 128 bytes for view/projection matrices at offsets 0/64; `PerFrameBlock` uses std140 binding 1 with one 16-byte framebuffer-size/inverse vector. Internal CPU packers write the accepted D-041/D-045 matrices in explicit column-major order, while an independent internal OpenGL reflection backend verifies both block sizes and bindings on the linked committed P5 shader program. No world/ECS camera ownership, general descriptor/reflection framework, uniform-buffer allocator, material block system, or P5-T07 draw behavior is introduced.

P5-T07 / D-054 makes `engine-render-opengl` consumable through the first bounded public production composition, `com.samo.engine.render.api.OpenGlRenderer`. The public renderer owns one known indexed triangle through the accepted P5-T03 resources, P5-T05 shaders, and P5-T06 uniform ABI; internal draw setup uses one VAO, position VBO, unsigned-int EBO, camera/per-frame UBOs, depth test `GL_LESS`, back-face culling, CCW front faces, and exactly one indexed draw. `GlfwWindow.present()` remains the platform-owned buffer-swap boundary and preserves native-handle encapsulation. `game-sandbox` consumes renderer/platform APIs only through compile-only plus non-consumable runtime composition, preserving the headless server dependency boundary. No arbitrary mesh/assets/materials/world submission/lighting/sRGB policy is introduced.

P5-T07A / D-055 repairs that public module boundary without changing renderer behavior or signatures. `engine-render-opengl` publishes `engine-core` and `engine-platform-lwjgl` as API dependencies because `OpenGlRenderer` exposes their types; JOML remains transitively available through `engine-core`. The renderer's normal Gradle compile variant uses an API-only artifact containing `com.samo.engine.render.api`, while the runtime variant retains the complete implementation/resources. This keeps `.internal` packages out of the supported consumer compile surface without adding JPMS, a new module edge, or another renderer entry point.

P5-T08 / D-056 adds the first explicit renderer color-space path without changing the public renderer signature. `GlfwWindow` requests an sRGB-capable default framebuffer, while the renderer queries the actual back-buffer color encoding because the hint is not guaranteed. Renderer-internal texture encoding distinguishes display-referred color (`GL_SRGB8_ALPHA8`) from linear data (`GL_RGBA8`); the fixed production triangle owns one 1x1 neutral-gray sRGB texture and internal sampler, and its fragment shader samples that texture in linear space. On `GL_SRGB` default buffers, `OpenGlRenderer.render(...)` enables `GL_FRAMEBUFFER_SRGB` around clear/draw and relies on one hardware encode. On `GL_LINEAR` default buffers, framebuffer sRGB stays disabled and the fixed fragment shader plus clear path apply exactly one bounded sRGB encode. State cleanup disables framebuffer sRGB before return in either mode. No public texture/material API, asset decoding/cooking, mip generation, HDR, tonemapping, fog, post-processing, or P5-T15 presentation layer is introduced.

P5-T09 / D-057 keeps the first material model internal to `engine-render-opengl` until P6 defines stable runtime asset/resource references. One immutable `RenderMaterialDescriptor` carries an explicit shader variant, immutable texture/sampler bindings, bounded linear scalar color multipliers, blend mode, depth mode, and cull mode without owning any native resource. The accepted material model supports distinct immutable material/state values and retains deterministic regression coverage, but the normal renderer presentation now draws the fixed P5-T18 room once across the full framebuffer through the opaque baseline material instead of exposing the earlier left/right validation split. Material state selects fixed OpenGL blend/depth/cull policy instead of entity-type branching; the render path restores the full framebuffer viewport and existing program/VAO/texture/framebuffer-sRGB cleanup boundaries. No public material API, P6 asset ID/handle, P5-T10 submission packet, lighting, or gameplay/world dependency is introduced.

P5-T10 / D-058 adds the first immutable renderer-facing per-frame submission snapshot as public `RenderFramePacket`. Construction validates positive framebuffer dimensions and finite D-041/D-045 view/projection matrices, then copies those mutable caller inputs so later source mutation cannot alter the submitted frame. The packet owns no native resources, has no cleanup lifecycle, exposes matrices only through caller-owned destination copies, and is consumed synchronously by `OpenGlRenderer.render(RenderFramePacket)` without retention. The previous matrix/size overload remains as a compatibility convenience and constructs the same packet. The current fixed P5 mesh/material resources remain internal; P5-T10 deliberately does not invent public mesh/material/asset/native handles before P6. There is no async queue, world/ECS dependency, culling, sorting, or network-snapshot semantics.

P5-T11 / D-059 adds bounded CPU frustum culling before the current fixed draw submissions. The renderer derives six inward-facing world-space planes from the accepted OpenGL `projection * view` clip-space convention, constructs the accepted P4 `Plane3f`/`Frustum3f` values, and delegates visibility decisions to inclusive `Frustum3f.intersects(Aabb3f)`. The current reference triangle has one renderer-owned immutable world AABB matching its vertices; each existing material draw candidate is tested before material/draw-state mutation. Public immutable `RenderCullingCounters` expose tested/visible/culled/submitted counts from the latest successful frame only. No world/ECS ownership, resource identity, sorting, broad phase, occlusion, or GPU culling is introduced.

P5-T12 / D-060 adds deterministic package-internal ordering after P5-T11 culling and before draw-state mutation. One immutable `DrawSubmission` carries renderer-owned logical program/material/mesh keys, camera-space depth, original sequence, existing internal material reference, and fixed viewport data. Opaque submissions sort first by program/material/mesh/sequence; transparent submissions sort back-to-front by camera depth, then use the same logical keys and original sequence as deterministic ties. The sorter copies its source list and does not mutate submissions, materials, or the public P5-T10 frame packet. Logical keys are not OpenGL handles or public P6 resource identities. The fixed P5 scene remains visually equivalent: opaque baseline first, transparent tinted second.







| Module | Intended responsibility | Current state | Direct project dependencies |
| --- | --- | --- | --- |
| `engine-core` | Lifecycle, time, IDs, events, math/spatial contracts | Lifecycle (P2-T01), dependency ordering (P2-T02), startup rollback (P2-T03), monotonic clock (P2-T04), fixed-step accumulator (P2-T05), bounded catch-up policy (P2-T06), interpolation alpha exposure (P2-T07), typed config validation (P2-T08), layered config loading (P2-T09), native-resource registry (P2-T10), structured logging boundary (P2-T12), orderly fatal termination (P2-T13), device-neutral tick command/codec (P3-T09), deterministic input-response settings (P3-T10), canonical spatial convention (P4-T01), JOML hot-loop allocation evidence (P4-T02), public cached hierarchical `Transform` and public JOML spatial math ownership (P4-T03); P2-T11 and #135 add test/evidence paths only | None |
| `engine-platform-lwjgl` | GLFW/window/input and platform-native boundary | P3-T01 production `GlfwWindow` lifecycle; P3-T02 logical/framebuffer size separation and owner-thread polling; P3-T03 in-place primary-monitor windowed/borderless/exclusive transitions; P3-T04 focus-loss-safe cursor capture and held-input cleanup; P3-T05 raw/fallback relative mouse acquisition; P3-T06 immutable renderer-frame `InputSnapshot` plus engine-defined key/button vocabulary; P3-T07 immutable action/binding metadata plus strict versioned JSON loading; P3-T08 caller-owned renderer-frame action aggregation/transitions; P3-T09 renderer-frame-to-tick command sampling; P3-T10 deterministic mouse response application; controller capture remains planned | `engine-core` |
| `engine-assets` | Runtime asset handles/formats and loading contracts | Skeleton | `engine-core` |
| `engine-ui` | Renderer-neutral runtime HUD/menu model and draw data | Skeleton | `engine-core`, `engine-assets` |
| `engine-render-opengl` | OpenGL renderer and runtime-UI draw adapter | Bounded public indexed-mesh renderer plus P5-T03..T12 resource/upload/shader/uniform/sRGB/material/frame-snapshot/CPU-culling/draw-ordering foundations | `engine-core`, `engine-platform-lwjgl`, `engine-assets`, `engine-ui` |
| `engine-world` | Scene/world/component/prefab runtime | Skeleton | `engine-core`, `engine-assets` |
| `engine-physics-jolt` | Jolt-backed physics adapter and ownership | Skeleton | `engine-core` |
| `engine-audio-openal` | OpenAL-backed positional audio | Skeleton | `engine-core` |
| `engine-network-api` | Transport-neutral session/message contracts | Skeleton | `engine-core` |
| `engine-network-ip` | Direct IP transport adapter | Skeleton | `engine-core`, `engine-network-api` |
| `engine-steam` | Steam social/session and transport integration | Skeleton | `engine-core`, `engine-network-api` |
| `engine-editor` | Internal authoring/debug tooling | Skeleton | `engine-core`, `engine-assets`, `engine-world`, `engine-render-opengl` |
| `game-sandbox` | Game rules/vertical-slice content plus persistent owner-facing engine playground | Persistent cumulative playground uses public production timing/window/input/action/tick-command/response APIs; future game rules/content remain skeletal | Published/runtime graph remains `engine-core`, `engine-world`, `engine-physics-jolt`, `engine-network-api`, `engine-ui`; the playground additionally uses non-consumable `engine-platform-lwjgl` and `engine-render-opengl` runtime composition |
| `game-client` | Client composition root | Minimal executable foundation entry point | `game-sandbox`, platform, render, audio, IP, Steam adapters |
| `game-server` | Headless/listen-server composition root | Minimal executable headless foundation entry point | `game-sandbox`, IP and Steam adapters |
| `test-support` | Shared JUnit 6/AssertJ support plus repository architecture verification | Implemented build/test support | None |
| `feasibility-spikes` | Disposable Phase 0 native/network feasibility executables | Experimental | None; external native/library dependencies only |

`feasibility-spikes` is not a seventeenth production engine module. It is deliberately outside the locked 16-module runtime target in `ENGINE_SCOPE.md`, and no production client/server module depends on it.

P5-T13 / D-061 adds exactly one renderer-owned unshadowed directional light without changing the public renderer boundary. Package-internal `DirectionalLight` stores normalized D-041 world-space ray-travel direction, linear RGB color, and bounded SDR intensity. The fixed reference vertex stream now contains +Z normals, and the P5 shader computes Lambert diffuse as `max(dot(normal, -direction), 0)` in linear space before the accepted D-056 presentation encode. The same light applies to the existing opaque baseline and transparent tinted material submissions after P5-T11 culling/P5-T12 ordering. No new native resource is owned, no project or production dependency is added, and `OpenGlRenderer` / `RenderFramePacket` remain unchanged. Public light submission, world/ECS ownership, local lights, shadows, PBR/IBL, HDR/tonemapping, and fog remain outside this bounded path.


P5-T14 / D-062 adds the first public local-light renderer submission boundary while keeping ownership outside world/ECS. `RenderPointLight` and `RenderSpotLight` are immutable public render values using D-041 meters/radians and linear bounded color/intensity. `RenderFramePacket` snapshots their ordered list defensively and retains its prior empty-light constructor. `OpenGlRenderer` adds a creation overload with caller-provided `EngineLogger` and configured maximum 1–8; the compatibility creation path uses the fixed ceiling of eight and emits overflow warnings to stderr. The renderer deterministically accepts the first N local lights and packs them into an independent fixed-capacity std140 `LocalLightBlock` at binding 2: position/range, normalized direction/type, linear color/intensity, cone cosines, and count. Point and spot Lambert/range/cone contributions are accumulated with D-061 directional light in linear space and the bounded SDR illumination vector is clamped before the existing D-056 single presentation encode. The new buffer is explicitly owned/closed with the existing renderer resources. No new project edge, production dependency, asset identity, world/ECS light component, shadows, clustered/Forward+, PBR/IBL, HDR/tonemapping, or fog is added.




## Phase 5R uniform/color/presentation naming normalization — P5R-T13 / Issue #273

The accepted T13 implementation applies only three justified package-private Java type renames: `CameraMatricesUniformBlock` names the view/projection std140 packer precisely, `FramebufferMetricsUniformBlock` names the width/height/inverse-metrics packer instead of implying general per-frame state, and `SrgbPresentationMode` names the bounded hardware-vs-manual sRGB presentation dimension explicitly.

The renderer ABI and behavior remain unchanged: GLSL block names stay `CameraBlock`, `PerFrameBlock`, and `LocalLightBlock`; bindings remain 0/1/2; byte sizes remain 128/16/528 with existing offsets; `SHERKO_MANUAL_SRGB_ENCODE`, `SrgbTransfer`, `TextureColorEncoding`, and exactly-one presentation encoding semantics remain unchanged. `LocalLightUniformBlock` and `UniformBlockLayoutVerifier` remain intentionally named and unchanged.

Accepted evidence: final PR head `ad89e199face9e4ad90da54a021de772c039d6ad` passed all five required jobs in run #467 / `35510543823`; PR #331 merged as `4d45a9468d96b72bdbfc911647bf57e622f64558`; exact merged-master Lightweight verification passed in run #468 / `35510816660`.

P5-T15 / D-063 finalizes the existing D-056 default-framebuffer presentation path without adding a public API. Renderer creation maps the queried default-back-buffer color encoding to package-internal `SrgbPresentationMode.HARDWARE_SRGB` or `SrgbPresentationMode.MANUAL_SRGB`; unsupported encodings fail creation before program/render use. The mode owns whether framebuffer sRGB is enabled, whether the committed fragment shader gets the manual sRGB encode define, and whether the production linear clear RGB is passed through the bounded package-internal `SrgbTransfer`. Texture decode semantics remain D-056 (`GL_SRGB8_ALPHA8` for display color, `GL_RGBA8` for linear data), and directional/local lighting remains linear until the one presentation conversion. This adds no framebuffer-object pipeline, HDR, tonemapping, fog, post-processing, render graph, public presentation setting, module edge, or dependency.


P5-T16 / D-064 adds renderer-neutral debug diagnostics to `engine-core`, deliberately below `engine-render-opengl`. Immutable `DebugLine`, `DebugAabb`, `DebugSphere`, `DebugRay`, `DebugTextCounter`, and bounded `DebugFrame` values contain only accepted core spatial/value semantics and no renderer/native types. `RenderFramePacket` may carry one per-frame snapshot; the OpenGL module adapts geometry into a fixed-capacity dynamic line VBO/VAO plus dedicated shaders/program while reusing the accepted camera UBO and D-063 presentation policy. Geometry draw state is depth-test `GL_LESS`, depth-write disabled, blending/culling disabled, with no material/texture/light evaluation. Text counters remain label/value diagnostics and are published only after a successful full render; they do not create font/UI ownership. This adds no dependency edge: physics/network modules already depend on `engine-core`, while renderer/sandbox consume the same public core values. No retained debug scene, duration/lifetime model, editor/ImGui runtime UI, physics/network implementation, or P5-T17 behavior is added.


P5-T17 / D-065 adds an internal renderer-owned first-person view-model stage without changing the public renderer API or world-camera boundary. `RenderFramePacket` continues to carry only the accepted world view/projection, lights, and debug snapshot. After world scene sorting/culling and P5-T16 debug geometry complete, `ViewModelRenderer` derives its own identity-view projection from the framebuffer dimensions, clears only depth, renders a fixed camera-relative validation rectangle through dedicated camera-only resources, then restores the world camera UBO binding/default VAO/program. This stage is intentionally excluded from world culling/sorting and owns no gameplay/world/asset identity. It adds no module edge, FBO/render graph, asset pipeline, animation, third-person system, or runtime UI ownership.


P5-T18 keeps the accepted public renderer boundary unchanged while replacing the fixed validation triangle geometry with one renderer-owned room fixture. The internal indexed mesh now contains mapped UVs across back/floor/ceiling/side surfaces plus a nearer depth panel, samples one non-uniform 4x4 sRGB texture, and remains subject to the existing D-059 culling, D-060 ordering, D-061 directional light, D-062 bounded local lights, D-063 presentation, P5-T16 debug pass, and P5-T17 view-model pass. The persistent sandbox owns a small first-person camera pose and applies tick-aligned `PlayerInputCommand` MOVE/LOOK values to rebuild the D-041/D-045 view matrix before each visible frame. No public mesh/material/texture/resource identity, asset format, world/ECS ownership, gameplay controller, physics, HUD, render graph, or Phase 6 dependency is introduced.

## Package/API boundary contract

`config/architecture/module-boundaries.properties` is the machine-readable registry for all 17 declared Gradle subprojects. Every declared subproject, including the experimental feasibility module, declares:

- one owned package root;
- one public cross-module API root under that package root;
- one internal implementation root under that package root.

Code inside a module may use its own implementation packages. Cross-module source references, whether imports or fully qualified names, must target the other module's declared API root. References to another module's internal root are architectural violations even when the Gradle project dependency itself is otherwise valid.

`ModulePackageBoundaryTest` lives under `test-support/src/test` because the root has no Java source tree. The `test-support` Gradle test task supplies the complete sorted `rootProject.subprojects` inventory; the test validates the registry against that live list rather than maintaining a second hard-coded list. It requires every scanned production Java file to declare a package under its owning module root, then uses the Java 25 compiler-tree API to inspect imports plus fully qualified references from both `src/main/java` and `src/test/java`. Overlapping roots are assigned to the most-specific owner before the source module is excluded.

Test-source package ownership is intentionally not enforced because the Phase 1 module smoke tests share the `com.samo.testing` package from `test-support`; their cross-module references are still checked. This remains source-level verification: it does not inspect compiled bytecode, reflective class names in strings/resources, or generated sources outside the conventional main/test Java trees. A deliberate fixture under `config/architecture/fixtures/` represents a forbidden `game-client -> engine-platform-lwjgl.internal` import and remains opt-in negative evidence; in-suite regressions cover a fully qualified shortcut, a missing production package, and overlapping network roots.

These package roots define boundaries, not future subsystem interfaces. P1-T10A does not create a reusable feasibility API or promote spike code into production architecture.

## Dependency rules

- Dependencies point from composition/game layers toward engine APIs and adapters, never from engine modules into game modules.
- `engine-core` is the lowest shared project layer and must not depend on LWJGL, OpenGL, Jolt, OpenAL, Steam, or game code. JOML is the scope-selected pure-Java math library and is exposed as an `engine-core` API dependency beginning with D-042/P4-T03.
- `engine-ui` exposes renderer-neutral state/draw data; it must not expose OpenGL or imgui-java types.
- `engine-network-api` contains no socket/Steam implementation details.
- `game-server` must remain runnable without window, renderer, or audio modules.
- `game-sandbox` may compile the persistent playground against public engine APIs through dedicated non-consumable sandbox runtime configurations, but those dependencies must not be published through the sandbox runtime consumed by `game-server`.
- `engine-editor` may consume runtime modules, but runtime modules must not depend on the editor.
- Asset authoring/import dependencies belong in offline tooling; shipped gameplay consumes cooked formats.
- Cross-module Java imports target only the destination module's declared API package root; `.internal` packages are never public contracts.
- Production engine/game modules must not depend on `feasibility-spikes`; its external dependencies exist only to reproduce Phase 0 evidence.

P3-T07 adds no project dependency edge. Jackson is a library implementation dependency of `engine-platform-lwjgl` only; public signatures contain no Jackson class and `game-server` remains independent of the platform module. P3-T08, P3-T09, and P3-T10 add no project dependency edge: the platform module already depends on `engine-core`, while tick-command/response value contracts live in core and the headless server never depends on the platform module. P4-T01 is Markdown-only and adds no source, dependency, module edge, or runtime type. P4-T02 adds JOML to `engine-core` without adding a project edge. P4-T03 promotes that already-selected library to `engine-core` API visibility because the public `Transform` contract uses JOML spatial values; downstream dependency locks change mechanically, but the Gradle project graph does not. Issue #165 changes only sandbox composition/presentation; its `sandboxRuntime` remains resolvable/non-consumable and therefore does not create a published server runtime edge.

## Runtime composition target

The client will compose platform/input, OpenGL rendering, runtime UI, audio, world/physics, game rules, and either IP or Steam networking. The server will compose world/physics, game rules, and networking without graphics/audio. Server authority owns gameplay state and dynamic physics; clients predict/present but do not submit authoritative transforms.

P1-T09 establishes only the runnable composition roots and their Gradle tasks. The current client entry point intentionally does not yet instantiate `GlfwWindow`; P3-T01 defines the reusable production platform ownership boundary first, P3-T02 adds bounded size-event/polling, P3-T03 adds display-mode transitions, P3-T04 adds focus-loss/cursor-capture safety, P3-T05 adds relative mouse acquisition, P3-T06 adds the public renderer-frame hardware snapshot boundary, P3-T07 adds configuration-time data-driven action-binding metadata/loading, P3-T08 adds caller-owned renderer-frame action aggregation/transitions, P3-T09 adds the separate tick-aligned device-neutral `PlayerInputCommand`/replay boundary, and P3-T10 applies deterministic response policy before renderer-frame action values reach that tick bridge. P3-T04A originally composed those public platform capabilities in a scripted owner-facing demo; Issue #165 supersedes that presentation model with the persistent `SandboxMain` playground while preserving the same public boundaries and non-exported platform runtime. Later composition work decides when the real client owns the platform subsystem. `game-server` additionally verifies that its runtime classpath contains no platform, renderer, audio, GLFW, OpenGL, or OpenAL dependencies.

## Canonical spatial convention — P4-T01 / Issue #94

D-041 establishes one repository-wide world-space contract before transform, camera, renderer, physics, audio, asset-conversion, or network-spatial implementation begins. The normative document is [`SPATIAL_CONVENTIONS.md`](SPATIAL_CONVENTIONS.md).

Sherko Engine world space is right-handed: +X points right, +Y points up, and -Z points forward (+Z backward). World position/distance use meters, linear velocity uses meters per second, internal angular values use radians, angular velocity uses radians per second, and positive rotation follows the right-hand rule around the positive axis. Transform scale is dimensionless and `(1,1,1)` is identity.

External libraries and authoring formats do not redefine this basis. Their adapters/importers/exporters must convert axes or units explicitly at their boundary when required. GLFW logical-window coordinates and framebuffer pixels are separate coordinate domains and do not modify the world convention.

P4-T01 is deliberately documentation-only. It does not select projection/NDC depth convention, reversed-Z, FOV/near/far policy, Euler storage/order, quaternion canonical sign, glTF/Jolt/OpenAL conversion details, or network quantization. Those remain bounded later tasks. Future renderer, physics, and asset-conversion tests must cite the canonical document when those production paths exist; P4-T01 does not fabricate those implementations merely to create evidence early.

## JOML allocation and cached hierarchical transforms — P4-T02/P4-T03

P4-T02 / Issue #95 establishes JOML 1.10.9 as the concrete `engine-core` math library and verifies the Phase 4 hot-loop policy with a test-only preallocated mutable vector/quaternion/matrix workload. Its Java 25 `ThreadMXBean` acceptance windows require repeated zero-byte current-thread allocation deltas after warm-up while an escaping control must report positive allocation. This is bounded HotSpot/Temurin evidence and does not replace P2-T11 sampled profiling or claim mathematical zero allocation across all JVMs.

D-042 / P4-T03 / Issue #96 makes JOML the public spatial math type family for `engine-core` beginning with `com.samo.engine.core.api.Transform`. The transform stores caller-independent local position, normalized quaternion rotation, local scale, optional parent identity, reusable local-matrix storage, and a cached world matrix. Public JOML value arguments are copied; read APIs copy into caller-owned destinations, so internal mutable state is never returned by alias.

Local composition is exactly `T * R * S` using JOML column-vector semantics. A root's world matrix is its local matrix; a child's world matrix is exactly `parentWorld * local`. Non-uniform parent scale therefore affects child translation and may combine with child rotation to produce shear in the world matrix; P4-T03 keeps the matrix directly and defines no world-TRS decomposition.

Local position/rotation/scale mutation explicitly marks the changed transform and all of its descendants dirty. Successful reparent or detach updates private child membership and marks only the moved subtree dirty; ancestors and unrelated branches remain cached. A world read first brings the parent cache current, then recomputes the requested transform only when its explicit dirty flag is set. P4-T03 originally used parent-world-revision validation as an interim cache-correctness fallback; P4-T05 replaced that mechanism with explicit descendant propagation and private child tracking. The transform continues to reuse transform-owned JOML matrices rather than creating per-read temporaries.

P4-T04 added synchronous cycle rejection to `Transform.setParent(...)`: self-parenting and indirect descendant cycles are rejected before hierarchy mutation, while same-parent no-op, detach, unrelated-parent reparenting, and legal reparenting to an existing ancestor remain valid. Zero and negative scale are allowed because the current transform contract defines forward composition only; inverse/decomposition semantics remain future work. `Transform` is mutable and externally serialized and makes no concurrent mutation/read guarantee.

## Single-subsystem lifecycle — P2-T01 / Issue #64

`com.samo.engine.core.api.EngineSubsystem` is an abstract `AutoCloseable` base class in `engine-core`. Final public `initialize`, `start`, `stop`, and `close` methods enforce ordering around protected `onInitialize`, `onStart`, `onStop`, and `onClose` hooks. No dependency or native library is added.

| Operation | Permitted entry state | Successful result |
| --- | --- | --- |
| `initialize()` | NEW | INITIALIZED; resources acquired |
| `start()` | INITIALIZED | STARTED; work active |
| `stop()` | STARTED | STOPPED; work quiescent, resources still owned |
| `close()` | NEW, INITIALIZED, STOPPED, FAILED | CLOSED; release hook attempted |
| repeated `close()` | CLOSED | No-op, including after a failed release hook |

Invalid calls fail before invoking a hook. Transient states reject reentrant lifecycle calls. The owner must externally serialize calls on its lifecycle thread; this is not a concurrent lifecycle manager.

An unchecked initialize/start/stop failure propagates unchanged and leaves FAILED, allowing only explicit close. The close hook must tolerate no setup, partial setup, or failed activation/stopping; it must quiesce remaining activity before releasing resources. A close attempt is terminal even if it throws, avoiding automatic repeated cleanup of potentially invalid native handles. CLOSED means the attempt ended, not that every resource was successfully released; cleanup failures must be reported by the owner.

Each instance has one lifetime, with no restart promise. This does not establish whether a native process-global subsystem supports reinitialization; P0-T14 remains the evidence gate. A successful running instance requires explicit stop before close. Client/server composition roots do not yet instantiate a subsystem.

## Subsystem dependency ordering — P2-T02 / Issue #72

`com.samo.engine.core.api.SubsystemGraph` snapshots a list of nested `Registration` records: an exact case-sensitive nonblank ID, an `EngineSubsystem` reference, and ordered prerequisite IDs. Lists are defensively copied. The constructor rejects duplicate IDs, repeated subsystem instances by identity, and missing dependencies after indexing the full list; forward references are valid. A registration rejects null/blank IDs and duplicate prerequisite IDs.

`initializationOrder()` returns an unmodifiable list of the original subsystem references in depth-first postorder, visiting roots in registration order and prerequisites in declaration order. Shared prerequisites appear once. An explicit heap traversal stack handles deep graphs without recursive call-stack growth. Each call uses fresh traversal state.

A cycle anywhere, including a self-cycle or later disconnected component, fails with `IllegalStateException` before any order is returned. Its message contains the closed dependent-to-dependency path, excluding any incoming noncyclic tail, for example `Subsystem dependency cycle: A -> B -> C -> A`. The caller can print/log that diagnostic; no logging framework is introduced.

The graph never calls lifecycle methods, checks subsystem state, owns resources, or performs cleanup. The caller must resolve the entire graph before invoking any hooks and retains explicit lifecycle ownership under D-018. Structural immutability does not make the referenced subsystems immutable. Synthetic composition tests exercise the real lifecycle guards but do not by themselves satisfy D-030's 60-second integrated Phase 2 gate or prove native safety.

## Coordinated startup rollback — P2-T03 / Issue #73

`com.samo.engine.core.api.SubsystemStartupCoordinator` is a stateless utility above D-018 and D-019. It accepts an already-resolved dependency-first `List<EngineSubsystem>`, snapshots the complete list before any hook executes, then calls `initialize()` and `start()` on each subsystem before advancing to the next.

Successful startup does not transfer ownership or register a normal shutdown callback. The composition owner remains responsible for reverse-order `stop()` and `close()` during ordinary shutdown.

If initialize/start fails, the exact `RuntimeException` or `Error` remains primary. The failing subsystem receives one `close()` attempt. Every previously started subsystem is then visited in reverse order and receives `stop()` followed by `close()`; close is still attempted when stop fails because D-018 transitions a failed stop to FAILED, which permits explicit close. Rollback failures are appended to the original failure with `addSuppressed` in cleanup-attempt order, except the same throwable instance is not self-suppressed. Cleanup continues after rollback failures.

This is intentionally not a general lifecycle manager, dependency injection framework, restart mechanism, or native-lifecycle proof. It adds no state accessor and does not change `EngineSubsystem` or `SubsystemGraph`. Synthetic Java tests establish ordering and failure preservation only; P0-T14 remains an independent native lifecycle gate, and D-030/#135 owns the 60-second Java headless integration-cleanup gate.

## Monotonic elapsed-time sampling — P2-T04 / Issue #74

`com.samo.engine.core.api.EngineClock` is the first production timing primitive in `engine-core`. It exposes a default constructor backed by `System.nanoTime()`, an injectable `LongSupplier` constructor for deterministic tests, and `sampleElapsedNanos()`.

Construction performs no source read. The first successful call establishes the baseline and returns `0`. Each later call reads the source once and computes `current - previousAccepted` with ordinary Java `long` arithmetic. Zero elapsed is valid. A negative signed difference fails with `IllegalStateException` and leaves the previous accepted reading intact, so a later valid sample is measured from the last accepted baseline. The source's absolute value may be negative because `System.nanoTime()` has an arbitrary origin.

Ordinary two's-complement subtraction is deliberate: a forward interval smaller than `2^63` nanoseconds remains a positive difference even when the raw source crosses `Long.MAX_VALUE` to `Long.MIN_VALUE`. Source `RuntimeException` or `Error` propagates unchanged before any baseline update. The clock does not expose floating-point seconds and does not impose synchronization; the runtime loop externally serializes calls.

`EngineClock` does not own the fixed-step accumulator, the locked 60 Hz target, frame-gap clamping, catch-up limits, render interpolation, sleeping/pacing, frame identity, wall-clock/calendar time, profiling, or subsystem lifecycle. Those remain separate Phase 2 contracts. P2-T04 tests prove deterministic elapsed-time semantics only; D-030/#135 owns the 60-second integrated fixed-tick evidence.

## Fixed-step simulation accumulation — P2-T05 / Issue #75

`com.samo.engine.core.api.FixedStepAccumulator` converts non-negative elapsed nanoseconds into newly due whole simulation ticks at exactly 60 ticks per second. It keeps one exact fractional remainder expressed in integer tick-nanosecond units over a one-billion denominator. The implementation decomposes each elapsed duration into whole seconds plus a sub-second remainder, so every non-negative `long` input can be processed without multiplying an arbitrary elapsed value by 60 and overflowing.

Across accepted calls, cumulative due ticks equal `floor(totalAcceptedElapsedNanos * 60 / 1_000_000_000)`. Zero elapsed is valid and preserves the remainder. Negative elapsed throws `IllegalArgumentException` before mutation. The runtime loop externally serializes calls.

The accumulator owns neither a clock nor tick execution. A caller typically samples `EngineClock`, passes the elapsed result to `advance`, then executes its fixed simulation update the returned number of times. Tick numbering and cumulative simulation counters remain caller-owned.

P2-T05 established exact fractional ownership but intentionally left interpolation exposure to P2-T07. Frame-gap clamping, catch-up limits, pacing, callbacks, lifecycle integration, and configurable tick rates remain separate concerns.

## Bounded frame-gap and catch-up policy — P2-T06 / Issue #76

`com.samo.engine.core.api.FixedStepCatchUpPolicy` composes with a caller-owned `FixedStepAccumulator`. The default policy accepts at most 250,000,000 ns from one update and exposes at most 5 whole simulation steps from that update. An explicit constructor accepts different strictly positive limits without changing the semantics.

`advance(accumulator, elapsedNanos)` rejects a null accumulator and negative elapsed input before mutation. Otherwise it clamps the elapsed duration to the configured frame-gap limit, advances the supplied accumulator exactly once with that clamped value, and returns at most the configured step cap.

Two kinds of recovery time are deliberately discarded: elapsed nanoseconds beyond the frame-gap clamp never reach the accumulator, and whole due ticks above the step cap are not returned or carried as future backlog. Fractional sub-tick progress from the accepted elapsed duration remains preserved inside `FixedStepAccumulator`. This keeps D-022's exact rational 60 Hz arithmetic separate from the recovery policy while preventing a spiral-of-death catch-up queue.

The default 2-second-stall behavior is therefore bounded: 2,000,000,000 ns is clamped to 250 ms; the accumulator makes 15 ticks due at 60 Hz; the policy exposes exactly 5 and discards the other 10 whole ticks. A later ordinary frame starts without those 10 ticks queued.

The policy owns no clock, simulation callback, tick numbering, cumulative simulation counter, pacing, lifecycle, synchronization, or interpolation. P2-T08/P2-T09 own configuration. D-030/#135 integrates this policy with the real clock/accumulator/lifecycle path for the 60-second Phase 2 exit evidence.

## Render interpolation alpha — P2-T07 / Issue #77

`FixedStepAccumulator.interpolationAlpha()` exposes the accumulator's retained sub-tick progress as a read-only `double` in `[0.0, 1.0)`. The value is computed from the existing exact integer remainder as `scaledRemainder / 1_000_000_000.0`; querying it neither mutates nor consumes progress.

The simulation/render boundary remains explicit. `advance(long)` still returns only whole fixed 60 Hz ticks for simulation. A caller executes those whole simulation updates, then reads interpolation alpha separately for renderer presentation between its previous/current simulated states. P2-T07 does not create transform interpolation, a renderer dependency, callbacks, a variable simulation-delta API, or a runtime loop.

Floating point exists only at the presentation boundary. Fixed-step accumulation, whole-tick due calculation, frame-gap clamping, and catch-up policy remain integer/rational under D-022 and D-023. Fresh or exact-boundary state yields `0.0`; retained half/quarter fractions map directly to normalized alpha, and the exact remainder invariant keeps the value strictly below `1.0`.

After P2-T06 recovery, alpha reflects only the retained fraction from accepted/clamped elapsed time. Whole due ticks discarded by the step cap and elapsed time discarded by the frame-gap clamp are not represented as alpha and do not become backlog. Calls remain externally serialized; no thread-safety promise is added.

## Typed startup configuration validation — P2-T08 / Issue #78

`EngineConfigSchema` validates one already-resolved `Map<String, ConfigEntry>` before subsystem startup. It does not load files or merge sources. The initial canonical keys are `fullscreen.width`, `fullscreen.height`, and `simulation.tickRate`; each key is represented by a public `ConfigKey<Integer>` with a documented default.

Defaults are 1920x1080 and 60 Hz. Width accepts `320..16384`, height accepts `200..16384`, and the tick-rate key accepts exactly 60. The tick-rate key therefore validates the locked timing contract rather than making simulation cadence configurable.

`ConfigSource` carries opaque caller-owned diagnostic text, while `ConfigEntry` pairs that source with one raw value. Integer parsing trims leading/trailing whitespace. Missing known keys use defaults. Unknown keys, malformed integers, out-of-range dimensions, and non-60 tick rates are user configuration errors. `EngineConfigSchema.validate` examines the complete supplied map, preserves input iteration order for errors, and throws one `ConfigValidationException` with an immutable ordered `ConfigError` list if any user error exists. Null programmer-contract inputs fail before ordinary validation.

Successful validation returns an immutable map containing all three canonical keys and typed integer values. Validation invokes no subsystem lifecycle hook. P2-T08 intentionally has no filesystem, JSON/properties, environment, CLI, hot-reload, or mutable-settings service; P2-T09 adds only the fixed source layering described below.

## Phase 5R reference-scene renderer decomposition — P5R-T10 / Issue #270

The accepted P5R-T10 implementation renames renderer-internal `IndexedStaticMeshPipeline` to `ReferenceSceneRenderer` because the implementation owns the renderer's fixed reference-room/world path rather than a generic indexed-mesh pipeline. `OpenGlRenderer` keeps its supported public signatures and delegates internally to that renamed implementation.

Package-private `ReferenceRoomFixture` owns only the fixed CPU-side room fixture data: exact vertex/index counts and byte sizes, the accepted reference-room world AABB, the exact 24 position/normal/UV vertices, the exact 36 unsigned-int indices, and the existing 4x4 sRGB texture bytes/dimensions. T10 intentionally left GL object creation, shader/program/material setup, presentation negotiation, frame orchestration, uniform/light upload, culling/sorting, draw execution, diagnostics, debug/view-model composition, and cleanup in `ReferenceSceneRenderer` for later bounded tasks.

`ReferenceSceneRenderer` remains Java-public solely because `OpenGlRenderer` is in a separate API package. It remains under the declared renderer `.internal` package, is excluded from the API compile artifact, and is not supported consumer API. `ReferenceRoomFixture` is package-private. No public mesh/material/texture/resource submission surface is introduced and Phase 5 visual/native behavior remains unchanged.

## Phase 5R renderer frame-orchestration decomposition — P5R-T11 / Issue #271

The accepted T11 implementation keeps `ReferenceSceneRenderer` as the internal reference-scene lifecycle and native-resource owner. Resource creation, rollback, explicit close ordering, shader/program/material construction, presentation negotiation, local-light selection, captured frame matrices, and the public-facade delegation remain there. Extracted collaborators are package-private and non-owning: `RendererFrameUniformUploader` owns reusable frame-uniform packing/upload state, `ReferenceSceneVisibilityPlanner` owns frustum extraction plus reference-scene visibility and ordered submission preparation, `ReferenceSceneDrawExecutor` owns the world -> debug -> view-model execution sequence and framebuffer/viewport restoration, and `RendererFrameDiagnostics` owns latest-success culling/text-counter publication state.

The observable frame sequence is preserved deliberately: owner-thread/open checks and local-light selection happen before renderer mutation; captured matrices feed frustum extraction before uniform uploads; visibility/sorting follows those uploads; draw execution restores viewport and framebuffer-sRGB state in `finally`; diagnostics publish only after world, debug, and view-model work all succeed. Failed render stages therefore leave the prior diagnostics snapshot intact. The collaborators hold borrowed backend/handle/renderer references only and do not close or transfer ownership of native resources.

T11 does not rename or redefine `CpuFrustumCuller`, `DrawSubmissionSorter`, material/light/uniform-block/presentation types, shader ABI/bindings/layouts, or resource wrappers; those remain bounded to T12-T15. `OpenGlRenderer` signatures and supported consumer behavior, module dependencies, spatial conventions, sandbox usage, and Phase 5 draw semantics remain unchanged.

Accepted evidence: final PR head `2512677512e657de4f82b64e09f0b74fabd75c95` passed the five-job matrix in run #463 / `35507186284`; PR #327 merged as `71ae263026f674bdfda2462f4bfb8c077ef24098`; exact merged-master Lightweight verification passed in run #464 / `35507442998`.

## Phase 5R renderer internal naming normalization — P5R-T12 / Issue #272

The accepted T12 implementation applies only three justified package-private renderer renames. `RenderMaterialDescriptor` names the immutable declarative material value without implying renderer/resource ownership. `OpenGlMaterialStatePolicy` makes explicit that the record contains concrete OpenGL blend/depth/cull enum decisions rather than renderer-neutral policy. `LocalLightSelector` names the stateful bounded first-N selector actor rather than sounding like a selected-result value.

Fresh review explicitly retains `DrawSubmission`, `DrawSubmissionSorter`, `CpuFrustumCuller`, `DirectionalLight`, `MaterialShaderVariant`, `MaterialTextureBinding`, `MaterialScalars`, `MaterialBlendMode`, `MaterialDepthMode`, and `MaterialCullMode` because those names already communicate their current responsibilities. No wrapper, alias, package move, new abstraction, or public API is added.

The rename preserves D-057 material payload/immutability, D-059 culling semantics, D-060 ordering keys and sort rules, D-061 directional-light semantics, D-062 local-light capacity/first-N overflow policy/warning behavior, shader ABI/layout/bindings, frame diagnostics, native ownership, and draw-state cleanup exactly.

Accepted evidence: final PR head `85de9922f0fef5bbedc17add1bc41b1065d94f4a` passed all five required jobs in run #465 / `35508358983`; PR #329 merged as `aa3cccf885216f4e955c5cf671a96bb73702af8d`; exact merged-master Lightweight verification passed in run #466 / `35508613959`.


## Phase 5R spatial/math naming audit — P5R-T09 / Issue #269

P5R-T09 re-audits the accepted engine-core spatial/math vocabulary against `docs/SPATIAL_CONVENTIONS.md` and D-041 through D-047 and records no rename or structural source change. `Transform`, `CameraMatrices`, `ScreenRays`, `Ray3f`, `Plane3f`, `Sphere3f`, `Aabb3f`, `Frustum3f`, `TransformQuantization`, `QuantizedPosition`, and `QuantizedRotation` already name their responsibilities directly and consistently with the accepted world/camera/screen/geometry/quantization contracts.

Implementation-level local/world/cached-matrix, hierarchy invalidation, camera basis, viewport/NDC/unprojection, geometry, frustum-plane, and quantization encode/decode vocabulary is also retained. No split, wrapper, alias, epsilon/tolerance change, allocation change, packet/layout change, package move, or public API change is introduced merely for naming symmetry.

This KEEP audit preserves D-041 handedness/axes/units, D-042 JOML ownership/allocation policy, D-043 transform hierarchy/cache semantics, D-044 exact geometry comparisons, D-045 camera/projection depth convention, D-046 screen-to-world mapping, and D-047 quantization ranges/error/layout meaning exactly. `docs/SPATIAL_CONVENTIONS.md` remains byte-unchanged.

## Phase 5R core config/logging/timing/native-resource naming audit — P5R-T08 / Issue #268

P5R-T08 re-audits the accepted core configuration, logging, timing, and native-resource ownership vocabulary against the live implementation and records no rename or decomposition. `EngineConfigLoader`, `EngineConfigSchema`, `ConfigKey`, `ConfigEntry`, `ConfigSource`, `ConfigError`, `ConfigValidationException`, `EngineLogger`, `EngineClock`, `FixedStepAccumulator`, `FixedStepCatchUpPolicy`, `NativeResourceRegistry`, and `NativeResourceRegistry.Registration` all already name their current responsibilities directly.

The audit also retains the implementation-level vocabulary used by those contracts: configuration merge/parse helpers, logger sink ownership/serialization, clock sampling/baseline fields, fixed-step elapsed/catch-up fields, and native-resource identity/allocation-site/registration-state names are specific to the behavior they own. No wrapper, manager, coordinator, helper, package move, compatibility alias, or responsibility split is introduced merely for symmetry.

This KEEP decision preserves D-021 through D-028 exactly: monotonic sampling, exact 60 Hz accumulation/interpolation, bounded catch-up, startup-schema validation, layered config precedence/diagnostics, synchronous structured logging, and explicit native ownership/terminal-close diagnostics are unchanged. No public API, configuration key/format, exception behavior, allocation/hot-loop policy, module edge, dependency, wiki usage, or sandbox behavior changes.

## Layered startup configuration — P2-T09 / Issue #79

`EngineConfigLoader` composes one startup configuration using fixed precedence `EngineConfigSchema` defaults < optional game UTF-8 key/value file < optional user UTF-8 key/value file < already-parsed command-line overrides, then validates the final effective raw map exactly once through D-025. File entries preserve normalized `path:line` sources; missing files are absent layers; malformed lines, blank keys, and duplicate keys within one file fail before schema validation; unreadable existing paths propagate `IOException`.

Every file value carries a `ConfigSource` of `<normalized-path>:<line>`. Command-line values use `ConfigSource("command line")`. Higher layers replace lower entries by key. After all layers are merged, the loader calls `EngineConfigSchema.validate(...)` once; therefore an invalid lower value hidden by a valid higher value does not fail, while the winning invalid value retains its source in D-025 diagnostics. Successful output remains the immutable typed map owned by the schema.

The loader invokes no lifecycle method and adds no dependency, environment-variable layer, generic provider framework, Java `Properties` escaping semantics, persistence, hot reload, or mutable settings service.

## Explicit native-resource ownership diagnostics — P2-T10 / Issue #80

`NativeResourceRegistry` gives later native wrappers one engine-core ownership diagnostic without depending on any native library. A successful registration normalizes a nonblank resource type, requires an opaque nonzero `long` handle, captures the first allocation call-site frame outside the registry, stores the caller-supplied `Runnable` closer, and returns a nested `Registration` implementing `AutoCloseable`.

A live identity is `(resourceType, handle)`. Duplicate live identities fail before ownership changes. Different resource types may reuse the same numeric handle because native APIs have separate namespaces, and an identity may be registered again after a successful release.

`Registration.close()` runs the closer synchronously on the caller thread. Successful close removes that exact registration and repeated close is a no-op. If the closer throws a `RuntimeException` or `Error`, the original throwable propagates unchanged, the attempt is terminal, and the registration remains tracked as `CLOSE_FAILED`; automatic retry is intentionally forbidden to avoid a possible double-free. Reentrant close while `CLOSING` is rejected.

`assertNoOpenResources()` is a non-cleaning debug-shutdown verifier. Empty registries pass. Otherwise it throws `IllegalStateException` with the tracked count and deterministic registration-order entries containing normalized type, decimal handle, state, and allocation site. Verification never invokes a closer, removes an entry, or changes ownership state.

The registry is not thread-safe. Registration, close, and verification remain externally serialized, preserving caller/native thread-affinity. Synthetic Java tests establish bookkeeping and diagnostics only; they are not evidence that GLFW/OpenGL/Jolt/OpenAL/Steam resources are leak-free, sustained-stable, or restartable. P0-T13/P0-T14 remain separate native evidence gates; D-030/#135 owns the 60-second Java headless integration-cleanup gate.

## Allocation observability evidence — P2-T11 / Issue #81

P2-T11 deliberately adds no runtime allocation-metric service or public engine API. `AllocationMetricBenchmarkTest` lives in `engine-core` test source and uses the Java 25 JFR `jdk.ObjectAllocationSample` event selected by the existing profiling baseline.

The benchmark measures controlled simulation-tick and synthetic/headless render-frame workloads in separate recording windows after warm-up. Each window runs on a uniquely named dedicated platform thread, and only samples attributed to that thread contribute. Positive JFR `weight` values are summed and divided by measured iteration count to report estimated bytes per iteration. The report also records sample count and measurement duration.

An allocating control must produce usable sampled evidence; otherwise the benchmark fails rather than treating absent samples as zero. A nonallocating arithmetic control is also measured, but a zero sampled result is only an observation and not proof of mathematical zero allocation.

This evidence is sampled Java-heap allocation pressure, not an exact per-call counter. It excludes direct/native allocations, GPU/driver memory, retained-heap size, and GC pause cost. The render channel is not evidence from the future OpenGL renderer. No product budget or threshold is established by P2-T11, and no durable architecture decision is added.

## Structured runtime logging boundary — P2-T12 / Issue #82

`EngineLogger` is the shared `engine-core` structured logging seam selected by D-028. It owns no persistence format or logging backend. A caller supplies one `Sink`, and each valid `log(...)` call synchronously creates an immutable `Event` containing `Instant.now()`, severity, message, the actual calling thread ID/name, and one immutable `Context` snapshot before invoking `Sink.write`.

`Context` carries nullable frame, simulation tick, subsystem, connection, and entity fields. Present frame/tick values must be nonnegative. Present string values are `String.strip()` normalized and must remain nonblank; `null` is the only missing-field representation. Connection/entity/subsystem remain opaque strings until later networking/world tasks define stronger domain ID types.

Every severity from `DEBUG` through `FATAL` is forwarded; the logger performs no threshold routing. `FATAL` is only a label and does not terminate the process by itself. `flush()` explicitly delegates to the sink. One private synchronization boundary serializes `write` and `flush` callbacks across concurrent callers while event thread fields still identify the original caller. Sink unchecked failures propagate unchanged and are neither retried nor swallowed.

The sink is caller-owned and is not closed by `EngineLogger`. P2-T12 adds no background worker, queue, buffering, retry/drop policy, shutdown hook, global singleton, file/console/JSON format, rotation policy, or composition-root wiring. P2-T13 composes this boundary without changing those ownership or format decisions.

## Orderly fatal termination — P2-T13 / Issue #83

`FatalTerminationCoordinator` is the D-029 one-shot fatal-shutdown coordinator in `engine-core`. The public constructor receives the existing `EngineLogger`; production termination uses `System.exit(1)`. A package-private `IntConsumer` constructor exists only as a unit-test seam so tests can inspect ordering without terminating the test JVM.

A valid `terminate(message, context, initializationOrder, resourceRegistry)` call validates and snapshots all caller inputs before any side effect, including identity-based duplicate subsystem rejection. The supplied order is the dependency-first order that completed startup successfully. After atomically claiming the coordinator, fatal shutdown runs synchronously on the calling lifecycle/native-affinity thread and does not hold an internal lock across user callbacks.

The exact orchestration is: emit one structured `FATAL` event; visit the subsystem snapshot in strict reverse order and attempt `stop()` then `close()` for each; invoke the existing non-cleaning `NativeResourceRegistry.assertNoOpenResources()` exactly once; emit best-effort structured `ERROR` events for failures captured before the reporting pass; flush the logger once; then invoke termination status `1`. If stop fails, D-018 leaves the subsystem FAILED so close remains legal and is still attempted. Registry verification never force-closes leaked entries.

Unchecked failures from the initial fatal log, stop/close hooks, registry verification, failure-report logs, and flush are accumulated in encounter order instead of aborting later cleanup. Failure-report logging failures are captured but not recursively re-logged. If the test terminator throws, its exact throwable remains primary with prior failures suppressed; if it returns, the coordinator throws terminal `IllegalStateException` because continuing after an expected process exit is invalid.

Each `FatalTerminationCoordinator` instance is one-shot. Reentrant, concurrent, and later calls are rejected before they can duplicate logging, cleanup, verification, flushing, or termination. The coordinator creates no worker, executor, JVM shutdown hook, global singleton, persisted log format, force-close API, module edge, or composition-root wiring.

JUnit tests use handwritten traces/counters plus a real child JVM. The child uses the public constructor and proves exit status `1` occurs only after a synthetic subsystem stops, closes its registered resource, and flushes its sink. This is Java orchestration evidence only; it does not establish native GLFW/OpenGL/Jolt/OpenAL/Steam cleanup, sustained stability, or restartability. D-030/#135 separately owns the 60-second integrated Phase 2 gate.

## Phase 2 integrated exit evidence — Issue #135 / D-030

`Phase2IntegratedGateTest` is a test-only integration harness in `engine-core`; it adds no production API, dependency, module edge, lifecycle manager, or force-close behavior. When explicitly enabled, it starts a synthetic `EngineSubsystem` through `SubsystemStartupCoordinator`, registers one synthetic owned handle through `NativeResourceRegistry`, and then runs a headless loop with the production `EngineClock`, `FixedStepAccumulator`, and default `FixedStepCatchUpPolicy`.

The gate runs for at least 60 continuous seconds. Normal iterations sample monotonic elapsed time and execute only the whole simulation steps returned by the exact 60 Hz accumulator/policy combination. Once after startup it injects a real two-second delay; the following clock sample must expose no more than the default five catch-up steps, demonstrating the 250 ms input clamp and five-step cap through the integrated path rather than an isolated policy test.

After the duration completes, the harness stops and closes the subsystem, the owner close releases the tracked registration exactly once, and `NativeResourceRegistry.assertNoOpenResources()` must pass. The retained report at `engine-core/build/reports/phase2/p2-exit-60-second-gate.txt` records observed duration, executed fixed ticks, loop update count, stall/catch-up observations, lifecycle trace, cleanup result, commit/environment, and evidence limits.

The test is opt-in for ordinary Gradle test runs and is explicitly enabled once by the CI evidence step so routine aggregate/coverage tasks do not duplicate the 60-second delay. A passing gate proves Java headless integration correctness for the Phase 2 contracts only. It does not exercise actual GLFW/OpenGL/Jolt/OpenAL/Steam ownership, does not claim sustained native stability or restartability, and does not replace P0-T13 or P0-T14.

## Production GLFW/OpenGL window lifecycle — P3-T01 / Issue #84

`com.samo.engine.platform.api.GlfwWindow` is the first concrete production platform subsystem and the D-031 ownership boundary. Construction validates positive dimensions, a nonblank preserved title, an `EngineLogger`, and a caller-owned `NativeResourceRegistry` without making a native call. The original P3-T01 public surface adds no raw GLFW handle, LWJGL capability type, event API, swap/poll API, size API, fullscreen API, or input API.

`initialize()` captures the lifecycle/native-affinity thread, installs one task-owned GLFW error callback while retaining any previous callback for later restoration, initializes GLFW, resets hints, and requests an OpenGL 4.6 Core forward-compatible context in a hidden resizable window. The nonzero GLFW window handle is immediately registered in the caller-owned D-027 registry, and that registration owns `glfwDestroyWindow`. Partial initialization rolls back all state acquired by that attempt while preserving the original unchecked failure and suppressing cleanup failures in attempt order.

`start()` requires the same owner thread, makes the context current, creates LWJGL capabilities, requires actual `OpenGL46` support, queries nonblank `GL_VERSION` and `GL_RENDERER`, emits exactly two D-028 INFO events with `subsystem=platform`, then shows the window. A start failure detaches the current context and clears the thread's LWJGL capabilities while retaining the window/GLFW ownership for D-018 close.

`stop()` hides the window, detaches its current context, and clears thread-local capabilities, attempting all cleanup steps even when an earlier step fails. `close()` is D-018 terminal cleanup: it handles NEW, failed initialization/start, or STOPPED state, releases any remaining context state, closes the native-registry registration exactly once, terminates the owned GLFW session, restores the previous GLFW error callback without freeing it, and frees only the callback created by this instance. Native-bearing lifecycle hooks remain on the initialize thread; no dispatcher, worker, shutdown hook, global window manager, multi-window/shared-context contract, or restartability claim is introduced.

The platform module places the selected LWJGL 3.4.3 core/GLFW/OpenGL libraries plus Windows natives on its production classpath. Its repository project dependency remains only `engine-core`; `engine-core` itself stays native-library free. Deterministic tests use a package-private backend seam to verify ordering/failure cleanup without a display, while an opt-in Windows x64 native test uses the public constructor, independently observes OpenGL >=4.6 and version/renderer strings, proves log equality, and verifies no current context plus an empty native-resource registry after close. This one lifecycle run is production integration evidence, not P0-T13 soak or P0-T14 repeated-lifecycle evidence.

P3-T01 intentionally did not install the OpenGL debug callback (P5-T01), run a frame/render loop, swap buffers/poll events, expose framebuffer/logical sizes, change fullscreen mode, capture input, or implement the Phase 3 replay exit gate. P3-T02 adds only the bounded polling/size behavior described next and does not retroactively broaden P3-T01's original acceptance.

## Logical window and framebuffer sizing — P3-T02 / Issue #85

D-032 extends `GlfwWindow` without exposing GLFW/LWJGL types. `WindowSizeListener` has two separate callbacks: `onLogicalWindowSizeChanged(width, height)` for GLFW logical/screen-coordinate dimensions and `onFramebufferSizeChanged(width, height)` for framebuffer pixel dimensions. The existing five-argument constructor remains source-compatible and uses an internal no-op listener; a six-argument overload accepts the explicit listener.

After the P3-T01 context/version/logging checks succeed, `start()` installs one owned GLFW window-size callback and one owned framebuffer-size callback, queries the actual initial logical and framebuffer sizes independently, stages both initial values, enables event polling, and then shows the window. Native callbacks never invoke consumer code. They stage only the latest pair for their own channel, so multiple notifications in one native poll may coalesce without mixing logical units and framebuffer pixels.

`GlfwWindow.pollEvents()` is a public bounded platform operation, not a renderer loop. It is legal only while the window is STARTED, requires the D-031 owner thread, calls GLFW event polling exactly once, then delivers the latest pending logical value followed by the latest pending framebuffer value. Listener `RuntimeException`/`Error` failures remain caller-visible. The implementation creates no asynchronous dispatcher, background worker, concurrent queue, or thread-safety promise.

Zero dimensions are valid platform states, especially framebuffer `0x0` while minimized, and are delivered normally. Negative dimensions from the platform boundary are treated as `IllegalStateException` contract violations before reaching the listener. Initial-size query failures or callback-installation failures fail `start()` and release any size callback/context state acquired by that attempt. `stop()` disables polling, clears undelivered staged sizes, releases both owned size callbacks, then continues hide/context/capability cleanup even if an earlier cleanup operation fails. Terminal `close()` also attempts any remaining callback cleanup before the native window is destroyed.

The size path is renderer-neutral. Renderer-facing code may consume framebuffer pixels later, while logical dimensions remain available for window/layout semantics, but P3-T02 introduces no renderer dependency, viewport mutation, buffer swapping, fullscreen mode, focus/input state, content-scale callback API, raw handle, multi-window management, or native restartability claim. Deterministic tests deliberately use unequal logical/framebuffer pairs; the Windows native acceptance compares production listener delivery against direct GLFW logical/framebuffer queries and records content scale without requiring the machine to have non-100% DPI scaling.

## Windowed and fullscreen mode transitions — P3-T03 / Issue #86

D-033 extends the same D-031/D-032 `GlfwWindow` boundary with public `WindowMode` and owner-thread `setWindowMode(WindowMode)`. Calls are legal only while STARTED; null is rejected before native work and requesting the already-active mode is a no-op. Successful transitions mutate the existing GLFW window in place and do not recreate its OpenGL context.

When leaving `WINDOWED`, the platform captures the current GLFW window position plus positive logical width/height as the restore geometry. `BORDERLESS_FULLSCREEN` disables decoration and uses a monitor-detached window at the primary monitor origin/current video-mode dimensions with no forced refresh. `EXCLUSIVE_FULLSCREEN` attaches the same window to the primary monitor through `glfwSetWindowMonitor` using that monitor's current video-mode dimensions and refresh. Returning to `WINDOWED` re-enables decoration, detaches from a monitor, restores the captured geometry, then clears it so a later fullscreen entry captures the then-current windowed state. Direct borderless/exclusive transitions preserve the same original restore geometry.

The primary monitor is deliberately the only production target in this task. Missing monitor/video-mode data or non-positive current-mode dimensions/refresh fail before committing the corresponding transition. Desktop X/Y coordinates may be negative and are preserved. No raw monitor/window handle, monitor-selection API, custom resolution/refresh selector, renderer presentation API, multi-window policy, focus/input behavior, or later Phase 3 API is exposed.

If a backend transition throws `RuntimeException` or `Error`, the original throwable remains primary and Java-visible mode state remains the previous mode. The platform makes one best-effort rollback attempt to the previous mode/geometry and suppresses any distinct rollback failure on the original throwable. P3-T02 size callbacks remain the only public logical/framebuffer notification path for dimensions caused by mode changes.

Deterministic tests use the existing package-private backend seam to prove capture/restore, direct fullscreen-mode transitions, fresh recapture after returning windowed, invalid monitor state, owner-thread/lifecycle rejection, same-mode no-op, and failure/rollback semantics without a display. The opt-in Windows x64 `GlfwWindowModeNativeTest` runs the production API through exactly 20 successful mode changes (five `BORDERLESS_FULLSCREEN -> WINDOWED -> EXCLUSIVE_FULLSCREEN -> WINDOWED` cycles), verifies after every step that the original context remains current and OpenGL remains usable, checks native monitor/window state and geometry, and requires final cleanup with an empty `NativeResourceRegistry`. This bounded run is not P0-T13 soak or P0-T14 repeated-lifecycle evidence.

## Focus-loss input safety and cursor capture — P3-T04 / Issue #87

D-034 adds the public owner-thread `GlfwWindow.setCursorCaptured(boolean)` operation and keeps the pre-snapshot hardware safety state inside `engine-platform-lwjgl`. While STARTED, a focused window may request GLFW disabled-cursor capture; explicit release restores the normal cursor. Focus, key, and mouse-button callbacks are installed and owned by the same window lifecycle and are released during stop/start-failure cleanup.

The platform tracks bounded internal held-state arrays for GLFW key and mouse-button indices. Key PRESS/REPEAT and mouse PRESS mark held; RELEASE clears held; invalid indices are ignored. On focus loss the platform marks the window unfocused, clears all held keys/buttons before any cursor operation, and if capture is effectively active requests `GLFW_CURSOR_NORMAL`. A previous capture request is remembered only to require explicit policy re-arm; focus regain never automatically recaptures. A later explicit `setCursorCaptured(true)` is required before gameplay pointer lock resumes.

Native focus callbacks never invoke game code. If cursor release throws while handling focus loss, held state is already cleared, effective capture is treated conservatively as off, and the original unchecked failure is staged instead of crossing the native callback boundary. The owning `pollEvents()` propagates that staged failure once after GLFW returns. Direct public capture calls still propagate backend unchecked failures unchanged and update requested/effective Java state only after successful native mutation.

P3-T04 itself intentionally added no raw mouse motion, public focus listener, public snapshot, action transitions, controller mapping, player commands, renderer behavior, or game pause/menu policy. Later P3 tasks extend this internal hardware state without changing the historical P3-T04 acceptance.

## Relative mouse-motion acquisition — P3-T05 / Issue #88

D-035 extends the existing D-031 through D-034 `GlfwWindow` ownership boundary with relative mouse-motion acquisition while effective cursor capture is active. The window owns one cursor-position callback alongside the focus/key/button callbacks. Cursor-position samples are considered only while the window is focused and cursor capture is effectively active.

On explicit successful capture, `GlfwWindow` first requests `GLFW_CURSOR_DISABLED`. If GLFW reports raw mouse motion support, the same window then enables `GLFW_RAW_MOUSE_MOTION`; otherwise raw mode remains off and successive disabled-cursor positions form the fallback relative stream. The fallback is screen-bound independent under GLFW disabled-cursor semantics, but it does not claim to bypass OS pointer acceleration. Raw mode is disabled whenever effective capture ends.

Relative accumulation uses a baseline to suppress discontinuity spikes. The first eligible position sample after start, capture/re-capture, focus/capture transition, or baseline invalidation establishes the baseline and contributes zero. Later eligible samples contribute signed `current - previous` X/Y deltas. Release, focus loss, stop/close, and startup cleanup clear pending motion and invalidate the baseline. Focus regain alone never re-enables capture or raw mode; D-034 still requires explicit recapture.

Native callbacks do not invoke gameplay code. Raw-enable failure during a direct capture attempt preserves the original unchecked throwable and performs best-effort raw/cursor rollback without committing requested/effective capture state. Focus-loss cleanup clears held/motion state before attempting native raw/cursor release; native cleanup failures are staged and propagate once from owner-thread `pollEvents()` using the existing D-034 callback-failure boundary.

Deterministic `GlfwWindowMouseMotionTest` proves ignored pre-capture/unfocused events, zero first sample, signed accumulation, absolute-position independence, raw-supported and forced-unsupported fallback selection, release/focus baseline reset, explicit recapture, failure rollback/staging, callback ownership, and lifecycle cleanup. The opt-in Windows x64 `GlfwWindowMouseMotionNativeTest` verifies real GLFW raw-mode state before/during/after capture, release, focus transfer, regain, and explicit recapture plus registry cleanup. It intentionally does not treat programmatic `glfwSetCursorPos` as a physical raw-device oracle; deterministic tests own exact delta arithmetic while the native test proves real mode/focus/lifecycle integration.

## Renderer-frame hardware input snapshot — P3-T06 / Issue #89

D-036 adds public `InputSnapshot`, `InputKey`, and `InputMouseButton` under `com.samo.engine.platform.api` plus owner-thread `GlfwWindow.captureInputSnapshot(long frameId)`. The snapshot is a client/platform hardware view; no new module dependency is introduced and `game-server` remains independent of `engine-platform-lwjgl`.

Snapshot capture is legal only while `GlfwWindow` is STARTED, requires the D-031 owner thread, rejects a negative caller-owned frame ID, and performs no native poll. Callers choose the renderer-frame sampling point by invoking `pollEvents()` and then `captureInputSnapshot(frameId)`. The returned object defensively copies its state and remains immutable after later polls, focus changes, or captures.

The public key/button vocabulary is engine-defined rather than raw GLFW integer codes. The initial keyboard vocabulary covers W/A/S/D, Space, left/right Shift/Control/Alt, Escape, E/Q/R/F; the mouse vocabulary covers left/right/middle plus buttons 4/5. Native codes outside that vocabulary remain internal and do not leak into the public API.

`GlfwWindow` now retains raw hardware edge booleans between successful snapshot captures. PRESS marks held plus pending pressed; RELEASE clears held plus pending released; key REPEAT preserves held state without generating another pressed edge. A complete press/release between two snapshots can therefore appear as pressed=true, released=true, held=false in the next snapshot. Successful capture consumes pending edge bits but not held levels.

D-035 mouse accumulation is exposed in the snapshot and consumed on successful capture. Snapshot capture clears only the accumulated delta, not the motion baseline, so ordinary frame boundaries preserve continuous relative motion. Focus loss converts currently held supported keys/buttons into release edges, discards stale pending presses, clears pending mouse motion, and preserves D-034's explicit-recapture behavior. Focus regain synthesizes no state.

Validation failures happen before consumption. Snapshot capture is pure Java and adds no native cleanup/rollback path. `InputSnapshot` is intentionally not the headless/tick/replay format: P3-T07/P3-T08 own action semantics, while P3-T09 owns `PlayerInputCommand` and replay/network-friendly device-neutral input. This separation prevents a platform dependency from entering server composition.

Deterministic `InputSnapshotTest` and `GlfwWindowInputSnapshotTest` cover defensive immutability, edge retention/consumption, key-repeat semantics, mouse-delta one-shot consumption without baseline reset, focus-loss releases, validation-before-consumption, stable shared snapshots, and proof that snapshot capture does not poll GLFW. Existing P3-T04/P3-T05 native acceptance continues to verify the real callback/focus/raw ingestion path because P3-T06 itself makes no new native call.

## Data-driven gameplay action bindings — P3-T07 / Issue #90

D-037 adds immutable configuration-time action metadata under `com.samo.engine.platform.api` without changing the module graph. `InputAction` defines exactly MOVE, LOOK, JUMP, CROUCH, SPRINT, INTERACT, GRAB, THROW, PRIMARY_USE, PAUSE, and PUSH_TO_TALK. MOVE and LOOK are `VECTOR2`; the other nine are `DIGITAL`. `InputAction.valueType()` is the single source of truth for that classification.

`InputBinding` maps one engine-owned hardware control descriptor to one `InputActionComponent` with a finite non-zero signed scale. Controls are sealed device-neutral descriptors: `KeyControl(InputKey)`, `MouseButtonControl(InputMouseButton)`, or `MouseDeltaControl(MouseDeltaAxis.X/Y)`. Digital actions require `VALUE`; vector actions require `X` or `Y`. Binding objects are immutable value records and intentionally contain no Jackson, GLFW, or LWJGL type.

`InputActionBindings` is one immutable complete binding set. Its public constructor defensively copies caller collections, requires every `InputAction` exactly once with at least one binding, validates action-component compatibility, and rejects exact duplicate binding descriptors. `bindingsFor(...)` and `asMap()` expose immutable views. Runtime action evaluation is intentionally absent from P3-T07; P3-T08 supplies that separate layer without changing the binding schema.

`InputActionBindings.load(Path)` delegates through package-private `InputActionBindingsLoader` to `InputActionBindingsJsonParser` and atomically returns either a complete valid set or throws `InputBindingLoadException`. The public constructor delegates complete-set domain validation/copying to package-private `InputActionBindingsValidator`; the JSON parser reuses the same action/component validation at the original parse point so contextual load failures retain their accepted ordering and diagnostics. JSON schema v1 is strict: the root contains only `schemaVersion` and `actions`; every action object contains only `action` and `bindings`; every action appears exactly once; binding objects use exactly the fields required by `KEY`, `MOUSE_BUTTON`, or `MOUSE_DELTA`; unknown/duplicate fields, unknown enum/control names, malformed JSON, invalid/missing versions, invalid components/scales, missing/unreadable paths, empty binding arrays, duplicate actions, or duplicate binding descriptors fail without partial output.

Jackson is the first production JSON parser dependency selected under the existing `ENGINE_SCOPE.md` Jackson decision. `engine-platform-lwjgl` uses implementation-only `jackson-databind:2.21.2`; its resolved graph includes `jackson-core:2.21.2` and the matching `jackson-annotations:2.21`. The dependency is absent from public signatures and does not create a new project edge. P3-T07 does not implement controllers, input response settings, live remapping UI/hot reload, `PlayerInputCommand`, replay/network codecs, or renderer/camera behavior.

Deterministic `InputActionBindingsTest` uses a committed complete schema-v1 fixture and handwritten expected descriptors. It covers exact action typing, all three descriptor kinds, defensive ownership/immutability, strict schema/property rejection, malformed/missing files, invalid action/component/control/scale values, duplicate actions/bindings/JSON fields, and null programmer-contract input. Existing architecture/headless tests remain responsible for proving no Jackson/native type leaks across public/module boundaries and no platform dependency enters `game-server`.

## Renderer-frame gameplay action evaluation — P3-T08 / Issue #91

D-038 adds `InputActionEvaluator`, immutable `InputActionSnapshot`, and immutable `InputActionState` under `com.samo.engine.platform.api`. One evaluator is constructed from one complete immutable `InputActionBindings` value and is owned/externally serialized by one caller. It accepts renderer-frame `InputSnapshot` values and retains only the last successful frame ID plus whether each action was active after that frame.

Each key or mouse-button binding contributes its signed scale while the bound control is held. In the original P3-T08 contract, each mouse-delta binding contributed the current hardware delta multiplied by signed scale with no response policy. P3-T10 now layers D-040 response shaping before that existing scale/aggregation step while keeping all P3-T08 transition/history rules unchanged. Contributions are still added in declared binding order per target component with finite `double` arithmetic. DIGITAL activity is `value != 0.0`; VECTOR2 activity is `x != 0.0 || y != 0.0`, so exact signed cancellation is inactive.

Ordinary action transitions derive from aggregate activity: inactive->active emits `pressed`, current activity becomes `held`, and active->inactive emits `released`. Hardware bindings do not expose separate gameplay transitions. Pressing an additional binding while an action is already active does not emit another press; releasing one binding while another keeps the aggregate active does not emit a release.

P3-T06 can retain a complete key/button press+release between renderer snapshots. When the same bound key or button carries both edges, the action was previously inactive, and its current aggregate ends inactive, P3-T08 preserves that hardware tap as `pressed=true`, `held=false`, `released=true` for the current action snapshot. Press evidence from one binding plus release evidence from a different binding is not treated as an ordered tap because `InputSnapshot` does not preserve event ordering across different controls. Mouse-delta controls have no discrete edge and transition only through aggregate nonzero/zero activity.

The first successful evaluation accepts any non-negative snapshot frame ID. Later successful calls require a strictly greater frame ID; gaps are valid. Null input, duplicate/decreasing frame identity, non-finite hardware mouse delta, response/binding multiplication overflow, or non-finite aggregate fails before evaluator state advances. All actions are computed into temporary state before the evaluator commits its previous-activity/frame baseline, so any failed evaluation is atomic. Returned snapshots/states are immutable and remain stable after later calls.

This layer is intentionally renderer-frame/client-platform state, not the replay/network command format. P3-T09 owns tick-aligned `PlayerInputCommand` and headless replay portability; P3-T10 owns device-response settings/application while `game-server` stays independent of `engine-platform-lwjgl`.

Deterministic `InputActionEvaluatorTest` plus P3-T10 response integration coverage exercise digital press/held/release, complete same-binding one-frame taps, mouse-button equivalents, simultaneous positive bindings, exact signed cancellation, no duplicate press/release through overlapping bindings, MOVE vector aggregation, LOOK response/scaling and nonzero->zero release, focus-loss-style release, immutable result stability, frame-order rejection, null/non-finite input rejection, response/aggregate overflow rejection, runtime settings replacement, and failure-state preservation.

## Tick-aligned replayable player input — P3-T09 / Issue #92

D-039 places immutable `PlayerInputCommand` and `PlayerInputCommandCodec` in `engine-core`, while caller-owned `PlayerInputCommandSampler` remains in `engine-platform-lwjgl` at the renderer-frame/tick boundary. No project/module edge is added and `game-server` remains independent of the platform module.

The sampler accepts successful renderer-frame `InputActionSnapshot` values. Latest MOVE and digital scalar/held levels repeat across due ticks until replaced. LOOK delta accumulates additively and digital pressed/released edges OR-accumulate across zero-tick renderer frames, then those one-shot values are consumed exactly once by the next emitted command. Later ticks without another renderer snapshot carry zero LOOK/no repeated edges. Submitted frame IDs and emitted tick IDs are independently strictly increasing after their first successful values; invalid/non-finite operations preserve prior state atomically.

`PlayerInputCommandCodec` is an explicit fixed-size replay/storage format, not a production network packet contract. Version 1 uses magic `SPIC`, big-endian fields, exactly nine digital actions, and a 126-byte layout. It uses caller-supplied `ByteBuffer`, preserves the caller's configured byte order, and rejects malformed/truncated/non-finite/negative-tick input before accepting a command. Java serialization remains forbidden.

A deterministic encode/decode/headless replay test uses platform-independent commands and an independently calculated final state. This is the Phase 3 input-recording/replay exit evidence at the command boundary only; it is not deterministic physics, production networking, or Steam evidence.

## Deterministic input response settings — P3-T10 / Issue #93

D-040 adds immutable `InputResponseSettings` to `engine-core` with mouse sensitivity, mouse Y inversion, controller dead zone, and controller curve exponent. Neutral defaults are `1.0`, `false`, `0.0`, and `1.0`. Construction rejects non-finite/negative sensitivity, dead zones outside `[0,1)`, and non-positive/non-finite exponents.

`applyMouseX`/`applyMouseY` validate finite input, multiply by sensitivity, and optionally invert Y after scaling; a non-finite result fails. `InputActionEvaluator(InputActionBindings)` remains source-compatible by using neutral defaults. The overload accepting `InputResponseSettings` and `setResponseSettings(...)` give the caller explicit response policy. Replacement affects future evaluations only and does not reset frame identity or previous action activity. For `MouseDeltaControl`, response shaping occurs before the existing signed binding scale and additive aggregation. Key/mouse-button behavior and P3-T09 tick sampling remain unchanged.

`applyControllerAxis` is deliberately an axis-local pure scalar primitive because no production controller input boundary exists yet. Input must be finite in `[-1,1]`. Magnitudes at/below the dead zone map to zero; remaining magnitude is renormalized as `(a-d)/(1-d)`, raised to the configured positive exponent, then receives the original sign. P3-T10 introduces no controller discovery, polling, connection lifecycle, button/axis vocabulary, callbacks, action bindings, radial stick policy, persistence/UI, mouse smoothing/acceleration, per-axis sensitivity, gameplay camera/movement, or network changes.

P3-T10 adds no native call, dependency, binding-schema change, project edge, or server runtime dependency. Its exact final candidate passed the required five-job heavy matrix, PR #160 merged to `master` as `e1801b11a713ce6cc73276c644aa15351ac508a1`, and the exact merged commit passed the lightweight master verifier. Together with the retained P3-T09 replay evidence, Phase 3 is accepted.

## Persistent owner-facing engine sandbox — P3-T04A / Issue #149, presentation policy superseded by Issue #165

P3-T04A established `game-sandbox` as the canonical owner-facing observation surface and originally implemented it as a scripted timeline. Issue #165 deliberately supersedes that presentation/maintenance policy while preserving the same production engine APIs and dependency boundary. The current canonical surface is `SandboxMain`, launched by `:game-sandbox:runSandbox`. P5R-T17 removes the obsolete `EngineDemoMain` / `runEngineDemo` compatibility surface after repository-wide reference verification.

The persistent playground composes only public production APIs: `GlfwWindow`, `OpenGlRenderer`, logical/framebuffer size delivery, window-owned `present()`, window-mode transitions, cursor capture/focus-loss behavior, P3-T06 `InputSnapshot`/`InputKey`, P3-T07 `InputActionBindings`, P3-T08 `InputActionEvaluator`/action snapshots, P3-T09 `PlayerInputCommandSampler`/core tick commands, P3-T10 `InputResponseSettings`, `CameraMatrices`, `EngineClock`, `FixedStepAccumulator`, `FixedStepCatchUpPolicy`, structured logging, and `NativeResourceRegistry` verification. It has no fixed duration or automatic feature tour; the owner keeps these capabilities active together and exits explicitly with `Ctrl+Q`.

Current owner controls are intentionally built from the existing public input vocabulary: `F` cycles window modes, `R` toggles cursor capture, `Right Shift + F` cycles mouse sensitivity, `Right Shift + R` toggles mouse-Y inversion, and `Ctrl+Q` exits. Normal action bindings continue to evaluate concurrently. `SandboxControlsTest` verifies control selection without a native window. P5-T07 adds one visible white indexed triangle on the development background through `OpenGlRenderer`; the sandbox still makes no direct OpenGL/LWJGL call.

The sandbox must not become a second engine architecture. It may not import another module's internal package, call LWJGL/native APIs directly, or expose a new production API merely to make owner observation easier. When a future task adds a capability that is meaningfully usable through its already-authorized public API, the same PR extends the existing cumulative sandbox and preserves other usable controls/capabilities. When that is not possible without pulling later roadmap work forward, the PR records `Sandbox impact: none — <reason>`. Separate throwaway demos are exceptional and require explicit Issue authorization.

The platform and renderer dependencies used by the playground are intentionally non-exported. `game-sandbox` compiles against `engine-platform-lwjgl` normally and, for the in-repository renderer module only, resolves `engine-render-opengl` compile-only from its `runtimeElements` variant with transitivity disabled. This keeps IntelliJ/Gradle module resolution on the real renderer source-set module while D-055's default `apiElements` remains API-only for ordinary consumers. Cross-module source imports are still restricted to declared API roots by `ModulePackageBoundaryTest`. A dedicated non-consumable `sandboxRuntime` supplies platform/renderer runtime composition for `runSandbox`; those dependencies are not part of the sandbox runtime elements consumed by `game-server`. P5R-T17 removes the former compatibility `runEngineDemo` task, leaving one canonical owner-facing sandbox command. The existing headless-server runtime gate remains the proof that this owner-facing playground did not contaminate server composition.

The sandbox is not verification authority. Its timing/input/action/tick lines are diagnostic only and are explicitly not FPS, benchmark, soak, replay-acceptance, or leak-proof evidence. Unit/native/integration tests, exact final-candidate heavy CI, exact-merge lightweight master verification, and P0 feasibility gates remain separate acceptance evidence.

## Experimental code boundary

Phase 0 code now lives under `feasibility-spikes/src/main/java/com/samo/spike/` and proves isolated capabilities:

- GLFW/OpenGL initialization;
- Jolt lifecycle;
- OpenAL lifecycle;
- localhost UDP and deterministic impairment;
- Steam initialization and FFM flat-API access;
- combined native smoke/soak executables.

The source was relocated without changing its experimental classification or promoting its behavior into reusable engine layers. Spike-only LWJGL/Jolt/Snaploader/OSHI/Steamworks dependencies are owned by `feasibility-spikes`, while root tasks with the historical names delegate to the matching subproject tasks so existing verification commands remain reproducible. P3-T01 separately places only the scope-approved LWJGL core/GLFW/OpenGL dependencies needed by the production platform adapter; P3-T02 through P3-T06 add no dependency. P3-T07 adds only the scope-approved Jackson JSON parser to `engine-platform-lwjgl`; P3-T08 through P3-T10 add no dependency or project edge. P3-T04A/P3-T09/Issue #165 consume the platform stack only through the sandbox's non-consumable runtime configuration. None of these tasks promotes the spike executable or its debug/render-loop behavior. P4-T01 adds documentation/decision state only; P4-T02/P4-T03 add only the scope-selected pure-Java JOML math dependency and core spatial API, with no native/spike dependency promotion.

P0-T09A / Issue #42, P0-T13 / Issue #43, and P0-T14 / Issue #44 remain independent follow-up gates. Moving or reusing proven dependency choices does not satisfy those gates or strengthen prior feasibility claims.

## Architecture verification status

| Property | Status |
| --- | --- |
| 16 production-target modules declared | Implemented |
| Experimental `feasibility-spikes` subproject isolated | Implemented by P1-T10A / Issue #56 |
| Root contains no Java source/runtime spike dependencies | Implemented by P1-T10A / Issue #56 |
| Shared Java 25/test conventions | Implemented |
| Dependency locking/version catalog | Implemented |
| Automated package/module boundary test | Implemented by P1-T07; relocated by P1-T10A; hardened by Issue #62 |
| Client/server executable composition roots | Implemented by P1-T09 / Issue #39 |
| Single-subsystem lifecycle order | Implemented by P2-T01 / Issue #64 with JUnit 6 tests |
| Subsystem dependency ordering without lifecycle side effects | Implemented by P2-T02 / Issue #72 with JUnit 6 tests |
| Coordinated partial-startup rollback | Implemented by P2-T03 / Issue #73 with JUnit 6 tests |
| Monotonic elapsed-time sampling | Implemented by P2-T04 / Issue #74 with JUnit 6 tests |
| Exact 60 Hz fixed-step accumulation | Implemented by P2-T05 / Issue #75 with JUnit 6 tests |
| Bounded frame-gap/catch-up recovery policy | Implemented by P2-T06 / Issue #76 with JUnit 6 tests |
| Renderer-facing interpolation alpha | Implemented by P2-T07 / Issue #77 with JUnit 6 tests |
| Typed startup configuration validation | Implemented by P2-T08 / Issue #78 with JUnit 6 tests |
| Layered startup configuration precedence | Implemented by P2-T09 / Issue #79 with JUnit 6 tests |
| Explicit native-resource ownership diagnostics | Implemented by P2-T10 / Issue #80 with JUnit 6 tests |
| Sampled Java-heap allocation observability evidence | P2-T11 test/evidence path; no production API |
| Synchronous structured logging boundary | Implemented by P2-T12 / Issue #82 with JUnit 6 tests |
| One-shot orderly fatal termination | Implemented by P2-T13 / Issue #83 with JUnit 6 + child-JVM tests |
| 60-second integrated Phase 2 headless gate | Completed under Issue #135 / D-030; retained exact-head PR and merged-`master` CI evidence passed |
| Production GLFW/OpenGL window lifecycle | P3-T01 / Issue #84: `GlfwWindow`, deterministic tests, and real Windows native acceptance |
| Logical/framebuffer size separation | P3-T02 / Issue #85: `WindowSizeListener`, owner-thread polling, deterministic tests, and Windows native acceptance path |
| Windowed/borderless/exclusive transitions | P3-T03 / Issue #86: `WindowMode`, in-place owner-thread transitions, deterministic tests, and 20-transition Windows native acceptance path |
| Focus-loss cursor/input safety | P3-T04 / Issue #87: `setCursorCaptured`, owned focus/key/button callbacks, deterministic tests, and real Windows focus-transfer acceptance path |
| Raw/fallback relative mouse acquisition | P3-T05 / Issue #88: internal `GlfwWindow` cursor-position accumulation, raw-mode selection, deterministic fallback/failure tests, and Windows native mode/focus acceptance path |
| Immutable renderer-frame hardware snapshot | P3-T06 / Issue #89: `InputSnapshot`, engine key/button vocabulary, retained hardware edges, relative-delta consumption, and deterministic frame-snapshot tests |
| Data-driven gameplay action-binding metadata | P3-T07 / Issue #90: strict JSON-v1 loader, eleven typed actions, immutable descriptors, Jackson implementation dependency, deterministic schema tests, completed PR/master CI acceptance |
| Renderer-frame action evaluation/transitions | P3-T08 / Issue #91: stateful evaluator, immutable action snapshots/state, deterministic aggregation/transition tests, sandbox diagnostics, completed PR/master CI acceptance |
| Tick-aligned replayable input commands | P3-T09 / Issue #92: core command/126-byte codec, platform sampler, deterministic headless replay exit evidence, completed PR/master CI acceptance |
| Deterministic input response settings | P3-T10 / Issue #93: core immutable response math plus evaluator mouse-response integration; completed PR #160 / merged-master acceptance |
| Canonical world-space convention | P4-T01 / Issue #94: D-041 + `docs/SPATIAL_CONVENTIONS.md` |
| JOML hot-loop allocation baseline | P4-T02 / Issue #95: JOML 1.10.9 plus warmed zero-byte ThreadMXBean acceptance evidence |
| Cached hierarchical transform API | P4-T03 through P4-T05 / Issues #96–#98: D-042 public JOML math ownership, local TRS, parent-world composition, atomic cycle rejection, private child tracking, and explicit descendant dirty propagation |
| Persistent owner-facing sandbox playground | P3-T04A origin, Issue #165 current presentation policy: public-API cumulative playground with non-exported platform runtime and deterministic control mapping |
| Other concrete production engine subsystems | Planned: later phases |

## Wiki synchronization

This file remains the architecture authority for module roles, boundaries, and accepted implementation maturity. The [`../wiki/`](../wiki/README.md) directory is a lower-authority consumer guide. Whenever an architecture task adds/removes/renames a public API or changes lifecycle, ownership, thread-affinity, failure, configuration, or other caller-visible semantics, update the relevant wiki pages and examples in the same PR. When no consumer behavior changes, record `Wiki impact: none — <reason>` rather than editing the wiki unnecessarily.

`game-sandbox` is also lower-authority than production code/tests and architecture. Its purpose is persistent owner interaction with already-implemented public behavior, not specification. Future tasks must keep the cumulative playground synchronized when appropriate under the `AGENTS.md` sandbox rule without using it to justify a production API or architecture change.

## Phase 5R OpenGL ownership cleanup — P5R-T14 / Issue #274

The accepted T14 implementation keeps the established D-050 resource-wrapper and backend vocabulary unchanged: `OwnedOpenGlHandle`, `OpenGlBuffer`, `OpenGlVertexArray`, `OpenGlTexture`, `OpenGlSampler`, `OpenGlShader`, `OpenGlProgram`, `OpenGlFramebuffer`, `OpenGlResourceBackend`, and `LwjglOpenGlResourceBackend` remain the canonical internal names.

The bounded refactor renames only the ambiguous cleanup utility `CleanupFailures` to `CleanupFailureSuppression` and centralizes the exact repeated rollback rule in `runAndSuppress(primary, cleanup)`: run the same cleanup action, preserve the original primary failure, add a distinct cleanup failure as suppressed, and never self-suppress. Resource creation/registration/deletion order, native handle ownership, owner-thread checks, registry behavior, wrapper-level idempotent close, dynamic-buffer fence ownership, and multi-resource close ordering remain unchanged. No resource manager, cache, pool, backend selector, new native abstraction layer, public API, or Phase 6 identity contract is introduced.

Accepted evidence: final PR head `c82e0276b57773d1524b47eb581c3a1a3bef520d` passed all five required jobs in run #469 / `35511357500`; PR #333 merged as `750e36a678ef70e497d019beafa0c0bf97d56324`; exact merged-master Lightweight verification passed in run #470 / `35511620539`.


## Phase 5R debug/view-model responsibility cleanup — P5R-T15 / Issue #275

The accepted T15 implementation keeps the accepted debug split unchanged: `DebugLineVertexPacker` continues to own bounded world-space debug primitive -> line-vertex packing, and `DebugLineRenderer` continues to own only the corresponding GL resources/upload/draw lifecycle. Their names and behavior remain canonical.

For the internal P5-T17 validation layer, `ViewModelProjectionFactory` is the canonical stateless creator for the fixed 55° vertical-FOV, framebuffer-aspect, 0.01 m near, 10 m far perspective. `ViewModelFixtureVertexPacker` owns only the fixed six-vertex validation-fixture byte packing and linear RGB `(0.95, 0.55, 0.15)`. `ViewModelRenderer` remains the GL resource/render owner and consumes those helpers without transferring ownership.

D-041/D-045 world/view conventions remain untouched, view-model view remains identity, ordering remains world -> debug -> depth-only reset -> view-model, and D-063 presentation plus GL state/restoration semantics remain unchanged. No public view-model/gameplay submission API, weapon/hand system, animation/IK, render graph/FBO, or asset/resource identity is introduced.

Accepted evidence: final PR head `96510a58644eea158c75c3327d3ac1959c0ab9ef` passed all five required jobs in run #471 / `35512198379`; PR #335 merged as `9c41a1db72834d162f58f503a9d03aa0fd00add3`; exact merged-master Lightweight verification passed in run #472 / `35512467254`.


## Phase 5R sandbox entry-point decomposition — P5R-T16 / Issue #276

The accepted T16 implementation keeps public `SandboxMain` as the canonical persistent-playground bootstrap and lifecycle owner while removing unrelated per-frame responsibilities from that entry point. Package-private `SandboxApplicationLoop` owns fixed-step/frame sequencing, public input sampling/evaluation, tick-command sampling, camera application, render/present ordering, and the 5 ms non-exit sleep. `SandboxControlState` owns the current window mode plus input-response settings and applies already-resolved `SandboxControls.SandboxAction` values in the existing side-effect/log order. `SandboxSceneSetup` owns the fixed public point/spot lights, P5-T16 debug primitives, 70° / framebuffer-aspect / 0.1 m / 100 m world projection, and per-frame `RenderFramePacket` construction. `SandboxDiagnostics` owns elapsed diagnostic time, accumulated mouse deltas, command/culling/debug-counter assembly, and once-per-second publication through the unchanged `SandboxDiagnosticFormatter`. `SandboxFramebufferSize` becomes a package-private top-level mutable value rather than a private nested type.

`SandboxCamera`, `SandboxControls`, and `SandboxDiagnosticFormatter` remain canonical and behaviorally unchanged. `SandboxMain` retains banner output, console logger construction, binding-resource materialization, framebuffer-size listener wiring, window/renderer startup ownership, interrupt/failure propagation, ordered shutdown, and native-registry verification. D-041/D-045 camera semantics, focus/cursor safety, owner controls, fixed scene values, render ordering, diagnostics, and the public-production-API-only sandbox boundary remain unchanged. P5R-T17 subsequently removes the verified-obsolete legacy compatibility surface and normalizes only the nested owner-control names.

Accepted evidence: final corrected PR head `8705d6b031b9bb437c74405f5963a60aa731b175` passed all five required jobs in run #479 / `35514285664`; PR #337 merged as `c4d9675e7c03cf046bc684b668ac1117af0f23fe`; exact merged-master Lightweight verification passed in run #480 / `35514559730`. Initial run #473 / `35514092541` is retained only as superseded provenance for the compile-only test-boundary correction.


## Phase 5R sandbox naming/compatibility cleanup — P5R-T17 / Issue #277

Fresh repository-reference verification established that the legacy `EngineDemoMain` class and `:game-sandbox:runEngineDemo` task had no live code/workflow/test consumers and were obsolete compatibility-only surfaces. The accepted T17 implementation removes both and leaves public `SandboxMain` plus `:game-sandbox:runSandbox` as the sole canonical persistent owner-facing entry path.

Within the package-private owner-control mapper, nested `SandboxControls.SandboxAction` and `SandboxControls.SandboxControlInput` are the canonical responsibility-revealing names after T16 decomposition. Enum members, record fields/order, control resolution, simultaneous-action behavior, and every owner-visible F/R/Right-Shift/Ctrl+Q rule remain unchanged. `SandboxApplicationLoop`, `SandboxControlState`, `SandboxSceneSetup`, `SandboxDiagnostics`, `SandboxFramebufferSize`, `SandboxCamera`, `SandboxControls`, `SandboxDiagnosticFormatter`, and `SandboxDiagnosticFormatter.DiagnosticValues` otherwise remain unchanged.

Accepted P5R-T17 evidence: final PR head `31172c12de2eb1a5c78d75a74b1a58268f67faa3` passed all five required jobs in run #482 / `35518803707`; PR #339 merged as `62e9bbd1683557193a6afe27e9b08fbacc32212a`; exact merged-master Lightweight verification passed in run #483 / `35519165178`. Run #481 / `35518792283` is superseded because a later documentation-only commit advanced the PR head.


## Phase 5R standalone renderer visual-demo decomposition — P5R-T18 / Issue #278

The accepted T18 implementation keeps public `RendererVisualDemo` as the owner-facing JavaExec entry point and keeps `:engine-render-opengl:runRendererVisualDemo` unchanged, while removing unrelated responsibilities from that entry class. Package-private `RendererVisualDemoApplication` owns demo composition plus window/native-resource lifecycle, `RendererVisualDemoLoop` owns elapsed-time/input/frame sequencing and renderer -> overlay -> present order, `RendererVisualDemoFramebufferSize` owns framebuffer-size callback state, `AnimatedDemoLighting` owns the exact moving point/spot lights plus their debug crosses/ray, and `MaterialComparisonOverlay` owns the isolated opaque/transparent overlay GL resources, draw state, rollback, and close ordering.

This remains entirely inside the dedicated non-production `visualDemo` source set. The accepted implementation does not change `OpenGlRenderer`, production renderer internals, Gradle dependency/module direction, material/shader ABI or values, D-041/D-045 camera semantics, D-062 local-light semantics, D-063 presentation behavior, native-resource ownership rules, or the persistent `game-sandbox`. No mesh/material/resource API is promoted and no Phase 6 identity/asset work is introduced.

Wiki impact: none — the decomposed types are not supported engine consumer API.
Sandbox impact: none — the persistent sandbox and its controls remain unchanged; only the already-separate renderer visual demo is refactored internally.

Accepted P5R-T18 evidence: corrected final head `5f6ff0c1dfb05a078486abf05f4f10860f14dac4` passed all five required jobs in run #485 / `35523275737`; PR #341 merged as `9fa3a5c831cd0d9884b7b6a89c26029a3d41a6dd`; exact merged-master Lightweight verification passed in run #486 / `35523595439`. Run #484 / `35523166142` is superseded after the malformed test-source newline was corrected.


## Phase 5R client/server version-report naming — P5R-T19 / Issue #279

The T19 candidate keeps `ClientMain` and `ServerMain` as the canonical executable bootstrap entry points because their current responsibilities are already small and explicit. It renames only the executable-specific internal reporting helpers to `com.samo.game.client.internal.ClientVersionReport` and `com.samo.game.server.internal.ServerVersionReport`.

The rename does not change `runClient` / `runServer`, the main-class FQCNs, default startup/shutdown output, `META-INF/sherko-version.properties`, generated metadata, report key order or values, the client/server dependency graphs, or the server headless-runtime boundary. No shared reporter abstraction or new module edge is introduced merely to remove trivial duplicated reporting code.

Wiki impact: none — these are executable/internal surfaces, not supported engine consumer API.
Sandbox impact: none — no engine capability or persistent sandbox behavior changes.
