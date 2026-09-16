package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class TransformTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void defaultsToIdentityLocalAndWorldTransform() {
        Transform transform = new Transform();

        assertNull(transform.parent());
        assertVector(transform.localPosition(new Vector3f()), 0.0f, 0.0f, 0.0f);
        assertQuaternion(transform.localRotation(new Quaternionf()), 0.0f, 0.0f, 0.0f, 1.0f);
        assertVector(transform.localScale(new Vector3f()), 1.0f, 1.0f, 1.0f);
        assertTransformedPoint(transform, 2.0f, -3.0f, 4.0f, 2.0f, -3.0f, 4.0f);
    }

    @Test
    void localTrsPreservesCanonicalRightHandedRotation() {
        Transform transform = new Transform();
        transform.setLocalPosition(3.0f, 2.0f, -5.0f);
        transform.setLocalRotation(new Quaternionf().rotationY((float) (Math.PI / 2.0)));
        transform.setLocalScale(2.0f, 3.0f, 4.0f);

        assertTransformedPoint(transform, 0.0f, 0.0f, -1.0f, -1.0f, 2.0f, -5.0f);
    }

    @Test
    void parentTranslationMovesChildOrigin() {
        Transform parent = new Transform();
        parent.setLocalPosition(5.0f, -2.0f, 7.0f);
        Transform child = new Transform();
        child.setLocalPosition(1.0f, 3.0f, -4.0f);
        child.setParent(parent);

        assertTransformedPoint(child, 0.0f, 0.0f, 0.0f, 6.0f, 1.0f, 3.0f);
    }

    @Test
    void parentPositiveYRotationRotatesChildLocalTranslation() {
        Transform parent = new Transform();
        parent.setLocalRotation(0.0f, (float) Math.sin(Math.PI / 4.0), 0.0f,
                (float) Math.cos(Math.PI / 4.0));
        Transform child = new Transform();
        child.setLocalPosition(0.0f, 0.0f, -2.0f);
        child.setParent(parent);

        assertTransformedPoint(child, 0.0f, 0.0f, 0.0f, -2.0f, 0.0f, 0.0f);
    }

    @Test
    void parentNonUniformScaleAffectsChildLocalTranslation() {
        Transform parent = new Transform();
        parent.setLocalScale(2.0f, 3.0f, 4.0f);
        Transform child = new Transform();
        child.setLocalPosition(1.0f, 2.0f, -1.0f);
        child.setParent(parent);

        assertTransformedPoint(child, 0.0f, 0.0f, 0.0f, 2.0f, 6.0f, -4.0f);
    }

    @Test
    void combinedParentAndChildTrsMatchesIndependentPointOracle() {
        Transform parent = new Transform();
        parent.setLocalPosition(10.0f, 1.0f, -5.0f);
        parent.setLocalRotation(0.0f, (float) Math.sin(Math.PI / 4.0), 0.0f,
                (float) Math.cos(Math.PI / 4.0));
        parent.setLocalScale(2.0f, 3.0f, 4.0f);

        Transform child = new Transform();
        child.setLocalPosition(1.0f, 2.0f, -1.0f);
        child.setLocalRotation(0.0f, 0.0f, (float) Math.sin(Math.PI / 4.0),
                (float) Math.cos(Math.PI / 4.0));
        child.setLocalScale(0.5f, 2.0f, 1.0f);
        child.setParent(parent);

        assertTransformedPoint(child, 2.0f, 1.0f, -3.0f, -6.0f, 10.0f, -3.0f);
    }

    @Test
    void parentMutationInvalidatesChildCacheLazily() {
        Transform parent = new Transform();
        Transform child = new Transform();
        child.setLocalPosition(1.0f, 0.0f, 0.0f);
        child.setParent(parent);

        assertTransformedPoint(child, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f);
        parent.setLocalPosition(5.0f, 0.0f, 0.0f);
        assertTransformedPoint(child, 0.0f, 0.0f, 0.0f, 6.0f, 0.0f, 0.0f);
    }

    @Test
    void localMutationInvalidatesCachedWorldMatrix() {
        Transform transform = new Transform();
        assertTransformedPoint(transform, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        transform.setLocalPosition(-4.0f, 2.0f, 8.0f);
        assertTransformedPoint(transform, 0.0f, 0.0f, 0.0f, -4.0f, 2.0f, 8.0f);
    }

    @Test
    void parentReassignmentAndDetachPreserveLocalTransform() {
        Transform firstParent = new Transform();
        firstParent.setLocalPosition(1.0f, 0.0f, 0.0f);
        Transform secondParent = new Transform();
        secondParent.setLocalPosition(10.0f, 0.0f, 0.0f);
        Transform child = new Transform();
        child.setLocalPosition(2.0f, 0.0f, 0.0f);

        child.setParent(firstParent);
        assertSame(firstParent, child.parent());
        assertTransformedPoint(child, 0.0f, 0.0f, 0.0f, 3.0f, 0.0f, 0.0f);

        child.setParent(secondParent);
        assertSame(secondParent, child.parent());
        assertTransformedPoint(child, 0.0f, 0.0f, 0.0f, 12.0f, 0.0f, 0.0f);

        child.setParent(null);
        assertNull(child.parent());
        assertTransformedPoint(child, 0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f);
        assertVector(child.localPosition(new Vector3f()), 2.0f, 0.0f, 0.0f);
    }

    @Test
    void inputsAndDestinationsAreNotRetainedOrAliased() {
        Transform transform = new Transform();
        Vector3f positionInput = new Vector3f(1.0f, 2.0f, 3.0f);
        Quaternionf rotationInput = new Quaternionf().rotationY(0.5f);
        Quaternionf expectedRotation = new Quaternionf(rotationInput);
        Vector3f scaleInput = new Vector3f(2.0f, 3.0f, 4.0f);

        transform.setLocalPosition(positionInput);
        transform.setLocalRotation(rotationInput);
        transform.setLocalScale(scaleInput);

        positionInput.set(99.0f, 99.0f, 99.0f);
        rotationInput.identity();
        scaleInput.set(99.0f, 99.0f, 99.0f);

        assertVector(transform.localPosition(new Vector3f()), 1.0f, 2.0f, 3.0f);
        assertQuaternion(transform.localRotation(new Quaternionf()), expectedRotation.x,
                expectedRotation.y, expectedRotation.z, expectedRotation.w);
        assertVector(transform.localScale(new Vector3f()), 2.0f, 3.0f, 4.0f);

        Vector3f positionDestination = transform.localPosition(new Vector3f());
        positionDestination.set(-77.0f, -77.0f, -77.0f);
        assertVector(transform.localPosition(new Vector3f()), 1.0f, 2.0f, 3.0f);

        Quaternionf rotationDestination = transform.localRotation(new Quaternionf());
        rotationDestination.identity();
        assertQuaternion(transform.localRotation(new Quaternionf()), expectedRotation.x,
                expectedRotation.y, expectedRotation.z, expectedRotation.w);

        Matrix4f worldDestination = transform.worldMatrix(new Matrix4f());
        worldDestination.zero();
        assertTransformedPoint(transform, 0.0f, 0.0f, 0.0f, 1.0f, 2.0f, 3.0f);
    }

    @Test
    void rotationInputIsNormalized() {
        Transform transform = new Transform();
        transform.setLocalRotation(0.0f, 0.0f, 0.0f, 2.0f);
        assertQuaternion(transform.localRotation(new Quaternionf()), 0.0f, 0.0f, 0.0f, 1.0f);
    }

    @Test
    void invalidRotationDoesNotPartiallyMutateState() {
        Transform transform = new Transform();
        transform.setLocalRotation(0.0f, 0.0f, 1.0f, 1.0f);
        Quaternionf before = transform.localRotation(new Quaternionf());

        assertThrows(IllegalArgumentException.class,
                () -> transform.setLocalRotation(0.0f, 0.0f, 0.0f, 0.0f));
        assertThrows(IllegalArgumentException.class,
                () -> transform.setLocalRotation(Float.NaN, 0.0f, 0.0f, 1.0f));

        Quaternionf after = transform.localRotation(new Quaternionf());
        assertQuaternion(after, before.x, before.y, before.z, before.w);
    }

    @Test
    void nonFinitePositionAndScaleFailBeforeMutation() {
        Transform transform = new Transform();
        transform.setLocalPosition(1.0f, 2.0f, 3.0f);
        transform.setLocalScale(4.0f, 5.0f, 6.0f);

        assertThrows(IllegalArgumentException.class,
                () -> transform.setLocalPosition(7.0f, Float.POSITIVE_INFINITY, 9.0f));
        assertThrows(IllegalArgumentException.class,
                () -> transform.setLocalScale(Float.NaN, 8.0f, 9.0f));

        assertVector(transform.localPosition(new Vector3f()), 1.0f, 2.0f, 3.0f);
        assertVector(transform.localScale(new Vector3f()), 4.0f, 5.0f, 6.0f);
    }

    @Test
    void zeroAndNegativeScaleAreAllowedForForwardComposition() {
        Transform transform = new Transform();
        transform.setLocalScale(-2.0f, 0.0f, 3.0f);
        assertTransformedPoint(transform, 1.0f, 2.0f, 1.0f, -2.0f, 0.0f, 3.0f);
    }

    @Test
    void repeatedWorldReadsAreStableAndDoNotMutateLocalState() {
        Transform transform = new Transform();
        transform.setLocalPosition(2.0f, 3.0f, -4.0f);
        transform.setLocalRotation(0.2f, -0.4f, 0.1f, 0.8f);
        transform.setLocalScale(1.5f, 0.75f, 2.25f);

        Matrix4f first = transform.worldMatrix(new Matrix4f());
        Matrix4f second = transform.worldMatrix(new Matrix4f());

        assertArrayEquals(matrixValues(first), matrixValues(second), EPSILON);
        assertVector(transform.localPosition(new Vector3f()), 2.0f, 3.0f, -4.0f);
        assertVector(transform.localScale(new Vector3f()), 1.5f, 0.75f, 2.25f);
    }

    @Test
    void nullInputsAndDestinationsAreRejected() {
        Transform transform = new Transform();

        assertThrows(NullPointerException.class, () -> transform.localPosition(null));
        assertThrows(NullPointerException.class, () -> transform.localRotation(null));
        assertThrows(NullPointerException.class, () -> transform.localScale(null));
        assertThrows(NullPointerException.class, () -> transform.worldMatrix(null));
        assertThrows(NullPointerException.class, () -> transform.setLocalPosition((Vector3f) null));
        assertThrows(NullPointerException.class, () -> transform.setLocalRotation((Quaternionf) null));
        assertThrows(NullPointerException.class, () -> transform.setLocalScale((Vector3f) null));
    }

    private static void assertTransformedPoint(
            Transform transform,
            float inputX,
            float inputY,
            float inputZ,
            float expectedX,
            float expectedY,
            float expectedZ) {
        Matrix4f matrix = transform.worldMatrix(new Matrix4f());
        Vector3f actual = new Vector3f(inputX, inputY, inputZ);
        matrix.transformPosition(actual);
        assertVector(actual, expectedX, expectedY, expectedZ);
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }

    private static void assertQuaternion(Quaternionf actual, float x, float y, float z, float w) {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
        assertEquals(w, actual.w, EPSILON);
    }

    private static float[] matrixValues(Matrix4f matrix) {
        return matrix.get(new float[16]);
    }
}
