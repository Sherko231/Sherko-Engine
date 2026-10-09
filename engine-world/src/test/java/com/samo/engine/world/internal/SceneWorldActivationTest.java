package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.world.api.EntityGuid;
import com.samo.engine.world.api.EntityId;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class SceneWorldActivationTest {
    private static final String PARENT = "00000000-0000-0000-0000-000000000001";
    private static final String CHILD = "00000000-0000-0000-0000-000000000002";
    private static final String ROOM = "44444444-4444-4444-4444-444444444444";
    private static final String LAMP = "55555555-5555-5555-5555-555555555555";
    private static final String MESH = "77777777-7777-7777-7777-777777777777";
    private static final String MATERIAL = "88888888-8888-8888-8888-888888888888";
    private static final String AUDIO = "99999999-9999-9999-9999-999999999999";

    @Test
    void stagesAllFiveComponentsAndForwardParentReferencesBeforeActivation() {

        SceneWorldActivation activation = new SceneWorldActivation();
        SceneWorld originallyEmpty = activation.activeWorld();
        assertThat(originallyEmpty.entityCount()).isZero();

        activation.loadAndActivateScene(fullScene());
        SceneWorld loaded = activation.activeWorld();
        EntityId child = loaded.entity(List.of(), guid(CHILD));
        EntityId parent = loaded.entity(List.of(), guid(PARENT));

        assertThat(loaded).isNotSameAs(originallyEmpty);
        assertThat(loaded.entityCount()).isEqualTo(2);
        assertThat(loaded.isAlive(child)).isTrue();
        assertThat(loaded.isAlive(parent)).isTrue();
        assertThat(child).isNotEqualTo(parent);
        assertThat(loaded.parent(child)).isEqualTo(parent);
        assertThat(loaded.parent(parent)).isNull();
        assertThat(loaded.name(child).name()).isEqualTo("Child");
        assertThat(loaded.meshRenderer(child).meshAssetId()).isEqualTo(id(MESH));
        assertThat(loaded.meshRenderer(child).materialAssetId()).isEqualTo(id(MATERIAL));
        assertThat(loaded.camera(child).verticalFovRadians()).isEqualTo(1.0f);
        assertThat(loaded.camera(child).nearPlaneMeters()).isEqualTo(0.1f);
        assertThat(loaded.camera(child).farPlaneMeters()).isEqualTo(100.0f);
        assertThat(loaded.audioEmitter(child).audioAssetId()).isEqualTo(id(AUDIO));
        assertThat(loaded.transform(child).transform().parent()).isSameAs(loaded.transform(parent).transform());
        assertThat(loaded.transform(child).transform().worldMatrix(new Matrix4f()).m30()).isCloseTo(12.0f, offset(0.00001f));
        assertThat(loaded.transform(child).transform().localPosition(new org.joml.Vector3f()).x).isCloseTo(2.0f, offset(0.00001f));

    }

    @Test
    void invalidSceneNeverMutatesThePreviouslyActiveWorld() {

        SceneWorldActivation activation = new SceneWorldActivation();
        activation.loadAndActivateScene(fullScene());
        SceneWorld old = activation.activeWorld();
        EntityId child = old.entity(List.of(), guid(CHILD));
        EntityId parent = old.entity(List.of(), guid(PARENT));
        Matrix4f before = old.transform(child).transform().worldMatrix(new Matrix4f());

        assertThatThrownBy(() -> activation.loadAndActivateScene("{")).isInstanceOf(SceneFormatException.class);
        assertThatThrownBy(() -> activation.loadAndActivateScene("""
            {"schemaVersion":1,"entities":[{"guid":"00000000-0000-0000-0000-000000000003","parentGuid":null,
             "components":{"futureRequired":{"schemaVersion":1}}}]}
            """)).isInstanceOf(SceneFormatException.class);
        assertThatThrownBy(() -> activation.loadAndActivateScene("""
            {"schemaVersion":1,"entities":[{"guid":"00000000-0000-0000-0000-000000000003",
             "parentGuid":"00000000-0000-0000-0000-000000000004","components":{}}]}
            """)).isInstanceOf(SceneFormatException.class);

        assertThat(activation.activeWorld()).isSameAs(old);
        assertThat(old.entityCount()).isEqualTo(2);
        assertThat(old.entity(List.of(), guid(CHILD))).isEqualTo(child);
        assertThat(old.parent(child)).isEqualTo(parent);
        assertThat(old.transform(child).transform().worldMatrix(new Matrix4f())).isEqualTo(before);
        assertThat(old.name(child).name()).isEqualTo("Child");

        activation.loadAndActivateScene("{\"schemaVersion\":1,\"entities\":[]}");
        assertThat(activation.activeWorld()).isNotSameAs(old);
        assertThat(activation.activeWorld().entityCount()).isZero();
        assertThat(activation.activeWorld().entity(List.of(), guid(CHILD))).isNull();
        assertThat(old.entity(List.of(), guid(CHILD))).isEqualTo(child);

    }

    @Test
    void activatesSiblingPrefabInstancesWithScopedGuidsAndDistinctOverrides() {

        PrefabDocument room = prefab(rootScene(), "[" + instance("left", LAMP, PARENT, "[" + overrideName("Left Lamp") + "]") + "," + instance("right", LAMP, PARENT, "[]") + "]");
        PrefabDocument lamp = prefab(lampScene(), "[]");
        SceneWorldActivation activation = new SceneWorldActivation();

        activation.loadAndActivatePrefab(id(ROOM), Map.of(id(ROOM), room, id(LAMP), lamp));
        SceneWorld loaded = activation.activeWorld();
        EntityId rootId = loaded.entity(List.of(), guid(PARENT));
        EntityId left = loaded.entity(List.of("left"), guid(CHILD));
        EntityId right = loaded.entity(List.of("right"), guid(CHILD));

        assertThat(loaded.entityCount()).isEqualTo(3);
        assertThat(left).isNotEqualTo(right);
        assertThat(loaded.parent(left)).isEqualTo(rootId);
        assertThat(loaded.parent(right)).isEqualTo(rootId);
        assertThat(loaded.name(left).name()).isEqualTo("Left Lamp");
        assertThat(loaded.name(right).name()).isEqualTo("Source Lamp");
        assertThat(loaded.meshRenderer(left).meshAssetId()).isEqualTo(id(MESH));
        assertThat(loaded.transform(left).transform().parent()).isSameAs(loaded.transform(rootId).transform());
        assertThat(loaded.transform(right).transform().worldMatrix(new Matrix4f()).m30()).isCloseTo(12.0f, offset(0.00001f));
        assertThat(lamp.scene().entities().getFirst().components().name().name()).isEqualTo("Source Lamp");

        SceneWorld old = activation.activeWorld();
        PrefabDocument cyclic = prefab(rootScene(), "[" + instance("loop", ROOM, PARENT, "[]") + "]");
        assertThatThrownBy(() -> activation.loadAndActivatePrefab(id(ROOM), Map.of(id(ROOM), cyclic))).isInstanceOf(PrefabFormatException.class)
            .hasMessageContaining("cyclic prefab reference");
        assertThatThrownBy(() -> activation.loadAndActivatePrefab(id(ROOM), Map.of(id(ROOM), room))).isInstanceOf(PrefabFormatException.class)
            .hasMessageContaining("missing referenced prefab");
        PrefabDocument invalidOverride = prefab(rootScene(), "[" + instance("broken", LAMP, PARENT,
            "[{\"instancePath\":[],\"entityGuid\":\"00000000-0000-0000-0000-000000000009\",\"component\":\"name\",\"property\":\"name\",\"value\":\"Missing\"}]") + "]");
        assertThatThrownBy(() -> activation.loadAndActivatePrefab(id(ROOM), Map.of(id(ROOM), invalidOverride, id(LAMP), lamp))).isInstanceOf(PrefabFormatException.class)
            .hasMessageContaining("missing entityGuid");
        assertThat(activation.activeWorld()).isSameAs(old);
        assertThat(old.name(left).name()).isEqualTo("Left Lamp");

    }

    @Test
    void lateAttachmentFailureInTemporaryWorldLeavesNoPartialCandidate() {

        SceneDocument root = SceneJsonCodec.decode(rootScene());
        SceneDocument nested = SceneJsonCodec.decode(lampScene());
        List<PrefabResolvedGroup> groups = List.of(new PrefabResolvedGroup(List.of(), id(ROOM), root, List.of(), null),
            new PrefabResolvedGroup(List.of("nested"), id(LAMP), nested, List.of(), guid("00000000-0000-0000-0000-000000000009")));

        assertThatThrownBy(() -> SceneWorld.fromPrefabs(groups)).isInstanceOf(SceneFormatException.class).hasMessageContaining("missing prefab attachment parentGuid");
        assertThat(root.entities().getFirst().guid()).isEqualTo(guid(PARENT));

    }

    @Test
    void parentWithoutTransformRetainsEntityHierarchyWithoutInventingSpatialParent() {

        SceneWorldActivation activation = new SceneWorldActivation();
        activation.loadAndActivateScene("""
            {"schemaVersion":1,"entities":[
              {"guid":"%s","parentGuid":null,"components":{"name":{"schemaVersion":1,"name":"Root"}}},
              {"guid":"%s","parentGuid":"%s","components":{"transform":{"schemaVersion":1,
                "position":[2,0,0],"rotation":[0,0,0,1],"scale":[1,1,1]}}}
            ]}
            """.formatted(PARENT, CHILD, PARENT));
        SceneWorld world = activation.activeWorld();
        EntityId root = world.entity(List.of(), guid(PARENT));
        EntityId child = world.entity(List.of(), guid(CHILD));
        assertThat(world.parent(child)).isEqualTo(root);
        assertThat(world.transform(root)).isNull();
        assertThat(world.transform(child).transform().parent()).isNull();

    }

    private static String fullScene() {

        return """
            {"schemaVersion":1,"editorData":{"authorNote":"ignored by runtime"},"entities":[
              {"guid":"%s","parentGuid":"%s","components":{
                "transform":{"schemaVersion":1,"position":[2,0,0],"rotation":[0,0,0,1],"scale":[1,1,1]},
                "name":{"schemaVersion":1,"name":"Child"},
                "meshRenderer":{"schemaVersion":1,"meshAssetId":"%s","materialAssetId":"%s"},
                "camera":{"schemaVersion":1,"verticalFovRadians":1.0,"nearPlaneMeters":0.1,"farPlaneMeters":100},
                "audioEmitter":{"schemaVersion":1,"audioAssetId":"%s"}
              }},
              {"guid":"%s","parentGuid":null,"components":{
                "transform":{"schemaVersion":1,"position":[10,0,0],"rotation":[0,0,0,1],"scale":[1,1,1]},
                "name":{"schemaVersion":1,"name":"Parent"}
              }}
            ]}
            """.formatted(CHILD, PARENT, MESH, MATERIAL, AUDIO, PARENT);

    }

    private static String rootScene() {

        return """
            {"schemaVersion":1,"entities":[{"guid":"%s","parentGuid":null,
            "components":{"transform":{"schemaVersion":1,"position":[10,0,0],"rotation":[0,0,0,1],"scale":[1,1,1]},
            "name":{"schemaVersion":1,"name":"Room"}}}]}
            """.formatted(PARENT);

    }

    private static String lampScene() {

        return """
            {"schemaVersion":1,"entities":[{"guid":"%s","parentGuid":null,
            "components":{"transform":{"schemaVersion":1,"position":[2,0,0],"rotation":[0,0,0,1],"scale":[1,1,1]},
            "name":{"schemaVersion":1,"name":"Source Lamp"},
            "meshRenderer":{"schemaVersion":1,"meshAssetId":"%s","materialAssetId":"%s"}}}]}
            """.formatted(CHILD, MESH, MATERIAL);

    }

    private static PrefabDocument prefab(String scene, String instances) {

        return PrefabJsonCodec.decode("{\"schemaVersion\":1,\"scene\":" + scene + ",\"instances\":" + instances + "}");

    }

    private static String instance(String key, String source, String parent, String overrides) {

        return "{\"instanceKey\":\"" + key + "\",\"prefabAssetId\":\"" + source + "\",\"parentGuid\":\"" + parent + "\",\"overrides\":" + overrides + "}";

    }

    private static String overrideName(String name) {

        return """
            {"instancePath":[],"entityGuid":"%s","component":"name","property":"name","value":"%s"}
            """.formatted(CHILD, name);

    }

    private static EntityGuid guid(String text) {

        return EntityGuid.parse(text);

    }

    private static AssetId id(String text) {

        return AssetId.parse(text);

    }
}
