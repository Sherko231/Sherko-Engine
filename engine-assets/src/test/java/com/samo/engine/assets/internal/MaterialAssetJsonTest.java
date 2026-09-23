package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.MaterialAsset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MaterialAssetJsonTest {
    @Test
    void decodesExactSchemaAndRejectsInvalidContracts() {

        MaterialAsset material = MaterialAssetJson.decode(validMaterial(0.25f, "opaque-baseline"));

        assertThat(material.shaderKey()).isEqualTo("opaque-baseline");
        assertThat(material.redMultiplier()).isEqualTo(0.25f);
        assertThat(material.greenMultiplier()).isEqualTo(0.5f);
        assertThat(material.blueMultiplier()).isEqualTo(0.75f);
        assertThat(material.alphaMultiplier()).isEqualTo(1.0f);

        assertThatThrownBy(() -> MaterialAssetJson.decode("{}".getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MaterialAssetJson.decode(validMaterial(0.25f, "Bad/Path"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MaterialAssetJson.decode("""
            {"schemaVersion":2,"shaderKey":"opaque-baseline","redMultiplier":1,"greenMultiplier":1,"blueMultiplier":1,"alphaMultiplier":1}
            """.getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MaterialAssetJson.decode("""
            {"schemaVersion":1,"shaderKey":"opaque-baseline","redMultiplier":2,"greenMultiplier":1,"blueMultiplier":1,"alphaMultiplier":1}
            """.getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MaterialAssetJson.decode("""
            {"schemaVersion":1,"shaderKey":"opaque-baseline","redMultiplier":1,"greenMultiplier":1,"blueMultiplier":1,"alphaMultiplier":1,"extra":1}
            """.getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);

    }

    private static byte[] validMaterial(float red, String shaderKey) {

        return """
            {
              "schemaVersion": 1,
              "shaderKey": "%s",
              "redMultiplier": %s,
              "greenMultiplier": 0.5,
              "blueMultiplier": 0.75,
              "alphaMultiplier": 1.0
            }
            """.formatted(shaderKey, Float.toString(red)).getBytes(StandardCharsets.UTF_8);

    }
}
