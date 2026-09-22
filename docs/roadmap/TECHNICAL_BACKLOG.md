# Sherko Engine Technical Backlog

> Canonical detailed task catalog. Existing task IDs remain stable; newly discovered work receives additive IDs and is not automatically materialized as GitHub Issues.
>
> Only the active roadmap phase should normally be materialized as Issues. Keep task IDs stable even when wording is refined.
>
> **Status warning:** every task is written as an unchecked catalog item for readability. These checkboxes are not maintained and do not mean “not done.” Use `docs/DEVELOPMENT_STATUS.md` for the containing-commit checkpoint and GitHub Issues/Project for newer live state.

See [`../../ROADMAP.md`](../../ROADMAP.md) for milestone-level planning and [`../../ENGINE_SCOPE.md`](../../ENGINE_SCOPE.md) for product/architecture boundaries.

## Phase 0 - Feasibility gates and irreversible decisions

Goal: disprove the risky assumptions before building the engine around them.

- [ ] P0-T01 Create `ENGINE_SCOPE.md` containing the exact platform, player count, hosting model, tick rates, world-size limit, and excluded features listed above. Acceptance: every field has one value; no field says “later” or “maybe.”
- [ ] P0-T02 Install a Java 25 toolchain and pin it through Gradle toolchains. Acceptance: `javaToolchains` reports Java 25 and CI uses the same major version.
- [ ] P0-T03 Create one LWJGL spike that opens a 1280x720 GLFW window and clears it with OpenGL 4.6 Core. Acceptance: GL debug output contains no high-severity messages for 10 minutes.
- [ ] P0-T04 Create one Jolt JNI spike containing a static floor and one falling dynamic box. Acceptance: the box settles, all native objects are explicitly released, and repeated start/stop does not grow native memory.
- [ ] P0-T05 Create one OpenAL spike that plays a mono sound from a moving 3D source. Acceptance: left/right positioning changes audibly and source/buffer counts return to zero on shutdown.
- [ ] P0-T06 Launch two JVM processes and exchange numbered UDP datagrams over localhost using `DatagramChannel`. Acceptance: both processes report sequence numbers and measured round-trip time.
- [ ] P0-T07 Initialize Steam from Java in a disposable spike and receive at least one callback. Acceptance: Steam user identity is printed and shutdown completes without a native crash.
- [ ] P0-T08 Verify whether the selected Java Steam binding exposes the exact `ISteamNetworkingSockets` calls required for listen sockets, outbound connections, accepting connections, sending messages, receiving messages, and status callbacks. Acceptance: a written API coverage table links every required operation to a callable Java method.
- [ ] P0-T09 If P0-T08 fails, implement a throwaway Java FFM proof that invokes one harmless Steam flat-API networking function. Acceptance: Java loads the official redistributable and receives a valid return value without authored C/C++ glue.
- [ ] P0-T09A Extend the Java FFM proof through the complete risky `ISteamNetworkingSockets` lifecycle: create a listen socket, connect a second process/account, receive and accept the status callback, send and receive a message, release received message memory, close connection/listen handles, and shut down. Acceptance: two Windows processes exchange numbered payloads through the official API, observe expected callbacks, and finish with all owned native handles released. This task blocks production Steam transport claims but does not block independent Phase 1 foundation work.
- [ ] P0-T10 If P0-T09 fails, change the product decision from listen server to reachable dedicated server before proceeding. Acceptance: `ENGINE_SCOPE.md` contains the replacement topology and expected server operating cost is recorded.
- [ ] P0-T11 Create a network impairment harness supporting configurable latency, jitter, packet loss, duplication, and reordering. Acceptance: the localhost spike observes each impairment independently.
- [ ] P0-T12 Produce a single feasibility executable combining GLFW, OpenGL, Jolt JNI, OpenAL, and UDP. Acceptance: it starts, runs for 15 seconds, exchanges UDP traffic, simulates physics, exercises audio/rendering, and exits cleanly under Java Flight Recorder. Classification: integrated smoke test, not soak evidence.
- [ ] P0-T13 Run the integrated native executable continuously for at least 15 minutes with periodic traffic and metrics. Acceptance: heap, direct memory, Jolt allocation balance, native handles, and UDP queue/echo counts show no monotonic growth and shutdown remains clean.
- [ ] P0-T14 Run 100 supported initialize/use/shutdown lifecycle cycles for independently restartable native subsystems or document which process-global subsystem cannot be restarted safely. Acceptance: every supported cycle returns tracked resources to baseline; process-global limitations become explicit lifecycle contracts.

Exit gates: Phase 1 may proceed after the original API-access/native smoke gates. P0-T09A must pass before P10/P13 may select Steam as production transport. P0-T13 and P0-T14 must pass before documentation claims sustained native stability.

## Phase 1 - Build, modules, and quality gates

Goal: make every later AI-generated change small, isolated, testable, and reversible.

- [ ] P1-T01 Initialize a Gradle Wrapper and multi-project build with only `engine-core`, `test-support`, `game-client`, and `game-server`. Acceptance: one command compiles and tests all four modules.
- [ ] P1-T02 Add the remaining empty modules from the target tree. Acceptance: `projects` lists every module and no circular project dependency exists.
- [ ] P1-T02A Add `engine-ui` as a renderer-neutral runtime game UI module before package boundaries freeze. Acceptance: it depends only on approved core/asset APIs, exposes no OpenGL/imgui types, appears in `projects`, and participates in root build/test tasks.
- [ ] P1-T03 Add a centralized dependency version catalog and dependency locking. Acceptance: clean builds resolve identical versions on two machines.
- [ ] P1-T03A Centralize shared group, version, repository, Java 25 toolchain, and test-platform configuration in the root build while leaving module-specific dependencies local. Acceptance: subproject build files contain no duplicated common metadata and all modules compile/test through the same convention.
- [ ] P1-T04 Add JUnit 6 and AssertJ to `test-support`. Acceptance: JUnit 6 is the active shared test baseline and a sample unit test runs in every engine module.
- [ ] P1-T05 Add Checkstyle with rules forbidding wildcard imports, empty catch blocks, and ignored return values where detectable. Acceptance: a deliberately invalid test file fails the check task.
- [ ] P1-T06 Add JaCoCo reporting without enforcing an arbitrary global percentage. Acceptance: XML and HTML reports are generated in CI.
- [ ] P1-T07 Define package roots so each Gradle module exports only its API packages. Acceptance: an architecture test rejects a game-to-platform implementation shortcut.
- [ ] P1-T08 Add CI jobs for compile, unit tests, architecture tests, and Windows native smoke tests. Acceptance: CI runs on pull requests and pushes to `master`, and a failure in any required job fails the workflow.
- [ ] P1-T09 Add client and headless-server run tasks with separate main classes. Acceptance: server starts without initializing GLFW, OpenGL, or OpenAL.
- [ ] P1-T10 Add a reproducible `--version` command reporting engine commit, protocol version, asset version, Java version, and native-library versions. Acceptance: client and server print compatible values.
- [ ] P1-T10A Move disposable Phase 0 spike sources/dependencies out of the root production project into a clearly named `feasibility-spikes` module after their evidence is preserved. Acceptance: the root becomes an aggregator, production client/server dependency graphs exclude spike-only libraries/resources, and all spike commands remain reproducible or are explicitly archived.

Exit gate: empty client and server applications build and run through repeatable commands.

## Phase 2 - Core lifecycle, time, configuration, and native ownership

Goal: establish the contracts every subsystem will follow.

