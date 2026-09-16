# Spatial conventions

Sherko Engine uses one canonical world-space convention. The normative architecture contract is [`../../docs/SPATIAL_CONVENTIONS.md`](../../docs/SPATIAL_CONVENTIONS.md); this page is a consumer-oriented summary and does not define a separate convention.

## World basis

- right-handed Cartesian world;
- `+X` points right;
- `+Y` points up;
- `-Z` points forward;
- `+Z` points backward.

So `(0, 0, -2)` means two meters forward from the world origin.

## Units

- position and distance: meters;
- linear velocity: meters per second;
- internal angles: radians;
- angular velocity: radians per second;
- transform scale: dimensionless, with `(1, 1, 1)` as identity.

Positive rotation follows the right-hand rule around the positive axis.

## External libraries and formats

Treat the engine convention as the stable side of every boundary. If a renderer, physics library, audio library, asset format, editor surface, or network representation uses different axes or units, its owning adapter converts explicitly when data enters or leaves engine world space.

Do not use transform scale as a hidden unit conversion.

## Do not infer unrelated coordinate rules

P4-T01 does not define:

- OpenGL clip/NDC depth convention or reversed-Z;
- camera FOV, near/far, or projection-matrix policy;
- window/framebuffer/screen coordinate origin;
- texture or UV origin;
- glTF/Jolt/OpenAL conversion details;
- Euler storage order or quaternion canonical sign;
- network transform quantization.

Those contracts are added only by their own executable tasks.

## API status

There is no new spatial Java API in P4-T01. Transform, geometry, camera, and quantization APIs are still future Phase 4 work. This page documents the world-space contract those APIs must obey when they are implemented.
