package com.samo.engine.assets.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AssetLoadErrorTest {
    private static final AssetId ASSET_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");

    @Test
    void preservesStructuredMissingContentDiagnostic() {

        AssetLoadError error = new AssetLoadError(ASSET_ID, AssetType.MESH, AssetLoadErrorCode.MISSING_CONTENT, "missing mesh");

        assertThat(error.assetId()).isEqualTo(ASSET_ID);
        assertThat(error.assetType()).isEqualTo(AssetType.MESH);
        assertThat(error.code()).isEqualTo(AssetLoadErrorCode.MISSING_CONTENT);
        assertThat(error.detail()).isEqualTo("missing mesh");
        assertThat(AssetLoadErrorCode.values()).containsExactly(AssetLoadErrorCode.MISSING_CONTENT, AssetLoadErrorCode.READ_FAILED, AssetLoadErrorCode.INVALID_CONTENT, AssetLoadErrorCode.HOT_RELOAD_FAILED);

    }

    @Test
    void rejectsNullFieldsAndBlankDetail() {

        assertThatThrownBy(() -> new AssetLoadError(null, AssetType.MESH, AssetLoadErrorCode.MISSING_CONTENT, "detail")).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("assetId");
        assertThatThrownBy(() -> new AssetLoadError(ASSET_ID, null, AssetLoadErrorCode.MISSING_CONTENT, "detail")).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("assetType");
        assertThatThrownBy(() -> new AssetLoadError(ASSET_ID, AssetType.MESH, null, "detail")).isInstanceOf(NullPointerException.class).hasMessageContaining("code");
        assertThatThrownBy(() -> new AssetLoadError(ASSET_ID, AssetType.MESH, AssetLoadErrorCode.MISSING_CONTENT, null)).isInstanceOf(NullPointerException.class)
            .hasMessageContaining("detail");
        assertThatThrownBy(() -> new AssetLoadError(ASSET_ID, AssetType.MESH, AssetLoadErrorCode.MISSING_CONTENT, "   ")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("detail");

    }

    @Test
    void publicSurfaceContainsNoNativeOrBackendTypes() {

        Set<String> methods = Set.of("assetId", "assetType", "code", "detail", "equals", "hashCode", "toString");
        assertThat(AssetLoadError.class.getDeclaredMethods()).extracting(Method::getName).containsAll(methods);
        assertThat(AssetLoadError.class.getRecordComponents()).allSatisfy(component -> {
            Class<?> type = component.getType();
            assertThat(type).isNotEqualTo(long.class).isNotEqualTo(int.class).isNotEqualTo(ByteBuffer.class);
            assertThat(type.getName()).doesNotStartWith("org.lwjgl").doesNotContain("opengl").doesNotContain("openal");
        });

    }
}
