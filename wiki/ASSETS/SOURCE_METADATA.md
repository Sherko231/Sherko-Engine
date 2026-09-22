# Source asset metadata

P6-T02 defines the first persisted authoring metadata format in `engine-assets`.

## Public API

Use `SourceAssetMetadata.load(Path)` when the exact metadata-file path is already known:

```java
SourceAssetMetadata metadata = SourceAssetMetadata.load(metadataPath);

AssetId id = metadata.assetId();
AssetType type = metadata.assetType();
```

`SourceAssetMetadata.CURRENT_SCHEMA_VERSION` is currently `1`.

The accepted source categories are:

- `MESH`
- `TEXTURE`
- `MATERIAL`
- `SKELETON`
- `ANIMATION`
- `AUDIO`
- `PREFAB`
- `SCENE`

## Schema version 1

The UTF-8 JSON document contains exactly three required root fields:

```json
{
  "schemaVersion": 1,
  "assetId": "01234567-89ab-cdef-fedc-ba9876543210",
  "assetType": "MESH"
}
```

Field order does not matter.

`assetId` must use the canonical lowercase P6-T01 `AssetId` text form. Identity is therefore not derived from the source-file path.

## Failure behavior

`SourceAssetMetadata.load(Path)` returns one complete valid value or throws; it never returns partial metadata.

`SourceAssetMetadataLoadException` reports:

- missing, non-regular, or unreadable metadata files;
- malformed JSON;
- duplicate or unknown JSON fields;
- missing required fields;
- wrong JSON value types;
- noncanonical `assetId` text;
- unknown `assetType` values;
- unsupported schema versions.

Unsupported versions include the phrase `upgrade required` in the error message. P6-T02 performs no migration or fallback.

Passing a null `Path` is a programmer-contract error and throws `NullPointerException`.

Jackson is an internal parsing dependency and does not appear in public signatures.

## Direct construction

A caller that already has validated values may construct:

```java
SourceAssetMetadata metadata =
    new SourceAssetMetadata(
        SourceAssetMetadata.CURRENT_SCHEMA_VERSION,
        assetId,
        AssetType.MESH);
```

Direct construction rejects unsupported schema versions and null identity/type values.

## Not implemented yet

P6-T02 does not define:

- how metadata files are named or discovered beside source files;
- a metadata writer/save API;
- schema migration tooling;
- type-specific importer/cooker options;
- the P6-T03 command-line cooker;
- asset manifests or dependency graphs;
- cooked binary formats;
- runtime resource handles, caches, reference counting, or hot reload;
- renderer/world/editor integration.

See [Asset identity](ASSET_ID.md) for the P6-T01 identity contract and [Current limitations](../LIMITATIONS.md) for broader status.
