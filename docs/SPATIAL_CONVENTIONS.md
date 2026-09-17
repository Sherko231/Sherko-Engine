# Sherko Engine Spatial Conventions

This document is the canonical spatial convention for Sherko Engine. It defines engine/world-space assumptions that later transform, camera, renderer, physics, audio, asset-conversion, and networking code must preserve or convert to explicitly at module boundaries.

## Canonical world basis

Sherko Engine world space is a right-handed Cartesian coordinate system.

| Axis / quantity | Canonical meaning |
| --- | --- |
| `+X` | Right |
| `-X` | Left |
| `+Y` | Up |
| `-Y` | Down |
| `-Z` | Forward |
| `+Z` | Backward |
| Position / distance | Meters |
| Linear velocity | Meters per second |
| Angle | Radians internally |
| Angular velocity | Radians per second |
| Transform scale | Dimensionless; `(1, 1, 1)` is identity |

The engine basis directions are therefore:

```text
RIGHT   = (+1,  0,  0)
UP      = ( 0, +1,  0)
FORWARD = ( 0,  0, -1)
```

Example: world position `(0, 0, -2)` is two meters forward from the origin. Position `(3, 1, 0)` is three meters right and one meter up from the origin.

## Handedness and positive rotation

The world basis is right-handed. Positive rotation follows the right-hand rule around the positive axis.

For any axis, point the right-hand thumb along the positive axis; curled fingers indicate the positive rotation direction around that axis.

This document does not select an Euler-angle storage order or require Euler angles as a runtime representation. Transform/camera tasks may use quaternions or matrices, but every representation must preserve the same right-handed world convention.

## Linear units

All engine/world linear quantities use SI-style meters unless a more specific contract explicitly says otherwise:

- position and displacement: meters;
- distances, radii, extents, and camera near/far planes: meters;
- linear velocity: meters per second;
- linear acceleration, when introduced: meters per second squared.

An external format or library that uses another unit must convert at its adapter/import/export boundary. A module must not silently reinterpret the engine's world unit.

## Angular units

Internal angular quantities use radians.

- angle: radians;
- perspective vertical field of view: radians;
- angular velocity: radians per second;
- angular acceleration, if introduced later: radians per second squared.

Human-facing tools or configuration may eventually accept degrees if an executable task explicitly authorizes that UX, but conversion must occur at the boundary and internal spatial contracts remain radians.

## Scale

Transform scale is dimensionless.

`(1, 1, 1)` is identity scale. Scale does not change the definition of a meter and must not be used as a hidden unit-conversion mechanism between modules or external formats.

## Camera view and perspective projection

D-045 / P4-T08 fixes the engine camera-matrix convention used by `CameraMatrices` and later screen-to-world work:

- view space is right-handed;
- supplied camera forward maps to view-space `-Z`;
- camera right maps to `+X` and corrected camera up maps to `+Y`;
- perspective FOV is vertical and expressed in radians;
- aspect ratio is `width / height` and is positive;
- near and far distances are meters with `near > 0` and `far > near`;
- perspective normalized-device-coordinate depth is the OpenGL convention `[-1,+1]`;
- a view-space point on the near plane maps to NDC `z=-1` and a point on the far plane maps to NDC `z=+1`;
- projection is conventional finite-depth and does not use reversed-Z.

This convention is renderer-independent math inside `engine-core`, while intentionally matching the selected OpenGL 4.6 renderer baseline. It does not authorize OpenGL calls or renderer dependencies in core.

## Screen coordinates and screen-to-world rays

D-046 / P4-T07 fixes the public screen-to-world mapping consumed by `ScreenRays`:

- viewport/screen origin is top-left;
- screen X increases right and screen Y increases down;
- screen sample coordinates are continuous and must use the same coordinate domain and units as the supplied viewport rectangle;
- when that domain is raster pixels, whole-number coordinates are pixel edges and a pixel center is at `index + 0.5`;
- viewport mapping is `ndcX = 2 * (screenX - viewportX) / viewportWidth - 1` and `ndcY = 1 - 2 * (screenY - viewportY) / viewportHeight`;
- samples on the closed viewport boundary are valid and map to NDC `±1`; samples outside are invalid;
- near/far clip depths come directly from D-045: NDC `z=-1` and `z=+1`;
- world unprojection uses the inverse of `projection * view` with homogeneous division;
- a screen ray begins at the unprojected near-plane sample and points toward the corresponding unprojected far-plane sample;
- the resulting `Ray3f` direction is normalized.

