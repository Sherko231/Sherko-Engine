# P5R-T21 Public API Naming Audit

Baseline: `master` `8783cd441b1b126f2bc1858a6706adb3c7bdee66`.

Issue: #301 — deliberate public P1-P5 API naming pass.

## Result

**KEEP all current supported public names.**

Fresh review found no public type/member whose current name materially misstates its responsibility, ownership, lifecycle, units, spatial meaning, representation, or caller-facing operation. The two lifecycle names that previously needed clarification were already corrected by P5R-T07 under D-066: `SubsystemStartupCoordinator` and `FatalTerminationCoordinator`.

A rename in T21 would therefore create source/test/wiki/consumer churn without reducing meaningful ambiguity. No alias, shim, package move, signature change, or compatibility migration is introduced.

## Scope and method

Supported consumer API roots reviewed:

- `com.samo.engine.core.api`
- `com.samo.engine.platform.api`
- `com.samo.engine.render.api`

Review coverage:

- 60 supported public top-level types;
- 15 supported nested public types;
- declared public constructors/factories/operations/queries/constants;
- implicit public record component accessors;
- public interface methods/default methods;
- public enum constants/vocabularies.

Java-public executable/internal declarations documented in `BOUNDARY_AUDIT.md` are not supported engine-consumer API and are not renamed here.

Each name was checked against the P5R naming standard, current source responsibility, accepted decisions, spatial conventions where applicable, public wiki/API guidance, and current consumer usage. Existing concise verbs remain valid where the enclosing type supplies the missing context.

## engine-core public naming review

