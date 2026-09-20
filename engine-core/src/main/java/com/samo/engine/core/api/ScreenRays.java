package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Builds world-space rays from top-left-origin screen/viewport coordinates. */
public final class ScreenRays {
    private ScreenRays() {

    }

    public static Ray3f worldRay(float screenX, float screenY, int viewportX, int viewportY, int viewportWidth, int viewportHeight, Matrix4fc view, Matrix4fc projection) {

        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(projection, "projection");
        requireFinite(screenX, "screenX");
        requireFinite(screenY, "screenY");
        if (viewportWidth <= 0) {
            throw new IllegalArgumentException("viewportWidth must be > 0");
        }
        if (viewportHeight <= 0) {
            throw new IllegalArgumentException("viewportHeight must be > 0");
        }

        double viewportMaxX = (double) viewportX + viewportWidth;
        double viewportMaxY = (double) viewportY + viewportHeight;
        if (screenX < viewportX || screenX > viewportMaxX || screenY < viewportY || screenY > viewportMaxY) {
            throw new IllegalArgumentException("screen sample must lie inside the closed viewport rectangle");
        }

        requireFinite(view, "view");
        requireFinite(projection, "projection");

        Matrix4f inverseViewProjection = new Matrix4f(projection).mul(view);
        float determinant = inverseViewProjection.determinant();
        if (!Float.isFinite(determinant) || determinant == 0.0f) {
            throw new IllegalArgumentException("projection * view must be finite and non-singular");
        }
        inverseViewProjection.invert();
        requireFinite(inverseViewProjection, "inverse projection * view");

        float ndcX = (float) (2.0 * (((double) screenX - viewportX) / viewportWidth) - 1.0);
        float ndcY = (float) (1.0 - 2.0 * (((double) screenY - viewportY) / viewportHeight));

        float nearX = transformX(inverseViewProjection, ndcX, ndcY, -1.0f);
        float nearY = transformY(inverseViewProjection, ndcX, ndcY, -1.0f);
        float nearZ = transformZ(inverseViewProjection, ndcX, ndcY, -1.0f);
        float nearW = transformW(inverseViewProjection, ndcX, ndcY, -1.0f);
        float farX = transformX(inverseViewProjection, ndcX, ndcY, 1.0f);
        float farY = transformY(inverseViewProjection, ndcX, ndcY, 1.0f);
        float farZ = transformZ(inverseViewProjection, ndcX, ndcY, 1.0f);
        float farW = transformW(inverseViewProjection, ndcX, ndcY, 1.0f);

        requireFiniteNonZero(nearW, "near homogeneous w");
        requireFiniteNonZero(farW, "far homogeneous w");

        nearX /= nearW;
        nearY /= nearW;
        nearZ /= nearW;
        farX /= farW;
        farY /= farW;
        farZ /= farW;
        requireFinite(nearX, "near world x");
        requireFinite(nearY, "near world y");
        requireFinite(nearZ, "near world z");
        requireFinite(farX, "far world x");
        requireFinite(farY, "far world y");
        requireFinite(farZ, "far world z");

        return new Ray3f(nearX, nearY, nearZ, farX - nearX, farY - nearY, farZ - nearZ);

    }

    private static float transformX(Matrix4fc matrix, float x, float y, float z) {

        return matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30();

    }

    private static float transformY(Matrix4fc matrix, float x, float y, float z) {

        return matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31();

    }

    private static float transformZ(Matrix4fc matrix, float x, float y, float z) {

        return matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32();

    }

    private static float transformW(Matrix4fc matrix, float x, float y, float z) {

        return matrix.m03() * x + matrix.m13() * y + matrix.m23() * z + matrix.m33();

    }

    private static void requireFinite(Matrix4fc matrix, String name) {

        requireFinite(matrix.m00(), name + ".m00");
        requireFinite(matrix.m01(), name + ".m01");
        requireFinite(matrix.m02(), name + ".m02");
        requireFinite(matrix.m03(), name + ".m03");
        requireFinite(matrix.m10(), name + ".m10");
        requireFinite(matrix.m11(), name + ".m11");
        requireFinite(matrix.m12(), name + ".m12");
        requireFinite(matrix.m13(), name + ".m13");
        requireFinite(matrix.m20(), name + ".m20");
        requireFinite(matrix.m21(), name + ".m21");
        requireFinite(matrix.m22(), name + ".m22");
        requireFinite(matrix.m23(), name + ".m23");
        requireFinite(matrix.m30(), name + ".m30");
        requireFinite(matrix.m31(), name + ".m31");
        requireFinite(matrix.m32(), name + ".m32");
        requireFinite(matrix.m33(), name + ".m33");

    }

    private static void requireFinite(float value, String name) {

        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }

    }

    private static void requireFiniteNonZero(float value, String name) {

        if (!Float.isFinite(value) || value == 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and non-zero");
        }

    }
}
