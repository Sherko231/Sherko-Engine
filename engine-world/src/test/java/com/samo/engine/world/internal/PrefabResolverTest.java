package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.world.api.EntityGuid;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrefabResolverTest {
    private static final String ENTITY = "00000000-0000-0000-0000-000000000001";
    private static final String MISSING_ENTITY = "00000000-0000-0000-0000-000000000009";
    private static final String ROOM = "44444444-4444-4444-4444-444444444444";
    private static final String LAMP = "55555555-5555-5555-5555-555555555555";
    private static final String TABLE = "66666666-6666-6666-6666-666666666666";
    private static final String MESH = "77777777-7777-7777-7777-777777777777";
    private static final String MATERIAL_A = "88888888-8888-8888-8888-888888888888";
    private static final String MATERIAL_B = "99999999-9999-9999-9999-999999999999";

    @Test
    void reResolvesCurrentSourceWithOverridesOnlyWhereExplicitlyRequested() {

        PrefabDocument originalLamp = document(localScene("Original Lamp", MATERIAL_A), "[]");
        PrefabDocument updatedLamp = document(localScene("Updated Lamp", MATERIAL_B), "[]");
        String instances = "[" + instance("left", LAMP, "null", "[" + override("[]", ENTITY, "name", "name", "\"Custom Lamp\"") + "]") + "," + instance("right", LAMP, "null", "[]")
            + "]";
        PrefabDocument room = document(emptyScene(), instances);

        List<PrefabResolvedGroup> before = PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), room, id(LAMP), originalLamp));
        assertThat(before).hasSize(3);
        assertThat(group(before, "left").scene().entities().get(0).components().name().name()).isEqualTo("Custom Lamp");
        assertThat(group(before, "right").scene().entities().get(0).components().name().name()).isEqualTo("Original Lamp");
        assertThat(group(before, "left").scene().entities().get(0).components().meshRenderer().materialAssetId()).isEqualTo(id(MATERIAL_A));

        List<PrefabResolvedGroup> after = PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), room, id(LAMP), updatedLamp));

        assertThat(group(after, "left").scene().entities().get(0).components().name().name()).isEqualTo("Custom Lamp");
        assertThat(group(after, "right").scene().entities().get(0).components().name().name()).isEqualTo("Updated Lamp");
        assertThat(group(after, "left").scene().entities().get(0).components().meshRenderer().materialAssetId()).isEqualTo(id(MATERIAL_B));
        assertThat(group(after, "right").scene().entities().get(0).components().meshRenderer().materialAssetId()).isEqualTo(id(MATERIAL_B));
        assertThat(originalLamp.scene().entities().get(0).components().name().name()).isEqualTo("Original Lamp");
        assertThat(room.instances().get(0).overrides().get(0).valueJson()).isEqualTo("\"Custom Lamp\"");

    }

    @Test
    void nestedInstancePathsSupportOuterOverridePrecedenceAndParentAttachment() {

        PrefabDocument lamp = document(localScene("Source Lamp", MATERIAL_A), "[]");
        PrefabDocument table = document(localScene("Table", MATERIAL_A),
            "[" + instance("lamp", LAMP, "null", "[" + override("[]", ENTITY, "name", "name", "\"Table Lamp\"") + "]") + "]");
        PrefabDocument room = document(localScene("Room", MATERIAL_A),
            "[" + instance("table", TABLE, "\"" + ENTITY + "\"", "[" + override("[\"lamp\"]", ENTITY, "name", "name", "\"Room Lamp\"") + "]") + "]");

        List<PrefabResolvedGroup> groups = PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), room, id(TABLE), table, id(LAMP), lamp));

        assertThat(groups).hasSize(3);
        assertThat(groups).extracting(PrefabResolvedGroup::instancePath).containsExactly(List.of(), List.of("table"), List.of("table", "lamp"));
        assertThat(group(groups, "table").parentGuid()).isEqualTo(EntityGuid.parse(ENTITY));
        assertThat(group(groups, "table").parentInstancePath()).isEmpty();
        assertThat(group(groups, "table", "lamp").parentInstancePath()).containsExactly("table");
        assertThat(group(groups, "table", "lamp").scene().entities().get(0).components().name().name()).isEqualTo("Room Lamp");
        assertThat(table.instances().get(0).overrides().get(0).valueJson()).isEqualTo("\"Table Lamp\"");

    }

    @Test
    void rejectsMissingReferencesPathsComponentsOrInvalidOverrideValues() {

        PrefabDocument lamp = document(localScene("Source", MATERIAL_A), "[]");
        PrefabDocument missingRef = document(emptyScene(), "[" + instance("lost", TABLE, "null", "[]") + "]");
        assertThatThrownBy(() -> PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), missingRef))).isInstanceOf(PrefabFormatException.class)
            .hasMessageContaining("missing referenced prefab");

        assertBadOverride(lamp, override("[\"unresolved\"]", ENTITY, "name", "name", "\"X\""), "unknown prefab override instancePath");
        assertBadOverride(lamp, override("[]", MISSING_ENTITY, "name", "name", "\"X\""), "missing entityGuid");
        assertBadOverride(lamp, override("[]", ENTITY, "camera", "nearPlaneMeters", "1"), "missing component property");
        assertBadOverride(lamp, override("[]", ENTITY, "name", "name", "42"), "invalid override value");
        assertBadOverride(lamp, override("[]", ENTITY, "meshRenderer", "materialAssetId", "\"not-an-id\""), "invalid override value");

    }

    @Test
    void rejectsCyclicSourcesWithoutRecursingIndefinitely() {

        PrefabDocument root = document(emptyScene(), "[" + instance("cycle", ROOM, "null", "[]") + "]");
        assertThatThrownBy(() -> PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), root))).isInstanceOf(PrefabFormatException.class)
            .hasMessageContaining("cyclic prefab reference");

    }

    @Test
    void reportsCompleteSelfReferenceChainBeforeApplyingOverrides() {

        PrefabDocument root = document(emptyScene(),
            "[" + instance("self", ROOM, "null", "[" + override("[]", MISSING_ENTITY, "name", "name", "\"Invalid\"") + "]") + "]");
        assertThatThrownBy(() -> PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), root)))
            .isInstanceOf(PrefabFormatException.class)
            .hasMessage("cyclic prefab reference: " + ROOM + " -> " + ROOM);

    }

    @Test
    void reportsOnlyClosedCycleAfterNoncyclicPrefix() {

        PrefabDocument room = document(emptyScene(), "[" + instance("table", TABLE, "null", "[]") + "]");
        PrefabDocument table = document(emptyScene(), "[" + instance("lamp", LAMP, "null", "[]") + "]");
        PrefabDocument lamp = document(emptyScene(), "[" + instance("table", TABLE, "null", "[]") + "]");
        assertThatThrownBy(() -> PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), room, id(TABLE), table, id(LAMP), lamp)))
            .isInstanceOf(PrefabFormatException.class)
            .hasMessage("cyclic prefab reference: " + TABLE + " -> " + LAMP + " -> " + TABLE);

    }

    @Test
    void sharedReachableSourcesAreNotCyclesAndUnreachableCyclesAreIgnored() {

        PrefabDocument room = document(emptyScene(),
            "[" + instance("left", TABLE, "null", "[]") + "," + instance("right", LAMP, "null", "[]") + "]");
        PrefabDocument table = document(emptyScene(), "[" + instance("shared", MESH, "null", "[]") + "]");
        PrefabDocument lamp = document(emptyScene(), "[" + instance("shared", MESH, "null", "[]") + "]");
        PrefabDocument shared = document(emptyScene(), "[]");
        PrefabDocument unreachable = document(emptyScene(), "[" + instance("self", MATERIAL_A, "null", "[]") + "]");
        List<PrefabResolvedGroup> groups = PrefabResolver.resolve(id(ROOM),
            Map.of(id(ROOM), room, id(TABLE), table, id(LAMP), lamp, id(MESH), shared, id(MATERIAL_A), unreachable));
        assertThat(groups).extracting(PrefabResolvedGroup::instancePath)
            .containsExactly(List.of(), List.of("left"), List.of("left", "shared"), List.of("right"), List.of("right", "shared"));

    }

    @Test
    void preflightReportsDeepCycleBeforeEarlierInvalidOverride() {

        PrefabDocument room = document(emptyScene(),
            "[" + instance("broken", TABLE, "null", "[" + override("[]", MISSING_ENTITY, "name", "name", "\"Bad\"") + "]") + ","
                + instance("cycle", LAMP, "null", "[]") + "]");
        PrefabDocument table = document(emptyScene(), "[]");
        PrefabDocument lamp = document(emptyScene(), "[" + instance("back", ROOM, "null", "[]") + "]");
        assertThatThrownBy(() -> PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), room, id(TABLE), table, id(LAMP), lamp)))
            .isInstanceOf(PrefabFormatException.class)
            .hasMessage("cyclic prefab reference: " + ROOM + " -> " + LAMP + " -> " + ROOM);

    }

    private static void assertBadOverride(PrefabDocument lamp, String overrideJson, String message) {

        PrefabDocument room = document(emptyScene(), "[" + instance("lamp", LAMP, "null", "[" + overrideJson + "]") + "]");
        assertThatThrownBy(() -> PrefabResolver.resolve(id(ROOM), Map.of(id(ROOM), room, id(LAMP), lamp))).isInstanceOf(PrefabFormatException.class).hasMessageContaining(message);

    }

    private static PrefabResolvedGroup group(List<PrefabResolvedGroup> groups, String... path) {

        return groups.stream().filter(group -> group.instancePath().equals(List.of(path))).findFirst().orElseThrow();

    }

    private static PrefabDocument document(String scene, String instances) {

        return PrefabJsonCodec.decode("""
            {"schemaVersion":1,"scene":%s,"instances":%s}
            """.formatted(scene, instances));

    }

    private static String emptyScene() {

        return "{\"schemaVersion\":1,\"entities\":[]}";

    }

    private static String localScene(String name, String material) {

        return """
            {"schemaVersion":1,"entities":[{
              "guid":"%s","parentGuid":null,
              "components":{
                "name":{"schemaVersion":1,"name":"%s"},
                "meshRenderer":{"schemaVersion":1,"meshAssetId":"%s","materialAssetId":"%s"}
              }
            }]}
            """.formatted(ENTITY, name, MESH, material);

    }

    private static String instance(String key, String prefabId, String parent, String overrides) {

        return """
            {"instanceKey":"%s","prefabAssetId":"%s","parentGuid":%s,"overrides":%s}
            """.formatted(key, prefabId, parent, overrides);

    }

    private static String override(String path, String guid, String component, String property, String value) {

        return """
            {"instancePath":%s,"entityGuid":"%s","component":"%s","property":"%s","value":%s}
            """.formatted(path, guid, component, property, value);

    }

    private static AssetId id(String text) {

        return AssetId.parse(text);

    }
}
