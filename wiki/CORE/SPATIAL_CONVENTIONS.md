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

## Transform quantization

`TransformQuantization` provides value-level quantization for later storage/network adapters without defining a packet.

Position:

- `TransformQuantization.quantizePosition(...)` accepts canonical engine-space meters;
- each axis uses one signed `short` with `1/64 m` steps;
- valid range is `-512.0 m` through `511.984375 m` inclusive;
- maximum per-axis round-trip error is `1/128 m` (`7.8125 mm`);
- invalid/non-finite/out-of-range input throws instead of clamping.

Rotation:

- `quantizeRotation(...)` accepts any finite non-zero quaternion and normalizes it;
- the largest-absolute component is omitted, with lowest-index tie breaking;
- sign is canonicalized so equivalent `q` and `-q` produce exactly the same `QuantizedRotation`;
- the remaining three components use signed codes `[-32767,+32767]`; `Short.MIN_VALUE` is reserved invalid;
- `dequantizeRotation(...)` rejects malformed records, reconstructs the omitted positive component, normalizes the output, and writes into the caller-owned `Quaternionf`;
- valid encode/decode angular orientation error is bounded by `0.0002 rad`.

Example:

```java
var encodedPosition = TransformQuantization.quantizePosition(
        new Vector3f(12.345f, -3.25f, 100.0f));
Vector3f decodedPosition = TransformQuantization.dequantizePosition(
        encodedPosition, new Vector3f());

var encodedRotation = TransformQuantization.quantizeRotation(sourceRotation);
Quaternionf decodedRotation = TransformQuantization.dequantizeRotation(
        encodedRotation, new Quaternionf());
```

The values do not say whether they represent local or world transforms. They also do not define byte order, packet field order/version, entity/tick IDs, origin rebasing, authority, transport, delta compression, or scale quantization. Those belong to later executable networking/storage contracts.

## External libraries and formats

Treat the engine convention as the stable side of every boundary. If a renderer, physics library, audio library, asset format, editor surface, or network representation uses different axes or units, its owning adapter converts explicitly when data enters or leaves engine world space.

Do not use transform scale as a hidden unit conversion.

## Still intentionally undefined

Current Phase 4 work still does not define:

- automatic logical-window ↔ framebuffer coordinate conversion;
- texture or UV origin;
- glTF/Jolt/OpenAL conversion details;
- Euler storage order;
- production network transform packet layout, origin policy, replication authority, protocol versioning, or delta compression.

## API status

Implemented public spatial APIs now include hierarchical `Transform`, immutable ray/plane/sphere/AABB/frustum primitives, `CameraMatrices` view/perspective construction, `ScreenRays` screen-to-world ray construction, and D-047 `TransformQuantization` position/quaternion value quantization. Production network packet/replication integration remains future work.
