# Prefab authoring format (P7-T09)

P7-T09 / Issue #432 defines an **internal, package-private** authored prefab document in `engine-world`. It is not yet a public editor or runtime World/prefab-loading API. The existing [scene JSON schema v1](SCENES.md) is embedded without altering its runtime component, GUID/parent, or optional `editorData` rules.

## Schema v1

```json
{
  "schemaVersion": 1,
  "scene": {
    "schemaVersion": 1,
    "entities": [
      {
        "guid": "00000000-0000-0000-0000-000000000001",
        "parentGuid": null,
        "components": {
          "name": {"schemaVersion": 1, "name": "Table"}
        }
      }
    ]
  },
  "instances": [
    {
      "instanceKey": "lamp",
      "prefabAssetId": "11111111-1111-1111-1111-111111111111",
      "parentGuid": "00000000-0000-0000-0000-000000000001",
      "overrides": [
        {
          "instancePath": [],
          "entityGuid": "00000000-0000-0000-0000-000000000002",
          "component": "name",
          "property": "name",
          "value": "Reading Lamp"
        }
      ]
    }
  ]
}
```

Root keys are exactly `schemaVersion`, `scene`, and `instances`. Version 1 is required. Each instance has exactly `instanceKey`, `prefabAssetId`, nullable `parentGuid`, and `overrides`. The instance key matches `[a-z][a-z0-9_-]*` and is unique among siblings; the prefab ID is canonical `AssetId` text. Non-null attachment `parentGuid` must name a local entity in the containing prefab's embedded scene.

## Property overrides

Each override is exactly `instancePath` (ordered array of child-instance keys, empty for the referenced prefab's own scene), `entityGuid` (source-local canonical GUID), `component`, `property`, and `value`. This targets a **single existing field on an existing component**, not a whole component or entity. Valid pairs:

| Component | Allowed individual properties |
| --- | --- |
| `transform` | `position`, `rotation`, `scale` (entire arrays; still validated using canonical D-041 transform rules) |
| `name` | `name` |
| `meshRenderer` | `meshAssetId`, `materialAssetId` |
| `camera` | `verticalFovRadians`, `nearPlaneMeters`, `farPlaneMeters` |
| `audioEmitter` | `audioAssetId` |

Component `schemaVersion`, root/entity `editorData`, GUID/parent identity, new/removed components and unknown runtime fields cannot be overridden. Duplicate target paths in one instance fail instead of silently adopting the last value. Strict duplicate JSON keys, unknown fields, unsupported schema versions and invalid values fail. The existing scene codec validates every resolved component after overrides are applied, so property values do not evade prior spatial or AssetId rules.

## Resolution and inheritance

The internal resolver accepts an explicit in-memory `AssetId -> PrefabDocument` snapshot and a root AssetId. It returns ordered, immutable **scene groups** identified by their full nested instance-key path, retaining each group's local GUIDs and authoring attachment metadata. This is **not** a single instantiated/activated world. An embedded scene may reference multiple independent instances of the same source prefab; paths distinguish them without inventing persistent runtime EntityIds.

Every resolution reads current source documents again. An explicit override stays fixed while all non-overridden properties inherit the current referenced source values. A nested child override is applied first, then an outer instance override aimed at the same nested target, so the enclosing authoring layer wins. Source documents are never mutated by resolution.

Missing references, malformed nested paths, invalid target entity/component/property/value, and cycles detected during traversal fail atomically (no partial result). The cycle guard is not the P7-T10 **pre-instantiation full reference-chain** diagnostic. Live GUID mapping, authored parent attachment, scene activation, renderer/physics/audio integration, cooked-prefab loading, and public API are deferred to later bounded tasks, particularly P7-T11.

Normative sources: D-083/D-084/D-085/D-086/D-087 and Issue #432. These are internal format notes, not a supported public engine API.
