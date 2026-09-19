package com.samo.engine.render.opengl.internal;

interface OpenGlDrawBackend {
    void configurePositionAndNormalAttributes(int vertexArray, int vertexBuffer);

    void bindElementBuffer(int vertexArray, int indexBuffer);

    void bindUniformBuffer(int bindingIndex, int buffer);

    void setViewport(int x, int y, int width, int height);

    void applyMaterialState(RendererMaterial material);

    int defaultFramebufferColorEncoding();

    void setFramebufferSrgbEnabled(boolean enabled);

    void clearFrame(PresentationMode presentationMode);

    void bindTextureAndSampler(int unit, int texture, int sampler);

    void setMaterialScalars(int program, MaterialScalars scalars);

    void setDirectionalLight(int program, DirectionalLight light);

    void useProgram(int program);

    void bindVertexArray(int vertexArray);

    void drawIndexedTriangle();

    void bindDefaultVertexArray();

    void useDefaultProgram();
}
