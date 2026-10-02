# World components

P7-T05 adds five public data-oriented component values under `com.samo.engine.world.api`. They can be constructed before a public `World`, scene JSON parser, renderer extraction bridge, or runtime audio system exists.

## Construct components from data

```java
AssetId meshId = new AssetId(1L, 2L);
AssetId materialId = new AssetId(3L, 4L);
AssetId audioId = new AssetId(5L, 6L);

TransformComponent transform = new TransformComponent(
    1.0f, 2.0f, -3.0f,   // local position, meters
    0.0f, 0.0f, 0.0f, 1.0f, // quaternion XYZW
    1.0f, 1.0f, 1.0f);   // dimensionless scale
NameComponent name = new NameComponent("Camera Rig");
MeshRendererComponent mesh = new MeshRendererComponent(meshId, materialId);
CameraComponent camera = new CameraComponent((float) Math.toRadians(60.0), 0.1f, 250.0f);
AudioEmitterComponent audio = new AudioEmitterComponent(audioId);
```

No constructor above performs file I/O, resource loading, renderer/audio lookup, or native allocation.

## Transform component

`TransformComponent` creates and owns a fresh `engine-core Transform` from the supplied local values. Position uses the canonical D-041 meter-based right-handed world (+X right, +Y up, -Z forward). Quaternion input is validated and normalized by the existing `Transform`; scale is dimensionless and follows the existing transform contract.

Call `transform()` to access the component-owned mutable `Transform` for local changes or later hierarchy integration. Construction starts unparented. The component does not accept a caller-owned `Transform`, so it cannot accidentally alias a mutable transform owned elsewhere.

P7-T05 does not define entity/transform parenting from scene data. Stable GUIDs and scene parent resolution remain later Phase 7 work.

## Name component

`NameComponent` is immutable. Names must be non-null and nonblank. Accepted text is preserved exactly; leading/trailing whitespace is not silently trimmed.

## Mesh renderer component

`MeshRendererComponent` stores exactly two stable authored identities: `meshAssetId` and `materialAssetId`.

Those values are references only. The component does not call `AssetLoader`, retain `ResourceHandle`, validate an asset's runtime type, upload GPU resources, or submit a draw. Storing these AssetIds therefore does **not** mean arbitrary world mesh/material rendering is implemented yet.

## Camera component

`CameraComponent` stores only perspective values that are stable scene data:

- vertical FOV in radians, strictly between 0 and PI;
- positive near plane in meters;
- finite far plane greater than near.

Camera pose comes from the same entity's `TransformComponent`. Framebuffer aspect is deliberately not stored because it depends on the active runtime viewport/framebuffer. P7-T05 adds no active-camera registry, viewport policy, matrix cache, or world-to-renderer camera extraction.

## Audio emitter component

`AudioEmitterComponent` stores one stable audio `AssetId` only. It does not load/decode audio, create an OpenAL source, start playback, store gain/loop/attenuation settings, or own native state. Runtime audio loading/playback/spatialization remain later bounded tasks.

## What is still missing

These public component values are not a public ECS/world lifecycle by themselves. There is currently no public `World`, entity allocator, component store/query API, scene JSON/schema/parser, stable authoring GUID resolution, prefab lifecycle, world-to-renderer extraction, or runtime audio bridge.

P7-T07 is the planned owner of versioned scene JSON. Until those later boundaries exist, construct/use these values directly only where your own code already owns their lifecycle; do not invent a persistence or renderer/audio integration contract around them.