| Public type | Public member vocabulary reviewed | T21 result |
| --- | --- | --- |
| `Aabb3f` | constructor; `minimum`, `maximum`, `containsPoint`, `intersects(Aabb3f)`, `intersects(Sphere3f)` | KEEP — conventional immutable 3D AABB vocabulary. |
| `CameraMatrices` | `view`, `perspective` | KEEP — class context makes the concise matrix-construction verbs exact; D-045-sensitive. |
| `ConfigEntry` | record components/accessors `value`, `source` | KEEP — raw value plus diagnostic source is explicit. |
| `ConfigError` | record components/accessors `key`, `source`, `message` | KEEP — exact validation-error vocabulary. |
| `ConfigKey<T>` | `name`, `defaultValue` | KEEP — canonical typed-key vocabulary. |
| `ConfigSource` | record component/accessor `description` | KEEP — diagnostic source identity is explicit. |
| `ConfigValidationException` | `errors` | KEEP — exact aggregate-failure vocabulary. |
| `DebugAabb` | record components/accessors `bounds`, `color` | KEEP — exact renderer-neutral debug value. |
| `DebugColor` | record components/accessors `red`, `green`, `blue` | KEEP — linear RGB components are explicit. |
| `DebugFrame` | `MAX_PRIMITIVES`, `MAX_TEXT_COUNTERS`, `EMPTY`; constructor; `primitives`, `textCounters` | KEEP — bounded immutable debug snapshot vocabulary is exact. |
| `DebugLine` | record components/accessors `startX/startY/startZ/endX/endY/endZ/color`; vector convenience constructor | KEEP — world-space endpoint vocabulary is explicit. |
| `DebugPrimitive` | sealed family marker | KEEP — exact common debug-geometry capability. |
| `DebugRay` | record components/accessors `ray`, `lengthMeters`, `color` | KEEP — unit-bearing finite-ray vocabulary is explicit. |
| `DebugSphere` | record components/accessors `sphere`, `color` | KEEP — exact debug wrapper vocabulary. |
| `DebugTextCounter` | `MAX_LABEL_LENGTH`; record components/accessors `label`, `value` | KEEP — deliberately bounded counter, not general text. |
| `EngineClock` | constructors; `sampleElapsedNanos` | KEEP — operation and unit are explicit. |
| `EngineConfigLoader` | constructor; `load` | KEEP — Loader + load is intentionally concise. |
| `EngineConfigSchema` | `FULLSCREEN_WIDTH`, `FULLSCREEN_HEIGHT`, `TICK_RATE`; constructor; `validate` | KEEP — constants map directly to the accepted startup schema; `TICK_RATE` is unambiguous in schema context and changing it would be cosmetic only. |
| `EngineLogger` | constructor; `log`, `flush` | KEEP — exact synchronous logging operations. |
| `EngineLogger.Level` | `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL` | KEEP — established severity vocabulary. |
| `EngineLogger.Context` | record components/accessors `frame`, `simulationTick`, `subsystem`, `connection`, `entity`; `empty` | KEEP — structured diagnostic context is explicit. |
| `EngineLogger.Event` | record components/accessors `timestamp`, `level`, `message`, `threadId`, `threadName`, `context` | KEEP — exact event fields. |
| `EngineLogger.Sink` | `write`, default `flush` | KEEP — caller-owned synchronous sink operations are clear. |
| `EngineSubsystem` | lifecycle constructor for subclasses; `initialize`, `start`, `stop`, `close` | KEEP — canonical lifecycle verbs; protected hooks remain outside the requested public-member rename surface. |
| `FatalTerminationCoordinator` | constructor; `terminate` | KEEP — T07/D-066 already applied the responsibility-bearing coordinator name. |
| `FixedStepAccumulator` | `TICKS_PER_SECOND`; constructor; `advance`, `interpolationAlpha` | KEEP — fixed-step accumulation and interpolation vocabulary is exact. |
| `FixedStepCatchUpPolicy` | `DEFAULT_MAX_FRAME_GAP_NANOS`, `DEFAULT_MAX_STEPS_PER_UPDATE`; constructors; `advance` | KEEP — bounds/units and policy role are explicit. |
| `Frustum3f` | constructor; `containsPoint`, `intersects(Sphere3f)`, `intersects(Aabb3f)` | KEEP — conventional geometry-query vocabulary; D-044-sensitive. |
| `InputResponseSettings` | record components/accessors `mouseSensitivity`, `invertMouseY`, `controllerDeadZone`, `controllerCurveExponent`; `defaults`, `applyMouseX`, `applyMouseY`, `applyControllerAxis` | KEEP — caller-facing response dimensions/operations are explicit. |
| `NativeResourceRegistry` | constructor; `register`, `assertNoOpenResources` | KEEP — ownership registry and terminal diagnostic operation are exact. |
| `NativeResourceRegistry.Registration` | `close` | KEEP — close token semantics are lifecycle-sensitive and clear. |
| `Plane3f` | constructor; `fromPointNormal`, `normal`, `offset`, `signedDistance` | KEEP — conventional plane vocabulary. |
| `PlayerInputCommand` | constructor; `tickId`, `moveX`, `moveY`, `lookX`, `lookY`, `digitalState`, `digitalStates`; standard `equals/hashCode/toString` | KEEP — tick-command vocabulary is explicit. |
| `PlayerInputCommand.DigitalAction` | `JUMP`, `CROUCH`, `SPRINT`, `INTERACT`, `GRAB`, `THROW`, `PRIMARY_USE`, `PAUSE`, `PUSH_TO_TALK` | KEEP — accepted command vocabulary; compatibility-sensitive. |
| `PlayerInputCommand.DigitalState` | record components/accessors `value`, `pressed`, `held`, `released` | KEEP — exact edge/level state vocabulary. |
| `PlayerInputCommandCodec` | `MAGIC`, `VERSION`, `ENCODED_SIZE`; `encode`, `decode` | KEEP — explicit codec direction and layout constants; binary-contract-sensitive. |
| `Ray3f` | constructor; `origin`, `direction`, `pointAt`, `intersectPlane`, `intersectSphere`, `intersectAabb` | KEEP — conventional ray/query vocabulary. |
| `ScreenRays` | `worldRay` | KEEP — direction of conversion is explicit under D-046. |
| `Sphere3f` | constructor; `center`, `radius`, `containsPoint`, `intersects(Sphere3f)`, `intersects(Aabb3f)` | KEEP — conventional geometry vocabulary. |
| `SubsystemGraph` | constructor; `initializationOrder` | KEEP — graph responsibility and resolved ordering are explicit. |
| `SubsystemGraph.Registration` | record components/accessors `id`, `subsystem`, `dependencies` | KEEP — enclosing graph/registration context makes prerequisite IDs clear; renaming only the accessor would add churn without changing meaning. |
| `SubsystemStartupCoordinator` | `start` | KEEP — T07/D-066 already corrected the type name; operation is exact in coordinator context. |
| `Transform` | `parent`, `setParent`, `localPosition`, `setLocalPosition`, `localRotation`, `setLocalRotation`, `localScale`, `setLocalScale`, `worldMatrix` | KEEP — local/world distinction is explicit and D-041/D-042-sensitive. |
| `TransformQuantization` | position/rotation bound constants; `quantizePosition`, `dequantizePosition`, `quantizeRotation`, `dequantizeRotation` | KEEP — conversion direction and units/bounds are explicit; D-047-sensitive. |
| `TransformQuantization.QuantizedPosition` | record components/accessors `x`, `y`, `z` | KEEP — encoded value context is explicit. |
| `TransformQuantization.QuantizedRotation` | record components/accessors `omittedComponent`, `a`, `b`, `c` | KEEP — names match the accepted smallest-three representation; changing them would imply representation churn without semantic benefit. |

