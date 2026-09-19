package com.samo.engine.render.opengl.internal;

final class SrgbTransfer {
    private SrgbTransfer() {
    }

    static float encodeLinear(float linear) {
        if (!Float.isFinite(linear) || linear < 0.0f || linear > 1.0f) {
            throw new IllegalArgumentException("linear must be finite and within [0,1]");
        }
        return linear <= 0.0031308f
                ? linear * 12.92f
                : 1.055f * (float) Math.pow(linear, 1.0 / 2.4) - 0.055f;
    }
}
