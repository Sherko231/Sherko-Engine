package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugRay;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import org.junit.jupiter.api.Test;

class AnimatedDemoLightingTest {
    @Test
    void producesTwoMovingLightsAndSevenDebugPrimitives() {
        AnimatedDemoLighting.LightingFrame start = AnimatedDemoLighting.at(0.0);
        AnimatedDemoLighting.LightingFrame later = AnimatedDemoLighting.at(1.5);

        assertEquals(2, start.lights().size());
        assertEquals(7, start.debugPrimitives().size());

        RenderPointLight startPoint = assertInstanceOf(RenderPointLight.class, start.lights().get(0));
        RenderSpotLight startSpot = assertInstanceOf(RenderSpotLight.class, start.lights().get(1));
        RenderPointLight laterPoint = assertInstanceOf(RenderPointLight.class, later.lights().get(0));
        RenderSpotLight laterSpot = assertInstanceOf(RenderSpotLight.class, later.lights().get(1));

        assertNotEquals(startPoint.positionX(), laterPoint.positionX());
        assertNotEquals(startPoint.positionY(), laterPoint.positionY());
        assertNotEquals(startSpot.positionX(), laterSpot.positionX());
        assertNotEquals(startSpot.directionX(), laterSpot.directionX());

        float spotDirectionLength = (float) Math
            .sqrt(startSpot.directionX() * startSpot.directionX() + startSpot.directionY() * startSpot.directionY() + startSpot.directionZ() * startSpot.directionZ());
        assertTrue(Math.abs(spotDirectionLength - 1.0f) < 0.0001f);

        assertInstanceOf(DebugLine.class, start.debugPrimitives().get(0));
        assertInstanceOf(DebugLine.class, start.debugPrimitives().get(5));
        assertInstanceOf(DebugRay.class, start.debugPrimitives().get(6));
    }

    @Test
    void preservesAcceptedInitialLightAndDebugValues() {
        AnimatedDemoLighting.LightingFrame frame = AnimatedDemoLighting.at(0.0);
        RenderPointLight point = assertInstanceOf(RenderPointLight.class, frame.lights().get(0));
        RenderSpotLight spot = assertInstanceOf(RenderSpotLight.class, frame.lights().get(1));
        DebugRay ray = assertInstanceOf(DebugRay.class, frame.debugPrimitives().get(6));

        assertEquals(0.0f, point.positionX(), 0.000001f);
        assertEquals(0.75f, point.positionY(), 0.000001f);
        assertEquals(-0.20f, point.positionZ(), 0.000001f);
        assertEquals(1.0f, point.intensity(), 0.000001f);
        assertEquals(4.5f, point.rangeMeters(), 0.000001f);

        assertEquals(1.25f, spot.positionX(), 0.000001f);
        assertEquals(0.95f, spot.positionY(), 0.000001f);
        assertEquals(-0.25f, spot.positionZ(), 0.000001f);
        assertEquals(1.0f, spot.intensity(), 0.000001f);
        assertEquals(5.0f, spot.rangeMeters(), 0.000001f);

        assertEquals(2.3f, ray.lengthMeters(), 0.000001f);
    }
}
