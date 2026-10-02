# Scene persistence format

P7-T07 defines the first persisted Sherko Engine scene document: strict UTF-8 JSON schema version 1. This page documents the accepted file contract. The production codec/document implementation is currently package-private inside `engine-world`; there is not yet a public `World` or public scene activation/loading API.

## Root document

```json
{
  "schemaVersion": 1,
  "entities": []
}
```

The root contains exactly `schemaVersion` and `entities`. Version must be integer `1`; other versions fail with an upgrade-required diagnostic. Entity array order is preserved across load/save and is authoring/document order, not runtime identity. Duplicate JSON fields are rejected.

## Entity identity and hierarchy

Each entity contains exactly `guid`, `parentGuid`, and `components`:

```json
{
  "guid": "00000000-0000-0000-0000-000000000001",
  "parentGuid": null,
  "components": {}
}
```

`guid` and non-null `parentGuid` use canonical lowercase `EntityGuid` text. Runtime `EntityId(index,generation)` values are never persisted. Entity GUIDs must be unique; a non-null parent must exist in the same document; self-parenting and parent cycles fail before a scene document is returned. P7-T07 stores authored hierarchy identity only and does not activate `Transform.parent()` relationships.

## Versioned components

The `components` object may contain any subset of exactly five known names. Every present component carries its own `schemaVersion: 1`.

- `transform`: `position` XYZ meters, `rotation` quaternion XYZW, `scale` dimensionless XYZ. Values are local D-041 engine-space values with no axis/unit/handedness conversion. Rotation is canonicalized through the existing `Transform` semantics.
- `name`: one required nonblank string preserved exactly.
- `meshRenderer`: canonical `meshAssetId` and `materialAssetId` text only; no paths or runtime handles.
- `camera`: finite `verticalFovRadians`, positive `nearPlaneMeters`, and farther `farPlaneMeters` under D-045. Pose comes from transform and aspect remains runtime input.
- `audioEmitter`: canonical `audioAssetId` text only; no backend/native audio object.

Unknown root/entity/component-object fields, unknown component names, duplicate fields, missing/wrong schema versions, malformed shapes, non-finite values, malformed identities, and component-domain violations fail under the P7-T07 strict baseline.

## Save/load semantics

The writer emits deterministic schema-v1 JSON with fixed known-field ordering and canonical GUID/AssetId text. Hand-authored input does not need to be reproduced byte-for-byte. The contract is semantic: load -> save -> load preserves the scene document's meaning, including entity order, identity, parent references, component values, normalized transform rotation, and optional component absence.

## Current compatibility boundary

P7-T07 intentionally does not define the final unknown-data forward-compatibility policy. P7-T08 owns the distinction between unknown required components and unknown optional editor data, including preserve/warn behavior.

There is also still no public scene codec, file-path/atomic-save API, runtime world instantiation/activation, prefab expansion, renderer/audio/physics integration, editor workflow, or networking/replication identity. Those remain later bounded tasks.

Normative sources: D-041/D-045/D-085 in `docs/DECISIONS.md`, `docs/SPATIAL_CONVENTIONS.md`, and Issue #426.
