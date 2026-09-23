# Fallback assets

P6-T12 defines deterministic engine-owned fallback content for missing MESH, TEXTURE, MATERIAL, and AUDIO requests.

The fallback policy is intentionally backend-neutral. It creates CPU-side values only and does not perform OpenGL upload, OpenAL playback, runtime file I/O, or asynchronous work.

## Structured missing-content error

`AssetLoadError` is the public immutable diagnostic paired with a fallback resolution.

It contains:

- the requested `AssetId`;
- the requested `AssetType`;
- an `AssetLoadErrorCode`;
- non-blank human-readable detail.

P6-T12 defines exactly one error code:

```text
MISSING_CONTENT
```

The error does not expose a native/backend handle, renderer/audio object, raw source path requirement, or owned Throwable.

## Handle behavior

The internal missing-content resolver preserves the requested AssetId.

For a supported fallback category it creates a new P6-T11 `ResourceHandle<T>`, completes that handle READY with deterministic fallback content, and returns the handle alongside one `MISSING_CONTENT` error.

Missing content itself therefore does not require a crash once a later runtime loader invokes this seam.

Each resolution has its own handle lifetime. Closing one fallback handle does not invalidate later fallback resolutions.

## Fallback mesh

The mesh fallback is one closed cube in canonical D-041 engine space:

- centered at the origin;
- one meter wide, high, and deep;
- corners at `±0.5 m`;
- valid triangle indices;
- no native/GPU allocation.

This CPU-side geometry is intended to be visually unmistakable once a later renderer-resource adapter uploads/submits it.

## Fallback texture

The texture fallback is a complete deterministic RGBA8 mip chain.

Base level: 2x2 opaque diagonal checker:

```text
magenta  black
black    magenta
```

where magenta is RGBA `255,0,255,255` and black is `0,0,0,255`.

The 1x1 mip is the P6-T08 integer floor-average:

```text
127,0,127,255
```

P6-T12 does not assign new sRGB-versus-linear meaning. D-074/P6-T08 color-space limitations remain unchanged.

## Fallback material

The material fallback is an adapter-neutral immutable opaque-magenta value:

```text
RGBA = (1.0, 0.0, 1.0, 1.0)
```

It contains no shader key, texture handle, native ID, blend/depth/cull state, or persisted material schema.

## Fallback sound

The sound fallback is deterministic runtime PCM data:

- mono;
- signed PCM16;
- 22050 Hz;
- 220 samples;
- bounded amplitude `±12000`;
- repeated positive and negative waveform crossings;
- non-silent.

It requires no stb or OpenAL call and is not a new persisted audio format. P6-T09 `SAUD` remains unchanged.

## Current integration limits

P6-T13 now invokes the P6-T12 MESH fallback from the public asynchronous loader when a requested identity or manifest-backed cooked file is missing. P6-T12/P6-T13 still do not provide:

- resource caching/reference counting;
- runtime texture/audio/material loading;
- a public renderer mesh submission/GPU-resource API;
- OpenAL decode/upload/playback;
- renderer/world asset submission;
- hot reload.

P6-T13 performs asynchronous runtime MESH read/SMES decode and provides an internal render-thread-only vertex/index buffer uploader. Later renderer/audio adapters remain responsible for actual arbitrary mesh presentation and sound playback.

Because those public integration paths do not exist yet, the persistent sandbox does not demonstrate P6-T12 without importing internal APIs.
