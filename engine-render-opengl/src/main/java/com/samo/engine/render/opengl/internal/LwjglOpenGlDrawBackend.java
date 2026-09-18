package com.samo.engine.render.opengl.internal;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

final class LwjglOpenGlDrawBackend implements OpenGlDrawBackend {
    @Override
    public void configurePositionAttribute(int vertexArray, int vertexBuffer) {
        GL45.glVertexArrayVertexBuffer(vertexArray, 0, vertexBuffer, 0L, 3 * Float.BYTES);
        GL45.glEnableVertexArrayAttrib(vertexArray, 0);
        GL45.glVertexArrayAttribFormat(vertexArray, 0, 3, GL11.GL_FLOAT, false, 0);
        GL45.glVertexArrayAttribBinding(vertexArray, 0, 0);
    }

    @Override
    public void bindElementBuffer(int vertexArray, int indexBuffer) {
        GL45.glVertexArrayElementBuffer(vertexArray, indexBuffer);
    }

    @Override
    public void bindUniformBuffer(int bindingIndex, int buffer) {
        GL30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingIndex, buffer);
    }

    @Override
    public void setViewport(int width, int height) {
        GL11.glViewport(0, 0, width, height);
    }

    @Override
    public void configureDepthAndBackFaceCull() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CCW);
    }

    @Override
    public void clearFrame() {
        GL11.glClearColor(0.08f, 0.10f, 0.14f, 1.0f);
        GL11.glClearDepth(1.0d);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void useProgram(int program) {
        GL20.glUseProgram(program);
    }

    @Override
    public void bindVertexArray(int vertexArray) {
        GL30.glBindVertexArray(vertexArray);
    }

    @Override
    public void drawIndexedTriangle() {
        GL11.glDrawElements(GL11.GL_TRIANGLES, 3, GL11.GL_UNSIGNED_INT, 0L);
    }

    @Override
    public void bindDefaultVertexArray() {
        GL30.glBindVertexArray(0);
    }

    @Override
    public void useDefaultProgram() {
        GL20.glUseProgram(0);
    }
}
