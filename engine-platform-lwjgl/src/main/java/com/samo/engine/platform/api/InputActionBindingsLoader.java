package com.samo.engine.platform.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class InputActionBindingsLoader {
    private static final int SCHEMA_VERSION = 1;
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "actions");
    private static final Set<String> ACTION_FIELDS = Set.of("action", "bindings");
    private static final Set<String> KEY_FIELDS = Set.of("type", "key", "component", "scale");
    private static final Set<String> MOUSE_BUTTON_FIELDS = Set.of("type", "button", "component", "scale");
    private static final Set<String> MOUSE_DELTA_FIELDS = Set.of("type", "axis", "component", "scale");
    private static final ObjectMapper MAPPER = new ObjectMapper(
            JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());

    private InputActionBindingsLoader() {
    }

    static InputActionBindings load(Path path) {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw failure(path, "binding file is missing or unreadable");
        }

        JsonNode root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            root = MAPPER.readTree(reader);
        } catch (IOException exception) {
            throw failure(path, "failed to read binding JSON", exception);
        }

        try {
            return parseRoot(path, root);
        } catch (InputBindingLoadException exception) {
            throw exception;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw failure(path, exception.getMessage(), exception);
        }
    }

    private static InputActionBindings parseRoot(Path path, JsonNode root) {
        requireObject(path, root, "root");
        requireOnlyFields(path, root, ROOT_FIELDS, "root");

        JsonNode versionNode = requireField(path, root, "schemaVersion", "root");
        if (!versionNode.isIntegralNumber() || !versionNode.canConvertToInt()
                || versionNode.intValue() != SCHEMA_VERSION) {
            throw failure(path, "schemaVersion must equal " + SCHEMA_VERSION);
        }

        JsonNode actionsNode = requireField(path, root, "actions", "root");
        if (!actionsNode.isArray()) {
            throw failure(path, "actions must be an array");
        }

        EnumMap<InputAction, List<InputBinding>> parsed = new EnumMap<>(InputAction.class);
        for (int index = 0; index < actionsNode.size(); index++) {
            parseAction(path, actionsNode.get(index), index, parsed);
        }

        try {
            return new InputActionBindings(parsed);
        } catch (IllegalArgumentException exception) {
            throw failure(path, exception.getMessage(), exception);
        }
    }

    private static void parseAction(
            Path path,
            JsonNode actionNode,
            int actionIndex,
            Map<InputAction, List<InputBinding>> parsed) {
        String context = "actions[" + actionIndex + "]";
        requireObject(path, actionNode, context);
        requireOnlyFields(path, actionNode, ACTION_FIELDS, context);

        InputAction action = parseEnum(
                path,
                InputAction.class,
                requireText(path, actionNode, "action", context),
                context + ".action");
        if (parsed.containsKey(action)) {
            throw failure(path, "duplicate action entry: " + action);
        }

        JsonNode bindingsNode = requireField(path, actionNode, "bindings", context);
        if (!bindingsNode.isArray()) {
            throw failure(path, context + ".bindings must be an array");
        }
        if (bindingsNode.isEmpty()) {
            throw failure(path, "action " + action + " must have at least one binding");
        }

        ArrayList<InputBinding> bindings = new ArrayList<>(bindingsNode.size());
        HashSet<InputBinding> unique = new HashSet<>();
        for (int index = 0; index < bindingsNode.size(); index++) {
            InputBinding binding = parseBinding(path, action, bindingsNode.get(index), context, index);
            if (!unique.add(binding)) {
                throw failure(path, "duplicate binding for action " + action + ": " + binding);
            }
            bindings.add(binding);
        }
        parsed.put(action, List.copyOf(bindings));
    }

    private static InputBinding parseBinding(
            Path path,
            InputAction action,
            JsonNode bindingNode,
            String actionContext,
            int bindingIndex) {
        String context = actionContext + ".bindings[" + bindingIndex + "]";
        requireObject(path, bindingNode, context);
        String type = requireText(path, bindingNode, "type", context);
        InputActionComponent component = parseEnum(
                path,
                InputActionComponent.class,
                requireText(path, bindingNode, "component", context),
                context + ".component");
        double scale = requireFiniteNonZeroScale(path, bindingNode, context);

        InputBinding.Control control = switch (type) {
            case "KEY" -> {
                requireOnlyFields(path, bindingNode, KEY_FIELDS, context);
                InputKey key = parseEnum(
                        path,
                        InputKey.class,
                        requireText(path, bindingNode, "key", context),
                        context + ".key");
                yield new InputBinding.KeyControl(key);
            }
            case "MOUSE_BUTTON" -> {
                requireOnlyFields(path, bindingNode, MOUSE_BUTTON_FIELDS, context);
                InputMouseButton button = parseEnum(
                        path,
                        InputMouseButton.class,
                        requireText(path, bindingNode, "button", context),
                        context + ".button");
                yield new InputBinding.MouseButtonControl(button);
            }
            case "MOUSE_DELTA" -> {
                requireOnlyFields(path, bindingNode, MOUSE_DELTA_FIELDS, context);
                InputBinding.MouseDeltaAxis axis = parseEnum(
                        path,
                        InputBinding.MouseDeltaAxis.class,
                        requireText(path, bindingNode, "axis", context),
                        context + ".axis");
                yield new InputBinding.MouseDeltaControl(axis);
            }
            default -> throw failure(path, "unknown binding type at " + context + ": " + type);
        };

        try {
            InputBinding binding = new InputBinding(control, component, scale);
            validateActionComponent(action, binding);
            return binding;
        } catch (IllegalArgumentException exception) {
            throw failure(path, context + ": " + exception.getMessage(), exception);
        }
    }

    private static void validateActionComponent(InputAction action, InputBinding binding) {
        if (action.valueType() == InputActionValueType.DIGITAL
                && binding.component() != InputActionComponent.VALUE) {
            throw new IllegalArgumentException("digital action " + action + " requires VALUE bindings");
        }
        if (action.valueType() == InputActionValueType.VECTOR2
                && binding.component() == InputActionComponent.VALUE) {
            throw new IllegalArgumentException("vector action " + action + " requires X or Y bindings");
        }
    }

    private static double requireFiniteNonZeroScale(Path path, JsonNode bindingNode, String context) {
        JsonNode scaleNode = requireField(path, bindingNode, "scale", context);
        if (!scaleNode.isNumber()) {
            throw failure(path, context + ".scale must be a number");
        }
        double scale = scaleNode.doubleValue();
        if (!Double.isFinite(scale) || scale == 0.0d) {
            throw failure(path, context + ".scale must be finite and non-zero");
        }
        return scale;
    }

    private static String requireText(Path path, JsonNode object, String field, String context) {
        JsonNode value = requireField(path, object, field, context);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw failure(path, context + "." + field + " must be a nonblank string");
        }
        return value.textValue();
    }

    private static JsonNode requireField(Path path, JsonNode object, String field, String context) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            throw failure(path, "missing required field " + context + "." + field);
        }
        return value;
    }

    private static void requireObject(Path path, JsonNode node, String context) {
        if (node == null || !node.isObject()) {
            throw failure(path, context + " must be a JSON object");
        }
    }

    private static void requireOnlyFields(Path path, JsonNode object, Set<String> allowed, String context) {
        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) {
                throw failure(path, "unknown field " + context + "." + field);
            }
        }
    }

    private static <E extends Enum<E>> E parseEnum(
            Path path,
            Class<E> enumType,
            String value,
            String context) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw failure(path, "unknown value at " + context + ": " + value, exception);
        }
    }

    private static InputBindingLoadException failure(Path path, String message) {
        return new InputBindingLoadException(path + ": " + message);
    }

    private static InputBindingLoadException failure(Path path, String message, Throwable cause) {
        return new InputBindingLoadException(path + ": " + message, cause);
    }
}
