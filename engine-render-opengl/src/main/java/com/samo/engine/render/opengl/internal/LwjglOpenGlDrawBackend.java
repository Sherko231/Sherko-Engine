package com.samo.engine.render.opengl.internal;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL41;
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
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingIndex, buffer);
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void applyMaterialState(RendererMaterial material) {
        MaterialStatePolicy policy = MaterialStatePolicy.from(material);

        if (policy.blendEnabled()) {
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
        GL14.glBlendEquation(policy.blendEquation());
        GL11.glBlendFunc(policy.blendSourceFactor(), policy.blendDestinationFactor());

        if (policy.depthTestEnabled()) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDepthFunc(policy.depthFunction());
        GL11.glDepthMask(policy.depthWriteEnabled());

        GL11.glFrontFace(policy.frontFace());
        if (policy.cullEnabled()) {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(policy.cullFace());
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
    }

    @Override
    public int defaultFramebufferColorEncoding() {
        return GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_FRAMEBUFFER,
                GL11.GL_BACK_LEFT,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING);
    }

    @Override
    public void setFramebufferSrgbEnabled(boolean enabled) {
        if (enabled) {
            GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
        } else {
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
        }
    }

    @Override
    public void clearFrame(boolean hardwareSrgbEncode) {
        if (hardwareSrgbEncode) {
            GL11.glClearColor(0.08f, 0.10f, 0.14f, 1.0f);
        } else {
            GL11.glClearColor(
                    linearToSrgb(0.08f),
                    linearToSrgb(0.10f),
                    linearToSrgb(0.14f),
                    1.0f);
        }
        GL11.glClearDepth(1.0d);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void bindTextureAndSampler(int unit, int texture, int sampler) {
        GL45.glBindTextureUnit(unit, texture);
        org.lwjgl.opengl.GL33.glBindSampler(unit, sampler);
    }

    @Override
    public void setMaterialScalars(int program, MaterialScalars scalars) {
        GL41.glProgramUniform4f(
                program,
                0,
                scalars.redMultiplier(),
                scalars.greenMultiplier(),
                scalars.blueMultiplier(),
                scalars.alphaMultiplier());
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

    private static float linearToSrgb(float linear) {
        return linear <= 0.0031308f
                ? linear * 12.92f
                : 1.055f * (float) Math.pow(linear, 1.0 / 2.4) - 0.055f;
    }
}
