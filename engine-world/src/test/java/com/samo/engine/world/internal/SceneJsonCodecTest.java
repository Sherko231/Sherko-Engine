package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.world.api.EntityGuid;
import org.junit.jupiter.api.Test;

class SceneJsonCodecTest {
    private static final String ROOT_GUID = "00000000-0000-0000-0000-000000000001";
    private static final String CHILD_GUID = "00000000-0000-0000-0000-000000000002";
    private static final String MESH_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MATERIAL_ID = "22222222-2222-2222-2222-222222222222";
    private static final String AUDIO_ID = "33333333-3333-3333-3333-333333333333";

    private static final String FULL_SCENE = """
        {
          "schemaVersion": 1,
          "entities": [
            {
              "guid": "00000000-0000-0000-0000-000000000001",
              "parentGuid": null,
              "components": {
                "transform": {
                  "schemaVersion": 1,
                  "position": [1.5, -2.0, 3.25],
                  "rotation": [0.0, 0.0, 0.0, 2.0],
                  "scale": [1.0, 2.0, 0.5]
                },
                "name": {
                  "schemaVersion": 1,
                  "name": "Root Entity"
                },
                "meshRenderer": {
                  "schemaVersion": 1,
                  "meshAssetId": "11111111-1111-1111-1111-111111111111",
                  "materialAssetId": "22222222-2222-2222-2222-222222222222"
                },
                "camera": {
                  "schemaVersion": 1,
                  "verticalFovRadians": 1.0,
                  "nearPlaneMeters": 0.1,
                  "farPlaneMeters": 100.0
                },
                "audioEmitter": {
                  "schemaVersion": 1,
                  "audioAssetId": "33333333-3333-3333-3333-333333333333"
                }
              }
            },
            {
              "guid": "00000000-0000-0000-0000-000000000002",
              "parentGuid": "00000000-0000-0000-0000-000000000001",
              "components": {
                "name": {
                  "schemaVersion": 1,
                  "name": "Child Entity"
                }
              }
            }
          ]
        }
        """;

    @Test
    void loadsFullSceneAndPreservesSemanticEqualityAcrossSaveReload() {

        SceneDocument first = SceneJsonCodec.decode(FULL_SCENE);

        assertThat(first.entities()).hasSize(2);
        SceneEntityDocument root = first.entities().get(0);
        assertThat(root.guid()).isEqualTo(EntityGuid.parse(ROOT_GUID));
        assertThat(root.parentGuid()).isNull();
        assertThat(root.components().name().name()).isEqualTo("Root Entity");
        assertThat(root.components().meshRenderer().meshAssetId()).isEqualTo(AssetId.parse(MESH_ID));
        assertThat(root.components().meshRenderer().materialAssetId()).isEqualTo(AssetId.parse(MATERIAL_ID));
        assertThat(root.components().audioEmitter().audioAssetId()).isEqualTo(AssetId.parse(AUDIO_ID));
        assertThat(root.components().camera().verticalFovRadians()).isEqualTo(1.0f);
        assertThat(root.components().camera().nearPlaneMeters()).isEqualTo(0.1f);
        assertThat(root.components().camera().farPlaneMeters()).isEqualTo(100.0f);

        SceneTransformData transform = root.components().transform();
        assertThat(transform.positionX()).isEqualTo(1.5f);
        assertThat(transform.positionY()).isEqualTo(-2.0f);
        assertThat(transform.positionZ()).isEqualTo(3.25f);
        assertThat(transform.rotationX()).isZero();
        assertThat(transform.rotationY()).isZero();
        assertThat(transform.rotationZ()).isZero();
        assertThat(transform.rotationW()).isEqualTo(1.0f);
        assertThat(transform.scaleX()).isEqualTo(1.0f);
        assertThat(transform.scaleY()).isEqualTo(2.0f);
        assertThat(transform.scaleZ()).isEqualTo(0.5f);

        SceneEntityDocument child = first.entities().get(1);
        assertThat(child.guid()).isEqualTo(EntityGuid.parse(CHILD_GUID));
        assertThat(child.parentGuid()).isEqualTo(EntityGuid.parse(ROOT_GUID));
        assertThat(child.components().name().name()).isEqualTo("Child Entity");
        assertThat(child.components().transform()).isNull();
        assertThat(child.components().meshRenderer()).isNull();
        assertThat(child.components().camera()).isNull();
        assertThat(child.components().audioEmitter()).isNull();

        String encoded = SceneJsonCodec.encode(first);
        SceneDocument second = SceneJsonCodec.decode(encoded);

        assertSceneSemanticallyEqual(first, second);
        assertThat(SceneJsonCodec.encode(second)).isEqualTo(encoded);
        assertThat(encoded.indexOf("\"transform\"")).isLessThan(encoded.indexOf("\"name\""));
        assertThat(encoded.indexOf("\"name\"")).isLessThan(encoded.indexOf("\"meshRenderer\""));
        assertThat(encoded.indexOf("\"meshRenderer\"")).isLessThan(encoded.indexOf("\"camera\""));
        assertThat(encoded.indexOf("\"camera\"")).isLessThan(encoded.indexOf("\"audioEmitter\""));

    }

