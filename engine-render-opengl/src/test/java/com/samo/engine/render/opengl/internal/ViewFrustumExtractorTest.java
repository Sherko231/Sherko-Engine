package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.Aabb3f;
import com.samo.engine.core.api.Frustum3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ViewFrustumExtractorTest {
    @Test
    void identityClipSpaceKeepsInsideAndContactButRejectsOutside() {
        Frustum3f frustum = ViewFrustumExtractor.extract(new Matrix4f(), new Matrix4f());

        Aabb3f inside = aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
        Aabb3f touchingRightPlane = aabb(1.0f, -0.1f, -0.1f, 1.0f, 0.1f, 0.1f);
        Aabb3f outsideRightPlane = aabb(1.01f, -0.1f, -0.1f, 1.2f, 0.1f, 0.1f);

        assertTrue(frustum.intersects(inside));
        assertTrue(frustum.intersects(touchingRightPlane));
        assertFalse(frustum.intersects(outsideRightPlane));
    }

    @Test
    void translatedViewMovesWorldBoundsRelativeToCamera() {
        Matrix4f view = new Matrix4f().translation(-5.0f, 0.0f, 0.0f);
        Frustum3f frustum = ViewFrustumExtractor.extract(view, new Matrix4f());

        assertTrue(frustum.intersects(aabb(4.5f, -0.5f, -0.5f, 5.5f, 0.5f, 0.5f)));
        assertFalse(frustum.intersects(aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f)));
    }

    private static Aabb3f aabb(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        return new Aabb3f(
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, maxY, maxZ));
    }
}
