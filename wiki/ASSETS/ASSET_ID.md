# Asset identity

`AssetId` is the first supported public API in `engine-assets`.

## Contract

`AssetId` is an immutable 128-bit value made from two Java `long` components:

```java
AssetId id = new AssetId(highBits, lowBits);
```

Its value is deliberately independent of source-file location. A source path, metadata path, cooked-cache path, renderer handle, or world/entity reference is not part of the identity.

Use `AssetId.generate()` when authoring a new identity:

```java
AssetId id = AssetId.generate();
```

The canonical text format is lowercase UUID-style hexadecimal with the shape `8-4-4-4-12`:

```java
String text = id.toString();
AssetId restored = AssetId.parse(text);
```

Parsing preserves all 128 bits exactly. Null or noncanonical text is rejected. The all-zero value is valid; P6-T01 does not assign it sentinel meaning.

## File moves

References should persist the identity, not the source path. If authoring metadata moves together with a source file and keeps the same `AssetId`, existing references remain the same identity value.

P6-T02 now defines strict schema-v1 source metadata containing `schemaVersion`, canonical `assetId`, and `assetType`. The important boundary remains unchanged: identity itself is not derived from the current source path. See [Source metadata](SOURCE_METADATA.md).

## Not implemented yet

The current asset foundation still does not provide:

- a metadata writer or schema migration tooling;
- a public runtime manifest/file loader or resource cache;
- reference counting or native/GPU/audio destruction policy;
- a public runtime loader/factory that resolves real content;
- asynchronous loading/upload;
- renderer/world asset submission;
- file watching or hot reload.

P6-T11 provides the public typed `ResourceHandle<T>` lifecycle boundary, and P6-T12 adds deterministic internal missing-content fallbacks plus structured errors. No public loader/factory resolves real runtime assets yet.

Use [Current limitations](../LIMITATIONS.md) for the wider implementation status.
