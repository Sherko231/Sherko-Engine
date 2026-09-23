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

P6-T11 is only the handle lifecycle boundary. It does not yet provide:

- a public loader or factory that creates real asset handles;
- runtime `manifest.json` / cooked-file loading;
- resource caches or cache-key policy;
- reference counting;
- fallback assets;
- structured failure payloads;
- asynchronous loading/decompression;
- render-thread GPU upload;
- OpenAL decode/upload/playback;
- renderer/world asset submission;
- hot reload.

In particular, `close()` means this handle view is released. It does not yet mean that a last reference destroyed a GPU/audio/native resource; later bounded tasks define cache/reference/native lifetime policy.

Because there is no public runtime loader yet, the persistent sandbox cannot meaningfully demonstrate P6-T11 without importing internals or implementing later Phase 6 work.
