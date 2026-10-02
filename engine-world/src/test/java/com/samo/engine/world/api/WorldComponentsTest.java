package com.samo.engine.world.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.core.api.Transform;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class WorldComponentsTest {
    @Test
    void createsEveryPlannedComponentEntirelyFromSceneLikeData() {

        SceneLikeData data = new SceneLikeData(1.0f, 2.0f, -3.0f, 0.0f, 0.0f, 0.0f, 2.0f, 1.0f, 1.0f, 1.0f, "Camera Rig",
            new AssetId(1L, 2L), new AssetId(3L, 4L), (float) Math.toRadians(60.0), 0.1f, 250.0f, new AssetId(5L, 6L));

        TransformComponent transformComponent = new TransformComponent(data.positionX(), data.positionY(), data.positionZ(), data.rotationX(), data.rotationY(),
            data.rotationZ(), data.rotationW(), data.scaleX(), data.scaleY(), data.scaleZ());
        NameComponent nameComponent = new NameComponent(data.name());
        MeshRendererComponent meshRendererComponent = new MeshRendererComponent(data.meshAssetId(), data.materialAssetId());
        CameraComponent cameraComponent = new CameraComponent(data.verticalFovRadians(), data.nearPlaneMeters(), data.farPlaneMeters());
        AudioEmitterComponent audioEmitterComponent = new AudioEmitterComponent(data.audioAssetId());

        Transform transform = transformComponent.transform();
        assertThat(transform.parent()).isNull();
        assertThat(transform.localPosition(new Vector3f())).isEqualTo(new Vector3f(1.0f, 2.0f, -3.0f));
        assertThat(transform.localRotation(new Quaternionf())).isEqualTo(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f));
        assertThat(transform.localScale(new Vector3f())).isEqualTo(new Vector3f(1.0f, 1.0f, 1.0f));
        assertThat(nameComponent.name()).isEqualTo("Camera Rig");
        assertThat(meshRendererComponent.meshAssetId()).isEqualTo(data.meshAssetId());
        assertThat(meshRendererComponent.materialAssetId()).isEqualTo(data.materialAssetId());
        assertThat(cameraComponent.verticalFovRadians()).isEqualTo(data.verticalFovRadians());
        assertThat(cameraComponent.nearPlaneMeters()).isEqualTo(data.nearPlaneMeters());
        assertThat(cameraComponent.farPlaneMeters()).isEqualTo(data.farPlaneMeters());
        assertThat(audioEmitterComponent.audioAssetId()).isEqualTo(data.audioAssetId());

    }

    @Test
    void transformComponentDelegatesValidationAndQuaternionNormalizationToCanonicalTransform() {

        TransformComponent normalized = new TransformComponent(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 4.0f, 1.0f, 1.0f, 1.0f);

        assertThat(normalized.transform().localRotation(new Quaternionf())).isEqualTo(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f));
        assertThatThrownBy(() -> new TransformComponent(Float.NaN, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("position.x");
        assertThatThrownBy(() -> new TransformComponent(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("rotation quaternion");
        assertThatThrownBy(() -> new TransformComponent(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, Float.POSITIVE_INFINITY, 1.0f, 1.0f))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("scale.x");

    }

    @Test
    void nameComponentRejectsNullAndBlankWithoutNormalizingAcceptedText() {

        NameComponent component = new NameComponent("  Display Name  ");

        assertThat(component.name()).isEqualTo("  Display Name  ");
        assertThatThrownBy(() -> new NameComponent(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
        assertThatThrownBy(() -> new NameComponent(" \t\n ")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("blank");

    }

    @Test
    void assetReferenceComponentsRejectNullWithoutResolvingAssets() {

        AssetId mesh = new AssetId(10L, 11L);
        AssetId material = new AssetId(12L, 13L);
        AssetId audio = new AssetId(14L, 15L);

        assertThat(new MeshRendererComponent(mesh, material)).isEqualTo(new MeshRendererComponent(mesh, material));
        assertThat(new AudioEmitterComponent(audio)).isEqualTo(new AudioEmitterComponent(audio));
        assertThatThrownBy(() -> new MeshRendererComponent(null, material)).isInstanceOf(NullPointerException.class).hasMessageContaining("meshAssetId");
        assertThatThrownBy(() -> new MeshRendererComponent(mesh, null)).isInstanceOf(NullPointerException.class).hasMessageContaining("materialAssetId");
        assertThatThrownBy(() -> new AudioEmitterComponent(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("audioAssetId");

    }

    @Test
    void cameraComponentEnforcesAcceptedPerspectiveDomainWithoutStoringAspect() {

        CameraComponent nearBoundary = new CameraComponent(Math.nextDown((float) Math.PI), Float.MIN_VALUE, Math.nextUp(Float.MIN_VALUE));

        assertThat(nearBoundary.verticalFovRadians()).isLessThan((float) Math.PI);
        assertThat(nearBoundary.nearPlaneMeters()).isPositive();
        assertThat(nearBoundary.farPlaneMeters()).isGreaterThan(nearBoundary.nearPlaneMeters());

        assertThatThrownBy(() -> new CameraComponent(Float.NaN, 0.1f, 10.0f)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("verticalFovRadians");
        assertThatThrownBy(() -> new CameraComponent(0.0f, 0.1f, 10.0f)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("verticalFovRadians");
        assertThatThrownBy(() -> new CameraComponent(-0.1f, 0.1f, 10.0f)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("verticalFovRadians");
        assertThatThrownBy(() -> new CameraComponent((float) Math.PI, 0.1f, 10.0f)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("verticalFovRadians");
        assertThatThrownBy(() -> new CameraComponent(1.0f, Float.POSITIVE_INFINITY, 10.0f)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nearPlaneMeters");
        assertThatThrownBy(() -> new CameraComponent(1.0f, 0.0f, 10.0f)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nearPlaneMeters");
        assertThatThrownBy(() -> new CameraComponent(1.0f, 1.0f, Float.NEGATIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("farPlaneMeters");
        assertThatThrownBy(() -> new CameraComponent(1.0f, 1.0f, 1.0f)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("farPlaneMeters");

    }

    private record SceneLikeData(float positionX, float positionY, float positionZ, float rotationX, float rotationY, float rotationZ, float rotationW, float scaleX,
        float scaleY, float scaleZ, String name, AssetId meshAssetId, AssetId materialAssetId, float verticalFovRadians, float nearPlaneMeters, float farPlaneMeters,
        AssetId audioAssetId) {
    }
}
