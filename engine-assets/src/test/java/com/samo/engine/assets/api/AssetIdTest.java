package com.samo.engine.assets.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AssetIdTest {
    @Test
    void preservesAll128BitsAcrossCanonicalTextRoundTrip() {

        AssetId id = new AssetId(0x0123456789ABCDEFL, 0xFEDCBA9876543210L);

        String text = id.toString();
        AssetId parsed = AssetId.parse(text);

        assertThat(text).isEqualTo("01234567-89ab-cdef-fedc-ba9876543210");
        assertThat(parsed).isEqualTo(id);
        assertThat(parsed.highBits()).isEqualTo(0x0123456789ABCDEFL);
        assertThat(parsed.lowBits()).isEqualTo(0xFEDCBA9876543210L);

    }

    @Test
    void generatedIdentityIsCanonicalAndPathIndependent() {

        AssetId generated = AssetId.generate();

        assertThat(generated.toString()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(AssetId.parse(generated.toString())).isEqualTo(generated);

    }

    @Test
    void movingSourceWithItsIdentityLeavesExistingReferenceUnchanged() {

        AssetId id = new AssetId(0x13579BDF2468ACE0L, 0x02468ACE13579BDFL);
        AssetReference existingReference = new AssetReference(id);
        MetadataLikeEntry original = new MetadataLikeEntry(Path.of("props/crate.glb"), id);

        MetadataLikeEntry moved = original.moveTo(Path.of("environment/props/crate.glb"));

        assertThat(moved.sourcePath()).isNotEqualTo(original.sourcePath());
        assertThat(moved.assetId()).isEqualTo(original.assetId());
        assertThat(existingReference.assetId()).isEqualTo(moved.assetId());

    }

    @Test
    void acceptsAllZeroIdentityWithoutInventingSentinelSemantics() {

        AssetId zero = new AssetId(0L, 0L);

        assertThat(zero.toString()).isEqualTo("00000000-0000-0000-0000-000000000000");
        assertThat(AssetId.parse(zero.toString())).isEqualTo(zero);

    }

    @Test
    void rejectsNullAndNonCanonicalText() {

        assertThatThrownBy(() -> AssetId.parse(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("text");
        assertThatThrownBy(() -> AssetId.parse("01234567-89AB-CDEF-FEDC-BA9876543210")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssetId.parse("1234")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssetId.parse("0123456789abcdef0123456789abcdef")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssetId.parse("g1234567-89ab-cdef-fedc-ba9876543210")).isInstanceOf(IllegalArgumentException.class);

    }

    private record AssetReference(AssetId assetId) {
    }

    private record MetadataLikeEntry(Path sourcePath, AssetId assetId) {
        private MetadataLikeEntry moveTo(Path newSourcePath) {

            return new MetadataLikeEntry(newSourcePath, assetId);

        }
    }
}
