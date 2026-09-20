package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DirectionalLightTest {
    private static final float TOLERANCE = 1.0e-6f;

    @Test
    void normalizesDirectionAndMatchesHandCalculatedDiffuseResponse() {
        DirectionalLight light = DirectionalLight.from(new Vector3f(0.0f, -2.0f, -2.0f), new Vector3f(1.0f, 0.8f, 0.6f), 0.8f);

        float inverseSqrtTwo = (float) (1.0 / Math.sqrt(2.0));
        assertEquals(0.0f, light.directionX(), TOLERANCE);
        assertEquals(-inverseSqrtTwo, light.directionY(), TOLERANCE);
        assertEquals(-inverseSqrtTwo, light.directionZ(), TOLERANCE);
        assertEquals(0.8f * inverseSqrtTwo, light.diffuseFactor(new Vector3f(0.0f, 0.0f, 1.0f)), TOLERANCE);
        assertEquals(0.8f, light.diffuseFactor(new Vector3f(0.0f, 1.0f, 1.0f)), TOLERANCE);
        assertEquals(0.0f, light.diffuseFactor(new Vector3f(1.0f, 0.0f, 0.0f)), TOLERANCE);
        assertEquals(0.0f, light.diffuseFactor(new Vector3f(0.0f, 0.0f, -1.0f)), TOLERANCE);
    }

    @Test
    void rejectsInvalidDirectionColorIntensityAndNormal() {
        assertThrows(IllegalArgumentException.class, () -> new DirectionalLight(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalLight(Float.NaN, 0.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalLight(0.0f, 0.0f, -1.0f, -0.01f, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalLight(0.0f, 0.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.01f));

        DirectionalLight valid = new DirectionalLight(0.0f, 0.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        assertThrows(IllegalArgumentException.class, () -> valid.diffuseFactor(new Vector3f(0.0f, 0.0f, 0.0f)));
    }
}