`ScreenRays` does not choose GLFW logical coordinates versus framebuffer pixels and performs no implicit conversion between them. A caller may use either domain only if both the screen sample and viewport rectangle are expressed in that same domain.

## Transform value quantization

D-047 / P4-T09 fixes reusable value-level quantization semantics consumed by `TransformQuantization` without defining a network packet or replication policy.

Position quantization:

- input position components are canonical engine-space meters;
- each axis is quantized independently with step `1/64 m` into one signed 16-bit code;
- valid range is exactly `[-512.0, 511.984375] m` per axis;
- maximum accepted round-trip absolute error is `1/128 m` (`7.8125 mm`) per axis;
- out-of-range or non-finite values fail; there is no saturation/clamping.

Quaternion quantization:

- finite non-zero inputs are normalized before encoding;
- the largest-absolute quaternion component is omitted; equal magnitudes choose the lowest component index;
- because `q` and `-q` represent the same orientation, the sign is canonicalized so the omitted component is non-negative and both signs encode identically;
- the remaining three components retain original component order and map to signed codes `[-32767,+32767]` over `[-1/sqrt(2),+1/sqrt(2)]`;
- code `-32768` is reserved invalid for stored quaternion components;
- decode reconstructs the omitted component as the positive square root, rejects malformed triplets whose stored squared magnitude leaves no positive omitted component, and normalizes the result;
- valid encode/decode round trips have angular orientation error bounded by `0.0002 rad`.

These semantics are value-level only. The caller still decides whether the values represent local, world, or origin-relative transforms under a future replication/storage contract. P4-T09 defines no byte order, packet field order, protocol version, entity/tick IDs, delta compression, transport, authority, or scale quantization.

## Boundary conversion rule

External systems may use different coordinate or unit conventions. Conversion belongs at the boundary that owns that external system.

Examples of boundaries that may require explicit conversion later include:

- asset import/export such as glTF/Assimp;
- Jolt/Jolt-JNI physics adaptation;
- OpenAL spatial-audio adaptation;
- renderer camera/uniform upload;
- network transform encoding/packet serialization around D-047 values;
- editor/tooling display or human-entered units.

The canonical engine convention does not change to match an external library. Instead, the adapter converts incoming data into the canonical convention and converts outgoing data from the canonical convention when necessary.

## Separate coordinate domains

Do not confuse world/view space with unrelated coordinate domains.

The following remain deliberately undefined here and are not implied by D-041/D-045/D-046/D-047:

- automatic conversion between GLFW logical-window coordinates and framebuffer pixels;
- texture/UV/image origin conventions;
- glTF authoring basis/conversion details;
- Jolt internal basis/conversion details;
- OpenAL internal spatial details;
- Euler-angle storage/order policy;
- production network transform packet layout, origin scheme, authority, delta/compression, or protocol versioning.

Later adapters and tests must cite this document and show any required conversion explicitly.

## Consumption rule for later phases

Any future task that defines or implements one of the following must treat this document as its spatial input contract:

- local/world transforms and hierarchy math;
- camera/view/projection matrices and screen-to-world rays;
- renderer world-space data;
- physics positions, rotations, velocities, or queries;
- spatial audio positions/directions;
- asset conversion into cooked runtime data;
- world-space geometry primitives;
- transform replication/quantization and any packet/adaptor wrapping D-047 values.

Future renderer, physics, and asset-conversion tests must cite the canonical conventions once those production paths exist. P4-T01/P4-T07/P4-T08/P4-T09 do not fabricate those implementations merely to satisfy forward-looking acceptance statements.

## Decision authority

D-041 records canonical world space, D-045 records the camera view/projection convention, D-046 records the screen-to-world mapping, and D-047 records transform value quantization in `docs/DECISIONS.md`. `ENGINE_SCOPE.md` remains authoritative for product boundaries and technology choices; this document defines accepted spatial semantics within that scope.
