# Spatial conventions

Sherko Engine uses one canonical spatial convention. The normative architecture contract is [`../../docs/SPATIAL_CONVENTIONS.md`](../../docs/SPATIAL_CONVENTIONS.md); this page is a consumer-oriented summary and does not define a separate convention.

## World basis

- right-handed Cartesian world;
- `+X` points right;
- `+Y` points up;
- `-Z` points forward;
- `+Z` points backward.

So `(0, 0, -2)` means two meters forward from the world origin.

## Units

- position and distance: meters;
- camera near/far distances: meters;
- linear velocity: meters per second;
- internal angles and perspective FOV: radians;
- angular velocity: radians per second;
- transform scale: dimensionless, with `(1, 1, 1)` as identity.

Positive rotation follows the right-hand rule around the positive axis.

## Camera view and projection

`CameraMatrices.view(...)` constructs a right-handed world-to-view matrix where the supplied camera forward direction becomes view-space `-Z`, corrected up becomes `+Y`, and right becomes `+X`.

`CameraMatrices.perspective(...)` uses:

- vertical FOV in radians;
- positive `aspectRatio = width / height`;
- `nearPlane > 0` and `farPlane > nearPlane`;
- OpenGL-style NDC depth `[-1,+1]`;
- near plane at NDC `z=-1` and far plane at `z=+1`;
- conventional finite projection, not reversed-Z.

Both APIs write into a caller-owned JOML `Matrix4f` destination after validating all inputs. `engine-core` does not depend on LWJGL/OpenGL to perform this math.

## External libraries and formats

Treat the engine convention as the stable side of every boundary. If a renderer, physics library, audio library, asset format, editor surface, or network representation uses different axes or units, its owning adapter converts explicitly when data enters or leaves engine world space.

Do not use transform scale as a hidden unit conversion.

## Still intentionally undefined

P4-T08 does not define:

- window/framebuffer screen origin or Y direction;
- pixel-center versus pixel-edge mapping;
- screen-to-NDC viewport mapping and screen-to-world ray origin policy;
- texture or UV origin;
- glTF/Jolt/OpenAL conversion details;
- Euler storage order or quaternion canonical sign;
- network transform quantization.

P4-T07 owns the screen-coordinate/unprojection choices required to build world rays.

## API status

Implemented public spatial APIs now include hierarchical `Transform`, immutable ray/plane/sphere/AABB/frustum primitives, and `CameraMatrices` view/perspective construction. Screen-to-world ray construction and network quantization remain later Phase 4 tasks.
