package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.CameraMatrices;
import org.joml.Matrix4f;

final class ViewModelProjectionFactory {
    static final float VERTICAL_FOV_RADIANS = (float) Math.toRadians(55.0);
    static final float NEAR_PLANE_METERS = 0.01f;
    static final float FAR_PLANE_METERS = 10.0f;

    private ViewModelProjectionFactory() {
    }

    static Matrix4f build(int framebufferWidth, int framebufferHeight, Matrix4f destination) {
        if (framebufferWidth <= 0) {
            throw new IllegalArgumentException("framebufferWidth must be positive");
        }
        if (framebufferHeight <= 0) {
            throw new IllegalArgumentException("framebufferHeight must be positive");
        }
        float aspectRatio = (float) framebufferWidth / framebufferHeight;
        return CameraMatrices.perspective(
                VERTICAL_FOV_RADIANS,
                aspectRatio,
                NEAR_PLANE_METERS,
                FAR_PLANE_METERS,
                destination);
    }
}
