# OpenGL renderer

`com.samo.engine.render.api.OpenGlRenderer` is the first bounded public production renderer composition.

It exists to establish one real engine-owned indexed draw without exposing OpenGL handles or prematurely defining a general mesh/material/world API.

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
renderer.render(view, projection, framebufferWidth, framebufferHeight);
window.present();
```

`render(...)` requires:

- the OpenGL owner thread;
- non-null caller-supplied JOML view/projection matrices;
- positive framebuffer pixel dimensions.

The matrices follow the accepted D-041/D-045 contract. The renderer does not create or own a gameplay/world camera.

Every call currently:

- uploads the P5-T06 `CameraBlock` and `PerFrameBlock`;
- uses the full framebuffer as viewport;
- enables depth testing with `GL_LESS`;
- enables back-face culling with `GL_BACK`;
- treats counter-clockwise winding as front-facing;
- detects whether the actual default back buffer is `GL_SRGB` or `GL_LINEAR`;
- clears the development color/depth buffers using the matching presentation-encoding path;
- samples one renderer-owned 1x1 neutral-gray sRGB reference texture through an internal sampler;
- draws exactly one engine-owned indexed triangle;
- uses hardware `GL_FRAMEBUFFER_SRGB` encoding on an sRGB default buffer, or one bounded fragment encode on a linear default buffer;
- disables `GL_FRAMEBUFFER_SRGB` before returning.

Presentation is intentionally separate through `GlfwWindow.present()`.

## Current fixed mesh

The current renderer owns one position-only triangle around the world origin:

- three vertices;
- three unsigned-int indices `0, 1, 2`;
- attribute location 0 contains three `float` position components.

This mesh is renderer-owned validation content, not an asset-system format or a reusable arbitrary mesh API. P5-T08 also gives the triangle one fixed internal reference texture. Display-referred color bytes use sRGB storage so OpenGL decodes them to linear shader values; linear-data textures use linear RGBA8 storage. This does not create a public texture or material API.

## Ownership and shutdown

`OpenGlRenderer.close()` is idempotent. It closes the linked program, shaders, reference sampler/texture, uniform buffers, index/vertex buffers, and vertex array through the production native-resource ownership path.

Close order at the composition root should be:

1. `OpenGlRenderer.close()`;
2. `GlfwWindow.stop()`;
3. `GlfwWindow.close()`;
4. `NativeResourceRegistry.assertNoOpenResources()`.

## Deliberate limitations

The current bounded renderer does not provide:

- arbitrary mesh creation/submission;
- asset import/loading;
- arbitrary/public texture creation or material records;
- lighting;
- HDR, tonemapping, fog, post-processing, or P5-T15 presentation architecture;
- world/ECS integration;
- gameplay camera ownership;
- batching, culling, sorting, or frame graphs;
- render-worker or command-queue behavior;
- raw OpenGL handles.

Those remain separate roadmap tasks.
