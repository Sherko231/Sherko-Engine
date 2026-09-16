package com.samo.engine.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InputResponseSettingsTest {
    @Test
    void defaultsAreNeutral() {
        InputResponseSettings settings = InputResponseSettings.defaults();

        assertThat(settings.mouseSensitivity()).isEqualTo(1.0d);
        assertThat(settings.invertMouseY()).isFalse();
        assertThat(settings.controllerDeadZone()).isEqualTo(0.0d);
        assertThat(settings.controllerCurveExponent()).isEqualTo(1.0d);
        assertThat(settings.applyMouseX(2.5d)).isEqualTo(2.5d);
        assertThat(settings.applyMouseY(-3.0d)).isEqualTo(-3.0d);
        assertThat(settings.applyControllerAxis(0.4d)).isEqualTo(0.4d);
    }

    @Test
    void appliesMouseSensitivityAndYInversionInOrder() {
        InputResponseSettings settings = new InputResponseSettings(2.0d, true, 0.0d, 1.0d);

        assertThat(settings.applyMouseX(3.0d)).isEqualTo(6.0d);
        assertThat(settings.applyMouseY(-4.0d)).isEqualTo(8.0d);
    }

    @Test
    void zeroMouseSensitivityProducesZero() {
        InputResponseSettings settings = new InputResponseSettings(0.0d, true, 0.0d, 1.0d);

        assertThat(settings.applyMouseX(42.0d)).isEqualTo(0.0d);
        assertThat(settings.applyMouseY(-42.0d)).isEqualTo(0.0d);
    }

    @Test
    void validatesSettingsValues() {
        assertThatThrownBy(() -> new InputResponseSettings(-0.1d, false, 0.0d, 1.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mouseSensitivity");
        assertThatThrownBy(() -> new InputResponseSettings(Double.NaN, false, 0.0d, 1.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mouseSensitivity");
        assertThatThrownBy(() -> new InputResponseSettings(1.0d, false, -0.1d, 1.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("controllerDeadZone");
        assertThatThrownBy(() -> new InputResponseSettings(1.0d, false, 1.0d, 1.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("controllerDeadZone");
        assertThatThrownBy(() -> new InputResponseSettings(1.0d, false, 0.0d, 0.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("controllerCurveExponent");
        assertThatThrownBy(() -> new InputResponseSettings(1.0d, false, 0.0d, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("controllerCurveExponent");
    }

    @Test
    void rejectsNonFiniteMouseInputAndOverflow() {
        InputResponseSettings settings = new InputResponseSettings(Double.MAX_VALUE, false, 0.0d, 1.0d);

        assertThatThrownBy(() -> settings.applyMouseX(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> settings.applyMouseY(2.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    void controllerDeadZoneAndCurveUseHandCalculatedMapping() {
        InputResponseSettings settings = new InputResponseSettings(1.0d, false, 0.2d, 2.0d);

        assertThat(settings.applyControllerAxis(0.0d)).isEqualTo(0.0d);
        assertThat(settings.applyControllerAxis(0.2d)).isEqualTo(0.0d);
        assertThat(settings.applyControllerAxis(-0.2d)).isEqualTo(0.0d);
        assertThat(settings.applyControllerAxis(0.6d)).isEqualTo(0.25d);
        assertThat(settings.applyControllerAxis(-0.6d)).isEqualTo(-0.25d);
        assertThat(settings.applyControllerAxis(1.0d)).isEqualTo(1.0d);
        assertThat(settings.applyControllerAxis(-1.0d)).isEqualTo(-1.0d);
    }

    @Test
    void linearControllerCurveRenormalizesOutsideDeadZone() {
        InputResponseSettings settings = new InputResponseSettings(1.0d, false, 0.25d, 1.0d);

        assertThat(settings.applyControllerAxis(0.625d)).isEqualTo(0.5d);
        assertThat(settings.applyControllerAxis(-0.625d)).isEqualTo(-0.5d);
    }

    @Test
    void rejectsInvalidControllerAxisInput() {
        InputResponseSettings settings = InputResponseSettings.defaults();

        assertThatThrownBy(() -> settings.applyControllerAxis(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> settings.applyControllerAxis(1.0001d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[-1, 1]");
        assertThatThrownBy(() -> settings.applyControllerAxis(-1.0001d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[-1, 1]");
    }
}