    @Test
    void preservesEmptySceneAndEntityOrderWithAbsentOptionalComponents() {

        SceneDocument empty = SceneJsonCodec.decode("{\"schemaVersion\":1,\"entities\":[]}");
        assertThat(empty.entities()).isEmpty();
        assertThat(SceneJsonCodec.decode(SceneJsonCodec.encode(empty)).entities()).isEmpty();

        String ordered = """
            {
              "schemaVersion": 1,
              "entities": [
                {"guid":"00000000-0000-0000-0000-000000000002","parentGuid":null,"components":{}},
                {"guid":"00000000-0000-0000-0000-000000000001","parentGuid":null,"components":{}}
              ]
            }
            """;
        SceneDocument first = SceneJsonCodec.decode(ordered);
        SceneDocument second = SceneJsonCodec.decode(SceneJsonCodec.encode(first));

        assertThat(second.entities()).extracting(SceneEntityDocument::guid).containsExactly(EntityGuid.parse(CHILD_GUID), EntityGuid.parse(ROOT_GUID));
        assertThat(second.entities()).allSatisfy(entity -> assertThat(entity.components()).isEqualTo(SceneComponentsDocument.EMPTY));

    }

    @Test
    void rejectsDuplicateAndUnknownFields() {

        assertFormatFailure(
            "{\"schemaVersion\":1,\"schemaVersion\":1,\"entities\":[]}",
            "{\"schemaVersion\":1,\"entities\":[],\"extra\":true}",
            sceneWithEntityFields("\"guid\":\"" + ROOT_GUID + "\",\"parentGuid\":null,\"components\":{},\"extra\":true"),
            sceneWithComponents("{\"futureComponent\":{\"schemaVersion\":1}}"),
            sceneWithComponents("{\"name\":{\"schemaVersion\":1,\"name\":\"ok\",\"extra\":true}}"));

    }

    @Test
    void rejectsWrongShapesAndSchemaVersions() {

        assertFormatFailure(
            "{\"entities\":[]}",
            "{\"schemaVersion\":2,\"entities\":[]}",
            "{\"schemaVersion\":\"1\",\"entities\":[]}",
            "[]",
            "{\"schemaVersion\":1,\"entities\":{}}",
            "{\"schemaVersion\":1,\"entities\":[1]}",
            sceneWithEntityFields("\"guid\":\"" + ROOT_GUID + "\",\"parentGuid\":null,\"components\":[]"),
            sceneWithComponents("{\"transform\":{\"position\":[0,0,0],\"rotation\":[0,0,0,1],\"scale\":[1,1,1]}}"),
            sceneWithComponents("{\"name\":{\"schemaVersion\":2,\"name\":\"ok\"}}"));

    }

    @Test
    void rejectsMalformedDuplicateAndInvalidHierarchyGuids() {

        assertFormatFailure(
            sceneWithEntityFields("\"guid\":\"not-a-guid\",\"parentGuid\":null,\"components\":{}"),
            sceneWithEntityFields("\"guid\":\"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\"parentGuid\":null,\"components\":{}"),
            sceneWithEntityFields("\"guid\":\"" + ROOT_GUID + "\",\"parentGuid\":\"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\"components\":{}"),
            sceneWithEntities(
                "{\"guid\":\"" + ROOT_GUID + "\",\"parentGuid\":null,\"components\":{}},"
                    + "{\"guid\":\"" + ROOT_GUID + "\",\"parentGuid\":null,\"components\":{}}"),
            sceneWithEntityFields("\"guid\":\"" + CHILD_GUID + "\",\"parentGuid\":\"" + ROOT_GUID + "\",\"components\":{}"),
            sceneWithEntityFields("\"guid\":\"" + ROOT_GUID + "\",\"parentGuid\":\"" + ROOT_GUID + "\",\"components\":{}"),
            sceneWithEntities(
                "{\"guid\":\"" + ROOT_GUID + "\",\"parentGuid\":\"" + CHILD_GUID + "\",\"components\":{}},"
                    + "{\"guid\":\"" + CHILD_GUID + "\",\"parentGuid\":\"" + ROOT_GUID + "\",\"components\":{}}"));

    }

    @Test
    void rejectsInvalidTransformAndNameData() {

        assertFormatFailure(
            sceneWithComponents("{\"transform\":{\"schemaVersion\":1,\"position\":[0,0],\"rotation\":[0,0,0,1],\"scale\":[1,1,1]}}"),
            sceneWithComponents("{\"transform\":{\"schemaVersion\":1,\"position\":[0,\"x\",0],\"rotation\":[0,0,0,1],\"scale\":[1,1,1]}}"),
            sceneWithComponents("{\"transform\":{\"schemaVersion\":1,\"position\":[1e100,0,0],\"rotation\":[0,0,0,1],\"scale\":[1,1,1]}}"),
            sceneWithComponents("{\"transform\":{\"schemaVersion\":1,\"position\":[0,0,0],\"rotation\":[0,0,0,0],\"scale\":[1,1,1]}}"),
            sceneWithComponents("{\"name\":{\"schemaVersion\":1,\"name\":\"   \"}}"),
            sceneWithComponents("{\"name\":{\"schemaVersion\":1,\"name\":null}}"));

    }