- [ ] P2-T01 Define `EngineSubsystem` with explicit `initialize`, `start`, `stop`, and `close` phases. Acceptance: lifecycle order is unit-tested.
- [ ] P2-T02 Implement a dependency graph that topologically sorts subsystems. Acceptance: a synthetic circular dependency fails before initialization and prints the cycle.
- [ ] P2-T03 Implement rollback shutdown when subsystem initialization fails halfway. Acceptance: already-started test subsystems close in reverse order exactly once.
- [ ] P2-T04 Implement `EngineClock` using a monotonic nanosecond source. Acceptance: clock tests reject negative elapsed durations.
- [ ] P2-T05 Implement a fixed-step accumulator with 1/60-second simulation steps. Acceptance: simulated time is equal for 30, 60, 144, and irregular render-frame sequences.
- [ ] P2-T06 Clamp a single incoming frame gap and cap catch-up steps. Acceptance: a simulated 2-second stall does not run unbounded update steps.
- [ ] P2-T07 Expose render interpolation alpha separately from simulation delta. Acceptance: simulation always receives the fixed delta and renderer receives alpha in [0,1).
- [ ] P2-T08 Implement typed configuration keys with defaults, bounds, source location, and validation errors. Acceptance: invalid fullscreen resolution and tick rate fail before subsystem startup.
- [ ] P2-T09 Implement layered config loading: engine defaults, game config, user config, command-line override. Acceptance: a test verifies the exact precedence order.
- [ ] P2-T10 Implement a native-resource registry storing resource type, allocation site, handle, and closer. Acceptance: debug shutdown fails if a test native handle remains open.
- [ ] P2-T11 Define a per-frame allocation metric using JFR or JDK allocation events. Acceptance: a benchmark reports bytes allocated per simulation tick and render frame.
- [ ] P2-T12 Add structured logging fields for frame, simulation tick, thread, subsystem, connection, and entity. Acceptance: a test log can be filtered by one connection ID.
- [ ] P2-T13 Add fatal assertion behavior that writes logs and terminates only after orderly subsystem shutdown. Acceptance: assertion test closes all registered resources.

