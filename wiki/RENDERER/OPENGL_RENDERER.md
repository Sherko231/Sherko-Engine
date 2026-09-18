# OpenGL renderer

`com.samo.engine.render.api.OpenGlRenderer` is the first bounded public production renderer composition.

It exists to establish one real engine-owned indexed draw without exposing OpenGL handles or prematurely defining a general mesh/material/world API.

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
- clears the development color/depth buffers;
- draws exactly one engine-owned indexed triangle.

Presentation is intentionally separate through `GlfwWindow.present()`.

## Current fixed mesh

The current renderer owns one position-only triangle around the world origin:

- three vertices;
- three unsigned-int indices `0, 1, 2`;
- attribute location 0 contains three `float` position components.

This mesh is renderer-owned validation content, not an asset-system format or a reusable arbitrary mesh API.

## Ownership and shutdown

`OpenGlRenderer.close()` is idempotent. It closes the linked program, shaders, uniform buffers, index/vertex buffers, and vertex array through the production native-resource ownership path.

Close order at the composition root should be:

1. `OpenGlRenderer.close()`;
2. `GlfwWindow.stop()`;
3. `GlfwWindow.close()`;
4. `NativeResourceRegistry.assertNoOpenResources()`.

## Deliberate limitations

P5-T07 does not provide:

- arbitrary mesh creation/submission;
- asset import/loading;
- textures or material records;
- lighting;
- sRGB/gamma policy;
- world/ECS integration;
- gameplay camera ownership;
- batching, culling, sorting, or frame graphs;
- render-worker or command-queue behavior;
- raw OpenGL handles.

Those remain separate roadmap tasks.
