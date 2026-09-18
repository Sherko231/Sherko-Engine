package com.samo.engine.render.opengl.internal;

interface OpenGlDrawBackend {
    void configurePositionAttribute(int vertexArray, int vertexBuffer);

    void bindElementBuffer(int vertexArray, int indexBuffer);

    void bindUniformBuffer(int bindingIndex, int buffer);

    void setViewport(int width, int height);

    void configureDepthAndBackFaceCull();

    void clearFrame();

    void useProgram(int program);

    void bindVertexArray(int vertexArray);

    void drawIndexedTriangle();

    void bindDefaultVertexArray();

    void useDefaultProgram();
}
