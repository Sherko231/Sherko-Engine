package com.samo.engine.world.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.samo.engine.assets.api.AssetId;
import com.samo.engine.world.api.EntityGuid;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class PrefabJsonCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "scene", "instances");
    private static final Set<String> INSTANCE_FIELDS = Set.of("instanceKey", "prefabAssetId", "parentGuid", "overrides");
    private static final Set<String> OVERRIDE_FIELDS = Set.of("instancePath", "entityGuid", "component", "property", "value");

    private PrefabJsonCodec() {

    }

    static PrefabDocument decode(String json) {

        Objects.requireNonNull(json, "json");
        try {
            return parseRoot(MAPPER.readTree(json));
        } catch (PrefabFormatException exception) {
            throw exception;
        } catch (SceneFormatException exception) {
            throw new PrefabFormatException("invalid prefab.scene: " + exception.getMessage(), exception);
        } catch (JsonProcessingException exception) {
            throw new PrefabFormatException("invalid prefab JSON: " + exception.getOriginalMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new PrefabFormatException("invalid prefab: " + exception.getMessage(), exception);
        }

    }

    static String encode(PrefabDocument document) {

        Objects.requireNonNull(document, "document");
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("schemaVersion", PrefabDocument.CURRENT_SCHEMA_VERSION);
            root.set("scene", MAPPER.readTree(SceneJsonCodec.encode(document.scene())));
            ArrayNode instances = root.putArray("instances");
            for (PrefabInstanceDocument instance : document.instances()) {
                ObjectNode instanceNode = instances.addObject();
                instanceNode.put("instanceKey", instance.instanceKey());
                instanceNode.put("prefabAssetId", instance.prefabAssetId().toString());
                if (instance.parentGuid() == null) {
                    instanceNode.putNull("parentGuid");
                } else {
                    instanceNode.put("parentGuid", instance.parentGuid().toString());
                }
                ArrayNode overrides = instanceNode.putArray("overrides");
                for (PrefabPropertyOverride override : instance.overrides()) {
                    ObjectNode overrideNode = overrides.addObject();
                    ArrayNode path = overrideNode.putArray("instancePath");
                    for (String segment : override.instancePath()) {
                        path.add(segment);
                    }
                    overrideNode.put("entityGuid", override.entityGuid().toString());
                    overrideNode.put("component", override.component());
                    overrideNode.put("property", override.property());
                    overrideNode.set("value", MAPPER.readTree(override.valueJson()));
                }
            }
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new PrefabFormatException("failed to encode prefab JSON", exception);
        }

    }

    private static PrefabDocument parseRoot(JsonNode root) throws JsonProcessingException {

        requireObject(root, "root");
        requireOnlyFields(root, ROOT_FIELDS, "root");
        requireVersion(root, "root");
        JsonNode sceneNode = requireField(root, "scene", "root");
        SceneDocument scene = SceneJsonCodec.decode(MAPPER.writeValueAsString(sceneNode));
        JsonNode instancesNode = requireArray(requireField(root, "instances", "root"), "root.instances");
        List<PrefabInstanceDocument> instances = new ArrayList<>();
        for (int i = 0; i < instancesNode.size(); i++) {
            instances.add(parseInstance(instancesNode.get(i), "root.instances[" + i + "]"));
        }
        return new PrefabDocument(scene, instances);

    }

    private static PrefabInstanceDocument parseInstance(JsonNode node, String context) throws JsonProcessingException {

        requireObject(node, context);
        requireOnlyFields(node, INSTANCE_FIELDS, context);
        String key = requireText(node, "instanceKey", context);
        AssetId id;
        try {
            id = AssetId.parse(requireText(node, "prefabAssetId", context));
        } catch (IllegalArgumentException exception) {
            throw new PrefabFormatException(context + ".prefabAssetId must be canonical AssetId text", exception);
        }
        JsonNode parentNode = requireField(node, "parentGuid", context);
        EntityGuid parent = null;
        if (!parentNode.isNull()) {
            if (!parentNode.isTextual()) {
                throw new PrefabFormatException(context + ".parentGuid must be null or canonical EntityGuid text");
            }
            parent = parseGuid(parentNode.textValue(), context + ".parentGuid");
        }
        JsonNode overridesNode = requireArray(requireField(node, "overrides", context), context + ".overrides");
        List<PrefabPropertyOverride> overrides = new ArrayList<>();
        for (int i = 0; i < overridesNode.size(); i++) {
            overrides.add(parseOverride(overridesNode.get(i), context + ".overrides[" + i + "]"));
        }
        return new PrefabInstanceDocument(key, id, parent, overrides);

    }

    private static PrefabPropertyOverride parseOverride(JsonNode node, String context) throws JsonProcessingException {

        requireObject(node, context);
        requireOnlyFields(node, OVERRIDE_FIELDS, context);
        JsonNode pathNode = requireArray(requireField(node, "instancePath", context), context + ".instancePath");
        List<String> path = new ArrayList<>();
        for (int i = 0; i < pathNode.size(); i++) {
            if (!pathNode.get(i).isTextual()) {
                throw new PrefabFormatException(context + ".instancePath[" + i + "] must be a string");
            }
            path.add(pathNode.get(i).textValue());
        }
        EntityGuid entityGuid = parseGuid(requireText(node, "entityGuid", context), context + ".entityGuid");
        String component = requireText(node, "component", context);
        String property = requireText(node, "property", context);
        String valueJson = MAPPER.writeValueAsString(requireField(node, "value", context));
        return new PrefabPropertyOverride(path, entityGuid, component, property, valueJson);

    }

    private static EntityGuid parseGuid(String text, String context) {

        try {
            return EntityGuid.parse(text);
        } catch (IllegalArgumentException exception) {
            throw new PrefabFormatException(context + " must be canonical EntityGuid text", exception);
        }

    }

    private static JsonNode requireField(JsonNode object, String field, String context) {

        JsonNode node = object.get(field);
        if (node == null) {
            throw new PrefabFormatException("missing required field " + context + "." + field);
        }
        return node;

    }

    private static String requireText(JsonNode object, String field, String context) {

        JsonNode value = requireField(object, field, context);
        if (!value.isTextual()) {
            throw new PrefabFormatException(context + "." + field + " must be a string");
        }
        return value.textValue();

    }

    private static JsonNode requireArray(JsonNode node, String context) {

        if (!node.isArray()) {
            throw new PrefabFormatException(context + " must be an array");
        }
        return node;

    }

    private static void requireObject(JsonNode node, String context) {

        if (node == null || !node.isObject()) {
            throw new PrefabFormatException(context + " must be an object");
        }

    }

    private static void requireOnlyFields(JsonNode node, Set<String> fields, String context) {

        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!fields.contains(key)) {
                throw new PrefabFormatException("unknown field " + context + "." + key);
            }
        }

    }

    private static void requireVersion(JsonNode root, String context) {

        JsonNode version = requireField(root, "schemaVersion", context);
        if (!version.isIntegralNumber()) {
            throw new PrefabFormatException(context + ".schemaVersion must be integer 1");
        }
        if (!version.canConvertToInt() || version.intValue() != PrefabDocument.CURRENT_SCHEMA_VERSION) {
            throw new PrefabFormatException(context + ": unsupported schemaVersion " + version.asText() + "; upgrade required");
        }

    }
}
