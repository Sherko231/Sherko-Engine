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

## Screen-to-world rays

`ScreenRays.worldRay(...)` converts one continuous screen sample and viewport rectangle into an immutable world-space `Ray3f`.

The public mapping is:

- viewport origin is top-left;
- X increases right and Y increases down;
- screen sample and viewport must use the same coordinate domain/units;
- when using raster-pixel coordinates, whole numbers are pixel edges and a pixel center is at `index + 0.5`;
- samples on the closed viewport boundary are valid;
- screen X maps to NDC `[-1,+1]` left-to-right;
- screen Y maps to NDC `[+1,-1]` top-to-bottom;
- near/far clip depths are D-045's `-1/+1`;
- unprojection uses inverse `projection * view` plus homogeneous division;
- the ray origin is the unprojected near-plane point;
- ray direction points from the near point toward the corresponding far point and is normalized.

The API intentionally does not convert between GLFW logical coordinates and framebuffer pixels. Use either domain only when the sample and viewport rectangle are both expressed in that domain.

## External libraries and formats

Treat the engine convention as the stable side of every boundary. If a renderer, physics library, audio library, asset format, editor surface, or network representation uses different axes or units, its owning adapter converts explicitly when data enters or leaves engine world space.

Do not use transform scale as a hidden unit conversion.

## Still intentionally undefined

Current Phase 4 work still does not define:

- automatic logical-window ↔ framebuffer coordinate conversion;
- texture or UV origin;
- glTF/Jolt/OpenAL conversion details;
- Euler storage order or quaternion canonical sign;
- network transform quantization.

## API status

Implemented public spatial APIs now include hierarchical `Transform`, immutable ray/plane/sphere/AABB/frustum primitives, `CameraMatrices` view/perspective construction, and `ScreenRays` screen-to-world ray construction. Network quantization remains a later Phase 4 task.
