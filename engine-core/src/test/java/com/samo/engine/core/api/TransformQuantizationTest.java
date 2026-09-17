package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class TransformQuantizationTest {
    private static final double UNIT_EPSILON = 1.0e-6;

    @Test
    void positionExactGridValuesRoundTripExactlyIntoCallerDestination() {
        TransformQuantization.QuantizedPosition quantized =
                TransformQuantization.quantizePosition(new Vector3f(1.5f, -2.25f, 0.015625f));

        assertEquals(new TransformQuantization.QuantizedPosition(
                (short) 96, (short) -144, (short) 1), quantized);

        Vector3f destination = new Vector3f(91.0f, 92.0f, 93.0f);
        assertSame(destination, TransformQuantization.dequantizePosition(quantized, destination));
        assertEquals(1.5f, destination.x, 0.0f);
        assertEquals(-2.25f, destination.y, 0.0f);
        assertEquals(0.015625f, destination.z, 0.0f);
    }

    @Test
    void positionEndpointsUseFullSignedShortDomain() {
        TransformQuantization.QuantizedPosition quantized = TransformQuantization.quantizePosition(
                new Vector3f(
                        TransformQuantization.POSITION_MIN_METERS,
                        TransformQuantization.POSITION_MAX_METERS,
                        0.0f));

        assertEquals(Short.MIN_VALUE, quantized.x());
        assertEquals(Short.MAX_VALUE, quantized.y());
        assertEquals((short) 0, quantized.z());

        Vector3f decoded = TransformQuantization.dequantizePosition(quantized, new Vector3f());
        assertEquals(TransformQuantization.POSITION_MIN_METERS, decoded.x, 0.0f);
        assertEquals(TransformQuantization.POSITION_MAX_METERS, decoded.y, 0.0f);
        assertEquals(0.0f, decoded.z, 0.0f);
    }

    @Test
    void positionRoundingStaysWithinWrittenPerAxisErrorBound() {
        float halfStep = TransformQuantization.POSITION_STEP_METERS * 0.5f;
        Vector3f source = new Vector3f(halfStep, -halfStep, 12.345f);
        TransformQuantization.QuantizedPosition quantized =
                TransformQuantization.quantizePosition(source);
        Vector3f decoded = TransformQuantization.dequantizePosition(quantized, new Vector3f());

        assertPositionErrorWithinBound(source.x, decoded.x);
        assertPositionErrorWithinBound(source.y, decoded.y);
        assertPositionErrorWithinBound(source.z, decoded.z);
    }

    @Test
    void positionNearStepFixturesCatchScaleAndRoundingErrors() {
        float step = TransformQuantization.POSITION_STEP_METERS;

        assertEquals((short) 0,
                TransformQuantization.quantizePosition(new Vector3f(step * 0.49f, 0, 0)).x());
        assertEquals((short) 1,
                TransformQuantization.quantizePosition(new Vector3f(step * 0.51f, 0, 0)).x());
        assertEquals((short) 0,
                TransformQuantization.quantizePosition(new Vector3f(-step * 0.49f, 0, 0)).x());
        assertEquals((short) -1,
                TransformQuantization.quantizePosition(new Vector3f(-step * 0.51f, 0, 0)).x());
    }

    @Test
    void invalidPositionInputsRejectWithoutSaturation() {
        assertThrows(NullPointerException.class, () -> TransformQuantization.quantizePosition(null));
        assertThrows(IllegalArgumentException.class,
                () -> TransformQuantization.quantizePosition(new Vector3f(Float.NaN, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> TransformQuantization.quantizePosition(new Vector3f(Float.POSITIVE_INFINITY, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> TransformQuantization.quantizePosition(
                        new Vector3f(Math.nextDown(TransformQuantization.POSITION_MIN_METERS), 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> TransformQuantization.quantizePosition(
                        new Vector3f(Math.nextUp(TransformQuantization.POSITION_MAX_METERS), 0, 0)));
    }

    @Test
    void positionNullDecodeInputDoesNotMutateDestination() {
        Vector3f destination = new Vector3f(11.0f, 12.0f, 13.0f);
        assertThrows(NullPointerException.class,
                () -> TransformQuantization.dequantizePosition(null, destination));
        assertEquals(11.0f, destination.x, 0.0f);
        assertEquals(12.0f, destination.y, 0.0f);
        assertEquals(13.0f, destination.z, 0.0f);
        assertThrows(NullPointerException.class,
                () -> TransformQuantization.dequantizePosition(
                        new TransformQuantization.QuantizedPosition((short) 0, (short) 0, (short) 0),
                        null));
    }

    @Test
    void identityAndAxisHalfTurnsUseExpectedOmittedComponents() {
        assertEquals(
                new TransformQuantization.QuantizedRotation(3, (short) 0, (short) 0, (short) 0),
                TransformQuantization.quantizeRotation(new Quaternionf(0, 0, 0, 1)));
        assertEquals(
                new TransformQuantization.QuantizedRotation(0, (short) 0, (short) 0, (short) 0),
                TransformQuantization.quantizeRotation(new Quaternionf(1, 0, 0, 0)));
        assertEquals(
                new TransformQuantization.QuantizedRotation(1, (short) 0, (short) 0, (short) 0),
                TransformQuantization.quantizeRotation(new Quaternionf(0, 1, 0, 0)));
        assertEquals(
                new TransformQuantization.QuantizedRotation(2, (short) 0, (short) 0, (short) 0),
                TransformQuantization.quantizeRotation(new Quaternionf(0, 0, 1, 0)));
    }

    @Test
    void quaternionSignAndInputMagnitudeCanonicalizeToOneRecord() {
        TransformQuantization.QuantizedRotation base =
                TransformQuantization.quantizeRotation(new Quaternionf(1, 2, 3, 4));
        TransformQuantization.QuantizedRotation negated =
                TransformQuantization.quantizeRotation(new Quaternionf(-1, -2, -3, -4));
        TransformQuantization.QuantizedRotation scaled =
                TransformQuantization.quantizeRotation(new Quaternionf(2, 4, 6, 8));

        assertEquals(base, negated);
        assertEquals(base, scaled);
    }

    @Test
    void equalLargestComponentsChooseLowestIndexAndKnownCodes() {
        TransformQuantization.QuantizedRotation tie =
                TransformQuantization.quantizeRotation(new Quaternionf(1, 1, 0, 0));
        assertEquals(
                new TransformQuantization.QuantizedRotation(
                        0, Short.MAX_VALUE, (short) 0, (short) 0),
                tie);

        float rootHalf = (float) Math.sqrt(0.5);
        TransformQuantization.QuantizedRotation ordered =
                TransformQuantization.quantizeRotation(new Quaternionf(0.5f, rootHalf, -0.5f, 0.0f));
        assertEquals(
                new TransformQuantization.QuantizedRotation(
                        1, (short) 23170, (short) -23170, (short) 0),
                ordered);
    }

    @Test
    void everyQuaternionComponentCanBeOmitted() {
        assertEquals(0, TransformQuantization.quantizeRotation(new Quaternionf(4, 1, 2, 3)).omittedComponent());
        assertEquals(1, TransformQuantization.quantizeRotation(new Quaternionf(1, 4, 2, 3)).omittedComponent());
        assertEquals(2, TransformQuantization.quantizeRotation(new Quaternionf(1, 2, 4, 3)).omittedComponent());
        assertEquals(3, TransformQuantization.quantizeRotation(new Quaternionf(1, 2, 3, 4)).omittedComponent());
    }

    @Test
    void decodedRotationUsesCallerDestinationAndRemainsUnitLength() {
        Quaternionf source = new Quaternionf(0.2f, -0.7f, 0.1f, 0.65f);
        TransformQuantization.QuantizedRotation quantized =
                TransformQuantization.quantizeRotation(source);
        Quaternionf destination = new Quaternionf(5, 6, 7, 8);

        assertSame(destination, TransformQuantization.dequantizeRotation(quantized, destination));
        assertRotationWithinBound(source, destination);
        assertUnit(destination);
    }

    @Test
    void fixedInteriorQuaternionFixturesStayWithinAngularErrorBound() {
        assertRotationRoundTrip(new Quaternionf(1, 2, 3, 4));
        assertRotationRoundTrip(new Quaternionf(-0.31f, 0.17f, 0.83f, -0.42f));
        assertRotationRoundTrip(new Quaternionf(0.499f, -0.501f, 0.503f, -0.497f));
        assertRotationRoundTrip(new Quaternionf(0.001f, 0.707f, -0.001f, 0.7072f));
        assertRotationRoundTrip(new Quaternionf(-0.62f, -0.11f, 0.44f, 0.64f));
    }

    @Test
    void invalidQuaternionInputsAndRecordFieldsReject() {
        assertThrows(NullPointerException.class, () -> TransformQuantization.quantizeRotation(null));
        assertThrows(IllegalArgumentException.class,
                () -> TransformQuantization.quantizeRotation(new Quaternionf(0, 0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> TransformQuantization.quantizeRotation(new Quaternionf(Float.NaN, 0, 0, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> TransformQuantization.quantizeRotation(
                        new Quaternionf(Float.POSITIVE_INFINITY, 0, 0, 1)));

        assertThrows(IllegalArgumentException.class,
                () -> new TransformQuantization.QuantizedRotation(-1, (short) 0, (short) 0, (short) 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TransformQuantization.QuantizedRotation(4, (short) 0, (short) 0, (short) 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TransformQuantization.QuantizedRotation(0, Short.MIN_VALUE, (short) 0, (short) 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TransformQuantization.QuantizedRotation(0, (short) 0, Short.MIN_VALUE, (short) 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TransformQuantization.QuantizedRotation(0, (short) 0, (short) 0, Short.MIN_VALUE));
    }

    @Test
    void malformedQuaternionDecodeRejectsBeforeDestinationMutation() {
        TransformQuantization.QuantizedRotation malformed =
                new TransformQuantization.QuantizedRotation(
                        3, Short.MAX_VALUE, Short.MAX_VALUE, Short.MAX_VALUE);
        Quaternionf destination = new Quaternionf(5, 6, 7, 8);

        assertThrows(IllegalArgumentException.class,
                () -> TransformQuantization.dequantizeRotation(malformed, destination));
        assertEquals(5.0f, destination.x, 0.0f);
        assertEquals(6.0f, destination.y, 0.0f);
        assertEquals(7.0f, destination.z, 0.0f);
        assertEquals(8.0f, destination.w, 0.0f);

        assertThrows(NullPointerException.class,
                () -> TransformQuantization.dequantizeRotation(null, destination));
        assertEquals(5.0f, destination.x, 0.0f);
        assertThrows(NullPointerException.class,
                () -> TransformQuantization.dequantizeRotation(
                        new TransformQuantization.QuantizedRotation(3, (short) 0, (short) 0, (short) 0),
                        null));
    }

    private static void assertPositionErrorWithinBound(float source, float decoded) {
        assertTrue(Math.abs((double) source - decoded)
                <= TransformQuantization.POSITION_MAX_ABSOLUTE_ERROR_METERS);
    }

    private static void assertRotationRoundTrip(Quaternionf source) {
        TransformQuantization.QuantizedRotation quantized =
                TransformQuantization.quantizeRotation(source);
        Quaternionf decoded = TransformQuantization.dequantizeRotation(quantized, new Quaternionf());
        assertRotationWithinBound(source, decoded);
        assertUnit(decoded);
    }

    private static void assertRotationWithinBound(Quaternionfc source, Quaternionfc decoded) {
        double sourceLength = Math.sqrt(
                (double) source.x() * source.x()
                        + (double) source.y() * source.y()
                        + (double) source.z() * source.z()
                        + (double) source.w() * source.w());
        double decodedLength = Math.sqrt(
                (double) decoded.x() * decoded.x()
                        + (double) decoded.y() * decoded.y()
                        + (double) decoded.z() * decoded.z()
                        + (double) decoded.w() * decoded.w());
        double dot = ((double) source.x() * decoded.x()
                        + (double) source.y() * decoded.y()
                        + (double) source.z() * decoded.z()
                        + (double) source.w() * decoded.w())
                / (sourceLength * decodedLength);
        dot = Math.min(1.0, Math.abs(dot));
        double angularError = 2.0 * Math.acos(dot);
        assertTrue(
                angularError <= TransformQuantization.ROTATION_MAX_ANGULAR_ERROR_RADIANS,
                () -> "angular error " + angularError + " exceeds written bound");
    }

    private static void assertUnit(Quaternionfc value) {
        double length = Math.sqrt(
                (double) value.x() * value.x()
                        + (double) value.y() * value.y()
                        + (double) value.z() * value.z()
                        + (double) value.w() * value.w());
        assertEquals(1.0, length, UNIT_EPSILON);
    }
}
