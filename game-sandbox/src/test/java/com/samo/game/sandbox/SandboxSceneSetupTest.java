package com.samo.game.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.samo.engine.core.api.DebugAabb;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugRay;
import com.samo.engine.core.api.DebugSphere;
import com.samo.engine.render.api.RenderFramePacket;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

final class SandboxSceneSetupTest {
    private static final float EPSILON = 1.0e-6f;

    @Test
    void buildsAcceptedFixedSceneProjectionLightsAndDebugOrder() {
        SandboxSceneSetup scene = new SandboxSceneSetup();
        SandboxCamera camera = new SandboxCamera();
        SandboxFramebufferSize framebuffer = new SandboxFramebufferSize(1600, 900);

        RenderFramePacket frame = scene.frame(camera, framebuffer, 42L, 7L);

        assertEquals(1600, frame.framebufferWidth());
        assertEquals(900, frame.framebufferHeight());
        assertEquals(2, frame.localLights().size());
        assertInstanceOf(RenderPointLight.class, frame.localLights().get(0));
        assertInstanceOf(RenderSpotLight.class, frame.localLights().get(1));

        assertEquals(4, frame.debugFrame().primitives().size());
        assertInstanceOf(DebugLine.class, frame.debugFrame().primitives().get(0));
        assertInstanceOf(DebugAabb.class, frame.debugFrame().primitives().get(1));
        assertInstanceOf(DebugSphere.class, frame.debugFrame().primitives().get(2));
        assertInstanceOf(DebugRay.class, frame.debugFrame().primitives().get(3));
        assertEquals("simulation/tick", frame.debugFrame().textCounters().get(0).label());
        assertEquals(42L, frame.debugFrame().textCounters().get(0).value());
        assertEquals("input/frame", frame.debugFrame().textCounters().get(1).label());
        assertEquals(7L, frame.debugFrame().textCounters().get(1).value());

        Matrix4f projection = frame.copyProjectionTo(new Matrix4f());
        float aspect = 1600.0f / 900.0f;
        float focal = (float) (1.0 / Math.tan(Math.toRadians(70.0) * 0.5));
        float near = 0.1f;
        float far = 100.0f;
        float denominator = near - far;
        assertEquals(focal / aspect, projection.m00(), EPSILON);
        assertEquals(focal, projection.m11(), EPSILON);
        assertEquals((far + near) / denominator, projection.m22(), EPSILON);
        assertEquals(-1.0f, projection.m23(), EPSILON);
        assertEquals((2.0f * far * near) / denominator, projection.m32(), EPSILON);
    }
}
