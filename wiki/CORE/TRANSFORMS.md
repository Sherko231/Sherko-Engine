# Transforms

`com.samo.engine.core.api.Transform` is the engine's mutable local/world transform API.

It uses the canonical spatial convention from `docs/SPATIAL_CONVENTIONS.md`:

- right-handed world;
- +X right;
- +Y up;
- -Z forward;
- positions/distances in meters;
- internal angles in radians;
- positive rotation by the right-hand rule;
- scale is dimensionless.

## JOML is part of the public spatial API

Beginning with P4-T03, `engine-core` exposes JOML math types through its public spatial API. Consumers can therefore use JOML `Vector3f`, `Quaternionf`, `Vector3fc`, `Quaternionfc`, and `Matrix4f` directly with `Transform`.

The engine does not add duplicate wrapper vector/quaternion/matrix types.

## Local transform

A new transform starts with:

```text
position = (0, 0, 0)
rotation = identity quaternion
scale    = (1, 1, 1)
parent   = null
```

Local composition is:

```text
T * R * S
```

World composition is:

```text
parentWorld * local
```

or just `local` when there is no parent.

## Basic use

```java
Transform parent = new Transform();
parent.setLocalPosition(3.0f, 0.0f, 0.0f);
parent.setLocalRotation(new Quaternionf().rotationY((float) (Math.PI / 2.0)));
parent.setLocalScale(2.0f, 1.0f, 1.0f);

Transform child = new Transform();
child.setLocalPosition(0.0f, 0.0f, -1.0f);
child.setParent(parent);

Matrix4f world = child.worldMatrix(new Matrix4f());
```

`worldMatrix(...)` copies the cached world matrix into the destination supplied by the caller. The internal matrix is never exposed directly.

The local getters work the same way:

```java
Vector3f position = transform.localPosition(new Vector3f());
Quaternionf rotation = transform.localRotation(new Quaternionf());
Vector3f scale = transform.localScale(new Vector3f());
```

The returned objects are caller-owned copies. Mutating them does not mutate the `Transform`.

Likewise, setters that accept JOML values copy the input immediately; the transform never retains the caller's vector/quaternion object.

## Rotation validation

Rotation input must contain finite components and have non-zero length. Accepted quaternions are normalized when copied into the transform.

An all-zero quaternion or non-finite rotation fails with `IllegalArgumentException` without partially changing the stored rotation.

## Scale

Scale components must be finite.

Zero and negative scale are allowed for forward transform composition. P4-T03 does not define inverse transforms or world/local decomposition, so consumers must not assume invertibility.

## Parenting and caching

`setParent(...)` changes hierarchy identity but preserves local position, rotation, and scale. `setParent(null)` detaches the transform.

World matrices are cached. Local changes mark that transform dirty. A child also tracks the revision of the parent's cached world matrix, so a later child read observes parent changes even before descendant dirty propagation exists.

No child collection or descendant walk is part of P4-T03.

## Current hierarchy limitation

General parent-cycle rejection is not implemented until P4-T04. Do not build cyclic parent graphs, including indirect cycles. Cyclic hierarchy behavior is unsupported in P4-T03.

P4-T05 separately owns explicit descendant dirty propagation. The current revision-based lazy validation keeps reads correct without implementing that future optimization early.

## Threading

`Transform` is mutable and externally serialized. It does not provide internal locking or concurrent mutation/read guarantees.
