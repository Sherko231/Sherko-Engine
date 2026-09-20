package com.samo.engine.render.opengl.internal;

import java.util.Objects;

record DrawSubmission(
        RenderMaterialDescriptor material,
        int programKey,
        int materialKey,
        int meshKey,
        float cameraDepth,
        int sequence,
        int viewportX,
        int viewportY,
        int viewportWidth,
        int viewportHeight) {

    DrawSubmission {
        Objects.requireNonNull(material, "material");
        if (programKey < 0) {
            throw new IllegalArgumentException("programKey must be non-negative");
        }
        if (materialKey < 0) {
            throw new IllegalArgumentException("materialKey must be non-negative");
        }
        if (meshKey < 0) {
            throw new IllegalArgumentException("meshKey must be non-negative");
        }
        if (!Float.isFinite(cameraDepth)) {
            throw new IllegalArgumentException("cameraDepth must be finite");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be non-negative");
        }
        if (viewportX < 0 || viewportY < 0) {
            throw new IllegalArgumentException("viewport origin must be non-negative");
        }
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("viewport dimensions must be positive");
        }
    }

    boolean transparent() {
        return material.blendMode() == MaterialBlendMode.ALPHA_BLEND;
    }
}
