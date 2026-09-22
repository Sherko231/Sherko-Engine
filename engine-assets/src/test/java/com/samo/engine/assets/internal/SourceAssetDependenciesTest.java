package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceAssetDependenciesTest {
    private static final String FIRST_ID = "00000000-0000-0000-0000-000000000001";
    private static final String SECOND_ID = "00000000-0000-0000-0000-000000000002";

    @TempDir
    Path tempDir;

    @Test
    void loadsStrictVersionOneDependencies() throws Exception {

        Path path = tempDir.resolve("material.deps.json");
        Files.writeString(path, """
            {
              "schemaVersion": 1,
              "assetDependencies": ["%s", "%s"],
              "shaderDependencies": ["opaque-baseline", "normal.map"]
            }
            """.formatted(FIRST_ID, SECOND_ID));

        SourceAssetDependencies dependencies = SourceAssetDependenciesJson.load(path);

        assertThat(dependencies.assetDependencies()).containsExactly(AssetId.parse(FIRST_ID), AssetId.parse(SECOND_ID));
        assertThat(dependencies.shaderDependencies()).containsExactly("opaque-baseline", "normal.map");

    }

    @Test
    void rejectsSchemaAndValueViolationsWithPath() throws Exception {

        assertRejected("unknown", """
            {"schemaVersion":1,"assetDependencies":[],"shaderDependencies":[],"extra":1}
            """, "unknown field");
        assertRejected("version", """
            {"schemaVersion":2,"assetDependencies":[],"shaderDependencies":[]}
            """, "upgrade required");
        assertRejected("missing", """
            {"schemaVersion":1,"assetDependencies":[]}
            """, "missing required field");
        assertRejected("duplicate-field", """
            {"schemaVersion":1,"assetDependencies":[],"assetDependencies":[],"shaderDependencies":[]}
            """, "duplicate");
        assertRejected("duplicate-asset", """
            {"schemaVersion":1,"assetDependencies":["%s","%s"],"shaderDependencies":[]}
            """.formatted(FIRST_ID, FIRST_ID), "duplicate asset dependency");
        assertRejected("duplicate-shader", """
            {"schemaVersion":1,"assetDependencies":[],"shaderDependencies":["opaque","opaque"]}
            """, "duplicate shader dependency");
        assertRejected("bad-shader", """
            {"schemaVersion":1,"assetDependencies":[],"shaderDependencies":["Bad/Path"]}
            """, "shaderDependencies");
        assertRejected("bad-id", """
            {"schemaVersion":1,"assetDependencies":["NOT-AN-ID"],"shaderDependencies":[]}
            """, "canonical lowercase AssetId");

    }

    private void assertRejected(String name, String json, String message) throws Exception {

        Path path = tempDir.resolve(name + ".deps.json");
        Files.writeString(path, json);
        assertThatThrownBy(() -> SourceAssetDependenciesJson.load(path)).isInstanceOf(RuntimeException.class).hasMessageContaining(path.toString()).hasMessageContaining(message);

    }
}