    @Test
    void rejectsInvalidAssetReferencesAndCameraValues() {

        assertFormatFailure(
            sceneWithComponents("{\"meshRenderer\":{\"schemaVersion\":1,\"meshAssetId\":\"bad\",\"materialAssetId\":\"" + MATERIAL_ID + "\"}}"),
            sceneWithComponents("{\"meshRenderer\":{\"schemaVersion\":1,\"meshAssetId\":\"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\"materialAssetId\":\"" + MATERIAL_ID + "\"}}"),
            sceneWithComponents("{\"audioEmitter\":{\"schemaVersion\":1,\"audioAssetId\":\"bad\"}}"),
            sceneWithComponents("{\"camera\":{\"schemaVersion\":1,\"verticalFovRadians\":0,\"nearPlaneMeters\":0.1,\"farPlaneMeters\":100}}"),
            sceneWithComponents("{\"camera\":{\"schemaVersion\":1,\"verticalFovRadians\":1,\"nearPlaneMeters\":0,\"farPlaneMeters\":100}}"),
            sceneWithComponents("{\"camera\":{\"schemaVersion\":1,\"verticalFovRadians\":1,\"nearPlaneMeters\":1,\"farPlaneMeters\":1}}"),
            sceneWithComponents("{\"camera\":{\"schemaVersion\":1,\"verticalFovRadians\":1e100,\"nearPlaneMeters\":0.1,\"farPlaneMeters\":100}}"));

    }

    @Test
    void rejectsNullCodecArguments() {

        assertThatThrownBy(() -> SceneJsonCodec.decode(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("json");
        assertThatThrownBy(() -> SceneJsonCodec.encode(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("document");

    }

    private static void assertSceneSemanticallyEqual(SceneDocument expected, SceneDocument actual) {

        assertThat(actual.entities()).hasSameSizeAs(expected.entities());
        for (int index = 0; index < expected.entities().size(); index++) {
            SceneEntityDocument expectedEntity = expected.entities().get(index);
            SceneEntityDocument actualEntity = actual.entities().get(index);
            assertThat(actualEntity.guid()).isEqualTo(expectedEntity.guid());
            assertThat(actualEntity.parentGuid()).isEqualTo(expectedEntity.parentGuid());
            assertComponentsSemanticallyEqual(expectedEntity.components(), actualEntity.components());
        }

    }

    private static void assertComponentsSemanticallyEqual(SceneComponentsDocument expected, SceneComponentsDocument actual) {

        assertThat(actual.name()).isEqualTo(expected.name());
        assertThat(actual.meshRenderer()).isEqualTo(expected.meshRenderer());
        assertThat(actual.camera()).isEqualTo(expected.camera());
        assertThat(actual.audioEmitter()).isEqualTo(expected.audioEmitter());
        if (expected.transform() == null) {
            assertThat(actual.transform()).isNull();
            return;
        }
        assertThat(actual.transform()).isNotNull();
        assertTransformSemanticallyEqual(expected.transform(), actual.transform());

    }

    private static void assertTransformSemanticallyEqual(SceneTransformData expected, SceneTransformData actual) {

        assertThat(actual.positionX()).isCloseTo(expected.positionX(), offset(0.000001f));
        assertThat(actual.positionY()).isCloseTo(expected.positionY(), offset(0.000001f));
        assertThat(actual.positionZ()).isCloseTo(expected.positionZ(), offset(0.000001f));
        assertThat(actual.rotationX()).isCloseTo(expected.rotationX(), offset(0.000001f));
        assertThat(actual.rotationY()).isCloseTo(expected.rotationY(), offset(0.000001f));
        assertThat(actual.rotationZ()).isCloseTo(expected.rotationZ(), offset(0.000001f));
        assertThat(actual.rotationW()).isCloseTo(expected.rotationW(), offset(0.000001f));
        assertThat(actual.scaleX()).isCloseTo(expected.scaleX(), offset(0.000001f));
        assertThat(actual.scaleY()).isCloseTo(expected.scaleY(), offset(0.000001f));
        assertThat(actual.scaleZ()).isCloseTo(expected.scaleZ(), offset(0.000001f));

    }

    private static void assertFormatFailure(String... documents) {

        for (String document : documents) {
            assertThatThrownBy(() -> SceneJsonCodec.decode(document)).isInstanceOf(SceneFormatException.class);
        }

    }

    private static String sceneWithComponents(String componentsJson) {

        return sceneWithEntityFields("\"guid\":\"" + ROOT_GUID + "\",\"parentGuid\":null,\"components\":" + componentsJson);

    }

    private static String sceneWithEntityFields(String fields) {

        return sceneWithEntities("{" + fields + "}");

    }

    private static String sceneWithEntities(String entities) {

        return "{\"schemaVersion\":1,\"entities\":[" + entities + "]}";

    }
}
