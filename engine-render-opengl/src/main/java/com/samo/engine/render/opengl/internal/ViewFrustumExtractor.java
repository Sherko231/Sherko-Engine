package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.Frustum3f;
import com.samo.engine.core.api.Plane3f;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

final class ViewFrustumExtractor {
    private ViewFrustumExtractor() {
    }

    static Frustum3f extract(Matrix4fc view, Matrix4fc projection) {
        Matrix4fc viewMatrix = Objects.requireNonNull(view, "view");
        Matrix4fc projectionMatrix = Objects.requireNonNull(projection, "projection");
        Matrix4f clip = new Matrix4f(projectionMatrix).mul(viewMatrix);

        return new Frustum3f(plane(clip.m00() + clip.m03(), clip.m10() + clip.m13(), clip.m20() + clip.m23(), clip.m30() + clip.m33()),
            plane(-clip.m00() + clip.m03(), -clip.m10() + clip.m13(), -clip.m20() + clip.m23(), -clip.m30() + clip.m33()),
            plane(clip.m01() + clip.m03(), clip.m11() + clip.m13(), clip.m21() + clip.m23(), clip.m31() + clip.m33()),
            plane(-clip.m01() + clip.m03(), -clip.m11() + clip.m13(), -clip.m21() + clip.m23(), -clip.m31() + clip.m33()),
            plane(clip.m02() + clip.m03(), clip.m12() + clip.m13(), clip.m22() + clip.m23(), clip.m32() + clip.m33()),
            plane(-clip.m02() + clip.m03(), -clip.m12() + clip.m13(), -clip.m22() + clip.m23(), -clip.m32() + clip.m33()));
    }

    private static Plane3f plane(float x, float y, float z, float offset) {
        return new Plane3f(new Vector3f(x, y, z), offset);
    }
}
