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
import com.samo.engine.world.api.AudioEmitterComponent;
import com.samo.engine.world.api.CameraComponent;
import com.samo.engine.world.api.EntityGuid;
import com.samo.engine.world.api.MeshRendererComponent;
import com.samo.engine.world.api.NameComponent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class SceneJsonCodec {
    private static final int COMPONENT_SCHEMA_VERSION = 1;
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "entities");
    private static final Set<String> ENTITY_FIELDS = Set.of("guid", "parentGuid", "components");
    private static final Set<String> COMPONENT_FIELDS = Set.of("transform", "name", "meshRenderer", "camera", "audioEmitter");
    private static final Set<String> TRANSFORM_FIELDS = Set.of("schemaVersion", "position", "rotation", "scale");
    private static final Set<String> NAME_FIELDS = Set.of("schemaVersion", "name");
    private static final Set<String> MESH_RENDERER_FIELDS = Set.of("schemaVersion", "meshAssetId", "materialAssetId");
    private static final Set<String> CAMERA_FIELDS = Set.of("schemaVersion", "verticalFovRadians", "nearPlaneMeters", "farPlaneMeters");
    private static final Set<String> AUDIO_EMITTER_FIELDS = Set.of("schemaVersion", "audioAssetId");
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private SceneJsonCodec() {

    }

    static SceneDocument decode(String json) {

        Objects.requireNonNull(json, "json");
        try {
            return parseRoot(MAPPER.readTree(json));
        } catch (SceneFormatException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw failure("invalid scene JSON", exception);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw failure(messageOrType(exception), exception);
        }

    }

    static String encode(SceneDocument document) {

        Objects.requireNonNull(document, "document");
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", SceneDocument.CURRENT_SCHEMA_VERSION);
        ArrayNode entities = root.putArray("entities");
        for (SceneEntityDocument entity : document.entities()) {
            writeEntity(entities.addObject(), entity);
        }

        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw failure("failed to encode scene JSON", exception);
        }

    }

    private static SceneDocument parseRoot(JsonNode root) {

        requireObject(root, "root");
        requireOnlyFields(root, ROOT_FIELDS, "root");
        requireSchemaVersion(root, SceneDocument.CURRENT_SCHEMA_VERSION, "root");

        JsonNode entitiesNode = requireField(root, "entities", "root");
        if (!entitiesNode.isArray()) {
            throw failure("root.entities must be a JSON array");
        }

        List<SceneEntityDocument> entities = new ArrayList<>(entitiesNode.size());
        for (int index = 0; index < entitiesNode.size(); index++) {
            entities.add(parseEntity(entitiesNode.get(index), "root.entities[" + index + "]"));
        }
        return new SceneDocument(entities);

    }

    private static SceneEntityDocument parseEntity(JsonNode node, String context) {

        requireObject(node, context);
        requireOnlyFields(node, ENTITY_FIELDS, context);

        EntityGuid guid = parseEntityGuid(requireText(node, "guid", context), context + ".guid");
        JsonNode parentNode = requireField(node, "parentGuid", context);
        EntityGuid parentGuid = null;
        if (!parentNode.isNull()) {
            if (!parentNode.isTextual()) {
                throw failure(context + ".parentGuid must be null or canonical EntityGuid text");
            }
            parentGuid = parseEntityGuid(parentNode.textValue(), context + ".parentGuid");
        }

        JsonNode componentsNode = requireField(node, "components", context);
        return new SceneEntityDocument(guid, parentGuid, parseComponents(componentsNode, context + ".components"));

    }

    private static SceneComponentsDocument parseComponents(JsonNode node, String context) {

        requireObject(node, context);
        requireOnlyFields(node, COMPONENT_FIELDS, context);

        SceneTransformData transform = node.has("transform") ? parseTransform(node.get("transform"), context + ".transform") : null;
        NameComponent name = node.has("name") ? parseName(node.get("name"), context + ".name") : null;
        MeshRendererComponent meshRenderer = node.has("meshRenderer") ? parseMeshRenderer(node.get("meshRenderer"), context + ".meshRenderer") : null;
        CameraComponent camera = node.has("camera") ? parseCamera(node.get("camera"), context + ".camera") : null;
        AudioEmitterComponent audioEmitter = node.has("audioEmitter") ? parseAudioEmitter(node.get("audioEmitter"), context + ".audioEmitter") : null;

        return new SceneComponentsDocument(transform, name, meshRenderer, camera, audioEmitter);

    }

    private static SceneTransformData parseTransform(JsonNode node, String context) {

        requireObject(node, context);
        requireOnlyFields(node, TRANSFORM_FIELDS, context);
        requireSchemaVersion(node, COMPONENT_SCHEMA_VERSION, context);

        float[] position = requireFloatArray(node, "position", 3, context);
        float[] rotation = requireFloatArray(node, "rotation", 4, context);
        float[] scale = requireFloatArray(node, "scale", 3, context);
        try {
            return new SceneTransformData(position[0], position[1], position[2], rotation[0], rotation[1], rotation[2], rotation[3], scale[0], scale[1], scale[2]);
        } catch (IllegalArgumentException exception) {
            throw failure(context + ": " + exception.getMessage(), exception);
        }

    }

    private static NameComponent parseName(JsonNode node, String context) {

        requireObject(node, context);
        requireOnlyFields(node, NAME_FIELDS, context);
        requireSchemaVersion(node, COMPONENT_SCHEMA_VERSION, context);
        try {
            return new NameComponent(requireText(node, "name", context));
        } catch (IllegalArgumentException exception) {
            throw failure(context + ": " + exception.getMessage(), exception);
        }

    }

    private static MeshRendererComponent parseMeshRenderer(JsonNode node, String context) {

        requireObject(node, context);
        requireOnlyFields(node, MESH_RENDERER_FIELDS, context);
        requireSchemaVersion(node, COMPONENT_SCHEMA_VERSION, context);
        AssetId meshAssetId = parseAssetId(requireText(node, "meshAssetId", context), context + ".meshAssetId");
        AssetId materialAssetId = parseAssetId(requireText(node, "materialAssetId", context), context + ".materialAssetId");
        return new MeshRendererComponent(meshAssetId, materialAssetId);

    }

    private static CameraComponent parseCamera(JsonNode node, String context) {

        requireObject(node, context);
        requireOnlyFields(node, CAMERA_FIELDS, context);
        requireSchemaVersion(node, COMPONENT_SCHEMA_VERSION, context);
        float verticalFovRadians = requireFloat(node, "verticalFovRadians", context);
        float nearPlaneMeters = requireFloat(node, "nearPlaneMeters", context);
        float farPlaneMeters = requireFloat(node, "farPlaneMeters", context);
        try {
            return new CameraComponent(verticalFovRadians, nearPlaneMeters, farPlaneMeters);
        } catch (IllegalArgumentException exception) {
            throw failure(context + ": " + exception.getMessage(), exception);
        }

    }

    private static AudioEmitterComponent parseAudioEmitter(JsonNode node, String context) {

        requireObject(node, context);
        requireOnlyFields(node, AUDIO_EMITTER_FIELDS, context);
        requireSchemaVersion(node, COMPONENT_SCHEMA_VERSION, context);
        AssetId audioAssetId = parseAssetId(requireText(node, "audioAssetId", context), context + ".audioAssetId");
        return new AudioEmitterComponent(audioAssetId);

    }

    private static EntityGuid parseEntityGuid(String text, String context) {

        try {
            return EntityGuid.parse(text);
        } catch (IllegalArgumentException exception) {
            throw failure(context + " must be canonical lowercase EntityGuid text", exception);
        }

    }

    private static AssetId parseAssetId(String text, String context) {

        try {
            return AssetId.parse(text);
        } catch (IllegalArgumentException exception) {
            throw failure(context + " must be canonical lowercase AssetId text", exception);
        }

    }

    private static float[] requireFloatArray(JsonNode object, String field, int expectedLength, String context) {

        JsonNode array = requireField(object, field, context);
        if (!array.isArray() || array.size() != expectedLength) {
            throw failure(context + "." + field + " must be an array of exactly " + expectedLength + " finite numbers");
        }
        float[] values = new float[expectedLength];
        for (int index = 0; index < expectedLength; index++) {
            values[index] = requireFiniteNumber(array.get(index), context + "." + field + "[" + index + "]");
        }
        return values;

    }

    private static float requireFloat(JsonNode object, String field, String context) {

        return requireFiniteNumber(requireField(object, field, context), context + "." + field);

    }

    private static float requireFiniteNumber(JsonNode node, String context) {

        if (!node.isNumber()) {
            throw failure(context + " must be a finite number");
        }
        float value = node.floatValue();
        if (!Float.isFinite(value)) {
            throw failure(context + " must be a finite number");
        }
        return value;

    }

    private static String requireText(JsonNode object, String field, String context) {

        JsonNode value = requireField(object, field, context);
        if (!value.isTextual()) {
            throw failure(context + "." + field + " must be a string");
        }
        return value.textValue();

    }

    private static void requireSchemaVersion(JsonNode object, int expectedVersion, String context) {

        JsonNode versionNode = requireField(object, "schemaVersion", context);
        if (!versionNode.isIntegralNumber()) {
            throw failure(context + ".schemaVersion must be an integer");
        }
        if (!versionNode.canConvertToInt() || versionNode.intValue() != expectedVersion) {
            throw failure(context + ": unsupported schemaVersion " + versionNode.asText() + "; upgrade required");
        }

    }

    private static JsonNode requireField(JsonNode object, String field, String context) {

        JsonNode value = object.get(field);
        if (value == null) {
            throw failure("missing required field " + context + "." + field);
        }
        return value;

    }

    private static void requireObject(JsonNode node, String context) {

        if (node == null || !node.isObject()) {
            throw failure(context + " must be a JSON object");
        }

    }

    private static void requireOnlyFields(JsonNode object, Set<String> allowedFields, String context) {

        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowedFields.contains(field)) {
                throw failure("unknown field " + context + "." + field);
            }
        }

    }

    private static void writeEntity(ObjectNode node, SceneEntityDocument entity) {

        node.put("guid", entity.guid().toString());
        if (entity.parentGuid() == null) {
            node.putNull("parentGuid");
        } else {
            node.put("parentGuid", entity.parentGuid().toString());
        }
        writeComponents(node.putObject("components"), entity.components());

    }

    private static void writeComponents(ObjectNode node, SceneComponentsDocument components) {

        if (components.transform() != null) {
            writeTransform(node.putObject("transform"), components.transform());
        }
        if (components.name() != null) {
            ObjectNode value = node.putObject("name");
            value.put("schemaVersion", COMPONENT_SCHEMA_VERSION);
            value.put("name", components.name().name());
        }
        if (components.meshRenderer() != null) {
            ObjectNode value = node.putObject("meshRenderer");
            value.put("schemaVersion", COMPONENT_SCHEMA_VERSION);
            value.put("meshAssetId", components.meshRenderer().meshAssetId().toString());
            value.put("materialAssetId", components.meshRenderer().materialAssetId().toString());
        }
        if (components.camera() != null) {
            ObjectNode value = node.putObject("camera");
            value.put("schemaVersion", COMPONENT_SCHEMA_VERSION);
            value.put("verticalFovRadians", components.camera().verticalFovRadians());
            value.put("nearPlaneMeters", components.camera().nearPlaneMeters());
            value.put("farPlaneMeters", components.camera().farPlaneMeters());
        }
        if (components.audioEmitter() != null) {
            ObjectNode value = node.putObject("audioEmitter");
            value.put("schemaVersion", COMPONENT_SCHEMA_VERSION);
            value.put("audioAssetId", components.audioEmitter().audioAssetId().toString());
        }

    }

    private static void writeTransform(ObjectNode node, SceneTransformData transform) {

        node.put("schemaVersion", COMPONENT_SCHEMA_VERSION);
        ArrayNode position = node.putArray("position");
        position.add(transform.positionX()).add(transform.positionY()).add(transform.positionZ());
        ArrayNode rotation = node.putArray("rotation");
        rotation.add(transform.rotationX()).add(transform.rotationY()).add(transform.rotationZ()).add(transform.rotationW());
        ArrayNode scale = node.putArray("scale");
        scale.add(transform.scaleX()).add(transform.scaleY()).add(transform.scaleZ());

    }

    private static SceneFormatException failure(String message) {

        return new SceneFormatException(message);

    }

    private static SceneFormatException failure(String message, Throwable cause) {

        return new SceneFormatException(message, cause);

    }

    private static String messageOrType(Throwable throwable) {

        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;

    }
}
