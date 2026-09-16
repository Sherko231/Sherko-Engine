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

## Boundary conversion rule

External systems may use different coordinate or unit conventions. Conversion belongs at the boundary that owns that external system.

Examples of boundaries that may require explicit conversion later include:

- asset import/export such as glTF/Assimp;
- Jolt/Jolt-JNI physics adaptation;
- OpenAL spatial-audio adaptation;
- renderer camera/uniform upload;
- network transform encoding/quantization;
- editor/tooling display or human-entered units.

The canonical engine convention does not change to match an external library. Instead, the adapter converts incoming data into the canonical convention and converts outgoing data from the canonical convention when necessary.

## Separate coordinate domains

Do not confuse world/view space with unrelated coordinate domains.

The following remain deliberately undefined here and are not implied by D-041/D-045:

- GLFW logical-window screen coordinates;
- framebuffer pixel coordinates;
- screen-space origin or Y direction;
- pixel-center versus pixel-edge sampling convention;
- screen-to-NDC viewport mapping and world-ray origin policy;
- texture/UV/image origin conventions;
- glTF authoring basis/conversion details;
- Jolt internal basis/conversion details;
- OpenAL internal spatial details;
- quaternion canonical-sign or serialization policy;
- network transform quantization or packet layout.

P4-T07 owns the screen-coordinate/viewport/unprojection choices needed for screen-to-world rays. Later adapters and tests must cite this document and show any required conversion explicitly.

## Consumption rule for later phases

Any future task that defines or implements one of the following must treat this document as its spatial input contract:

- local/world transforms and hierarchy math;
- camera/view/projection matrices and screen-to-world rays;
- renderer world-space data;
- physics positions, rotations, velocities, or queries;
- spatial audio positions/directions;
- asset conversion into cooked runtime data;
- world-space geometry primitives;
- transform replication/quantization.

Future renderer, physics, and asset-conversion tests must cite the canonical conventions once those production paths exist. P4-T01/P4-T08 do not fabricate those implementations merely to satisfy forward-looking acceptance statements.

## Decision authority

D-041 records canonical world space and D-045 records the camera view/projection convention in `docs/DECISIONS.md`. `ENGINE_SCOPE.md` remains authoritative for product boundaries and technology choices; this document defines accepted spatial semantics within that scope.
