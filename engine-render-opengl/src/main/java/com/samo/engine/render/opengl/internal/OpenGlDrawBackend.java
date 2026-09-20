package com.samo.engine.render.opengl.internal;

interface OpenGlDrawBackend {
    void configurePositionNormalUvAttributes(int vertexArray, int vertexBuffer);

    void configureDebugLineAttributes(int vertexArray, int vertexBuffer);

    void configureViewModelAttributes(int vertexArray, int vertexBuffer);

    void bindElementBuffer(int vertexArray, int indexBuffer);

    void bindUniformBuffer(int bindingIndex, int buffer);

    void setViewport(int x, int y, int width, int height);

    void applyMaterialState(RenderMaterialDescriptor material);

    void applyDebugLineState();

    void clearDepthOnly();

    void applyViewModelState();

    int defaultFramebufferColorEncoding();

    void setFramebufferSrgbEnabled(boolean enabled);

    void clearFrame(SrgbPresentationMode presentationMode);

    void bindTextureAndSampler(int unit, int texture, int sampler);

    void setMaterialScalars(int program, MaterialScalars scalars);

    void setDirectionalLight(int program, DirectionalLight light);

    void useProgram(int program);

    void bindVertexArray(int vertexArray);

    void drawIndexedTriangles(int indexCount);

    void drawDebugLines(int vertexCount);

    void drawViewModelTriangles(int vertexCount);

    void bindDefaultVertexArray();

    void useDefaultProgram();
}
