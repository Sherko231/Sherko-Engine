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

Every current `.bin` payload is still an opaque byte-for-byte copy of the source and must be nonzero. For `MESH` metadata, P6-T04 now first imports `.gltf` / `.glb` through LWJGL Assimp during validation, but the persisted payload remains the original source bytes until P6-T07 defines the final mesh schema.

## Mesh glTF validation/import

For metadata with `assetType: "MESH"`, the paired source must use a `.gltf` or `.glb` extension.

Before creating the output cache, the cooker imports every Assimp mesh and copies these values into Java-owned internal data:

- positions as XYZ;
- normals as XYZ when authored;
- tangent XYZ when authored or generated;
- one tangent-handedness sign per tangent vertex;
- UV channel 0 as XY when authored;
- triangle indices in Assimp face order.

P6-T06 enables exactly one Assimp post-process flag: `aiProcess_CalcTangentSpace`. Assimp's glTF importer still compacts/remaps indexed vertices by first use and converts glTF UV V to its lower-left convention; the cooker accepts those format-intrinsic decode semantics. It does not add a second UV flip, flip winding, change handedness, pre-transform nodes, generate normals/UVs, optimize meshes, or perform spatial coordinate/unit conversion inside Assimp.

The imported values first exist in Assimp import basis. P6-T05 then converts every imported mesh exactly once into D-041 engine space before output-cache creation:

```text
engineX = -importX
engineY =  importY
engineZ = -importZ
```

Positions, normals, and tangent xyz use that mapping with scale factor 1.0. Because the mapping is a 180-degree +Y rotation with determinant +1, winding, triangle indices, and tangent handedness signs remain unchanged. UV0 remains exactly the accepted Assimp value. Distinct internal `ImportedMesh` and `EngineMesh` values prevent accidental repeat conversion.

For meshes whose referenced glTF material contains a tangent-space normal map, UV0 and authored normals are required. Normal maps selecting another UV channel are rejected in this slice. Missing required UV0/normals or missing tangent-generation output fails before output creation with source-path and mesh-name context. Meshes without tangent-space normal mapping are not forced to provide UV0. Non-triangle faces and out-of-range indices still fail import. Assimp scene memory is released before import returns; native pointers do not escape into later cooker/runtime code.

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

The current P6-T06 cooker still does not perform:

- normal or UV generation;
- final mesh binary schema, magic, bounds, or checksum;
- PNG/JPEG or audio decoding;
- dependency graph/incremental invalidation;
- runtime manifest loading;
- resource handles, caches, reference counting, or hot reload;
- renderer/world/editor integration.

Those remain later bounded Phase 6 tasks.

See [Source metadata](SOURCE_METADATA.md) and [Asset identity](ASSET_ID.md) for the contracts consumed by the cooker.
