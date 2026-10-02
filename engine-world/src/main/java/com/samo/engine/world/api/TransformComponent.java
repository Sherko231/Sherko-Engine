package com.samo.engine.world.api;

import com.samo.engine.core.api.Transform;

/**
 * World transform component backed by the canonical mutable engine transform.
 *
 * <p>
 * Construction copies explicit local transform data into a fresh owned {@link Transform}. The
 * component does not accept a caller-owned mutable transform and starts without a parent.
 */
public final class TransformComponent {
    private final Transform transform;

    /**
     * Creates a component from explicit local transform data.
     *
     * @param positionX
     *            local X position in meters
     * @param positionY
     *            local Y position in meters
     * @param positionZ
     *            local Z position in meters
     * @param rotationX
     *            local quaternion X component
     * @param rotationY
     *            local quaternion Y component
     * @param rotationZ
     *            local quaternion Z component
     * @param rotationW
     *            local quaternion W component
     * @param scaleX
     *            local dimensionless X scale
     * @param scaleY
     *            local dimensionless Y scale
     * @param scaleZ
     *            local dimensionless Z scale
     */
    public TransformComponent(float positionX, float positionY, float positionZ, float rotationX, float rotationY, float rotationZ, float rotationW,
        float scaleX, float scaleY, float scaleZ) {

        Transform ownedTransform = new Transform();
        ownedTransform.setLocalPosition(positionX, positionY, positionZ);
        ownedTransform.setLocalRotation(rotationX, rotationY, rotationZ, rotationW);
        ownedTransform.setLocalScale(scaleX, scaleY, scaleZ);
        transform = ownedTransform;

    }

    /**
     * Returns this component's owned mutable canonical transform.
     *
     * @return the owned transform
     */
    public Transform transform() {

        return transform;

    }
}
