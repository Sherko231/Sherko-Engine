# Asset cooker

P6-T03 provides the first offline command-line cooker foundation.

The cooker is tooling under `engine-assets`; it is not a supported runtime/library API.

## Run it

On Windows:

```powershell
.\gradlew.bat :engine-assets:runAssetCooker --args="<input-directory> <output-cache>"
```

Exactly two positional arguments are required.

The input must be an existing readable directory. The output path must not already exist and must not overlap the input tree.

## Source layout

Metadata is adjacent to its source file:

```text
content/
  models/
    crate.glb
    crate.glb.asset.json
```

The cooker recursively discovers regular files ending in `.asset.json` without following symbolic links. Removing the suffix identifies the paired source path.

The sidecar itself uses the strict P6-T02 schema:

```json
{
  "schemaVersion": 1,
  "assetId": "01234567-89ab-cdef-fedc-ba9876543210",
  "assetType": "MESH"
}
```

Unpaired ordinary source files are ignored. A discovered sidecar with no readable paired source is an error.

## Output layout

A successful clean cook creates:

```text
<output-cache>/
  manifest.json
  assets/
    01234567-89ab-cdef-fedc-ba9876543210.bin
```

P6-T03 derives cooked filenames from `AssetId`, not from source path.

Every current `.bin` payload is an opaque byte-for-byte copy of the source and must be nonzero. This is temporary cooker-foundation behavior, not the final format-specific cooked representation.

## Manifest version 1

`manifest.json` is deterministic UTF-8 JSON:

```json
{
  "schemaVersion": 1,
  "assets": [
    {
      "assetId": "01234567-89ab-cdef-fedc-ba9876543210",
      "assetType": "MESH",
      "sourcePath": "models/crate.glb",
      "cookedPath": "assets/01234567-89ab-cdef-fedc-ba9876543210.bin",
      "byteSize": 123
    }
  ]
}
```

Entries are sorted by canonical `AssetId` text. Source and cooked paths use `/` separators and are relative to their respective roots.

`sourcePath` exists for tooling/diagnostics. Runtime identity remains `AssetId`.

## Failure behavior

Cooking fails before successful output when any discovered asset has invalid metadata, a missing source, zero-byte source, duplicate `AssetId`, or unsupported metadata version.

The cooker also rejects:

- wrong CLI argument count;
- missing/non-directory input;
- input/output overlap;
- a pre-existing output path;
- an input tree with no metadata sidecars;
- metadata/source symbolic links.

A pre-existing output path is never deleted or overwritten. If a failure occurs after the cooker creates its new output tree, it attempts to remove that partial tree before propagating the failure.

## Not implemented yet

P6-T03 does not perform:

- glTF/Assimp import;
- coordinate/unit conversion;
- tangent generation or UV validation;
- final mesh binary schema, magic, bounds, or checksum;
- PNG/JPEG or audio decoding;
- dependency graph/incremental invalidation;
- runtime manifest loading;
- resource handles, caches, reference counting, or hot reload;
- renderer/world/editor integration.

Those remain later bounded Phase 6 tasks.

See [Source metadata](SOURCE_METADATA.md) and [Asset identity](ASSET_ID.md) for the contracts consumed by the cooker.
