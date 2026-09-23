# Runtime mesh loading

P6-T13 introduces the first supported runtime cooked-asset loader path: asynchronous MESH loading from an existing cooker cache.

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

P6-T13 uses:

- `MISSING_CONTENT` — unknown AssetId or manifest-backed cooked file missing; the handle becomes READY with the P6-T12 fallback cube;
- `READ_FAILED` — the manifest entry exists but a non-missing I/O failure prevents reading; the handle becomes FAILED;
- `INVALID_CONTENT` — byte-size mismatch or invalid/corrupt SMES; the handle becomes FAILED.

`drainErrors()` removes the returned errors and preserves occurrence order.

Requesting `loadMesh` for an identity whose manifest type is not MESH is a synchronous `IllegalArgumentException`; it is not converted into a fallback.

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
- runtime texture, audio, material, scene, prefab, skeleton, or animation loading;
- a public GPU mesh/resource API;
- arbitrary mesh rendering/submission;
- world/ECS integration;
- OpenAL runtime loading/playback;
- hot reload.

Because no public arbitrary mesh submission path exists yet, the persistent sandbox does not demonstrate this capability without importing renderer internals.
