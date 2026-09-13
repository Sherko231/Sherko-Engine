package com.samo.engine.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InputActionBindingsTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsCommittedFixtureWithEveryRequiredActionAndControlKind() throws IOException {
        InputActionBindings bindings = InputActionBindings.load(copyFixture());

        assertThat(bindings.asMap()).hasSize(InputAction.values().length);
        assertThat(bindings.bindingsFor(InputAction.MOVE)).containsExactly(
                new InputBinding(
                        new InputBinding.KeyControl(InputKey.W), InputActionComponent.Y, 1.0d),
                new InputBinding(
                        new InputBinding.KeyControl(InputKey.S), InputActionComponent.Y, -1.0d),
                new InputBinding(
                        new InputBinding.KeyControl(InputKey.D), InputActionComponent.X, 1.0d),
                new InputBinding(
                        new InputBinding.KeyControl(InputKey.A), InputActionComponent.X, -1.0d));
        assertThat(bindings.bindingsFor(InputAction.LOOK)).containsExactly(
                new InputBinding(
                        new InputBinding.MouseDeltaControl(InputBinding.MouseDeltaAxis.X),
                        InputActionComponent.X,
                        1.0d),
                new InputBinding(
                        new InputBinding.MouseDeltaControl(InputBinding.MouseDeltaAxis.Y),
                        InputActionComponent.Y,
                        1.0d));
        assertThat(bindings.bindingsFor(InputAction.GRAB)).containsExactly(
                new InputBinding(
                        new InputBinding.MouseButtonControl(InputMouseButton.LEFT),
                        InputActionComponent.VALUE,
                        1.0d));
    }

    @Test
    void exposesTheDeclaredActionValueTypes() {
        assertThat(InputAction.MOVE.valueType()).isEqualTo(InputActionValueType.VECTOR2);
        assertThat(InputAction.LOOK.valueType()).isEqualTo(InputActionValueType.VECTOR2);
        assertThat(List.of(InputAction.values()))
                .filteredOn(action -> action != InputAction.MOVE && action != InputAction.LOOK)
                .allMatch(action -> action.valueType() == InputActionValueType.DIGITAL);
    }

    @Test
    void defensivelyOwnsBindingsAndExposesImmutableCollections() {
        EnumMap<InputAction, List<InputBinding>> source = completeBindings();
        @SuppressWarnings("unchecked")
        ArrayList<InputBinding> moveBindings = (ArrayList<InputBinding>) source.get(InputAction.MOVE);
        InputActionBindings bindings = new InputActionBindings(source);

        moveBindings.add(new InputBinding(
                new InputBinding.KeyControl(InputKey.D), InputActionComponent.X, 1.0d));

        assertThat(bindings.bindingsFor(InputAction.MOVE)).hasSize(1);
        assertThatThrownBy(() -> bindings.bindingsFor(InputAction.MOVE).add(
                        new InputBinding(
                                new InputBinding.KeyControl(InputKey.A),
                                InputActionComponent.X,
                                -1.0d)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> bindings.asMap().put(
                        InputAction.MOVE,
                        List.of(new InputBinding(
                                new InputBinding.KeyControl(InputKey.W),
                                InputActionComponent.X,
                                1.0d))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMissingDuplicateUnknownAndEmptyActions() throws IOException {
        assertLoadFails("{\"schemaVersion\":1,\"actions\":[]}", "missing bindings for action");
        assertLoadFails(
                "{\"schemaVersion\":1,\"actions\":["
                        + jumpEntry() + "," + jumpEntry() + "]}",
                "duplicate action entry: JUMP");
        assertLoadFails(
                "{\"schemaVersion\":1,\"actions\":[{\"action\":\"FLY\",\"bindings\":[]}]}",
                "unknown value");
        assertLoadFails(
                "{\"schemaVersion\":1,\"actions\":[{\"action\":\"JUMP\",\"bindings\":[]}]}",
                "must have at least one binding");
    }

    @Test
    void rejectsMalformedJsonMissingFilesAndNullPath() throws IOException {
        assertLoadFails("{", "failed to read binding JSON");
        assertThatThrownBy(() -> InputActionBindings.load(tempDir.resolve("missing.json")))
                .isInstanceOf(InputBindingLoadException.class)
                .hasMessageContaining("missing or unreadable");
        assertThatThrownBy(() -> InputActionBindings.load(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("path");
    }

    @Test
    void rejectsUnknownPropertiesAndSchemaVersions() throws IOException {
        assertLoadFails(
                "{\"schemaVersion\":1,\"actions\":[],\"extra\":true}",
                "unknown field root.extra");
        assertLoadFails(
                "{\"schemaVersion\":2,\"actions\":[]}",
                "schemaVersion must equal 1");
        assertLoadFails(
                "{\"schemaVersion\":1,\"actions\":[{\"action\":\"JUMP\","
                        + "\"bindings\":[],\"extra\":true}]}",
                "unknown field actions[0].extra");
        assertLoadFails(
                "{\"schemaVersion\":1,\"actions\":[{\"action\":\"JUMP\",\"bindings\":["
                        + "{\"type\":\"KEY\",\"key\":\"SPACE\",\"component\":\"VALUE\","
                        + "\"scale\":1.0,\"extra\":true}]}]}",
                "unknown field actions[0].bindings[0].extra");
    }

    @Test
    void rejectsUnknownBindingTypesAndControls() throws IOException {
        assertLoadFails(
                actionDocument("JUMP", "{\"type\":\"CONTROLLER\",\"component\":\"VALUE\",\"scale\":1.0}"),
                "unknown binding type");
        assertLoadFails(
                actionDocument(
                        "JUMP",
                        "{\"type\":\"KEY\",\"key\":\"UP\",\"component\":\"VALUE\",\"scale\":1.0}"),
                "unknown value");
        assertLoadFails(
                actionDocument(
                        "GRAB",
                        "{\"type\":\"MOUSE_BUTTON\",\"button\":\"BUTTON_99\","
                                + "\"component\":\"VALUE\",\"scale\":1.0}"),
                "unknown value");
        assertLoadFails(
                actionDocument(
                        "LOOK",
                        "{\"type\":\"MOUSE_DELTA\",\"axis\":\"Z\","
                                + "\"component\":\"X\",\"scale\":1.0}"),
                "unknown value");
    }

    @Test
    void rejectsInvalidComponentsScalesAndDuplicateBindings() throws IOException {
        assertLoadFails(
                actionDocument(
                        "JUMP",
                        "{\"type\":\"KEY\",\"key\":\"SPACE\",\"component\":\"X\",\"scale\":1.0}"),
                "requires VALUE bindings");
        assertLoadFails(
                actionDocument(
                        "MOVE",
                        "{\"type\":\"KEY\",\"key\":\"W\",\"component\":\"VALUE\",\"scale\":1.0}"),
                "requires X or Y bindings");
        assertLoadFails(
                actionDocument(
                        "JUMP",
                        "{\"type\":\"KEY\",\"key\":\"SPACE\",\"component\":\"VALUE\",\"scale\":0.0}"),
                "finite and non-zero");
        String binding = "{\"type\":\"KEY\",\"key\":\"SPACE\",\"component\":\"VALUE\",\"scale\":1.0}";
        assertLoadFails(
                "{\"schemaVersion\":1,\"actions\":[{\"action\":\"JUMP\",\"bindings\":["
                        + binding + "," + binding + "]}]}",
                "duplicate binding for action JUMP");
    }

    @Test
    void bindingConstructorRejectsNonFiniteAndZeroScale() {
        InputBinding.Control control = new InputBinding.KeyControl(InputKey.SPACE);

        assertThatThrownBy(() -> new InputBinding(control, InputActionComponent.VALUE, 0.0d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InputBinding(control, InputActionComponent.VALUE, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InputBinding(control, InputActionComponent.VALUE, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateJsonObjectFields() throws IOException {
        assertLoadFails(
                "{\"schemaVersion\":1,\"schemaVersion\":1,\"actions\":[]}",
                "failed to read binding JSON");
    }

    private Path copyFixture() throws IOException {
        Path destination = tempDir.resolve("action-bindings-v1.json");
        try (InputStream stream = Objects.requireNonNull(
                getClass().getResourceAsStream("/input/action-bindings-v1.json"),
                "fixture resource")) {
            Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination;
    }

    private void assertLoadFails(String json, String messagePart) throws IOException {
        Path path = Files.createTempFile(tempDir, "bindings-", ".json");
        Files.writeString(path, json, StandardCharsets.UTF_8);
        assertThatThrownBy(() -> InputActionBindings.load(path))
                .isInstanceOf(InputBindingLoadException.class)
                .hasMessageContaining(messagePart);
    }

    private static String jumpEntry() {
        return "{\"action\":\"JUMP\",\"bindings\":["
                + "{\"type\":\"KEY\",\"key\":\"SPACE\",\"component\":\"VALUE\",\"scale\":1.0}]}";
    }

    private static String actionDocument(String action, String binding) {
        return "{\"schemaVersion\":1,\"actions\":[{\"action\":\""
                + action + "\",\"bindings\":[" + binding + "]}]}";
    }

    private static EnumMap<InputAction, List<InputBinding>> completeBindings() {
        EnumMap<InputAction, List<InputBinding>> bindings = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            InputActionComponent component = action.valueType() == InputActionValueType.DIGITAL
                    ? InputActionComponent.VALUE
                    : InputActionComponent.X;
            ArrayList<InputBinding> values = new ArrayList<>();
            values.add(new InputBinding(new InputBinding.KeyControl(InputKey.W), component, 1.0d));
            bindings.put(action, values);
        }
        return bindings;
    }
}
