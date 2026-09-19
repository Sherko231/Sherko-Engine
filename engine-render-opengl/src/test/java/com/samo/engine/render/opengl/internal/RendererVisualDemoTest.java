package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.DebugRay;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import org.junit.jupiter.api.Test;

class RendererVisualDemoTest {
    @Test
    void lightingAnimationProducesTwoMovingLightsAndSevenDebugPrimitives() {
        RendererVisualDemo.DemoLighting start = RendererVisualDemo.lightingAt(0.0);
        RendererVisualDemo.DemoLighting later = RendererVisualDemo.lightingAt(1.5);

        assertEquals(2, start.lights().size());
        assertEquals(7, start.debugPrimitives().size());

        RenderPointLight startPoint =
                assertInstanceOf(RenderPointLight.class, start.lights().get(0));
        RenderSpotLight startSpot =
                assertInstanceOf(RenderSpotLight.class, start.lights().get(1));
        RenderPointLight laterPoint =
                assertInstanceOf(RenderPointLight.class, later.lights().get(0));
        RenderSpotLight laterSpot =
                assertInstanceOf(RenderSpotLight.class, later.lights().get(1));

        assertNotEquals(startPoint.positionX(), laterPoint.positionX());
        assertNotEquals(startPoint.positionY(), laterPoint.positionY());
        assertNotEquals(startSpot.positionX(), laterSpot.positionX());
        assertNotEquals(startSpot.directionX(), laterSpot.directionX());

        float spotDirectionLength = (float) Math.sqrt(
                startSpot.directionX() * startSpot.directionX()
                        + startSpot.directionY() * startSpot.directionY()
                        + startSpot.directionZ() * startSpot.directionZ());
        assertTrue(Math.abs(spotDirectionLength - 1.0f) < 0.0001f);
        assertInstanceOf(DebugRay.class, start.debugPrimitives().get(6));
    }
}
