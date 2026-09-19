# OpenGL renderer

`com.samo.engine.render.api.OpenGlRenderer` is the first bounded public production renderer composition.

It exists to establish a real engine-owned indexed rendering path without exposing OpenGL handles or prematurely defining a public mesh/material/world API.

A Gradle consumer should declare only `engine-render-opengl` for this renderer boundary. Its compile variant exports the public signature dependencies required by `OpenGlRenderer` (`engine-core`, `engine-platform-lwjgl`, and JOML transitively through core) while exposing only `com.samo.engine.render.api` renderer classes. Renderer `.internal` implementation classes are not part of the supported consumer compile surface; the runtime variant still contains the complete implementation and shader resources.

## Create

Create the renderer only after the production `GlfwWindow` has successfully started and its OpenGL 4.6 context is current on the owner thread:

```java
NativeResourceRegistry nativeResources = new NativeResourceRegistry();
GlfwWindow window = ...;

window.initialize();
window.start();

OpenGlRenderer renderer =
        OpenGlRenderer.create(window.openGlThreadGuard(), nativeResources);
```

The renderer owns its GPU resources and must be closed before the window/context is stopped.

## Render

```java
RenderFramePacket frame =
        new RenderFramePacket(view, projection, framebufferWidth, framebufferHeight);
renderer.render(frame);
window.present();
```

`RenderFramePacket` construction requires non-null finite JOML view/projection matrices plus positive framebuffer pixel dimensions. Construction copies both matrices immediately. Later mutation of the caller's source matrices cannot change the packet, and matrix reads copy into caller-owned destinations rather than exposing the packet's internal mutable storage.

`render(RenderFramePacket)` requires the OpenGL owner thread and consumes the packet synchronously without retaining it. Packets own no native resources and require no cleanup. The matrices follow the accepted D-041/D-045 contract. The renderer does not create or own a gameplay/world camera.

The previous `render(view, projection, width, height)` overload remains as a compatibility convenience and constructs the same immutable packet internally.

Every call currently:

- derives the active CPU view frustum from the packet's accepted `projection * view` matrices;
- tests the fixed reference mesh world AABB through P4 `Frustum3f.intersects(Aabb3f)` before each draw candidate;
- sorts the remaining internal draw submissions deterministically: opaque by renderer-owned logical program/material/mesh keys, transparent back-to-front by camera-space depth with stable ties;
- uploads the P5-T06 `CameraBlock` and `PerFrameBlock`;
- detects whether the actual default back buffer is `GL_SRGB` or `GL_LINEAR`;
- clears the full development color/depth buffers using the matching presentation-encoding path;
- samples one renderer-owned 1x1 neutral-gray sRGB reference texture through an internal sampler;
- draws the same owned indexed triangle twice using two internal immutable P5-T09 material values;
- applies blend/depth/cull state from each material instead of entity-type branches;
- applies each material's bounded linear color multipliers after sRGB texture decode and before presentation encode;
- uses hardware `GL_FRAMEBUFFER_SRGB` encoding on an sRGB default buffer, or one bounded fragment encode on a linear default buffer;
- restores the full framebuffer viewport, unbinds program/VAO/texture state, and disables `GL_FRAMEBUFFER_SRGB` before returning.

After a successful render, `lastCullingCounters()` returns immutable `RenderCullingCounters` for that frame: tested candidates, visible candidates, culled candidates, and submitted draws. Failed renders leave the prior successful counters unchanged. These are correctness/diagnostic counters, not a performance benchmark.

Presentation is intentionally separate through `GlfwWindow.present()`.

## Current fixed mesh

The current renderer owns one position-only triangle around the world origin:

- three vertices;
- three unsigned-int indices `0, 1, 2`;
- attribute location 0 contains three `float` position components.

This mesh is renderer-owned validation content, not an asset-system format or a reusable arbitrary mesh API. P5-T08 gives it one fixed internal reference texture. P5-T09 reuses that same mesh/texture/program in two viewport halves with internal baseline and tinted material values carrying shader/textures/scalars/blend/depth/cull policy. Display-referred color bytes use sRGB storage so OpenGL decodes them to linear shader values; material color multipliers operate in linear space. This still does not create a public texture or material API.

## Ownership and shutdown

`OpenGlRenderer.close()` is idempotent. It closes the linked program, shaders, reference sampler/texture, uniform buffers, index/vertex buffers, and vertex array through the production native-resource ownership path.

Close order at the composition root should be:

1. `OpenGlRenderer.close()`;
2. `GlfwWindow.stop()`;
3. `GlfwWindow.close()`;
4. `NativeResourceRegistry.assertNoOpenResources()`.

## Deliberate limitations

The current bounded renderer does not provide:

- arbitrary mesh creation/submission or public mesh/material/resource identities inside `RenderFramePacket`;
- asset import/loading;
- arbitrary/public texture creation or public material records;
- lighting;
- HDR, tonemapping, fog, post-processing, or P5-T15 presentation architecture;
- world/ECS integration;
- gameplay camera ownership;
- batching, render graphs/pass scheduling, broad-phase/occlusion/GPU culling, GPU-driven sorting, order-independent transparency, or instancing;
- render-worker or command-queue behavior;
- raw OpenGL handles.

Those remain separate roadmap tasks.
