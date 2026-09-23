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

For `MESH` metadata, P6-T07 writes deterministic cooked `SMES` schema v1 bytes instead of copying the source glTF/glb. For `TEXTURE` metadata, P6-T08 decodes supported PNG/JPEG sources offline and writes deterministic cooked `STEX` schema v1 bytes containing RGBA8 mip levels. For `AUDIO` metadata, P6-T09 validates Ogg Vorbis sources offline and writes deterministic cooked `SAUD` schema v1 bytes containing validated metadata plus the exact compressed Vorbis payload. Other asset types still use the P6-T03 opaque byte-for-byte source payload until their own bounded cooking tasks.

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

## Cooked MESH binary version 1

MESH `.bin` files use little-endian `SMES` schema v1.

The file begins with a 20-byte header:

- 4 raw bytes: ASCII `SMES`;
- int32 schema version: `1`;
- int32 positive mesh count;
- int32 exact body byte length;
- uint32 CRC32C bits for the complete body.

CRC32C is verified before mesh records are parsed.

The body is a sequence of mesh records with no padding. Every record starts with a 48-byte header containing mesh index, strict UTF-8 name length, vertex count, triangle-index count, attribute flags, exact vertex stride, and min/max XYZ AABB in already-converted D-041 engine space. It is followed by name bytes, interleaved vertices, and little-endian int32 indices.

Vertex order is fixed:

1. position XYZ;
2. optional normal XYZ;
3. optional tangent XYZS, where S is the P6-T06 handedness sign;
4. optional UV0 XY.

The decoder rejects bad magic/version/length/checksum, malformed UTF-8, unknown flags, invalid stride/counts, non-finite numeric data, invalid tangent signs, invalid triangle indices, duplicate mesh indices, AABB mismatch, truncation, and trailing bytes. No mesh value is returned when validation fails.

This format is package-internal today. There is still no public/runtime resource loader or GPU upload path; a later runtime path must validate cooked bytes before upload.

## Texture PNG/JPEG cooking

For metadata with `assetType: "TEXTURE"`, the paired source must use `.png`, `.jpg`, or `.jpeg` case-insensitively. The offline cooker decodes through LWJGL stb to tightly packed RGBA8 without vertical flipping, then frees the native decode buffer before later cooking continues.

P6-T08 builds a complete deterministic mip chain to 1x1. Each next width/height is `max(1, previous / 2)`; each RGBA channel is the unsigned integer floor-average of available in-bounds 2x2 source samples. This is a byte-space filter only. The source metadata schema currently has no color-space/usage field, so P6-T08 does not declare the texture to be sRGB or linear, does not gamma-correct mip generation, does not renormalize normal maps, and does not premultiply alpha.

Cooked texture files use little-endian `STEX` schema v1. The header contains magic `STEX`, schema version 1, fixed RGBA8 format, base width/height, complete mip count, exact body byte length, and CRC32C. The body stores one ordered record per mip with width, height, byte length, and tightly packed RGBA8 bytes. Package-private decode validates exact length/checksum and the complete expected mip sequence before returning cooked texture values.

There is still no public/runtime texture resource API or GPU upload path; source PNG/JPEG decoding is confined to the cooker.

## Audio Ogg Vorbis cooking

For metadata with `assetType: "AUDIO"`, the paired source must use `.ogg` case-insensitively and must be a valid Ogg Vorbis stream accepted by the scope-selected stb_vorbis implementation. Before output creation, the cooker opens the complete in-memory stream through stb_vorbis, reads channel/sample-rate metadata, drains interleaved samples to EOF, requires no terminal decoder error, closes the decoder and frees the temporary validation buffer, accepts only mono or stereo channel counts, requires a positive sample rate, and preserves the original compressed Vorbis bytes exactly.

Cooked audio files use little-endian `SAUD` schema v1. The header contains magic `SAUD`, schema version 1, Ogg Vorbis codec id 1, channel count, sample rate, exact compressed-payload byte length, and CRC32C. The body is the exact validated Ogg Vorbis source payload. Package-private `SAUD` decode validates metadata, exact length/checksum, truncation, and trailing data without invoking stb_vorbis or OpenAL.

P6-T09 does not persist PCM, transcode/resample, define streaming versus whole-clip runtime decode, add loop/loudness metadata, or create a public runtime audio/resource API.

## Dependency sidecars and graph

P6-T10 adds an optional adjacent `<source>.deps.json` only for MATERIAL, PREFAB, and SCENE assets. It is separate from the unchanged three-field `<source>.asset.json` schema v1.

The dependency sidecar contains exactly `schemaVersion`, `assetDependencies`, and `shaderDependencies`. Asset references use canonical existing `AssetId` text. MATERIAL asset dependencies may reference TEXTURE assets only and may also declare lowercase opaque logical shader keys; those shader keys are not paths, AssetIds, a SHADER asset type, or a runtime shader API. PREFAB and SCENE may reference existing AssetIds and must keep `shaderDependencies` empty.

The cooker rejects duplicate/self/missing/type-invalid references, cycles, orphan sidecars, and dependency sidecars attached to other asset types before output creation. A successful cook writes deterministic `dependencies.json` schema v1 with MATERIAL/PREFAB/SCENE owners sorted by AssetId and their dependency arrays sorted canonically. The graph can calculate reverse transitive dependents for a changed AssetId or logical shader key. P6-T10 does not update an existing cache; P6-T03's fresh-output rule remains unchanged.

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

The current Phase 6 cooker still does not perform:

- normal or UV generation;
- runtime audio decoding/playback;
- incremental cache mutation/recooking (dependency graph and invalidation closure now exist);
- runtime manifest/file loading;
- resource caches, reference counting, fallback resources, asynchronous loading/upload, or hot reload;
- renderer/world/editor integration.

P6-T11 now provides the public typed `ResourceHandle<T>` lifecycle boundary, but the cooker does not create runtime handles and no public runtime loader/factory exists yet. Those remain later bounded Phase 6 tasks.

See [Source metadata](SOURCE_METADATA.md) and [Asset identity](ASSET_ID.md) for the contracts consumed by the cooker.
