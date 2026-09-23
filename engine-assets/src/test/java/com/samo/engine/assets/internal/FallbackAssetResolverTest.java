package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.AssetLoadErrorCode;
import com.samo.engine.assets.api.AssetType;
import com.samo.engine.assets.api.ResourceHandleState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FallbackAssetResolverTest {
    private static final AssetId ASSET_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");

    @Test
    void rejectsNullRequestedIdentityForEveryFallbackCategory() {

        assertThatThrownBy(() -> FallbackAssetResolver.missingMesh(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("assetId");
        assertThatThrownBy(() -> FallbackAssetResolver.missingTexture(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("assetId");
        assertThatThrownBy(() -> FallbackAssetResolver.missingMaterial(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("assetId");
        assertThatThrownBy(() -> FallbackAssetResolver.missingSound(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("assetId");

    }

    @Test
    void missingMeshReturnsReadyOneMeterClosedCubeAndStructuredError() {

        FallbackResolution<List<EngineMesh>> resolution = FallbackAssetResolver.missingMesh(ASSET_ID);

        assertReadyMissing(resolution, AssetType.MESH);
        List<EngineMesh> meshes = resolution.handle().requireReady();
        assertThat(meshes).hasSize(1);

        EngineMesh mesh = meshes.getFirst();
        assertThat(mesh.name()).isEqualTo("FallbackCube");
        float[] positions = mesh.positions();
        int[] indices = mesh.indices();
        assertThat(positions).hasSize(24);
        assertThat(indices).hasSize(36);
        for (int index : indices) {
            assertThat(index).isBetween(0, 7);
        }

        assertAxisBounds(positions, 0);
        assertAxisBounds(positions, 1);
        assertAxisBounds(positions, 2);
        assertThat(uniqueVertices(positions)).hasSize(8);

        Map<String, Integer> edgeUse = new HashMap<>();
        for (int triangle = 0; triangle < indices.length; triangle += 3) {
            int a = indices[triangle];
            int b = indices[triangle + 1];
            int c = indices[triangle + 2];
            assertThat(a).isNotEqualTo(b);
            assertThat(b).isNotEqualTo(c);
            assertThat(c).isNotEqualTo(a);
            addEdge(edgeUse, a, b);
            addEdge(edgeUse, b, c);
            addEdge(edgeUse, c, a);
        }
        assertThat(edgeUse).hasSize(18);
        assertThat(edgeUse.values()).allMatch(count -> count == 2);

    }

    @Test
    void missingTextureReturnsExactMagentaBlackCheckerAndCompleteMip() {

        FallbackResolution<CookedTexture> resolution = FallbackAssetResolver.missingTexture(ASSET_ID);

        assertReadyMissing(resolution, AssetType.TEXTURE);
        CookedTexture texture = resolution.handle().requireReady();
        assertThat(texture.width()).isEqualTo(2);
        assertThat(texture.height()).isEqualTo(2);
        assertThat(texture.mipLevels()).hasSize(2);
        assertThat(texture.mipLevels().get(0).rgba8()).containsExactly(
            (byte) 255, (byte) 0, (byte) 255, (byte) 255,
            (byte) 0, (byte) 0, (byte) 0, (byte) 255,
            (byte) 0, (byte) 0, (byte) 0, (byte) 255,
            (byte) 255, (byte) 0, (byte) 255, (byte) 255);
        assertThat(texture.mipLevels().get(1).width()).isEqualTo(1);
        assertThat(texture.mipLevels().get(1).height()).isEqualTo(1);
        assertThat(texture.mipLevels().get(1).rgba8()).containsExactly((byte) 127, (byte) 0, (byte) 127, (byte) 255);

    }

    @Test
    void missingMaterialReturnsExactFiniteOpaqueMagenta() {

        FallbackResolution<FallbackMaterial> resolution = FallbackAssetResolver.missingMaterial(ASSET_ID);

        assertReadyMissing(resolution, AssetType.MATERIAL);
        FallbackMaterial material = resolution.handle().requireReady();
        assertThat(material.red()).isEqualTo(1.0f);
        assertThat(material.green()).isZero();
        assertThat(material.blue()).isEqualTo(1.0f);
        assertThat(material.alpha()).isEqualTo(1.0f);
        assertThat(Float.isFinite(material.red())).isTrue();
        assertThat(Float.isFinite(material.green())).isTrue();
        assertThat(Float.isFinite(material.blue())).isTrue();
        assertThat(Float.isFinite(material.alpha())).isTrue();

    }

    @Test
    void missingSoundReturnsBoundedNonSilentMonoPcmWithRepeatedCrossings() {

        FallbackResolution<FallbackSound> resolution = FallbackAssetResolver.missingSound(ASSET_ID);

        assertReadyMissing(resolution, AssetType.AUDIO);
        FallbackSound sound = resolution.handle().requireReady();
        short[] samples = sound.pcm16();
        assertThat(sound.channels()).isEqualTo(1);
        assertThat(sound.sampleRate()).isEqualTo(22050);
        assertThat(samples.length).isEqualTo(220);
        boolean hasPositive = false;
        boolean hasNegative = false;
        for (short sample : samples) {
            hasPositive |= sample > 0;
            hasNegative |= sample < 0;
            assertThat(Math.abs((int) sample)).isLessThanOrEqualTo(12000);
        }
        assertThat(hasPositive).isTrue();
        assertThat(hasNegative).isTrue();
        double durationSeconds = samples.length / (double) sound.sampleRate();
        assertThat(durationSeconds).isGreaterThanOrEqualTo(0.009).isLessThanOrEqualTo(0.011);
        assertThat(nonZeroSignCrossings(samples)).isGreaterThan(20);

    }

    @Test
    void repeatedFallbacksAreDeterministicWithIndependentHandleLifetimes() {

        FallbackResolution<CookedTexture> first = FallbackAssetResolver.missingTexture(ASSET_ID);
        FallbackResolution<CookedTexture> second = FallbackAssetResolver.missingTexture(ASSET_ID);

        assertThat(first.handle()).isNotSameAs(second.handle());
        assertThat(first.handle().requireReady().mipLevels().get(0).rgba8()).isEqualTo(second.handle().requireReady().mipLevels().get(0).rgba8());

        first.handle().close();
        assertThat(first.handle().state()).isEqualTo(ResourceHandleState.RELEASED);
        assertThat(second.handle().state()).isEqualTo(ResourceHandleState.READY);
        assertThat(second.handle().readyValue()).isPresent();

        FallbackResolution<CookedTexture> third = FallbackAssetResolver.missingTexture(ASSET_ID);
        assertThat(third.handle().state()).isEqualTo(ResourceHandleState.READY);
        assertThat(third.handle().requireReady().mipLevels().get(0).rgba8()).isEqualTo(second.handle().requireReady().mipLevels().get(0).rgba8());

    }

    private static void assertReadyMissing(FallbackResolution<?> resolution, AssetType type) {

        assertThat(resolution.handle().assetId()).isEqualTo(ASSET_ID);
        assertThat(resolution.handle().state()).isEqualTo(ResourceHandleState.READY);
        assertThat(resolution.handle().readyValue()).isPresent();
        assertThat(resolution.error().assetId()).isEqualTo(ASSET_ID);
        assertThat(resolution.error().assetType()).isEqualTo(type);
        assertThat(resolution.error().code()).isEqualTo(AssetLoadErrorCode.MISSING_CONTENT);
        assertThat(resolution.error().detail()).contains(ASSET_ID.toString()).contains(type.name());

    }

    private static void assertAxisBounds(float[] positions, int axis) {

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int index = axis; index < positions.length; index += 3) {
            min = Math.min(min, positions[index]);
            max = Math.max(max, positions[index]);
        }
        assertThat(min).isEqualTo(-0.5f);
        assertThat(max).isEqualTo(0.5f);
        assertThat(max - min).isEqualTo(1.0f);

    }

    private static java.util.Set<String> uniqueVertices(float[] positions) {

        java.util.Set<String> vertices = new java.util.HashSet<>();
        for (int index = 0; index < positions.length; index += 3) {
            vertices.add(positions[index] + "," + positions[index + 1] + "," + positions[index + 2]);
        }
        return vertices;

    }

    private static void addEdge(Map<String, Integer> edges, int left, int right) {

        int min = Math.min(left, right);
        int max = Math.max(left, right);
        edges.merge(min + ":" + max, 1, Integer::sum);

    }

    private static int nonZeroSignCrossings(short[] samples) {

        int crossings = 0;
        int previousSign = 0;
        for (short sample : samples) {
            int sign = Integer.compare(sample, 0);
            if (sign == 0) {
                continue;
            }
            if (previousSign != 0 && sign != previousSign) {
                crossings++;
            }
            previousSign = sign;
        }
        return crossings;

    }
}
