# Scene persistence format

P7-T07 defines the first persisted Sherko Engine scene document: UTF-8 JSON schema version 1. P7-T08 refines its unknown-data compatibility policy with an explicit optional editor-only metadata namespace. The production codec/document implementation remains package-private inside `engine-world`; there is not yet a public `World`, public scene activation/loading API, or public editor API.

## Root document

The runtime fields remain:

```json
{
  "schemaVersion": 1,
  "entities": []
}
```

The root may additionally contain optional `editorData`:

```json
{
  "schemaVersion": 1,
  "entities": [],
  "editorData": {
    "futureEditor": {"gridSnap": 0.25},
    "notes": ["authoring-only"]
  }
}
```

`schemaVersion` must be integer `1`. Entity array order is preserved across load/save and is authoring/document order, not runtime identity. Unknown root fields other than the reserved `editorData` field remain invalid. Duplicate JSON fields are rejected.

## Entity identity and hierarchy

Each entity retains the required runtime fields `guid`, `parentGuid`, and `components`, with optional `editorData`:

```json
{
  "guid": "00000000-0000-0000-0000-000000000001",
  "parentGuid": null,
  "components": {},
  "editorData": {
    "futureInspector": {"collapsed": true}
  }
}
```

`guid` and non-null `parentGuid` use canonical lowercase `EntityGuid` text. Runtime `EntityId(index,generation)` values are never persisted. Entity GUIDs must be unique; a non-null parent must exist in the same document; self-parenting and parent cycles fail before a scene document is returned. Scene persistence stores authored hierarchy identity only and does not activate `Transform.parent()` relationships.

Unknown entity fields other than `editorData` remain invalid.

## Versioned runtime components

The `components` object may contain any subset of exactly five known runtime component names. Every present component carries its own `schemaVersion: 1`.

- `transform`: `position` XYZ meters, `rotation` quaternion XYZW, `scale` dimensionless XYZ. Values are local D-041 engine-space values with no axis/unit/handedness conversion. Rotation is canonicalized through the existing `Transform` semantics.
- `name`: one required nonblank string preserved exactly.
- `meshRenderer`: canonical `meshAssetId` and `materialAssetId` text only; no paths or runtime handles.
- `camera`: finite `verticalFovRadians`, positive `nearPlaneMeters`, and farther `farPlaneMeters` under D-045. Pose comes from transform and aspect remains runtime input.
- `audioEmitter`: canonical `audioAssetId` text only; no backend/native audio object.

An unknown key directly under `components` is treated as an **unknown required runtime component** and the scene fails to load. It is not preserved or ignored. Known component objects remain strict: unknown fields and unsupported schema versions fail.

## Optional editor data

`editorData` is optional at the root and entity levels only. When present, it must be a JSON object.

Its nested keys and values are opaque editor-only metadata. Objects, arrays, strings, numbers, booleans, and null values may appear inside it. The runtime scene codec does not interpret those values as components, assets, entity references, spatial data, or lifecycle instructions.

The codec preserves `editorData` semantically through load -> save -> load. Original whitespace is not preserved. Presence is preserved too: an explicit empty `editorData: {}` remains present rather than becoming absent.

Duplicate fields inside `editorData` are rejected by the same strict JSON parser used for the rest of the scene.

## Save/load semantics

The writer emits deterministic schema-v1 JSON for known runtime fields with canonical GUID/AssetId text. Hand-authored input does not need to be reproduced byte-for-byte. The runtime contract is semantic: load -> save -> load preserves entity order, identity, parent references, known component values, normalized transform rotation, optional component absence, and any accepted opaque editor data.

## Current compatibility boundary

P7-T08 deliberately permits unknown content only inside the explicit `editorData` namespace. It does **not** establish a general "ignore unknown JSON" rule. Unknown required runtime components and unknown fields outside the reserved namespace remain hard failures.

P7-T09 introduces a separate **package-private** prefab JSON composition format using this unchanged scene schema as an embedded value; see [Prefab authoring format](PREFABS.md). There is still no public scene/prefab codec, file-path/atomic-save API, live world/prefab instantiation or activation, renderer/audio/physics integration, editor workflow/API, or networking/replication identity. Those remain later bounded tasks.

Normative sources: D-041/D-045/D-085/D-086 in `docs/DECISIONS.md`, `docs/SPATIAL_CONVENTIONS.md`, Issue #426, and Issue #429.
