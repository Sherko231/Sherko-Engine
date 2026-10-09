package com.samo.engine.world.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrefabCycleValidatorTest {
    private static final String ROOT = "11111111-1111-1111-1111-111111111111";
    private static final String TABLE = "22222222-2222-2222-2222-222222222222";
    private static final String LAMP = "33333333-3333-3333-3333-333333333333";
    private static final String SHARED = "44444444-4444-4444-4444-444444444444";

    @Test
    void reportsCompleteDirectSelfReference() {

        Map<AssetId, PrefabDocument> sources = Map.of(id(ROOT), prefab(ref("self", ROOT)));

        assertThatThrownBy(() -> PrefabCycleValidator.validate(id(ROOT), sources)).isInstanceOf(PrefabFormatException.class)
            .hasMessage("cyclic prefab reference: " + ROOT + " --[self]--> " + ROOT);

    }

    @Test
    void reportsEntireRootPrefixAndTheClosingEdgeForAnIndirectCycle() {

        Map<AssetId, PrefabDocument> sources = Map.of(id(ROOT), prefab(ref("table", TABLE)), id(TABLE), prefab(ref("lamp", LAMP)), id(LAMP),
            prefab(ref("back", TABLE)));

        assertThatThrownBy(() -> PrefabCycleValidator.validate(id(ROOT), sources)).isInstanceOf(PrefabFormatException.class)
            .hasMessage("cyclic prefab reference: " + ROOT + " --[table]--> " + TABLE + " --[lamp]--> " + LAMP + " --[back]--> " + TABLE);

    }

    @Test
    void choosesTheFirstReachableCycleByInstanceDeclarationOrder() {

        Map<AssetId, PrefabDocument> sources = Map.of(id(ROOT), prefab(ref("first", TABLE), ref("second", LAMP)), id(TABLE), prefab(ref("back", ROOT)),
            id(LAMP), prefab(ref("self", LAMP)));

        assertThatThrownBy(() -> PrefabCycleValidator.validate(id(ROOT), sources)).isInstanceOf(PrefabFormatException.class)
            .hasMessage("cyclic prefab reference: " + ROOT + " --[first]--> " + TABLE + " --[back]--> " + ROOT);

    }

    @Test
    void acceptsSharedReusableSourcesAndIgnoresUnreachableCycles() {

        Map<AssetId, PrefabDocument> sources = Map.of(id(ROOT), prefab(ref("table", TABLE), ref("lamp", LAMP)), id(TABLE), prefab(ref("shared", SHARED)),
            id(LAMP), prefab(ref("also-shared", SHARED)), id(SHARED), prefab(), new AssetId(0, 9), prefab(ref("unreachable-cycle", new AssetId(0, 9))));

        assertThatCode(() -> PrefabCycleValidator.validate(id(ROOT), sources)).doesNotThrowAnyException();

    }

    @Test
    void leavesMissingReferencesToTheExistingResolverFailurePath() {

        PrefabDocument root = prefab(ref("missing", TABLE));
        Map<AssetId, PrefabDocument> sources = Map.of(id(ROOT), root);

        assertThatCode(() -> PrefabCycleValidator.validate(id(ROOT), sources)).doesNotThrowAnyException();
        assertThatThrownBy(() -> PrefabResolver.resolve(id(ROOT), sources)).isInstanceOf(PrefabFormatException.class)
            .hasMessageContaining("missing referenced prefab AssetId: " + TABLE);

    }

    @Test
    void validatesLongAcyclicReferenceChainsWithoutRecursiveJavaCalls() {

        int nodes = 2_000;
        Map<AssetId, PrefabDocument> sources = new HashMap<>();
        for (int index = 0; index < nodes; index++) {
            AssetId current = new AssetId(0, index + 1);
            sources.put(current, index == nodes - 1 ? prefab() : prefab(ref("next", new AssetId(0, index + 2))));
        }

        assertThatCode(() -> PrefabCycleValidator.validate(new AssetId(0, 1), sources)).doesNotThrowAnyException();

    }

    private static PrefabDocument prefab(PrefabInstanceDocument... references) {

        return new PrefabDocument(new SceneDocument(List.of()), List.of(references));

    }

    private static PrefabInstanceDocument ref(String instanceKey, String target) {

        return ref(instanceKey, id(target));

    }

    private static PrefabInstanceDocument ref(String instanceKey, AssetId target) {

        return new PrefabInstanceDocument(instanceKey, target, null, List.of());

    }

    private static AssetId id(String text) {

        return AssetId.parse(text);

    }
}
