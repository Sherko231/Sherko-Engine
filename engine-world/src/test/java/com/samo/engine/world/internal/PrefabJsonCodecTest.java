package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.world.api.EntityGuid;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrefabJsonCodecTest {
    private static final String GUID = "00000000-0000-0000-0000-000000000001";
    private static final String ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void preservesAuthoredSceneEditorMetadataAndOverridesOnRoundTrip() {

        String json = """
            {
              "schemaVersion": 1,
              "scene": {
                "schemaVersion": 1,
                "entities": [{
                  "guid": "00000000-0000-0000-0000-000000000001",
                  "parentGuid": null,
                  "components": {"name": {"schemaVersion": 1, "name": "Parent"}},
                  "editorData": {}
                }],
                "editorData": {"tools": ["grid", {"snap": 0.5}]}
              },
              "instances": [{
                "instanceKey": "lamp_1",
                "prefabAssetId": "11111111-1111-1111-1111-111111111111",
                "parentGuid": "00000000-0000-0000-0000-000000000001",
                "overrides": [{
                  "instancePath": ["bulb"],
                  "entityGuid": "00000000-0000-0000-0000-000000000001",
                  "component": "name",
                  "property": "name",
                  "value": "Changed"
                }]
              }]
            }
            """;

        PrefabDocument first = PrefabJsonCodec.decode(json);
        assertThat(first.scene().editorData().jsonObject()).isEqualTo("{\"tools\":[\"grid\",{\"snap\":0.5}]}");
        assertThat(first.scene().entities().get(0).editorData().jsonObject()).isEqualTo("{}");
        assertThat(first.instances()).hasSize(1);
        PrefabInstanceDocument instance = first.instances().get(0);
        assertThat(instance.instanceKey()).isEqualTo("lamp_1");
        assertThat(instance.prefabAssetId()).isEqualTo(AssetId.parse(ID));
        assertThat(instance.parentGuid()).isEqualTo(EntityGuid.parse(GUID));
        assertThat(instance.overrides()).hasSize(1);
        assertThat(instance.overrides().get(0).instancePath()).containsExactly("bulb");
        assertThat(instance.overrides().get(0).valueJson()).isEqualTo("\"Changed\"");

        String saved = PrefabJsonCodec.encode(first);
        assertThat(PrefabJsonCodec.decode(saved)).isEqualTo(first);
        assertThat(PrefabJsonCodec.encode(PrefabJsonCodec.decode(saved))).isEqualTo(saved);
        assertThat(saved.indexOf("\"scene\"")).isLessThan(saved.indexOf("\"instances\""));

    }

    @Test
    void acceptsEmptySceneAndInstancesAndPreservesImmutableValueLists() {

        PrefabDocument decoded = PrefabJsonCodec.decode(emptyPrefab());
        assertThat(decoded.scene().entities()).isEmpty();
        assertThat(decoded.instances()).isEmpty();
        assertThat(PrefabJsonCodec.decode(PrefabJsonCodec.encode(decoded))).isEqualTo(decoded);

        PrefabInstanceDocument instance = new PrefabInstanceDocument("foo", AssetId.parse(ID), null, List.of());
        assertThat(new PrefabDocument(decoded.scene(), List.of(instance)).instances()).hasSize(1);

    }

    @Test
    void rejectsInvalidSchemasShapesAndStrictUnknownOrDuplicateFields() {

        assertFailure("{}", "{\"schemaVersion\":2,\"scene\":{\"schemaVersion\":1,\"entities\":[]},\"instances\":[]}",
            "{\"schemaVersion\":\"1\",\"scene\":{\"schemaVersion\":1,\"entities\":[]},\"instances\":[]}",
            "{\"schemaVersion\":1,\"scene\":{\"schemaVersion\":1,\"entities\":[]},\"instances\":{},\"extra\":true}",
            "{\"schemaVersion\":1,\"scene\":{\"schemaVersion\":1,\"entities\":[]},\"instances\":[],\"extra\":true}",
            "{\"schemaVersion\":1,\"schemaVersion\":1,\"scene\":{\"schemaVersion\":1,\"entities\":[]},\"instances\":[]}",
            "{\"schemaVersion\":1,\"scene\":{\"schemaVersion\":1,\"entities\":[],\"bad\":true},\"instances\":[]}", emptyPrefab() + " {}");

    }

    @Test
    void rejectsUnknownAndDuplicateInstanceFieldsAndInvalidAttachment() {

        assertFailure(withInstances("[{\"instanceKey\":\"x\",\"prefabAssetId\":\"" + ID + "\",\"parentGuid\":null,\"overrides\":[],\"unknown\":0}]"),
            withInstances("[{\"instanceKey\":\"X\",\"prefabAssetId\":\"" + ID + "\",\"parentGuid\":null,\"overrides\":[]}]"),
            withInstances("[{\"instanceKey\":\"x\",\"prefabAssetId\":\"bad\",\"parentGuid\":null,\"overrides\":[]}]"),
            withInstances("[{\"instanceKey\":\"x\",\"prefabAssetId\":\"" + ID + "\",\"parentGuid\":null,\"overrides\":[]}," + "{\"instanceKey\":\"x\",\"prefabAssetId\":\"" + ID
                + "\",\"parentGuid\":null,\"overrides\":[]}]"),
            withInstances("[{\"instanceKey\":\"x\",\"prefabAssetId\":\"" + ID + "\",\"parentGuid\":\"" + GUID + "\",\"overrides\":[]}]"),
            withInstances("[{\"instanceKey\":\"x\",\"instanceKey\":\"y\",\"prefabAssetId\":\"" + ID + "\",\"parentGuid\":null,\"overrides\":[]}]"));

    }

    @Test
    void rejectsUnsupportedDuplicateOrMalformedOverrideTargets() {

        String prefix = "{\"instancePath\":[],\"entityGuid\":\"" + GUID + "\",\"component\":\"name\",\"property\":\"name\",\"value\":\"ok\"}";
        assertFailure(withOverrides("[{\"instancePath\":[],\"entityGuid\":\"" + GUID + "\",\"component\":\"name\",\"property\":\"schemaVersion\",\"value\":1}]"),
            withOverrides("[{\"instancePath\":[],\"entityGuid\":\"" + GUID + "\",\"component\":\"unknown\",\"property\":\"name\",\"value\":\"x\"}]"),
            withOverrides("[{\"instancePath\":[123],\"entityGuid\":\"" + GUID + "\",\"component\":\"name\",\"property\":\"name\",\"value\":\"x\"}]"),
            withOverrides("[{\"instancePath\":[],\"entityGuid\":\"" + GUID + "\",\"component\":\"name\",\"property\":\"name\"}]"), withOverrides("[" + prefix + "," + prefix + "]"),
            withOverrides("[{\"instancePath\":[],\"entityGuid\":\"" + GUID + "\",\"component\":\"name\",\"property\":\"name\",\"value\":\"x\",\"extra\":1}]"));

    }

    private static String emptyPrefab() {

        return "{\"schemaVersion\":1,\"scene\":{\"schemaVersion\":1,\"entities\":[]},\"instances\":[]}";

    }

    private static String withInstances(String instances) {

        return "{\"schemaVersion\":1,\"scene\":{\"schemaVersion\":1,\"entities\":[]},\"instances\":" + instances + "}";

    }

    private static String withOverrides(String overrides) {

        return withInstances("[{\"instanceKey\":\"x\",\"prefabAssetId\":\"" + ID + "\",\"parentGuid\":null,\"overrides\":" + overrides + "}]");

    }

    private static void assertFailure(String... jsonValues) {

        for (String json : jsonValues) {
            assertThatThrownBy(() -> PrefabJsonCodec.decode(json)).as(json).isInstanceOf(PrefabFormatException.class);
        }

    }
}
