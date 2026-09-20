package com.samo.engine.render.opengl.internal;

import com.samo.engine.render.api.RenderLocalLight;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4fc;

final class RendererFrameUniformUploader {
    private final OpenGlResourceBackend resourceBackend;
    private final int cameraBuffer;
    private final int perFrameBuffer;
    private final int localLightBuffer;
    private final ByteBuffer cameraBytes =
            ByteBuffer.allocateDirect(CameraUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
    private final ByteBuffer perFrameBytes =
            ByteBuffer.allocateDirect(PerFrameUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());
    private final ByteBuffer localLightBytes =
            ByteBuffer.allocateDirect(LocalLightUniformBlock.SIZE_BYTES).order(ByteOrder.nativeOrder());

    RendererFrameUniformUploader(
            OpenGlResourceBackend resourceBackend,
            int cameraBuffer,
            int perFrameBuffer,
            int localLightBuffer) {
        this.resourceBackend = Objects.requireNonNull(resourceBackend, "resourceBackend");
        this.cameraBuffer = cameraBuffer;
        this.perFrameBuffer = perFrameBuffer;
        this.localLightBuffer = localLightBuffer;
    }

    void upload(
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix,
            int framebufferWidth,
            int framebufferHeight,
            List<RenderLocalLight> localLights) {
        cameraBytes.clear();
        CameraUniformBlock.write(viewMatrix, projectionMatrix, cameraBytes);
        cameraBytes.flip();
        resourceBackend.uploadBufferSubData(cameraBuffer, 0L, cameraBytes);

        perFrameBytes.clear();
        PerFrameUniformBlock.write(framebufferWidth, framebufferHeight, perFrameBytes);
        perFrameBytes.flip();
        resourceBackend.uploadBufferSubData(perFrameBuffer, 0L, perFrameBytes);

        localLightBytes.clear();
        LocalLightUniformBlock.write(localLights, localLightBytes);
        localLightBytes.flip();
        resourceBackend.uploadBufferSubData(localLightBuffer, 0L, localLightBytes);
    }
}
