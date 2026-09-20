package com.samo.engine.render.opengl.internal;

interface OpenGlResourceBackend {
    enum FenceStatus {
        SIGNALED, TIMEOUT, FAILED
    }

    int createBuffer();

    void deleteBuffer(int handle);

    void allocateDynamicBufferStorage(int handle, long capacityBytes);

    void uploadBufferSubData(int handle, long offsetBytes, java.nio.ByteBuffer data);

    long createFence();

    FenceStatus fenceStatus(long fenceHandle);

    void deleteFence(long fenceHandle);

    int createVertexArray();

    void deleteVertexArray(int handle);

    int createTexture();

    void deleteTexture(int handle);

    default void allocateRgba8Texture(int handle, TextureColorEncoding colorEncoding, int width, int height, java.nio.ByteBuffer rgbaBytes) {
        throw new UnsupportedOperationException("RGBA8 texture allocation is not implemented by this backend");
    }

    int createSampler();

    void deleteSampler(int handle);

    default void configureLinearClampSampler(int handle) {
        throw new UnsupportedOperationException("Sampler configuration is not implemented by this backend");
    }

    int createFramebuffer();

    void deleteFramebuffer(int handle);

    int createShader(int shaderType);

    void shaderSource(int shader, String source);

    void compileShader(int shader);

    boolean shaderCompileSucceeded(int shader);

    String shaderInfoLog(int shader);

    void deleteShader(int shader);

    int createProgram();

    void attachShader(int program, int shader);

    void linkProgram(int program);

    boolean programLinkSucceeded(int program);

    String programInfoLog(int program);

    void detachShader(int program, int shader);

    void deleteProgram(int program);
}
