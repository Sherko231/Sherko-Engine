package com.samo.engine.render.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RenderLocalLightTest {
    private static final float TOLERANCE = 1.0e-6f;

    @Test
    void pointLightPreservesValidatedLinearInputs() {
        RenderPointLight light = new RenderPointLight(1.0f, 2.0f, -3.0f, 0.2f, 0.4f, 0.6f, 0.8f, 5.0f);

        assertEquals(1.0f, light.positionX());
        assertEquals(2.0f, light.positionY());
        assertEquals(-3.0f, light.positionZ());
        assertEquals(0.2f, light.red());
        assertEquals(0.4f, light.green());
        assertEquals(0.6f, light.blue());
        assertEquals(0.8f, light.intensity());
        assertEquals(5.0f, light.rangeMeters());
    }

    @Test
    void spotLightNormalizesRayTravelDirectionAndPreservesConeRadians() {
        RenderSpotLight light = new RenderSpotLight(0.0f, 1.0f, 2.0f, 0.0f, -2.0f, -2.0f, 1.0f, 0.5f, 0.25f, 0.75f, 8.0f, 0.2f, 0.5f);

        float inverseSqrtTwo = (float) (1.0 / Math.sqrt(2.0));
        assertEquals(0.0f, light.directionX(), TOLERANCE);
        assertEquals(-inverseSqrtTwo, light.directionY(), TOLERANCE);
        assertEquals(-inverseSqrtTwo, light.directionZ(), TOLERANCE);
        assertEquals(0.2f, light.innerConeRadians());
        assertEquals(0.5f, light.outerConeRadians());
    }

    @Test
    void rejectsInvalidPointAndSpotInputs() {
        assertThrows(IllegalArgumentException.class, () -> new RenderPointLight(Float.NaN, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new RenderPointLight(0.0f, 0.0f, 0.0f, 1.01f, 1.0f, 1.0f, 1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new RenderPointLight(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new RenderSpotLight(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 0.1f, 0.5f));
        assertThrows(IllegalArgumentException.class, () -> new RenderSpotLight(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 0.5f, 0.5f));
        assertThrows(IllegalArgumentException.class, () -> new RenderSpotLight(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 0.5f, (float) (Math.PI * 0.5)));
    }
}
