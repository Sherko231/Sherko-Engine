# Spatial primitives

`engine-core` exposes immutable world-space geometry primitives under `com.samo.engine.core.api`:

- `Ray3f`
- `Plane3f`
- `Sphere3f`
- `Aabb3f`
- `Frustum3f`

They follow the canonical D-041 world convention: right-handed, +X right, +Y up, -Z forward, and meters for world-space distances.

## Ownership and numeric policy

Constructor inputs are copied. Mutable internal vectors are never exposed; getters copy into caller-provided JOML destinations.

All required numeric inputs must be finite. Ray directions and plane normals must have non-zero length and are normalized when stored. Sphere radius must be non-negative. AABB minimum must be <= maximum on every axis; zero radius/zero box extent are valid.

Contact is inclusive. Production queries use exact comparisons and do not add a hidden epsilon. If an application needs tolerance, it must express that tolerance explicitly.

## Ray queries

```java
Ray3f ray = new Ray3f(
        new Vector3f(0.0f, 0.0f, 0.0f),
        new Vector3f(0.0f, 0.0f, -1.0f));

float sphereHit = ray.intersectSphere(
        new Sphere3f(new Vector3f(0.0f, 0.0f, -5.0f), 1.0f));
```

For valid primitives, ray intersections return the nearest non-negative parameter `t`. `0` means the ray origin is already on/inside the target. `Float.NaN` means there is no forward hit.

`Ray3f.pointAt(t, destination)` accepts only finite `t >= 0`.

`ScreenRays.worldRay(...)` can now construct a `Ray3f` from screen/viewport coordinates plus view/projection matrices. That mapping follows D-046: top-left/Y-down screen space, same-domain sample/viewport coordinates, D-045 clip depths, inverse `projection * view`, and a near-plane ray origin. See [Spatial conventions](SPATIAL_CONVENTIONS.md) for the exact mapping.

## Planes

A `Plane3f` uses:

```text
normal dot point + offset = 0
```

The stored normal is normalized. Positive signed distance is the normal-facing side; negative distance is the opposite side; zero is contact.

```java
Plane3f ground = Plane3f.fromPointNormal(
        new Vector3f(0.0f, 0.0f, 0.0f),
        new Vector3f(0.0f, 1.0f, 0.0f));
```

## Sphere and AABB

Point containment and shape intersection are boundary-inclusive.

```java
Sphere3f sphere = new Sphere3f(new Vector3f(0.0f, 0.0f, -5.0f), 1.0f);
Aabb3f box = new Aabb3f(
        new Vector3f(-1.0f, -1.0f, -6.0f),
        new Vector3f(1.0f, 1.0f, -4.0f));

boolean overlap = sphere.intersects(box);
```

## Frustum

`Frustum3f` is constructed from six already-normalized `Plane3f` values whose normals face inward:

```java
Frustum3f frustum = new Frustum3f(left, right, bottom, top, near, far);
```

A point is inside/on the frustum when every plane reports `signedDistance >= 0`. Sphere and AABB contact with a plane counts as intersection.

`Frustum3f` still does not extract planes from view/projection matrices; matrix-to-frustum extraction remains separate future work. D-045 now defines camera projection and D-046 defines screen-to-world rays without changing the direct-plane frustum contract.
