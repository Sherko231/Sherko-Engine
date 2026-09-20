# P5R-T25 Final Cleanup Audit

Baseline: `master` `4bba10629e73a7899e2aac04f381db501773afd3`.

Issue: #305 — final consistency, dead-code, stale-name, compatibility, documentation, and refactor-scaffolding cleanup for the completed P1-P5 / P5R-T01 through T24 codebase.

## Result

**No Java/Gradle/resource/wiki/sandbox cleanup is justified.**

The final repository-wide audit found no unintended live use of obsolete P5R names, no orphaned helper introduced by the refactor sequence, no authored-Java deprecation shim, and no compatibility alias that can be removed without contradicting an earlier accepted contract.

The correct T25 change is therefore documentation/evidence only. Creating source churn solely to make T25 modify code would violate the Phase 5R rule against unnecessary renames/refactors.

## Obsolete-name sweep

The following names were searched repository-wide because P5R explicitly renamed or removed them:

- `SubsystemStartup`
- `FatalTermination`
- `IndexedStaticMeshPipeline`
- `RendererMaterial`
- `MaterialStatePolicy`
- `LocalLightSelection`
- `CameraUniformBlock`
- `PerFrameUniformBlock`
- `PresentationMode`
- `CleanupFailures`
- `ViewModelProjection`
- `EngineDemoMain`
- `runEngineDemo`
- `SandboxControls.Action`
- `SandboxControls.Input`
- `IntegratedNativeSoakSpike`
- `broken.frag`

Current Java, tests, Gradle wiring, workflow selectors, wiki usage, and sandbox guidance use the accepted current vocabulary. Remaining old-name occurrences are historical provenance in decisions/status/refactor/backlog/build documentation that explicitly describe the rename/removal or preserved evidence identity.

Historical D-020 and D-029 references to `SubsystemStartup` and `FatalTermination` are intentionally retained because D-066 records the later rename and explicitly preserves those historical decision rows.

Historical references to `IntegratedNativeSoakSpike` and `broken.frag` are intentionally retained only where documents explain the accepted rename and evidence provenance.

## Refactor-scaffolding reference audit

Retained collaborators introduced or stabilized by P5R-T03 through T24 were checked for current source/test consumers. No orphaned helper was found.

Representative retained groups include:

- GLFW backend/input/cursor/window-mode/deferred-size collaborators behind `GlfwWindow`;
- input-binding loader/parser/validator collaborators;
- lifecycle coordinators;
- reference-scene fixture/frame planning/upload/draw/diagnostic collaborators;
- renderer material/light/uniform/presentation/cleanup/view-model helpers;
- sandbox application-loop/control/scene/diagnostic/framebuffer collaborators;
- renderer visual-demo application/loop/framebuffer/lighting/overlay collaborators;
- client/server version-report helpers;
- `IntegratedNativeEvidenceHarness`;
- `OpenGlBackendSet`.

Every retained production helper has a production consumer; test-only evidence surfaces remain intentional.

## Compatibility review

### Public API

P5R-T21 deliberately reviewed the supported public P1-P5 type/member vocabulary and selected KEEP for the current surface. T25 therefore does not remove convenience constructors/overloads or introduce source-incompatible cleanup without a separate explicit API-change Issue.

No authored production Java currently uses `@Deprecated` as a compatibility shim for a P5R rename.

### Build/evidence entry points

P5R-T20 deliberately preserves historical root Phase-0 Gradle task aliases and evidence identifiers. They remain compatibility/evidence entry points, not stale production API, and are not removed by T25.

### Sandbox

P5R-T17 already removed `EngineDemoMain` / `runEngineDemo`. The persistent sandbox uses `SandboxMain` / `runSandbox`; no stale compatibility entry point remains.

## Documentation and consumer guidance

README, roadmap, backlog, architecture/boundary/refactor records, build/verification guidance, wiki, and sandbox guidance were compared against current code and the accepted P5R sequence.

No wiki edit is required because T25 changes no supported public API, signature, ownership, lifecycle, configuration, failure semantics, or consumer usage.

No sandbox edit is required because T25 changes no engine capability or owner-facing behavior.

## Verification

Verification for this Markdown-only candidate consists of repository-wide static/reference searches plus complete-diff inspection.

Required T25 checks:

1. Search the obsolete identifier set above and classify every remaining occurrence as current-name containment or historical provenance.
2. Confirm no obsolete removed file/class/resource/task is present in the current tree.
3. Confirm retained P5R collaborators have current production/test consumers as appropriate.
4. Search authored Java for `@Deprecated` compatibility shims.
5. Confirm the candidate changes only `.md` files.
6. Confirm README, roadmap, backlog, development status, boundary audit, and symbol inventory agree that T25 is the current/final cleanup task and T26 is next.
7. Confirm P6-T01 remains blocked until P5R-T26 records PASS.

Because the complete candidate is Markdown-only, the AGENTS.md Markdown-only exemption applies: the heavy five-job PR matrix and post-merge Lightweight verifier are not required. Their absence is not a pass claim.

## Impact

Wiki impact: none — no supported API or consumer usage change.

Sandbox impact: none — no capability or owner-facing behavior change.

Architecture/decision impact: none — no module, dependency, ownership, spatial, persisted/config, protocol, or public-contract change.

P6-T01 remains blocked until P5R-T26 records PASS.
