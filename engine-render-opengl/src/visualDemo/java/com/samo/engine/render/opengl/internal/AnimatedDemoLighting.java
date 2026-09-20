package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugPrimitive;
import com.samo.engine.core.api.DebugRay;
import com.samo.engine.core.api.Ray3f;
import com.samo.engine.render.api.RenderLocalLight;
import com.samo.engine.render.api.RenderPointLight;
import com.samo.engine.render.api.RenderSpotLight;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

final class AnimatedDemoLighting {
    private static final DebugColor POINT_DEBUG = new DebugColor(1.0f, 0.45f, 0.10f);
    private static final DebugColor SPOT_DEBUG = new DebugColor(0.10f, 0.75f, 1.0f);

    private AnimatedDemoLighting() {
    }

    static LightingFrame at(double seconds) {
        float pointX = (float) (Math.sin(seconds * 0.85) * 1.35);
        float pointY = 0.45f + (float) (Math.cos(seconds * 1.10) * 0.30);
        float pointZ = -0.55f + (float) (Math.cos(seconds * 0.85) * 0.35);

        float spotX = (float) (Math.cos(seconds * 0.60) * 1.25);
        float spotY = 0.95f + (float) (Math.sin(seconds * 0.75) * 0.20);
        float spotZ = -0.25f + (float) (Math.sin(seconds * 0.60) * 0.30);

        Vector3f spotPosition = new Vector3f(spotX, spotY, spotZ);
        Vector3f spotDirection =
                new Vector3f(0.0f, 0.0f, -2.25f).sub(spotPosition).normalize();

        RenderPointLight point = new RenderPointLight(
                pointX,
                pointY,
                pointZ,
                1.0f,
                0.28f,
                0.06f,
                1.0f,
                4.5f);
        RenderSpotLight spot = new RenderSpotLight(
                spotX,
                spotY,
                spotZ,
                spotDirection.x,
                spotDirection.y,
                spotDirection.z,
                0.10f,
                0.60f,
                1.0f,
                1.0f,
                5.0f,
                (float) Math.toRadians(12.0),
                (float) Math.toRadians(28.0));

        ArrayList<DebugPrimitive> debug = new ArrayList<>();
        addCross(debug, new Vector3f(pointX, pointY, pointZ), 0.16f, POINT_DEBUG);
        addCross(debug, spotPosition, 0.16f, SPOT_DEBUG);
        debug.add(new DebugRay(
                new Ray3f(spotPosition, spotDirection),
                2.3f,
                SPOT_DEBUG));

        return new LightingFrame(List.of(point, spot), List.copyOf(debug));
    }

    private static void addCross(
            List<DebugPrimitive> destination,
            Vector3f center,
            float halfExtent,
            DebugColor color) {
        destination.add(new DebugLine(
                center.x - halfExtent, center.y, center.z,
                center.x + halfExtent, center.y, center.z,
                color));
        destination.add(new DebugLine(
                center.x, center.y - halfExtent, center.z,
                center.x, center.y + halfExtent, center.z,
                color));
        destination.add(new DebugLine(
                center.x, center.y, center.z - halfExtent,
                center.x, center.y, center.z + halfExtent,
                color));
    }

    record LightingFrame(
            List<RenderLocalLight> lights,
            List<DebugPrimitive> debugPrimitives) {
    }
}
