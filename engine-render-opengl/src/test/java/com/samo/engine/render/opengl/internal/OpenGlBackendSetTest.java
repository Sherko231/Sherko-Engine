package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class OpenGlBackendSetTest {
    @Test
    void preservesInjectedAdapterIdentities() {
        OpenGlResourceBackend resources = new StubResourceBackend();
        OpenGlDrawBackend draw = new StubDrawBackend();
        OpenGlUniformBlockReflectionBackend reflection = new StubReflectionBackend();

        OpenGlBackendSet backends = new OpenGlBackendSet(resources, draw, reflection);

        assertSame(resources, backends.resourceBackend());
        assertSame(draw, backends.drawBackend());
        assertSame(reflection, backends.reflectionBackend());
    }

    @Test
    void rejectsNullAdapters() {
        OpenGlResourceBackend resources = new StubResourceBackend();
        OpenGlDrawBackend draw = new StubDrawBackend();
        OpenGlUniformBlockReflectionBackend reflection = new StubReflectionBackend();

        assertThrows(NullPointerException.class, () -> new OpenGlBackendSet(null, draw, reflection));
        assertThrows(NullPointerException.class, () -> new OpenGlBackendSet(resources, null, reflection));
        assertThrows(NullPointerException.class, () -> new OpenGlBackendSet(resources, draw, null));
    }

    @Test
    void productionUsesTheCurrentLwjglAdapters() {
        OpenGlBackendSet backends = OpenGlBackendSet.production();

        assertInstanceOf(LwjglOpenGlResourceBackend.class, backends.resourceBackend());
        assertInstanceOf(LwjglOpenGlDrawBackend.class, backends.drawBackend());
        assertInstanceOf(LwjglOpenGlUniformBlockReflectionBackend.class, backends.reflectionBackend());
    }

    private static final class StubResourceBackend implements OpenGlResourceBackend {
        @Override public int createBuffer() { return 1; }
        @Override public void deleteBuffer(int handle) { }
        @Override public void allocateDynamicBufferStorage(int handle, long capacityBytes) { }
        @Override public void uploadBufferSubData(int handle, long offsetBytes, ByteBuffer data) { }
        @Override public long createFence() { return 1L; }
        @Override public FenceStatus fenceStatus(long fenceHandle) { return FenceStatus.SIGNALED; }
        @Override public void deleteFence(long fenceHandle) { }
        @Override public int createVertexArray() { return 1; }
        @Override public void deleteVertexArray(int handle) { }
        @Override public int createTexture() { return 1; }
        @Override public void deleteTexture(int handle) { }
        @Override public void allocateRgba8Texture(
                int handle,
                TextureColorEncoding colorEncoding,
                int width,
                int height,
                ByteBuffer rgbaBytes) { }
        @Override public int createSampler() { return 1; }
        @Override public void deleteSampler(int handle) { }
        @Override public void configureLinearClampSampler(int handle) { }
        @Override public int createFramebuffer() { return 1; }
        @Override public void deleteFramebuffer(int handle) { }
        @Override public int createShader(int shaderType) { return 1; }
        @Override public void shaderSource(int shader, String source) { }
        @Override public void compileShader(int shader) { }
        @Override public boolean shaderCompileSucceeded(int shader) { return true; }
        @Override public String shaderInfoLog(int shader) { return ""; }
        @Override public void deleteShader(int shader) { }
        @Override public int createProgram() { return 1; }
        @Override public void attachShader(int program, int shader) { }
        @Override public void linkProgram(int program) { }
        @Override public boolean programLinkSucceeded(int program) { return true; }
        @Override public String programInfoLog(int program) { return ""; }
        @Override public void detachShader(int program, int shader) { }
        @Override public void deleteProgram(int program) { }
    }

    private static final class StubDrawBackend implements OpenGlDrawBackend {
        @Override public void configurePositionNormalUvAttributes(int vertexArray, int vertexBuffer) { }
        @Override public void configureDebugLineAttributes(int vertexArray, int vertexBuffer) { }
        @Override public void configureViewModelAttributes(int vertexArray, int vertexBuffer) { }
        @Override public void bindElementBuffer(int vertexArray, int indexBuffer) { }
        @Override public void bindUniformBuffer(int bindingIndex, int buffer) { }
        @Override public void setViewport(int x, int y, int width, int height) { }
        @Override public void applyMaterialState(RenderMaterialDescriptor material) { }
        @Override public void applyDebugLineState() { }
        @Override public void clearDepthOnly() { }
        @Override public void applyViewModelState() { }
        @Override public int defaultFramebufferColorEncoding() { return 0; }
        @Override public void setFramebufferSrgbEnabled(boolean enabled) { }
        @Override public void clearFrame(SrgbPresentationMode presentationMode) { }
        @Override public void bindTextureAndSampler(int unit, int texture, int sampler) { }
        @Override public void setMaterialScalars(int program, MaterialScalars scalars) { }
        @Override public void setDirectionalLight(int program, DirectionalLight light) { }
        @Override public void useProgram(int program) { }
        @Override public void bindVertexArray(int vertexArray) { }
        @Override public void drawIndexedTriangles(int indexCount) { }
        @Override public void drawDebugLines(int vertexCount) { }
        @Override public void drawViewModelTriangles(int vertexCount) { }
        @Override public void bindDefaultVertexArray() { }
        @Override public void useDefaultProgram() { }
    }

    private static final class StubReflectionBackend implements OpenGlUniformBlockReflectionBackend {
        @Override public int uniformBlockIndex(int programHandle, String blockName) { return 0; }
        @Override public int uniformBlockDataSize(int programHandle, int blockIndex) { return 0; }
        @Override public int uniformBlockBinding(int programHandle, int blockIndex) { return 0; }
    }
}