## engine-platform-lwjgl public naming review

Package-private collaborators colocated under `com.samo.engine.platform.api` are excluded from this consumer API table and remain implementation details under T02/T03-T06.

| Public type | Public member vocabulary reviewed | T21 result |
| --- | --- | --- |
| `GlfwWindow` | four public constructors; `openGlThreadGuard`, `present`, `pollEvents`, `captureInputSnapshot`, `setCursorCaptured`, `setWindowMode` | KEEP — facade and operations state their platform/lifecycle responsibility precisely. |
| `InputAction` | `MOVE`, `LOOK`, `JUMP`, `CROUCH`, `SPRINT`, `INTERACT`, `GRAB`, `THROW`, `PRIMARY_USE`, `PAUSE`, `PUSH_TO_TALK`; `valueType` | KEEP — accepted gameplay/config vocabulary. |
| `InputActionBindings` | constructor; `load`, `bindingsFor`, `asMap` | KEEP — immutable complete binding-set operations are clear in type context. |
| `InputActionComponent` | `VALUE`, `X`, `Y` | KEEP — exact binding target components. |
| `InputActionEvaluator` | constructors; `setResponseSettings`, `evaluate` | KEEP — stateful evaluation and policy replacement are explicit. |
| `InputActionSnapshot` | `frameId`, `state`, `asMap` | KEEP — immutable one-frame action snapshot queries are concise and clear. |
| `InputActionState` | `action`, `pressed`, `held`, `released`, `value`, `x`, `y` | KEEP — exact per-action level/edge/value vocabulary. |
| `InputActionValueType` | `DIGITAL`, `VECTOR2` | KEEP — exact action value shapes. |
| `InputBinding` | record components/accessors `control`, `component`, `scale` | KEEP — exact binding descriptor vocabulary; schema-sensitive. |
| `InputBinding.Control` | sealed marker capability | KEEP — concise in enclosing `InputBinding` context. |
| `InputBinding.KeyControl` | record component/accessor `key` | KEEP — exact device-neutral keyboard control descriptor. |
| `InputBinding.MouseButtonControl` | record component/accessor `button` | KEEP — exact mouse-button descriptor. |
| `InputBinding.MouseDeltaControl` | record component/accessor `axis` | KEEP — exact relative-motion descriptor. |
| `InputBinding.MouseDeltaAxis` | `X`, `Y` | KEEP — accepted schema vocabulary. |
| `InputBindingLoadException` | no public constructor/member beyond inherited exception API | KEEP — public catch type is named exactly for the load/schema/validation boundary. |
| `InputKey` | `W`, `A`, `S`, `D`, `SPACE`, `LEFT_SHIFT`, `RIGHT_SHIFT`, `LEFT_CONTROL`, `RIGHT_CONTROL`, `LEFT_ALT`, `RIGHT_ALT`, `ESCAPE`, `E`, `Q`, `R`, `F` | KEEP — bounded engine-owned key vocabulary; binding-schema-sensitive. |
| `InputMouseButton` | `LEFT`, `RIGHT`, `MIDDLE`, `BUTTON_4`, `BUTTON_5` | KEEP — bounded device-neutral mouse vocabulary. |
| `InputSnapshot` | `frameId`, `focused`, `cursorCaptured`, key held/pressed/released queries, mouse-button held/pressed/released queries, `mouseDeltaX`, `mouseDeltaY` | KEEP — hardware snapshot level/edge vocabulary is explicit. |
| `OpenGlDebugMode` | `DISABLED`, `FAIL_ON_HIGH_SEVERITY` | KEEP — policy dimension and failure behavior are explicit. |
| `OpenGlThreadGuard` | `assertOwnerThread` | KEEP — non-owning affinity check is precise. |
| `PlayerInputCommandSampler` | `submit`, `nextCommand` | KEEP — frame snapshot ingestion and tick-command emission are clear in sampler context. |
| `WindowMode` | `WINDOWED`, `BORDERLESS_FULLSCREEN`, `EXCLUSIVE_FULLSCREEN` | KEEP — exact display modes. |
| `WindowSizeListener` | `onLogicalWindowSizeChanged`, `onFramebufferSizeChanged` | KEEP — callback domain and logical-vs-pixel distinction are explicit. |

## engine-render-opengl public naming review

