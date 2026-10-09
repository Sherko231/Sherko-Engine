package com.samo.engine.world.internal;

import com.samo.engine.world.api.EntityGuid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

record PrefabPropertyOverride(List<String> instancePath, EntityGuid entityGuid, String component, String property, String valueJson) {
    private static final Pattern INSTANCE_KEY = Pattern.compile("[a-z][a-z0-9_-]*");
    private static final Map<String, Set<String>> PROPERTIES = Map.of(
        "transform", Set.of("position", "rotation", "scale"),
        "name", Set.of("name"),
        "meshRenderer", Set.of("meshAssetId", "materialAssetId"),
        "camera", Set.of("verticalFovRadians", "nearPlaneMeters", "farPlaneMeters"),
        "audioEmitter", Set.of("audioAssetId"));

    PrefabPropertyOverride {

        Objects.requireNonNull(instancePath, "instancePath");
        instancePath = List.copyOf(instancePath);
        for (String segment : instancePath) {
            if (!INSTANCE_KEY.matcher(segment).matches()) {
                throw new PrefabFormatException("invalid override instance path segment: " + segment);
            }
        }
        Objects.requireNonNull(entityGuid, "entityGuid");
        if (!PROPERTIES.getOrDefault(component, Set.of()).contains(property)) {
            throw new PrefabFormatException("unsupported prefab override property: " + component + "." + property);
        }
        Objects.requireNonNull(valueJson, "valueJson");
        if (valueJson.isBlank()) {
            throw new PrefabFormatException("override value JSON must not be blank");
        }

    }
}
