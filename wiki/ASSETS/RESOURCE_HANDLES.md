# Resource handles

P6-T11 introduces the first public typed runtime resource-handle lifecycle boundary in `engine-assets`.

## Public types

`ResourceHandle<T>` is a typed Java view over one asset identity. It exposes:

- `assetId()` — the stable `AssetId` represented by the handle;
- `state()` — one of `LOADING`, `READY`, `FAILED`, or `RELEASED`;
- `readyValue()` — an `Optional<T>` containing the exact typed value only while READY;
- `requireReady()` — the typed value while READY, otherwise an `IllegalStateException` whose message includes the AssetId and current state;
- `close()` — idempotent local release inherited from `AutoCloseable`.

`ResourceHandleState` contains exactly:

```text
LOADING
READY
FAILED
RELEASED
```

## Lifecycle

A handle starts in LOADING. Only engine-internal code may complete it as READY or FAILED.

```text
LOADING ----> READY ----> RELEASED
   |                         ^
   +-------> FAILED ---------+
   |
   +-----------------------> RELEASED
```

READY and FAILED are mutually exclusive. RELEASED is terminal. Repeated `close()` calls are harmless.

Closing a READY handle clears its retained Java value. After release, `readyValue()` is empty and `requireReady()` rejects.

## Type and backend boundary

The generic type parameter is the only resource value exposed to consumers. The public handle does not expose:

- OpenGL/OpenAL object numbers;
- raw `int` / `long` native handles;
- LWJGL pointers or buffers;
- cache entries;
- the internal completion/controller object;
- public methods for LOADING -> READY or LOADING -> FAILED transitions.

This lets gameplay retain compile-time resource typing without depending on one backend or native ownership model.

## Current limits

P6-T11 defines the handle lifecycle boundary, P6-T12 adds deterministic fallback/error policy, and P6-T13 adds the first public asynchronous MESH loader. The public asset surface still does not yet provide:

- resource caches or cache-key policy;
- reference counting;
- runtime loaders for texture/audio/material/scene/prefab/skeleton/animation;
- a public renderer mesh submission/GPU-resource API;
- OpenAL decode/upload/playback;
- renderer/world asset submission;
- hot reload.

In particular, `close()` means this handle view is released. It does not yet mean that a last reference destroyed a GPU/audio/native resource; later bounded tasks define cache/reference/native lifetime policy.

P6-T13 `AssetLoader.loadMesh(...)` returns a LOADING `ResourceHandle<MeshAsset>` for manifest-backed meshes and completes it from worker-side read/SMES decode. Missing identities/files reuse the P6-T12 fallback and become READY with MISSING_CONTENT. Renderer GPU upload is internal and owner-thread-only; there is still no public arbitrary mesh submission path for the sandbox.
