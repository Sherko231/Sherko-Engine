# Runtime asset loading

P6-T13 introduces asynchronous MESH loading from an existing cooker cache. P6-T14 adds asynchronous MATERIAL loading plus explicit development MATERIAL polling.

## Open a loader

Use the public factory with the cooked cache root that contains `manifest.json` and `assets/`:

```java
try (AssetLoader loader = AssetLoaders.open(cacheDirectory)) {
    ResourceHandle<MeshAsset> handle = loader.loadMesh(assetId);
}
```

Opening validates manifest schema version 1 before returning. The runtime manifest uses the unchanged cooker contract; P6-T13 does not change manifest or SMES persistence.

## Asynchronous mesh lifecycle

For a manifest-backed MESH:

1. `loadMesh(assetId)` returns a typed `ResourceHandle<MeshAsset>`;
2. the handle starts LOADING;
3. production loading reads the cooked file on a Java virtual worker thread;
4. the worker checks exact manifest byte size and validates/decodes SMES v1;
5. successful content completes READY with immutable Java-owned `MeshAsset`.

`MeshAsset` contains one or more primitives preserving cooked mesh index/name, positions, optional normals, optional tangent xyz/handedness, optional UV0, and triangle indices. Array access returns defensive copies.

Positions are already D-041 engine-space meters because P6-T05/P6-T07 performed and persisted the exactly-once conversion. Runtime loading does not convert them again.

SMES v1 is currently uncompressed. P6-T13 therefore performs asynchronous read plus CPU-side validation/decode, not a fabricated compression stage.

## Missing and invalid content

Structured load errors are drained separately:

```java
List<AssetLoadError> errors = loader.drainErrors();
```

Runtime loading uses:

- `MISSING_CONTENT` — unknown AssetId or manifest-backed cooked file missing; the handle becomes READY with the P6-T12 fallback cube;
- `READ_FAILED` — the manifest entry exists but a non-missing I/O failure prevents reading; the handle becomes FAILED;
- `INVALID_CONTENT` — byte-size mismatch or invalid/corrupt SMES; the handle becomes FAILED.

`drainErrors()` removes the returned errors and preserves occurrence order.

Requesting `loadMesh` for an identity whose manifest type is not MESH is a synchronous `IllegalArgumentException`; it is not converted into a fallback.

## Material loading and development reload

`loadMaterial(assetId)` returns `ResourceHandle<MaterialAsset>` for manifest-backed MATERIAL content. MATERIAL bytes use strict UTF-8 JSON schema v1 with exactly `schemaVersion`, `shaderKey`, and four RGBA multipliers in `[0,1]`. The shader key is the existing D-076 lowercase logical key; it is not a path, AssetId, or SHADER asset type.

Initial MATERIAL loading follows the same worker boundary as MESH. Missing identities/files yield the deterministic opaque-magenta fallback, non-missing read failures fail the handle, and invalid schema/content fails the handle.

Development reload is opt-in and caller-driven:

```java
loader.pollDevelopmentReloads();
```

The loader compares actual cooked bytes for tracked READY MATERIAL handles. A valid change replaces the value on the same READY handle while preserving its AssetId. An invalid, missing, or unreadable changed candidate preserves the previous READY MaterialAsset and emits one `HOT_RELOAD_FAILED` diagnostic for that candidate. RELEASED handles remain terminal and are removed from reload tracking. No background file-watcher thread is created.

The OpenGL adapter has a package-private development shader/material reloader. It maps `shaderKey` to `<shaderKey>.vert` and `<shaderKey>.frag` under a caller-selected development shader root, compiles/links a complete candidate on the D-049 owner thread, and swaps only after success. Failed reads, compilation, or linking keep the previous valid program/material descriptor.

## Release and loader close

Closing a handle while its worker is still running makes RELEASED terminal. A late worker completion is discarded and cannot resurrect the handle.

Closing the loader stops new submissions. Existing handles retain their own state/lifetime. P6-T13 does not introduce resource caching or reference counting.

## GPU upload boundary

The public asset API remains backend-neutral and exposes no OpenGL/native buffer ID.

The OpenGL renderer contains an internal P6-T13 adapter that can consume a READY `ResourceHandle<MeshAsset>`. It asserts the existing `OpenGlThreadGuard` before any backend mutation, then creates/allocates/uploads owned vertex and index buffers on the render owner thread only.

Worker-side file read and SMES decode never call OpenGL. P6-T13 does not add a public arbitrary mesh draw/VAO/material submission API, so the uploaded buffer value remains renderer-internal.

## Current limits

P6-T13 does not implement:

- resource caches or reference counting;
- runtime texture, audio, scene, prefab, skeleton, or animation loading;
- a public GPU mesh/resource API;
- arbitrary mesh rendering/submission;
- world/ECS integration;
- OpenAL runtime loading/playback;
- hot reload for MESH, TEXTURE, AUDIO, SCENE, PREFAB, SKELETON, or ANIMATION; MATERIAL/shader reload is development-only polling.

Because no public arbitrary mesh submission path exists yet, the persistent sandbox does not demonstrate this capability without importing renderer internals.
