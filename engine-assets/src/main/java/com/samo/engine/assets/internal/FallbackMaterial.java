package com.samo.engine.assets.internal;

record FallbackMaterial(float red, float green, float blue, float alpha) {
    FallbackMaterial {

        if (!Float.isFinite(red) || !Float.isFinite(green) || !Float.isFinite(blue) || !Float.isFinite(alpha)) {
            throw new IllegalArgumentException("Fallback material components must be finite");
        }

    }
}