Exit gate (D-030 / Issue #135): a headless loop can run deterministic fixed 60 Hz simulation ticks for at least 60 continuous seconds with bounded catch-up and verified cleanup. This is an integration-correctness gate, not a replacement for P0-T13/P0-T14 sustained/repeated native evidence.

## Phase 3 - Platform and input

Goal: produce stable platform events and tick-aligned player commands.

- [ ] P3-T01 Implement `GlfwWindow` creation with explicit OpenGL version/profile hints. Acceptance: the actual GL version and renderer string are logged.
- [ ] P3-T02 Handle framebuffer-size events separately from logical window-size events. Acceptance: renderer receives pixel dimensions after DPI scaling.
- [ ] P3-T03 Implement windowed, borderless, and exclusive fullscreen transitions. Acceptance: switch modes 20 times without losing the context.
- [ ] P3-T04 Implement focus-loss handling that releases captured cursor and clears stuck input states. Acceptance: holding a key while alt-tabbing does not leave it pressed.
- [ ] P3-T04A Turn `game-sandbox` into the canonical owner-facing manual engine demo and establish the sandbox-maintenance rule for future tasks. Acceptance: `:game-sandbox:runEngineDemo` exercises already-public Phase 2 timing and P3-T01 through P3-T04 platform behavior without raw native/internal shortcuts, the scripted timeline is unit-tested, the demo-only platform runtime does not contaminate `game-server`, and future tasks must update the sandbox when a capability is meaningfully observable through authorized public APIs or explicitly record `Sandbox impact: none — <reason>`.
- [ ] P3-T05 Implement raw mouse motion when supported and a documented fallback when unavailable. Acceptance: mouse delta is independent of cursor screen position.
- [ ] P3-T06 Store hardware state in `InputSnapshot` once per render frame. Acceptance: gameplay code cannot call GLFW directly.
- [ ] P3-T07 Define data-driven input actions for move, look, jump, crouch, sprint, interact, grab, throw, primary use, pause, and push-to-talk. Acceptance: bindings load from JSON.
- [ ] P3-T08 Implement action transitions: pressed, held, released, and analog value. Acceptance: transition tests cover one-frame taps and simultaneous bindings.
- [ ] P3-T09 Convert input snapshots into numbered `PlayerInputCommand` records per simulation tick. Acceptance: the same command record can be serialized and replayed.
- [ ] P3-T10 Add mouse sensitivity, Y inversion, dead zone, and controller curve settings. Acceptance: each setting has a deterministic unit test.

Exit gate: an input-recording test can replay an identical sequence into the headless simulation.

## Phase 4 - Math and spatial conventions

Goal: prevent coordinate-system and transform bugs from spreading across renderer, physics, audio, and networking.

- [ ] P4-T01 Document one coordinate convention: right-handed world, +Y up, -Z forward, meters, radians internally. Acceptance: renderer, physics, and asset conversion tests cite this document.
- [ ] P4-T02 Use JOML mutable vectors/quaternions/matrices and forbid temporary-object arithmetic in hot loops. Acceptance: transform benchmark allocates zero heap bytes after warm-up.
- [ ] P4-T03 Implement `Transform` with local position, local rotation, local scale, parent, dirty flag, and cached world matrix. Acceptance: parent rotation and non-uniform scale tests pass.
- [ ] P4-T04 Detect and reject transform-parent cycles. Acceptance: assigning an entity below its descendant returns a specific error.
- [ ] P4-T05 Implement transform dirty propagation only to descendants. Acceptance: modifying one leaf does not recompute unrelated world matrices.
- [ ] P4-T06 Implement ray, plane, sphere, AABB, and frustum primitives. Acceptance: edge/contact/inside/outside tests cover each primitive.
- [ ] P4-T07 Implement screen-to-world ray construction from camera matrices. Acceptance: the center pixel produces the camera forward ray within tolerance.
- [ ] P4-T08 Implement view and perspective projection construction with documented near/far planes and depth convention. Acceptance: known points map to expected normalized device coordinates.
- [ ] P4-T09 Implement transform network quantization helpers without integrating networking yet. Acceptance: position and quaternion round-trip errors stay below written tolerances.

Exit gate: spatial tests pass independently of OpenGL and Jolt.

## Phase 5 - Rendering foundation

Goal: render a stable, inspectable 3D room without game or physics dependencies. Reach a basic end-to-end room before adding visual polish.

- [ ] P5-T01 Enable the OpenGL debug callback in debug builds and promote high-severity messages to test failures. Acceptance: an intentional invalid call is captured with source and type.
- [ ] P5-T02 Enforce render-thread ownership for every OpenGL wrapper. Acceptance: a GPU call from a worker thread throws before entering OpenGL.
- [ ] P5-T03 Implement explicit wrappers for buffers, vertex arrays, textures, samplers, shaders, programs, and framebuffers. Acceptance: every wrapper is idempotently closeable and registered for leak detection.
- [ ] P5-T04 Implement the simplest bounded dynamic-buffer upload path that satisfies the first room workload; defer persistent-mapping comparison until representative frame data exists. Acceptance: upload bounds and synchronization are tested, and no unsupported performance claim is recorded.
- [ ] P5-T05 Implement offline GLSL compilation/validation in the build and runtime program-link validation. Acceptance: a broken shader fails before the game enters its loop.
- [ ] P5-T06 Define camera and per-frame uniform blocks with fixed binding indices. Acceptance: shader reflection test verifies size and binding consistency.
- [ ] P5-T07 Render one indexed static mesh with depth testing and back-face culling. Acceptance: RenderDoc shows one expected indexed draw and no validation/debug error.
- [ ] P5-T08 Implement sRGB framebuffer output and sRGB texture sampling rules. Acceptance: a reference gray texture matches expected linear-space output within screenshot tolerance.
- [ ] P5-T09 Implement a material record containing shader variant, textures, scalar parameters, blend mode, depth mode, and cull mode. Acceptance: two materials render the same mesh differently without branching on entity type.
- [ ] P5-T10 Implement render submission as immutable per-frame packets built from world state. Acceptance: renderer has no dependency on gameplay component classes.
- [ ] P5-T11 Implement CPU frustum culling using world AABBs. Acceptance: debug counters prove off-camera meshes generate no draw submission.
- [ ] P5-T12 Sort opaque draws by program/material/mesh and transparent draws back-to-front. Acceptance: a capture confirms order for a controlled scene.
- [ ] P5-T13 Implement one unshadowed directional light with explicit linear-space inputs. Acceptance: a reference normal/light direction produces the expected brightness without requiring the shadow pipeline.
- [ ] P5-T14 Add point and spot lights with a strict configurable combined maximum per frame. Acceptance: the bounded first-N submission order is deterministic, point/spot attenuation semantics are explicit, and exceeding the maximum logs one warning without corrupting buffers or draw state.
- [ ] P5-T15 Finalize correct gamma/sRGB presentation only. Acceptance: known linear reference colors match independently calculated IEC sRGB display-space output with exactly one encode on either supported default-framebuffer mode; fog, tonemapping, HDR/post-processing, bloom, exposure, and color grading remain deferred.
- [ ] P5-T16 Add bounded renderer-neutral debug line, AABB, sphere, ray, and text counters. Acceptance: lower physics/network-capable modules can produce per-frame debug values through `engine-core` without importing OpenGL; the renderer visibly adapts geometry, publishes bounded counter traces after successful frames, and no retained debug scene or runtime UI/font system is introduced.
- [ ] P5-T17 Add a bounded first-person view-model render layer with explicit independent FOV/projection and isolated depth handling. Acceptance: an engine-owned camera-relative fixture remains visible over a controlled closer world-depth overlap while world D-045 semantics remain unchanged; no public asset/gameplay submission API is introduced.
- [ ] P5-T18 Close the Phase 5 integration gap with one fixed renderer-owned textured-room validation scene and owner-controlled sandbox camera movement/look using existing public input/camera boundaries. Acceptance: one integrated retained scenario demonstrates mapped non-uniform sRGB room texture, depth/occlusion, movable rendered camera, the accepted directional light, correct presentation, debug geometry, and the view-model layer together without adding public asset/resource identity, ECS/world ownership, gameplay, physics, HUD, or Phase 6+ systems.

Exit gate: a textured room with depth, camera movement, one directional light, correct sRGB/gamma, and debug geometry renders without gameplay code. Shadows, fog, tonemapping, and other polish do not block the foundation.

## Phase 5R - Architecture and refactor hardening

Goal: harden the completed P1-P5 codebase before Phase 6 multiplies asset/world dependencies. Improve readability, responsibility boundaries, naming clarity, and scalability without changing accepted runtime behavior or pulling later features forward.

Execution rules for this phase:

- Treat every P5R task as refactor/maintenance work unless its active Issue explicitly authorizes a contract change.
- Execute exactly one bounded P5R task/Issue/branch at a time under `AGENTS.md`.
- No broken intermediate states: every merged P5R task must independently leave the repository buildable, testable, runnable where applicable, behaviorally valid, and documentation-consistent. No task may depend on a later P5R task to restore compilation, tests, runtime behavior, contracts, ownership/lifecycle guarantees, or documentation consistency.
- Temporary migration scaffolding may exist only inside one active bounded Issue when genuinely necessary; it must remain valid throughout the task and be removed or fully reconciled before merge rather than deferred to a later P5R task.
- Prefer internal decomposition and internal renaming before public API renaming.
- Review every touched class/interface/record/enum/method/field/parameter/package/test name for clarity. Rename names that do not communicate responsibility; do not churn names that are already precise.
- Prefer responsibility-bearing suffixes such as `Renderer`, `Controller`, `Coordinator`, `Loader`, `Parser`, `Validator`, `Selector`, `Sorter`, `Factory`, `Descriptor`, and `Snapshot` where they describe the actual role. Avoid vague `Manager`, `Helper`, `Util`, or `Data` names unless they are genuinely the clearest domain term.
- Preserve public behavior, module dependency direction, native ownership/lifetime, D-041/D-045 spatial semantics, persisted/config/wire formats, and accepted validation evidence unless an individual active Issue explicitly says otherwise.
- Public API renames or signature changes require synchronized consumer/wiki updates and independent review.
- Do not introduce patterns speculatively. Facade, Adapter, Strategy/Policy, Command, explicit dependency injection/composition, or other patterns are accepted only where the task identifies a concrete responsibility, coupling, replaceability, ownership, or testability problem.
- No Phase 6 asset/resource identity, ECS/world, physics, gameplay, networking, editor, rendering feature, or unrelated optimization work belongs in P5R.

- [x] P5R-T01 Define the refactor/naming standard and produce a repository-wide symbol inventory with proposed keep/rename/decompose decisions. Acceptance: every production/runtime Java class plus relevant nested public/package-level types is classified as keep, rename, decompose, move, or remove with a reason; no Java behavior changes. Candidate evidence: `docs/refactor/NAMING_STANDARD.md` + `docs/refactor/SYMBOL_INVENTORY.md`; Issue #261.
- [x] P5R-T02 Audit public/internal/package boundaries before renaming. Acceptance: every public package/type touched by later P5R work has a documented consumer role, and no implementation type is promoted to public merely for convenience. Candidate evidence: `docs/refactor/BOUNDARY_AUDIT.md` + `docs/ARCHITECTURE.md`; Issue #262.
- [x] P5R-T03 Decompose `GlfwWindow` native/backend plumbing while preserving `GlfwWindow` as the public platform facade. Acceptance: native callback/backend responsibilities move to descriptively named internal classes, all existing lifecycle/window/native tests remain behaviorally unchanged, and no public signature/module edge changes. Accepted via Issue #263 / PR #313.
- [x] P5R-T04 Decompose `GlfwWindow` input/focus/cursor responsibilities. Acceptance: focus safety, held/edge state, cursor capture, and relative/raw mouse tracking have clear single-purpose internal owners and all P3 focus/input regressions pass unchanged. Accepted via Issue #264 / PR #315.
- [x] P5R-T05 Decompose `GlfwWindow` window-mode/size transition responsibilities. Acceptance: windowed/borderless/exclusive transition planning, monitor targeting, geometry restore, and deferred size delivery are owned by clearly named internal types with existing P3 window-mode behavior unchanged. Accepted via Issue #265 / PR #317.
- [x] P5R-T06 Refactor input-binding loading/parsing/validation names and responsibilities. Acceptance: JSON parsing and semantic validation responsibilities are clear from class/method names, schema v1 and error behavior remain byte/meaning compatible, and input tests stay green. Accepted via Issue #266 / PR #319.
- [x] P5R-T07 Normalize core lifecycle naming and responsibilities. Acceptance: lifecycle coordination types are named by their actual role, startup/rollback/fatal-shutdown semantics remain unchanged, and any public rename follows wiki/review requirements in the active Issue. Accepted via Issue #267 / PR #321.
- [x] P5R-T08 Audit core configuration, logging, timing, and native-resource naming. Acceptance: ambiguous names are replaced with responsibility-revealing names where justified, already-clear names are explicitly retained, and behavior/ownership contracts do not change. Accepted via Issue #268 as a Markdown-only KEEP audit; no code rename was justified.
- [x] P5R-T09 Audit spatial/math naming and structure. Acceptance: camera, transform, geometry, screen-ray, and quantization APIs read consistently with D-041/D-045/D-046/D-047; no coordinate, unit, tolerance, allocation, or serialization semantic changes. Accepted via Issue #269 as a Markdown-only KEEP audit; no source rename or restructuring was justified.
- [x] P5R-T10 Rename/decompose the fixed reference-scene renderer path. Accepted via Issue #270 / PR #325: `IndexedStaticMeshPipeline` became internal `ReferenceSceneRenderer`, package-private `ReferenceRoomFixture` owns fixed room vertex/index/texture/bounds/count data only, Phase 5 output/public renderer contracts remained unchanged, and T11+ orchestration work was not pulled forward.
- [x] P5R-T11 Split renderer frame orchestration by responsibility. Accepted via Issue #271 / PR #327: package-private non-owning frame-uniform, visibility/submission-planning, draw-execution, and diagnostics owners now make the orchestration boundaries explicit while `ReferenceSceneRenderer` retains lifecycle/native-resource ownership and public `OpenGlRenderer` behavior/draw semantics remain unchanged.
- [x] P5R-T12 Normalize renderer material/submission/culling/light internal names. Accepted via Issue #272 / PR #329: `RendererMaterial` became `RenderMaterialDescriptor`, `MaterialStatePolicy` became `OpenGlMaterialStatePolicy`, and `LocalLightSelection` became `LocalLightSelector`; already-clear submission/culling/directional-light/material-companion names were retained and ordering/state/limits/shader behavior remained unchanged.
- [x] P5R-T13 Normalize uniform-block/color/presentation naming. Accepted via Issue #273 / PR #331: `CameraUniformBlock` became `CameraMatricesUniformBlock`, `PerFrameUniformBlock` became `FramebufferMetricsUniformBlock`, and `PresentationMode` became `SrgbPresentationMode`; already-clear color/reflection/local-light names and all shader ABI/binding/layout plus D-056/D-063 output semantics remained unchanged.
- [x] P5R-T14 Refactor OpenGL resource/backend ownership internals. Accepted via Issue #274 / PR #333: all established resource-wrapper/backend names stayed unchanged; `CleanupFailures` became `CleanupFailureSuppression`, equivalent rollback suppression was centralized through `runAndSuppress(...)`, and native ownership/idempotent-close/thread-affinity/leak regressions remained green.
- [x] P5R-T15 Refactor debug-geometry and view-model internals. Accepted via Issue #275 / PR #335: `DebugLineVertexPacker`, `DebugLineRenderer`, and `ViewModelRenderer` remained canonical; `ViewModelProjection` became `ViewModelProjectionFactory`; fixed six-vertex fixture packing moved to `ViewModelFixtureVertexPacker`; P5-T16/P5-T17/P5-T18 behavior and spatial/order/ownership contracts remained unchanged.
- [x] P5R-T16 Decompose `SandboxMain` into a small entry point plus clearly named application-loop/input/scene/diagnostic responsibilities. Accepted via Issue #276 / PR #337: public `SandboxMain` remains bootstrap/lifecycle owner while `SandboxApplicationLoop`, `SandboxControlState`, `SandboxSceneSetup`, `SandboxDiagnostics`, and top-level `SandboxFramebufferSize` own extracted responsibilities; owner controls, camera/focus behavior, renderer usage, diagnostics, scene values, and shutdown behavior remained unchanged.
- [x] P5R-T17 Clean sandbox naming and legacy compatibility surface. Accepted via Issue #277 / PR #339: removed verified-obsolete `EngineDemoMain` / `runEngineDemo`, renamed nested `SandboxControls.Action` -> `SandboxAction` and `SandboxControls.Input` -> `SandboxControlInput`, retained T16 helper names, and kept canonical `SandboxMain` / `runSandbox` behavior unchanged.
- [x] P5R-T18 Decompose the standalone renderer visual demo. Accepted via Issue #278 / PR #341: public `RendererVisualDemo` / `runRendererVisualDemo` remain unchanged while package-private `RendererVisualDemoApplication`, `RendererVisualDemoLoop`, `RendererVisualDemoFramebufferSize`, `AnimatedDemoLighting`, and `MaterialComparisonOverlay` own the separated lifecycle, frame-loop/framebuffer, animation/debug, and GL-overlay responsibilities; corrected final head `5f6ff0c1dfb05a078486abf05f4f10860f14dac4` passed run #485 / `35523275737`, merged as `9fa3a5c831cd0d9884b7b6a89c26029a3d41a6dd`, and exact-merge Lightweight verification passed in run #486 / `35523595439`.
- [x] P5R-T19 Normalize client/server bootstrap and version-report naming. Accepted via Issue #279 / PR #343: `ClientMain` / `ServerMain` and their Gradle entry tasks remain unchanged; internal `VersionReport` helpers became `ClientVersionReport` / `ServerVersionReport`; final head `47ae52f78f9f9d99d462975266337025b84ae9f6` passed all five jobs in run #487 / `35524411941`, merged as `7215fc12c123325adb62521e971f5dc4965064d7`, and exact-merge Lightweight verification passed in run #488 / `35524646827` with compatibility output and headless/dependency boundaries unchanged.
- [x] P5R-T20 Audit feasibility-spike naming/isolation. Accepted via Issue #280 / PR #345: renamed only `IntegratedNativeSoakSpike` -> `IntegratedNativeEvidenceHarness`, kept the other eight reviewed names, preserved historical task/evidence behavior, and added `verifyFeasibilitySpikeIsolation`; final head `244041127c1851d477f557c92734c981ee5649b7` passed all five jobs in run #489 / `35525360797`, merged as `510e61d6d44eab4cb986d5c03078138aaa40a020`, and exact-merge Lightweight verification passed in run #490 / `35525718982`.
- [x] P5R-T21 Perform the deliberate public-API naming pass after internals stabilize. Accepted via Issue #301 / PR #347: all 60 supported public top-level types, 15 supported nested public types, and their public member vocabulary were reviewed; every current public name remains KEEP, no compatibility shim/source/wiki/sandbox change was introduced, and Markdown-only final head `32886cb82e69c199ed6747630aa42b12ac55b125` merged as `d79f6d6c18490c839157770e51e9908eb2f7e13d` under the documented CI exemption.
- [x] P5R-T22 Synchronize test/fixture/evidence naming with the refactored production vocabulary. Accepted via Issue #302 / PR #349: all 99 test classes and current `@Test` method names were reviewed; only `broken.frag` -> `invalid-syntax.frag` plus the directly coupled test-method/path wording changed, with byte-identical fixture content and historical CI/evidence identifiers preserved. Final head `5bda2120526a40c4176aa161ba1492060118860f` passed all five required jobs in run #491 / `35530836855`, merged as `387549c83ffa6af9d8d69b0373772e84ac23053c`, and exact-merge Lightweight verification passed in run #492 / `35532808303`.
- [x] P5R-T23 Reorganize internal packages only where stable responsibility groups are now evident. Accepted via Issue #303 / PR #351: fresh audit found no justified move; renderer internals remain one package-private connected component, platform helpers stay colocated with public facades to avoid visibility widening, visual-demo internals retain package-private renderer access, and client/server internal roots remain unfragmented. Markdown-only final head `d6ce4a096c47ec4226fc070ff65f2a76eba4486e` merged as `36352d874c04c383ce53527001e279f7634f973b` under the documented CI exemption.
- [x] P5R-T24 Perform a pattern/scalability hardening pass based on observed code. Accepted via Issue #304 / PR #353: package-private `OpenGlBackendSet` now composes the existing resource/draw/reflection adapter trio without merging interfaces or changing public/native behavior; speculative Singleton, Service Locator, Object Pool, ECS, job-system, backend-registry, and mega-interface work was explicitly rejected. Final head `aa43d2fee5e4f4f08723ba1323232eb7c8dc5005` passed all five required jobs in run #493 / `35534567287`, merged as `3d7a3fbf302b1b4caf46ca780aeb68f651810fa1`, and exact-merge Lightweight verification passed in run #494 / `35534881306`.
- [x] P5R-T25 Run the final consistency/dead-code/stale-name cleanup. Accepted via Issue #305 / Markdown-only PR #355: final audit on baseline `4bba10629e73a7899e2aac04f381db501773afd3` found no justified source/Gradle/resource/wiki/sandbox cleanup, no authored-Java `@Deprecated` compatibility shim, no orphaned P5R helper, and no avoidable compatibility alias; historical evidence identifiers remain intentionally preserved. Final audit head `12681d0a1261926a32961d86ee541c1376e7ff7f` merged as `9ab1ef0d449c7ee5c390767c3ba0b58f967b7c6a` under the Markdown-only CI exemption.
- [x] P5R-T26 Execute the Phase 5R exit review. Accepted via Issue #306 / PR #357: T01-T25 acceptance, sandbox/visual-demo entry contracts, public/internal/module/native-ownership/spatial/persisted/config/wire/protocol reconciliation, stale-name cleanup, and Phase 6 readiness were reviewed with no blocker found. Exact candidate `40f2bb91ae3afeeee07d7f8be92dd7b088419fa0` passed all five required jobs in run #495 / `35537699489`; PR #357 merged as `5e0cac4e7748a66b7c2d19e0cf444eaca0a49fce`; exact-merge Lightweight verification passed in run #496 / `35537989459`. Phase 5R exit result: PASS.

Exit gate: **PASS.** The completed P1-P5 codebase passed full repository verification and owner-facing regression review after responsibility decomposition and naming normalization, with no unintended behavior/module/ownership/spatial/format changes and no unresolved stale names/docs. Phase 6 is no longer blocked by Phase 5R; P6-T01 still requires fresh materialization/refinement before implementation.

## Phase 6 - Asset pipeline and resource lifetime

Goal: remove source-format parsing and raw file paths from runtime gameplay.

- [ ] P6-T01 Define `AssetId` as a stable 128-bit identifier independent of file path. Acceptance: moving a source file does not change references after metadata moves with it. Accepted through Issue #362 / PR #363; final head `ae34d0cd84be2899fecd821f9af485a6f4c1f084` passed run #504, merged as `37f3c163d2bb492ee08f6306e07b017c6ea26119`, and exact-merge Lightweight verification passed in run #505.
- [ ] P6-T02 Define versioned source metadata for mesh, texture, material, skeleton, animation, audio, prefab, and scene assets. Acceptance: unknown versions fail with an upgrade-required message. Accepted through Issue #365 / PR #366; final head `ab15ccb5f1b03f9b2d655ce846afb6f849f87e4b` passed run #507, merged as `f91956c7d455b01d0895ca787bb110b802083ec1`, and exact-merge Lightweight verification passed in run #508.
- [ ] P6-T03 Implement a command-line asset cooker with one input directory and one output cache. Acceptance: clean cooking produces a manifest and nonzero cooked files. Accepted through Issue #368 / PR #369; final head `1843321e380302c9748fd55a61db5353076c6e3e` passed run #511, merged as `3d73c0de0cc534a95021b38560f8690e24a18065`, and exact-merge Lightweight verification passed in run #512.
- [ ] P6-T04 Import glTF meshes through Assimp in the cooker. Acceptance: positions, normals, tangents, UVs, and indices match a known reference asset. Accepted through Issue #371 / PR #372; final head `1d3ac786664f8e4ea1f892f600e4ed42326febb3` passed run #531, merged as `cf034184495f0e914032d330eca85dd869694d71`, and exact-merge Lightweight verification passed in run #532.
- [ ] P6-T05 Convert imported coordinates and units into the engine convention exactly once during cooking. Acceptance: a one-meter reference cube has a one-meter engine AABB. Accepted through Issue #374 / PR #375; final head `f9291a8143b514fe3ad4e3954dd664b3a012ed5e` passed run #536, merged as `019bbaec98b37d92791c375bc8d553c06257b1ee`, and exact-merge Lightweight verification passed in run #537.
- [ ] P6-T06 Generate missing tangents and reject meshes missing required UVs for tangent-space materials. Acceptance: errors include asset path and mesh name. Accepted through Issue #377 / PR #378; final head `3e531c87e5c6438a722d8b00db59a9259ba2f542` passed run #540, merged as `7b0a59a1518dd58804e81c6bae5b83cc9095cec8`, and exact-merge Lightweight verification passed in run #541.
- [ ] P6-T07 Cook vertex/index streams into a documented binary schema with magic, schema version, counts, bounds, and checksum. Acceptance: corrupted payload is rejected before GPU upload. Accepted through Issue #380 / PR #381; final head `57b6926e9626cc96bee47453e6d249057a064972` passed run #544, merged as `c43e703e54ee325b0be5287a4520a38fc05da939`, and exact-merge Lightweight verification passed in run #545.
- [ ] P6-T08 Decode PNG/JPEG only in the cooker and generate mip chains. Acceptance: runtime loads the cooked texture without invoking image decoding.
- [ ] P6-T09 Cook mono/stereo audio metadata and Vorbis payloads with validated sample rate/channel count. Acceptance: a malformed source is rejected by the cooker.
- [ ] P6-T10 Build a dependency graph from materials to textures/shaders and scenes/prefabs to their assets. Acceptance: recooking a texture invalidates only affected dependents.
- [ ] P6-T11 Implement `ResourceHandle<T>` with loading, ready, failed, and released states. Acceptance: gameplay cannot obtain an untyped native handle.
- [ ] P6-T12 Implement fallback mesh, texture, material, and sound assets. Acceptance: missing content yields visible/audible fallback and a structured error instead of a crash.
- [ ] P6-T13 Implement asynchronous file read/decompression but perform GPU upload on the render thread. Acceptance: loading a large mesh does not call OpenGL off-thread.
- [ ] P6-T14 Add development hot reload for shader and material assets only. Acceptance: a material edit appears without restarting and failed reload preserves the prior valid resource.

Exit gate: the runtime starts using only a cooked asset directory and manifest.

## Phase 7 - World, entities, components, prefabs, and scenes

Goal: create a data-driven gameplay foundation without a monolithic inheritance tree.

- [ ] P7-T01 Implement a generational `EntityId(index,generation)`. Acceptance: destroying and reusing an index never makes an old ID valid.
- [ ] P7-T02 Implement one packed component store keyed by entity index. Acceptance: add/get/remove/iteration and stale-ID tests pass.
- [ ] P7-T03 Add deferred structural commands for entity/component creation and removal during iteration. Acceptance: a system can destroy its current entity without corrupting iteration.
- [ ] P7-T04 Define fixed world update phases: input, pre-physics, physics, post-physics, gameplay, replication capture, presentation extraction. Acceptance: phase order is asserted in a test.
- [ ] P7-T05 Implement `TransformComponent`, `NameComponent`, `MeshRendererComponent`, `CameraComponent`, and `AudioEmitterComponent`. Acceptance: a scene creates each entirely from data.
- [ ] P7-T06 Separate stable authoring GUIDs from transient runtime entity IDs. Acceptance: saving and reloading resolves cross-entity references after runtime IDs change.
- [ ] P7-T07 Define scene JSON with schema version, entity GUID, parent GUID, and versioned component objects. Acceptance: load-save-load preserves semantic equality.
- [ ] P7-T08 Reject unknown required components while preserving or warning on unknown optional editor data. Acceptance: behavior is covered by fixtures.
- [ ] P7-T09 Define prefab JSON with nested prefabs and explicit property overrides. Acceptance: changing the prefab updates non-overridden instance values.
- [ ] P7-T10 Detect circular prefab references before instantiation. Acceptance: error prints the complete reference chain.
- [ ] P7-T11 Implement scene load into a temporary world followed by atomic activation. Acceptance: a failed scene load leaves the current world unchanged.
- [ ] P7-T12 Implement a bounded event queue with typed event IDs and explicit payload codecs. Acceptance: event overflow is observable and cannot grow memory without limit.
- [ ] P7-T13 Add a world query API for component combinations used by renderer, physics, and audio. Acceptance: these systems do not depend on game-specific entity subclasses.

Exit gate: a JSON scene instantiates a complete rendered room through components and stable asset IDs.

## Phase 8 - Physics and local interaction

Goal: create the local physics sandbox that networking must later reproduce.

- [ ] P8-T01 Wrap Jolt JNI initialization/shutdown behind `PhysicsBackend`. Acceptance: `engine-world` contains no Jolt imports.
- [ ] P8-T02 Define collision layers for static world, player, dynamic prop, trigger, held prop, enemy, and projectile/query. Acceptance: a collision matrix test covers every pair.
- [ ] P8-T03 Implement box, sphere, capsule, convex hull, and static triangle-mesh shape assets. Acceptance: dynamic triangle meshes are rejected unless explicitly supported.
- [ ] P8-T04 Cook convex hull and static mesh collision in the asset cooker. Acceptance: runtime never constructs large static mesh shapes from raw vertices.
- [ ] P8-T05 Implement rigid-body creation from `RigidBodyComponent` and `ColliderComponent`. Acceptance: static, dynamic, and kinematic examples match their declared behavior.
- [ ] P8-T06 Establish one-way ownership rules: kinematic transform drives physics; dynamic physics drives transform after each step. Acceptance: no entity is written both ways in one tick.
- [ ] P8-T07 Step physics only from the fixed simulation phase. Acceptance: changing render FPS does not change a recorded drop test beyond tolerance.
- [ ] P8-T08 Queue contact begin, persist, and end events without mutating the world inside native callbacks. Acceptance: callback stress test produces no concurrent modification.
- [ ] P8-T09 Implement raycast, shape cast, overlap, and ground-probe query APIs with reusable result buffers. Acceptance: hot query benchmark allocates zero heap bytes.
- [ ] P8-T10 Implement a `CharacterMotor` using the supported Jolt character-controller approach. Acceptance: walk, jump, slope limit, stair step, ceiling, and moving-platform fixtures pass.
- [ ] P8-T11 Implement interaction targeting using a camera ray plus short-radius shape cast. Acceptance: thin and slightly off-center objects remain selectable without selecting through walls.
- [ ] P8-T12 Implement grab as a configurable physical constraint to a hand target. Acceptance: the held body keeps collision and has maximum force/distance limits.
- [ ] P8-T13 Implement release and throw using server-ready intent parameters: target entity and normalized charge, not arbitrary client force. Acceptance: identical commands produce bounded forces.
- [ ] P8-T14 Implement pushable buttons, hinged doors, and breakable constraints through reusable components. Acceptance: none of these require a renderer-specific or object-name branch.
- [ ] P8-T15 Add Jolt body/shape/constraint leak accounting. Acceptance: loading and unloading the sandbox 100 times returns counts to baseline.
- [ ] P8-T16 Implement production OpenAL device/context/buffer/source lifecycle in `engine-audio-openal`. Acceptance: one mono sound plays through the engine API and every native object is released idempotently.
- [ ] P8-T17 Add renderer/world-independent listener and emitter pose inputs with distance attenuation. Acceptance: a moving source pans/attenuates correctly without gameplay code importing OpenAL.
- [ ] P8-T18 Convert bounded physics impacts into cooldown/threshold-filtered local audio events. Acceptance: controlled collisions produce one readable sound event while resting contacts cannot create an audio storm.

Exit gate: one local player can traverse a room, grab/throw props, open doors, press buttons, and trigger collision sounds.

## Phase 9 - Local first-person vertical slice

Goal: prove the engine can support the intended game before network complexity is added.

- [ ] P9-T01 Split player logic into input source, controller, character motor, camera, interactor, hands, status, and presentation components/services. Acceptance: no `Player` class owns all responsibilities.
- [ ] P9-T02 Implement camera yaw on the body and pitch on a camera pivot with clamped pitch. Acceptance: physics capsule never inherits camera pitch.
- [ ] P9-T03 Add crouch with clearance validation before standing. Acceptance: player cannot stand into a ceiling.
- [ ] P9-T04 Add sprint as a validated movement mode, not a direct position multiplier. Acceptance: diagonal movement does not exceed target speed.
- [ ] P9-T05 Implement camera bob, sway, shake, and landing response as presentation-only effects. Acceptance: disabling presentation does not change authoritative position.
- [ ] P9-T06 Implement `InteractableComponent` with supported verbs and server-validatable range/line-of-sight rules. Acceptance: interaction test rejects occluded and out-of-range targets.
- [ ] P9-T07 Implement an inventory with explicit slot count and stable item asset IDs. Acceptance: duplicate pickup and drop paths preserve exactly one item instance.
- [ ] P9-T08 Implement downed, revive, respawn, and team-wipe states as a state machine. Acceptance: every transition and illegal transition is tested.
- [ ] P9-T09 Implement one cooperative objective requiring two distinct interactions. Acceptance: objective completion is driven by component state/events, not level object names.
- [ ] P9-T10 Add deterministic input recording and local replay for the vertical slice. Acceptance: replay reaches the same non-physics gameplay states and records physics divergence metrics.
- [ ] P9-T11 Define renderer-neutral runtime UI nodes, layout inputs, and immutable draw commands in `engine-ui`. Acceptance: UI code contains no OpenGL or imgui imports and a fake renderer consumes a deterministic draw list.
- [ ] P9-T12 Implement font-atlas/text measurement with bounded glyph fallback and cooked font metadata. Acceptance: known strings measure/render consistently and missing glyphs produce an explicit fallback.
- [ ] P9-T13 Implement anchors, padding, alignment, stacking, scaling, clipping, buttons, labels, images, sliders, and scroll lists. Acceptance: layout fixtures pass at 1280x720, 1920x1080, and 2560x1440.
- [ ] P9-T14 Implement UI focus, keyboard/mouse/controller navigation, and input consumption. Acceptance: controller-only navigation reaches every interactive control and consumed UI input never triggers gameplay actions.
- [ ] P9-T15 Implement main menu, pause, settings, loading, results/retry, and error-dialog screen states. Acceptance: the local vertical slice enters/exits each screen without directly manipulating renderer state.
- [ ] P9-T16 Implement HUD, interaction prompt, objective state, and downed/revive indicators from presentation data. Acceptance: HUD reads immutable presentation state and cannot mutate authoritative gameplay.
- [ ] P9-T17 Add UI accessibility hooks for remapping, text scale, reduced motion, subtitle metadata, and non-color-only critical state. Acceptance: settings are data-driven and persist through the engine configuration layer.

Exit gate: a five-minute local level demonstrates movement, props, basic audio, runtime menu/HUD flows, one cooperative objective, failure, and restart.

## Phase 10 - Network transport and protocol

Goal: build a transport-independent, bounded protocol before replicating gameplay.

- [ ] P10-T01 Define `GameTransport` for listen, connect, poll events, send with `RELIABLE_ORDERED` or `UNRELIABLE_SEQUENCED`, disconnect, and close. Acceptance: loopback, IP-development, and eventual Steam implementations pass the same contract tests.
- [ ] P10-T02 Implement in-memory loopback transport with separate client/server queues. Acceptance: tests can run a complete session without sockets or sleeping.
- [ ] P10-T03 Implement a nonblocking development IP transport with one selector thread owning `ServerSocketChannel`/`SocketChannel` for reliable traffic and `DatagramChannel` for time-sensitive traffic. Acceptance: simulation thread never blocks on socket I/O.
- [ ] P10-T04 Define the UDP header fields: protocol magic, protocol version, connection ID, packet sequence, flags, and payload length; define a separate length-prefixed TCP frame. Acceptance: golden-byte tests fix byte order and exact layouts.
- [ ] P10-T05 Reject wrong magic, unsupported version, unknown connection, undersized headers, oversized frames/datagrams, and declared-length mismatch before payload parsing. Acceptance: fuzz corpus causes no uncaught exception or allocation explosion.
- [ ] P10-T06 Implement sequence-number wrap comparison. Acceptance: tests cover the full wrap boundary.
- [ ] P10-T07 Implement unreliable-sequenced messages for snapshots. Acceptance: older messages arriving late are dropped.
- [ ] P10-T08 Map reliable-ordered messages to framed TCP in the development IP adapter and to the transport-provided reliable mode in SteamNetworkingSockets. Acceptance: control messages arrive once and in order without implementing a second reliability layer over Steam.
- [ ] P10-T09 Bound each connection's inbound queue, outbound reliable queue, outbound unreliable queue, TCP frame size, UDP datagram size, and total queued bytes. Acceptance: malicious enqueue attempts cannot grow memory beyond configured limits.
- [ ] P10-T10 Implement handshake states with nonce/cookie, protocol version, build version, player identity, and explicit rejection reason. Acceptance: replayed or mismatched handshakes are rejected.
- [ ] P10-T11 Implement timeout, keepalive, graceful disconnect, and abrupt-loss transitions. Acceptance: all state transitions are covered under the impairment harness.
- [ ] P10-T12 Implement client/server clock offset and RTT estimation without changing the JVM wall clock. Acceptance: synthetic delay changes converge within documented error.
- [ ] P10-T13 Implement packet capture to a versioned debug file with sensitive tokens excluded. Acceptance: a captured session can be decoded offline.
- [ ] P10-T14 Expose live transport metrics: RTT, loss, jitter, bytes/sec, packets/sec, reliable backlog, and dropped queue count. Acceptance: impairment settings produce corresponding metric changes.

Exit gate: four clients remain connected to one server for 60 minutes under 100 ms latency, 20 ms jitter, 2% loss, and 0.2% reordering.

## Phase 11 - Replication and join-in-progress

Goal: reproduce authoritative world state without coupling every component to socket code.

- [ ] P11-T01 Define a session-scoped `NetworkEntityId` allocator owned only by the server. Acceptance: clients cannot allocate authoritative IDs.
- [ ] P11-T02 Define `ReplicationDescriptor` per replicated component with explicit field codecs and schema version. Acceptance: adding a non-replicated Java field does not silently change the wire format.
- [ ] P11-T03 Implement authoritative spawn containing prefab ID, network entity ID, owner ID, initial transform, and initial replicated state. Acceptance: duplicate spawn is idempotent.
- [ ] P11-T04 Implement despawn with tombstone retention long enough to reject late updates. Acceptance: reordered state cannot resurrect a destroyed entity.
- [ ] P11-T05 Capture world snapshots only after the fixed tick completes. Acceptance: one snapshot never mixes pre- and post-physics state.
- [ ] P11-T06 Implement per-connection baselines and delta encoding. Acceptance: unchanged entities produce no component payload after acknowledgement.
- [ ] P11-T07 Quantize positions, velocities, rotations, and normalized values with documented ranges. Acceptance: out-of-range values clamp or fail explicitly and round-trip error is tested.
- [ ] P11-T08 Implement relevance by scene plus distance with an always-relevant set for players/objectives. Acceptance: distant props stop consuming bandwidth while objectives remain visible.
- [ ] P11-T09 Implement replication priority using player, interacting/awake body, nearby body, sleeping body, and cosmetic tiers. Acceptance: a bandwidth cap drops low-priority updates first.
- [ ] P11-T10 Implement dormancy for unchanged/sleeping entities and wake notification. Acceptance: sleeping prop sends no recurring state and wakes correctly after collision.
- [ ] P11-T11 Define RPC direction and reliability in a registry. Acceptance: client cannot invoke server-to-client-only RPCs and unknown IDs disconnect or reject safely.
- [ ] P11-T12 Add monotonically increasing event IDs for non-idempotent cosmetic events. Acceptance: duplicate packets do not replay the same impact sound/effect twice.
- [ ] P11-T13 Implement join-in-progress as world header, required asset manifest hash, initial spawn set, full baseline, then live deltas. Acceptance: a fourth client joins a running session without pausing existing players.
- [ ] P11-T14 Reject join when protocol or cooked-asset manifest hashes differ. Acceptance: the client receives a human-readable incompatibility reason.

Exit gate: four clients see consistent spawn/despawn, players, doors, buttons, objective state, and sleeping/awake props.

## Phase 12 - Prediction, reconciliation, interpolation, and physics correction

Goal: make the co-op game playable on real networks rather than only on localhost.

- [ ] P12-T01 Send numbered input commands containing tick, move axes, view angles, action bits, and acknowledged server tick. Acceptance: codec golden test fixes exact fields and bounds.
- [ ] P12-T02 Process inputs only on the server and reject impossible tick jumps, input ranges, and command rates. Acceptance: malformed input cannot create movement or unbounded queues.
- [ ] P12-T03 Predict only the owning player's character motor on the client. Acceptance: non-owned characters never consume local input.
- [ ] P12-T04 Store a bounded ring of predicted states and input commands. Acceptance: memory usage is constant over a one-hour run.
- [ ] P12-T05 Reconcile to server state and replay only commands newer than the acknowledged input. Acceptance: induced error converges and replay count is observable.
- [ ] P12-T06 Separate authoritative transform from smoothed presentation transform. Acceptance: smoothing never changes collision queries or server state.
- [ ] P12-T07 Implement a timestamped interpolation buffer for remote players. Acceptance: 20 Hz snapshots render smoothly at 144 Hz under jitter.
- [ ] P12-T08 Bound interpolation delay and extrapolation duration. Acceptance: a stalled connection freezes gracefully instead of extrapolating indefinitely.
- [ ] P12-T09 Interpolate remote rigid bodies using position, orientation, linear velocity, and angular velocity. Acceptance: rotating thrown props do not visibly step between snapshots.
- [ ] P12-T10 Define server-only authority for dynamic props. Acceptance: clients can request impulses/interactions but cannot submit authoritative transforms.
- [ ] P12-T11 Add local visual prediction for the currently held prop while preserving a separate authoritative physics body. Acceptance: correction is smooth and release uses authoritative velocity.
- [ ] P12-T12 Implement snap-versus-smooth correction thresholds for props. Acceptance: large penetrations snap; small errors converge without oscillation.
- [ ] P12-T13 Replicate body sleep/wake state and suppress interpolation while sleeping. Acceptance: sleeping objects remain visually stationary without continuous packets.
- [ ] P12-T14 Add server rewind only if the game introduces latency-sensitive hitscan. Acceptance: if absent, the task is explicitly marked not applicable instead of implementing unused complexity.
- [ ] P12-T15 Run the vertical slice at 0/50/100/150/200 ms latency and 0/1/2/5% loss. Acceptance: a written matrix records movement error, correction frequency, prop error, bandwidth, and disconnects.

Exit gate: the grab-and-throw vertical slice remains playable at 100 ms RTT and 2% packet loss.

## Phase 13 - Steam session and production transport

Goal: replace manual endpoints with a real invite/join flow without mixing Steam concepts into gameplay.

- [ ] P13-T01 Implement `OnlineServices` separately from `GameTransport`. Acceptance: offline/LAN mode runs with a null online-services implementation.
- [ ] P13-T02 Pump Steam callbacks at a documented point in the client loop. Acceptance: callback delay remains bounded while rendering is throttled or unfocused.
- [ ] P13-T03 Implement create-lobby with owner, build version, protocol version, mode, map, slots, and joinable state. Acceptance: another Steam account can discover compatible metadata.
- [ ] P13-T04 Implement lobby search filters for compatible build, open slots, and region/distance policy. Acceptance: incompatible lobbies never appear as joinable.
- [ ] P13-T05 Implement invite acceptance when the game is closed and when already running. Acceptance: both routes reach the same lobby-join state machine.
- [ ] P13-T06 Implement ready/unready and owner-only launch arbitration. Acceptance: non-owner launch requests are rejected.
- [ ] P13-T07 Implement lobby-to-transport handoff using the Phase 0 validated route. Acceptance: no public IP or secret token is stored in public lobby metadata.
- [ ] P13-T08 If using SteamNetworkingSockets, map Steam connection callbacks into `GameTransport` without exposing Steam types above the adapter. Acceptance: replication contract tests run over the Steam adapter.
- [ ] P13-T09 If using dedicated servers, implement allocation/address retrieval and Steam authentication before connecting. Acceptance: an unauthenticated client cannot enter the world.
- [ ] P13-T10 Handle host disconnect with an explicit return-to-lobby result; host migration remains excluded from v1. Acceptance: clients do not hang or keep simulating an ownerless world.
- [ ] P13-T11 Add Steam-offline error paths for initialization failure, overlay absence, invite failure, and relay unavailability. Acceptance: each path has a user-facing message and clean shutdown.

Exit gate: four remote Steam accounts can invite, join, ready, launch, play, disconnect, and return to menu.

## Phase 14 - Audio, animation, and AI required by the genre

Goal: add presentation and server-driven behaviors after network correctness exists.

### Audio

- [ ] P14-T01 Extend the Phase 8 OpenAL lifecycle with capability reporting plus device-loss/mute/recovery behavior. Acceptance: device loss produces a controlled mute/error state rather than a crash and recovery does not leak sources/buffers.
- [ ] P14-T02 Implement pooled 3D sources with distance attenuation and priority stealing. Acceptance: exceeding source count steals the lowest-priority inaudible source.
- [ ] P14-T03 Update listener pose from presentation camera and source pose from interpolated presentation transforms. Acceptance: network corrections do not create abrupt audio teleport artifacts.
- [ ] P14-T04 Define data-driven audio events with random clips, pitch range, gain range, cooldown, and concurrency limit. Acceptance: repeated prop impacts vary but cannot create an audio storm.
- [ ] P14-T05 Trigger authoritative gameplay sounds from replicated events and local cosmetic sounds from prediction with deduplication. Acceptance: owner hears one interaction sound after confirmation, not two.

### Animation

- [ ] P14-T06 Cook skeleton hierarchy, inverse bind matrices, animation clips, and skin weights from glTF. Acceptance: reference bind pose renders without deformation.
- [ ] P14-T07 Implement CPU pose sampling and two-clip crossfade. Acceptance: a walk-to-idle fixture has continuous transforms.
- [ ] P14-T08 Upload skin matrices through a bounded GPU buffer. Acceptance: oversized skeletons fail with a clear asset-cook error.
- [ ] P14-T09 Separate first-person arms/view-model animation from third-person replicated body animation. Acceptance: remote clients never render the owner's first-person-only mesh.
- [ ] P14-T10 Implement hand IK target from the held item's authored grip points. Acceptance: two differently sized props align both hands without item-specific player code.
- [ ] P14-T11 Add ragdoll activation, authoritative server state, and recovery transition only after ordinary prop replication is stable. Acceptance: ragdoll bandwidth and correction error are measured under impairment.

### AI/navigation

- [ ] P14-T12 Integrate Recast4j only in the cooker/editor to generate navmesh data. Acceptance: runtime loads cooked navmesh without rebuilding it.
- [ ] P14-T13 Implement server-only navmesh queries and path following. Acceptance: clients receive AI state but never decide authoritative paths.
- [ ] P14-T14 Implement perception with explicit vision cone, distance, occlusion ray, hearing event radius, and memory timeout. Acceptance: each sensor has isolated fixtures.
- [ ] P14-T15 Implement a small explicit AI state machine: idle, investigate, chase, interact/attack, recover. Acceptance: transitions are logged and replayable from recorded stimuli.

### Third-person presentation and visual feedback

- [ ] P14-T16 Implement a third-person orbit/follow camera with configurable distance, shoulder side, pitch limits, collision sweep, and obstruction recovery. Acceptance: the camera cannot remain inside level geometry and camera motion never changes the authoritative capsule.
- [ ] P14-T17 Implement third-person character facing and aim alignment rules separate from movement authority. Acceptance: local aim, remote replicated facing, and camera-relative movement remain consistent under network correction.
- [ ] P14-T18 Add a first/third-person presentation acceptance map covering view-model/body visibility, camera switching policy, held-object alignment, and animation continuity. Acceptance: perspective changes permitted by the game do not duplicate meshes, alter collision, or transfer authority.
- [ ] P14-T19 Add only gameplay-proven visual feedback such as particles, decals, shadows, fog, tonemapping, or post-processing, each behind an independent budget/config switch. Acceptance: every added effect cites a target interaction and has CPU/GPU evidence; unused generic effects remain absent.

Exit gate: remote player animation, hand IK, collision audio, third-person presentation, one server-controlled enemy, and optional ragdoll work in the network test map.

## Phase 15 - Editor and debugging tools

Goal: make content creation and diagnosis possible without editing JSON and logs manually.

- [ ] P15-T01 Integrate imgui-java with GLFW/OpenGL input capture. Acceptance: UI focus prevents accidental player input while the editor is active.
- [ ] P15-T02 Add world hierarchy filtered by name, component, and network ID. Acceptance: selecting an entry highlights the exact render and physics entity.
- [ ] P15-T03 Add inspectors for every v1 component using explicit editor adapters, not Java reflection over arbitrary fields. Acceptance: invalid values show validation errors before applying.
- [ ] P15-T04 Add local/world transform editing with undo/redo command objects. Acceptance: 100 undo/redo cycles reproduce the original transform.
- [ ] P15-T05 Add asset browser showing asset ID, source, cooked status, dependencies, and load state. Acceptance: broken references navigate to the missing asset record.
- [ ] P15-T06 Add prefab create/apply/revert and override visualization. Acceptance: override state survives scene save/reload.
- [ ] P15-T07 Add play mode by cloning authoring world into a runtime world. Acceptance: runtime destruction does not modify the saved editor scene.
- [ ] P15-T08 Add physics visualization for shapes, contacts, constraints, sleeping bodies, and collision layers. Acceptance: visualization is removable from release builds.
- [ ] P15-T09 Add network panel for connections, RTT, jitter, loss, bandwidth, reliable backlog, relevant entities, and per-entity bytes. Acceptance: top bandwidth users can be identified during play.
- [ ] P15-T10 Add an in-game command console with typed argument validation and authority restrictions. Acceptance: client cannot execute server-only mutation commands.
- [ ] P15-T11 Add frame timing panel splitting simulation, physics, replication, render extraction, GPU, audio, and GC/allocation. Acceptance: a deliberate delay appears under the correct subsystem.
- [ ] P15-T12 Add deterministic test-scene launch arguments for map, player count, impairment profile, and scripted inputs. Acceptance: one CI command reproduces the multiplayer physics test.

Exit gate: a developer can construct the test level, inspect entities, diagnose physics/networking, and reproduce a failure without source edits.

## Phase 16 - Production hardening and release gate

Goal: turn the engine slice into a shippable base rather than a permanent prototype.

- [ ] P16-T01 Define CPU frame budgets for 60 Hz and GPU budget for representative supported hardware. Acceptance: automated performance scene reports pass/fail per subsystem.
- [ ] P16-T02 Record JFR allocation profiles for menu, level load, quiet gameplay, prop chaos, and disconnect/reconnect. Acceptance: unexpected per-tick allocations have owners or documented exceptions.
- [ ] P16-T03 Run RenderDoc captures for shadow, opaque, transparent, post-process, UI, and view-model passes. Acceptance: redundant clears, invalid resources, and obvious state churn are removed or recorded.
- [ ] P16-T04 Implement bounded pools only where profiling proves allocation or native churn. Acceptance: each pool cites a before/after benchmark; speculative pools are rejected.
- [ ] P16-T05 Add protocol fuzzing for every packet decoder with maximum length/depth/count limits. Acceptance: one million malformed inputs cause no crash or unbounded memory use.
- [ ] P16-T06 Validate every client action on the server: rate, range, ownership, line of sight, state, cooldown, and payload bounds. Acceptance: a test exists for each rejected rule.
- [ ] P16-T07 Run 4-client soak tests for 8 hours with periodic join/leave, scene restart, prop spawning, and impairment. Acceptance: native handles, heap, direct memory, entity count, and reliable backlog return to baselines.
- [ ] P16-T08 Test compatibility on at least NVIDIA, AMD, and Intel GPUs representative of the project's supported Windows hardware. Acceptance: known driver workarounds are capability-gated, not vendor-wide guesses.
- [ ] P16-T09 Create a custom runtime image with only required Java modules. Acceptance: game runs on a Windows machine with no Java installation.
- [ ] P16-T10 Package client with `jpackage`, native DLLs, cooked assets, licenses, and crash/log directory rules. Acceptance: installation and uninstall work from a clean VM.
- [ ] P16-T11 Package a separate headless server image with no graphics/audio natives. Acceptance: dependency inspection confirms it cannot initialize client subsystems.
- [ ] P16-T12 Add save/settings migration with schema version and atomic temp-file replacement. Acceptance: interrupted writes preserve the previous valid settings.
- [ ] P16-T13 Freeze engine API v1 only after the network vertical slice ships internally. Acceptance: public engine packages and compatibility policy are documented.

Final release gate:

1. Four Steam users can form a lobby and launch a session.
2. All players move responsively at 100 ms RTT and 2% packet loss.
3. Two players can contend for, grab, carry, throw, and collide the same prop without permanent divergence.
4. A late joiner receives the correct world, objective, player, door, and prop state.
5. Host departure returns everyone to a defined state.
6. Eight-hour soak test has no monotonic heap, direct-memory, native-handle, entity, or reliable-queue growth.
7. Client is distributed with its JVM; players do not install Java separately.

## Rule for AI-generated tasks

Give the coding agent exactly one task ID at a time, on a dedicated task branch with a linked pull request. Every task prompt must include:

1. Allowed modules and files.
2. Interfaces it may change.
3. Interfaces it must not change.
4. Required tests from the acceptance criterion.
5. Explicit non-goals.
6. Commands used to verify the result.
7. A requirement to stop if the task needs an undeclared architectural change.
8. Wiki impact: list the relevant [`../../wiki/`](../../wiki/README.md) pages that must change when the task adds/removes/renames public API or changes consumer-visible usage/lifecycle/ownership/configuration behavior, or explicitly state `Wiki impact: none — <reason>`.
9. Sandbox impact: update the canonical `game-sandbox` demo when the task's new behavior is meaningfully observable through already-authorized public production APIs, or explicitly state `Sandbox impact: none — <reason>` when demonstrating it would require exposing internals, calling native APIs directly, or pulling future roadmap work forward.

Planned backlog entries do not become usable wiki APIs or sandbox behavior merely because they are written here. Add/update consumer guidance and the owner-facing demo only when the corresponding production capability is actually implemented and verified.