| Public type | Public member vocabulary reviewed | T21 result |
| --- | --- | --- |
| `OpenGlRenderer` | `create` overloads; `render(RenderFramePacket)`; matrix/size `render` overload; `lastCullingCounters`, `lastDebugTextCounters`, `close` | KEEP — bounded OpenGL facade and latest-successful diagnostics are explicit. |
| `RenderCullingCounters` | `EMPTY`; record components/accessors `testedCandidates`, `visibleCandidates`, `culledCandidates`, `submittedDraws` | KEEP — exact CPU culling diagnostic vocabulary. |
| `RenderFramePacket` | constructors; `framebufferWidth`, `framebufferHeight`, `localLights`, `debugFrame`, `copyViewTo`, `copyProjectionTo` | KEEP — “Packet” is accepted renderer-submission vocabulary here and is explicitly documented as an immutable render snapshot, not a network packet. |
| `RenderLocalLight` | `positionX/Y/Z`, `red/green/blue`, `intensity`, `rangeMeters` | KEEP — shared renderer-facing local-light values and units are explicit. |
| `RenderPointLight` | record components/accessors `positionX/Y/Z`, `red/green/blue`, `intensity`, `rangeMeters` | KEEP — exact point-light submission vocabulary. |
| `RenderSpotLight` | record components/accessors `positionX/Y/Z`, `directionX/Y/Z`, `red/green/blue`, `intensity`, `rangeMeters`, `innerConeRadians`, `outerConeRadians` | KEEP — direction, units, and cone semantics are explicit and D-041-sensitive. |

## Cross-document and consumer consistency

Reviewed against:

- `docs/refactor/NAMING_STANDARD.md`;
- `docs/refactor/SYMBOL_INVENTORY.md`;
- `docs/refactor/BOUNDARY_AUDIT.md`;
- `docs/DECISIONS.md`;
- `docs/SPATIAL_CONVENTIONS.md`;
- `wiki/API_INDEX.md`;
- `wiki/LIMITATIONS.md`;
- relevant lifecycle/input/spatial/renderer wiki guidance;
- current sandbox usage of supported public APIs.

Repository search confirms current wiki/source consumers use the accepted post-T07 lifecycle names. Historical decision/status text may retain old names only when explicitly describing the pre-D-066 contract or rename provenance.

No source/wiki mismatch requiring a consumer-facing edit was found.

## Rename decisions deliberately rejected

- `RenderFramePacket` -> a “Submission”/“Snapshot” alternative: rejected because `Packet` is already accepted renderer-facing vocabulary, the class contract is clear, and a rename would create broad churn without correcting a semantic error.
- `EngineConfigSchema.TICK_RATE` -> a longer simulation-prefixed constant: rejected because the enclosing schema and underlying `simulation.tickRate` key already establish the domain, while the existing public constant is accepted and clear.
- `SubsystemGraph.Registration.dependencies` -> `dependencyIds`: rejected because the record Javadoc/type already states prerequisite IDs and the enclosing registration/graph context makes the meaning clear.
- geometry destination-copy query renames such as `minimum(...)`, `normal(...)`, or `worldMatrix(...)`: rejected because caller-owned destination style is already documented/JOML-conventional and the current domain nouns are unambiguous.
- adding responsibility suffixes to short domain types such as `Transform`, `Ray3f`, `WindowMode`, or `DebugFrame`: rejected by the naming standard's preference for concise established domain nouns.

## Verification and review record

- Fresh activation baseline: `8783cd441b1b126f2bc1858a6706adb3c7bdee66`.
- P5R-T20 is accepted; no open PR existed at activation.
- Supported public source roots were inspected directly.
- Public type/member names were reconciled against the naming standard, boundary audit, decisions, spatial contract, wiki API index/limitations, and current consumer/sandbox usage.
- No Java/Gradle/resource/workflow/wiki/sandbox change is required.
- Independent public-API change review: **not applicable** because T21 proposes no public API change. If a rename had been selected, the final renamed candidate would require independent review evidence under `AGENTS.md`.
- Final PR may use the Markdown-only CI exemption only if its complete file list remains non-empty and every changed path ends in `.md`.

Wiki impact: none — no supported public type/member name, signature, or consumer-visible usage changes.

Sandbox impact: none — no capability, command, control, or public API usage changes.


## Acceptance record

P5R-T21 was accepted through PR #347.

- Final audit head: `32886cb82e69c199ed6747630aa42b12ac55b125`.
- Merge commit: `d79f6d6c18490c839157770e51e9908eb2f7e13d`.
- Complete PR diff: 8 Markdown files only.
- CI policy: Markdown-only exemption applied; no heavy PR matrix or post-merge Lightweight verifier was required.
- Public API result: KEEP all current supported names.
- Independent public-API-change review: not applicable because no public API change was selected.
