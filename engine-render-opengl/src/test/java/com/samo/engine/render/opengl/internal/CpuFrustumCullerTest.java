package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.Aabb3f;
import com.samo.engine.core.api.Frustum3f;
import com.samo.engine.core.api.Plane3f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class CpuFrustumCullerTest {
    private final CpuFrustumCuller culler = new CpuFrustumCuller();

    @Test
    void delegatesInclusiveAabbVisibilityToAcceptedFrustumSemantics() {
        Frustum3f frustum = unitCubeFrustum();

        assertTrue(culler.isVisible(
                frustum,
                aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f)));
        assertTrue(culler.isVisible(
                frustum,
                aabb(1.0f, -0.2f, -0.2f, 1.0f, 0.2f, 0.2f)));
        assertFalse(culler.isVisible(
                frustum,
                aabb(1.001f, -0.2f, -0.2f, 1.2f, 0.2f, 0.2f)));
    }

    private static Frustum3f unitCubeFrustum() {
        return new Frustum3f(
                new Plane3f(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f),
                new Plane3f(new Vector3f(-1.0f, 0.0f, 0.0f), 1.0f),
                new Plane3f(new Vector3f(0.0f, 1.0f, 0.0f), 1.0f),
                new Plane3f(new Vector3f(0.0f, -1.0f, 0.0f), 1.0f),
                new Plane3f(new Vector3f(0.0f, 0.0f, 1.0f), 1.0f),
                new Plane3f(new Vector3f(0.0f, 0.0f, -1.0f), 1.0f));
    }

    private static Aabb3f aabb(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        return new Aabb3f(
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, maxY, maxZ));
    }
}
