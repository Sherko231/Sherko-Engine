package com.samo.engine.world.internal;

import com.samo.engine.core.api.Transform;
import com.samo.engine.world.api.TransformComponent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

record SceneTransformData(float positionX, float positionY, float positionZ, float rotationX, float rotationY, float rotationZ, float rotationW, float scaleX, float scaleY,
    float scaleZ) {

    SceneTransformData {

        TransformComponent component = new TransformComponent(positionX, positionY, positionZ, rotationX, rotationY, rotationZ, rotationW, scaleX, scaleY, scaleZ);
        Transform transform = component.transform();

        Vector3f position = transform.localPosition(new Vector3f());
        Quaternionf rotation = transform.localRotation(new Quaternionf());
        Vector3f scale = transform.localScale(new Vector3f());

        positionX = position.x;
        positionY = position.y;
        positionZ = position.z;
        rotationX = rotation.x;
        rotationY = rotation.y;
        rotationZ = rotation.z;
        rotationW = rotation.w;
        scaleX = scale.x;
        scaleY = scale.y;
        scaleZ = scale.z;

    }

    TransformComponent toComponent() {

        return new TransformComponent(positionX, positionY, positionZ, rotationX, rotationY, rotationZ, rotationW, scaleX, scaleY, scaleZ);

    }
}
