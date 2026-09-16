package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Mutable local/world transform using the engine's canonical right-handed spatial convention.
 *
 * <p>Local composition is {@code T * R * S}; world composition is
 * {@code parentWorld * local}. Instances are externally serialized and do not retain caller-owned
 * JOML value/destination objects. Parent graphs must remain acyclic; explicit cycle rejection is
 * owned by P4-T04.
 */
public final class Transform {
    private static final long NO_PARENT_REVISION = -1L;

    private final Vector3f localPosition = new Vector3f();
    private final Quaternionf localRotation = new Quaternionf();
    private final Vector3f localScale = new Vector3f(1.0f, 1.0f, 1.0f);
    private final Matrix4f localMatrix = new Matrix4f();
    private final Matrix4f cachedWorldMatrix = new Matrix4f();

    private Transform parent;
    private boolean dirty = true;
    private long worldRevision;
    private long cachedParentWorldRevision = NO_PARENT_REVISION;

    public Transform parent() {
        return parent;
    }

    public void setParent(Transform parent) {
        if (this.parent == parent) {
            return;
        }
        this.parent = parent;
        markDirty();
    }

    public Vector3f localPosition(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(localPosition);
    }

    public void setLocalPosition(float x, float y, float z) {
        requireFinite(x, "position.x");
        requireFinite(y, "position.y");
        requireFinite(z, "position.z");
        localPosition.set(x, y, z);
        markDirty();
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
        localRotation.set(
                (float) (x * inverseLength),
                (float) (y * inverseLength),
                (float) (z * inverseLength),
                (float) (w * inverseLength));
        markDirty();
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
        markDirty();
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

    private void ensureWorldMatrixCurrent() {
        long parentRevision = NO_PARENT_REVISION;
        if (parent != null) {
            parent.ensureWorldMatrixCurrent();
            parentRevision = parent.worldRevision;
        }

        if (!dirty && cachedParentWorldRevision == parentRevision) {
            return;
        }

        localMatrix.identity().translate(localPosition).rotate(localRotation).scale(localScale);
        if (parent == null) {
            cachedWorldMatrix.set(localMatrix);
        } else {
            parent.cachedWorldMatrix.mul(localMatrix, cachedWorldMatrix);
        }

        cachedParentWorldRevision = parentRevision;
        dirty = false;
        worldRevision++;
    }

    private void markDirty() {
        dirty = true;
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
