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

This document does not select an Euler-angle storage order or require Euler angles as a runtime representation. Later transform/camera tasks may use quaternions or matrices, but every representation must preserve the same right-handed world convention.

## Linear units

All engine/world linear quantities use SI-style meters unless a more specific contract explicitly says otherwise:

- position and displacement: meters;
- distances, radii, extents, and near/far values when later defined in world space: meters;
- linear velocity: meters per second;
- linear acceleration, when introduced: meters per second squared.

An external format or library that uses another unit must convert at its adapter/import/export boundary. A module must not silently reinterpret the engine's world unit.

## Angular units

Internal angular quantities use radians.

- angle: radians;
- angular velocity: radians per second;
- angular acceleration, if introduced later: radians per second squared.

Human-facing tools or configuration may eventually accept degrees if an executable task explicitly authorizes that UX, but conversion must occur at the boundary and internal spatial contracts remain radians.

## Scale

Transform scale is dimensionless.

`(1, 1, 1)` is identity scale. Scale does not change the definition of a meter and must not be used as a hidden unit-conversion mechanism between modules or external formats.

## Boundary conversion rule

External systems may use different coordinate or unit conventions. Conversion belongs at the boundary that owns that external system.

Examples of boundaries that may require explicit conversion later include:

- asset import/export such as glTF/Assimp;
- Jolt/Jolt-JNI physics adaptation;
- OpenAL spatial-audio adaptation;
- renderer view/projection construction;
- network transform encoding/quantization;
- editor/tooling display or human-entered units.

The canonical engine convention does not change to match an external library. Instead, the adapter converts incoming data into the canonical convention and converts outgoing data from the canonical convention when necessary.

## Separate coordinate domains

Do not confuse world space with unrelated coordinate domains.

The following are deliberately not defined by P4-T01 and are not implied by the world-space convention above:

- OpenGL clip-space or normalized-device-coordinate depth convention;
- reversed-Z policy;
- perspective FOV axis, near/far-plane policy, or projection-matrix helper choice;
- GLFW logical window screen coordinates;
- framebuffer pixel coordinates;
- screen-space origin or Y direction;
- texture/UV/image origin conventions;
- glTF authoring basis/conversion details;
- Jolt internal basis/conversion details;
- OpenAL internal spatial details;
- quaternion canonical-sign or serialization policy;
- network transform quantization or packet layout.

Those decisions belong to later executable tasks. When they are defined, their adapters and tests must cite this document and show any required conversion explicitly.

## Consumption rule for later phases

Any future task that defines or implements one of the following must treat this document as its world-space input contract:

- local/world transforms and hierarchy math;
- camera/view matrices and screen-to-world rays;
- renderer world-space data;
- physics positions, rotations, velocities, or queries;
- spatial audio positions/directions;
- asset conversion into cooked runtime data;
- world-space geometry primitives;
- transform replication/quantization.

Future renderer, physics, and asset-conversion tests must cite this document once those production paths exist. P4-T01 does not fabricate those implementations or tests merely to satisfy a forward-looking acceptance statement.

## Decision authority

D-041 in `docs/DECISIONS.md` records this convention as a durable architecture decision. `ENGINE_SCOPE.md` remains authoritative for product boundaries and technology choices; this document defines the canonical spatial semantics within that scope.
