package com.samo.engine.render.opengl.internal;

import java.util.Objects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

record MaterialStatePolicy(
        boolean blendEnabled,
        int blendEquation,
        int blendSourceFactor,
        int blendDestinationFactor,
        boolean depthTestEnabled,
        boolean depthWriteEnabled,
        int depthFunction,
        boolean cullEnabled,
        int cullFace,
        int frontFace) {

    static MaterialStatePolicy from(RendererMaterial material) {
        RendererMaterial value = Objects.requireNonNull(material, "material");

        boolean blendEnabled;
        int blendSource;
        int blendDestination;
        switch (value.blendMode()) {
            case OPAQUE -> {
                blendEnabled = false;
                blendSource = GL11.GL_ONE;
                blendDestination = GL11.GL_ZERO;
            }
            case ALPHA_BLEND -> {
                blendEnabled = true;
                blendSource = GL11.GL_SRC_ALPHA;
                blendDestination = GL11.GL_ONE_MINUS_SRC_ALPHA;
            }
            default -> throw new IllegalStateException("Unhandled blend mode: " + value.blendMode());
        }

        boolean depthEnabled;
        boolean depthWrite;
        switch (value.depthMode()) {
            case TEST_WRITE -> {
                depthEnabled = true;
                depthWrite = true;
            }
            case TEST_NO_WRITE -> {
                depthEnabled = true;
                depthWrite = false;
            }
            case DISABLED -> {
                depthEnabled = false;
                depthWrite = false;
            }
            default -> throw new IllegalStateException("Unhandled depth mode: " + value.depthMode());
        }

        boolean cullEnabled;
        int cullFace;
        switch (value.cullMode()) {
            case BACK -> {
                cullEnabled = true;
                cullFace = GL11.GL_BACK;
            }
            case FRONT -> {
                cullEnabled = true;
                cullFace = GL11.GL_FRONT;
            }
            case NONE -> {
                cullEnabled = false;
                cullFace = GL11.GL_BACK;
            }
            default -> throw new IllegalStateException("Unhandled cull mode: " + value.cullMode());
        }

        return new MaterialStatePolicy(
                blendEnabled,
                GL14.GL_FUNC_ADD,
                blendSource,
                blendDestination,
                depthEnabled,
                depthWrite,
                GL11.GL_LESS,
                cullEnabled,
                cullFace,
                GL11.GL_CCW);
    }
}
