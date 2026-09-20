package com.samo.engine.core.api;

import java.util.ArrayList;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Mutable local/world transform using the engine's canonical right-handed spatial convention.
 *
 * <p>
 * Local composition is {@code T * R * S}; world composition is
 * {@code parentWorld * local}. Instances are externally serialized and do not retain caller-owned
 * JOML value/destination objects. Parent assignments that would create a hierarchy cycle are
 * rejected before mutation. Local and parent mutations explicitly invalidate only this transform
 * and its descendants.
 */
public final class Transform {
    private static final String CYCLE_ERROR_MESSAGE = "parent assignment would create a transform cycle";

    private final Vector3f localPosition = new Vector3f();
    private final Quaternionf localRotation = new Quaternionf();
    private final Vector3f localScale = new Vector3f(1.0f, 1.0f, 1.0f);
    private final Matrix4f localMatrix = new Matrix4f();
    private final Matrix4f cachedWorldMatrix = new Matrix4f();

    private Transform parent;
    private ArrayList<Transform> children;
    private boolean dirty = true;
    private long worldRevision;

    public Transform parent() {

        return parent;

    }

    public void setParent(Transform parent) {

        if (this.parent == parent) {
            return;
        }
        validateParentDoesNotCreateCycle(parent);

        Transform previousParent = this.parent;
        if (parent != null) {
            parent.addChild(this);
        }
        if (previousParent != null) {
            previousParent.removeChild(this);
        }
        this.parent = parent;
        markSubtreeDirty();

    }

    public Vector3f localPosition(Vector3f destination) {

        return Objects.requireNonNull(destination, "destination").set(localPosition);

    }

    public void setLocalPosition(float x, float y, float z) {

        requireFinite(x, "position.x");
        requireFinite(y, "position.y");
        requireFinite(z, "position.z");
        localPosition.set(x, y, z);
        markSubtreeDirty();

    }

    public void setLocalPosition(Vector3fc value) {

        Objects.requireNonNull(value, "value");
        setLocalPosition(value.x(), value.y(), value.z());

    }

    public Quaternionf localRotation(Quaternionf destination) {

        return Objects.requireNonNull(destination, "destination").set(localRotation);

    }

    public void setLocalRotation(float x, float y, float z, float w) {

        requireFinite(x, "rotation.x");
        requireFinite(y, "rotation.y");
        requireFinite(z, "rotation.z");
        requireFinite(w, "rotation.w");

        double lengthSquared = (double) x * x + (double) y * y + (double) z * z + (double) w * w;
        if (!(lengthSquared > 0.0) || !Double.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("rotation quaternion must have finite non-zero length");
        }

        double inverseLength = 1.0 / Math.sqrt(lengthSquared);
        localRotation.set((float) (x * inverseLength), (float) (y * inverseLength), (float) (z * inverseLength), (float) (w * inverseLength));
        markSubtreeDirty();

    }

    public void setLocalRotation(Quaternionfc value) {

        Objects.requireNonNull(value, "value");
        setLocalRotation(value.x(), value.y(), value.z(), value.w());

    }

    public Vector3f localScale(Vector3f destination) {

        return Objects.requireNonNull(destination, "destination").set(localScale);

    }

    public void setLocalScale(float x, float y, float z) {

        requireFinite(x, "scale.x");
        requireFinite(y, "scale.y");
        requireFinite(z, "scale.z");
        localScale.set(x, y, z);
        markSubtreeDirty();

    }

    public void setLocalScale(Vector3fc value) {

        Objects.requireNonNull(value, "value");
        setLocalScale(value.x(), value.y(), value.z());

    }

    public Matrix4f worldMatrix(Matrix4f destination) {

        Objects.requireNonNull(destination, "destination");
        ensureWorldMatrixCurrent();
        return destination.set(cachedWorldMatrix);

    }

    private void validateParentDoesNotCreateCycle(Transform proposedParent) {

        for (Transform ancestor = proposedParent; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor == this) {
                throw new IllegalArgumentException(CYCLE_ERROR_MESSAGE);
            }
        }

    }

    private void addChild(Transform child) {

        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);

    }

    private void removeChild(Transform child) {

        if (children == null) {
            throw new IllegalStateException("transform child membership is inconsistent");
        }
        for (int index = 0; index < children.size(); index++) {
            if (children.get(index) == child) {
                children.remove(index);
                if (children.isEmpty()) {
                    children = null;
                }
                return;
            }
        }
        throw new IllegalStateException("transform child membership is inconsistent");

    }

    private void ensureWorldMatrixCurrent() {

        if (parent != null) {
            parent.ensureWorldMatrixCurrent();
        }

        if (!dirty) {
            return;
        }

        localMatrix.identity().translate(localPosition).rotate(localRotation).scale(localScale);
        if (parent == null) {
            cachedWorldMatrix.set(localMatrix);
        } else {
            parent.cachedWorldMatrix.mul(localMatrix, cachedWorldMatrix);
        }

        dirty = false;
        worldRevision++;

    }

    private void markSubtreeDirty() {

        dirty = true;
        if (children == null) {
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            children.get(index).markSubtreeDirty();
        }

    }

    private static void requireFinite(float value, String name) {

        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }

    }
}
