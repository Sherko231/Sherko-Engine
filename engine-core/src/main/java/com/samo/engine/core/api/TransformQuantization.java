package com.samo.engine.core.api;

import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Deterministic bounded quantization for canonical engine-space transform values. */
public final class TransformQuantization {
    public static final float POSITION_STEP_METERS = 1.0f / 64.0f;
    public static final float POSITION_MIN_METERS = -512.0f;
    public static final float POSITION_MAX_METERS = 32767.0f / 64.0f;
    public static final float POSITION_MAX_ABSOLUTE_ERROR_METERS = 1.0f / 128.0f;
    public static final float ROTATION_MAX_ANGULAR_ERROR_RADIANS = 0.0002f;

    private static final double ROTATION_COMPONENT_LIMIT = 1.0 / Math.sqrt(2.0);
    private static final double ROTATION_ENCODE_SCALE = Short.MAX_VALUE / ROTATION_COMPONENT_LIMIT;
    private static final double ROTATION_DECODE_SCALE = ROTATION_COMPONENT_LIMIT / Short.MAX_VALUE;
    private static final long ROTATION_UNIT_SQUARED_CODE_LIMIT =
            2L * Short.MAX_VALUE * Short.MAX_VALUE;

    private TransformQuantization() {}

    /** Quantizes canonical engine-space position in meters into three signed 16-bit values. */
    public static QuantizedPosition quantizePosition(Vector3fc position) {
        Objects.requireNonNull(position, "position");
        short x = quantizePositionComponent(position.x(), "position.x");
        short y = quantizePositionComponent(position.y(), "position.y");
        short z = quantizePositionComponent(position.z(), "position.z");
        return new QuantizedPosition(x, y, z);
    }

    /** Dequantizes position into the supplied caller-owned destination. */
    public static Vector3f dequantizePosition(
            QuantizedPosition quantized, Vector3f destination) {
        Objects.requireNonNull(quantized, "quantized");
        Objects.requireNonNull(destination, "destination");

        float x = quantized.x() * POSITION_STEP_METERS;
        float y = quantized.y() * POSITION_STEP_METERS;
        float z = quantized.z() * POSITION_STEP_METERS;
        return destination.set(x, y, z);
    }

    /**
     * Quantizes a finite non-zero quaternion using deterministic smallest-three encoding.
     *
     * <p>The input is normalized first. The largest-absolute component is omitted, ties select the
     * lowest component index, and the quaternion sign is canonicalized so the omitted component is
     * non-negative. Equivalent {@code q} and {@code -q} orientations therefore encode identically.
     */
    public static QuantizedRotation quantizeRotation(Quaternionfc rotation) {
        Objects.requireNonNull(rotation, "rotation");

        double x = rotation.x();
        double y = rotation.y();
        double z = rotation.z();
        double w = rotation.w();
        requireFinite(x, "rotation.x");
        requireFinite(y, "rotation.y");
        requireFinite(z, "rotation.z");
        requireFinite(w, "rotation.w");

        double lengthSquared = x * x + y * y + z * z + w * w;
        if (!(lengthSquared > 0.0) || !Double.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("rotation quaternion must have finite non-zero length");
        }

        double inverseLength = 1.0 / Math.sqrt(lengthSquared);
        x *= inverseLength;
        y *= inverseLength;
        z *= inverseLength;
        w *= inverseLength;

        int omittedComponent = 0;
        double largestMagnitude = Math.abs(x);
        double yMagnitude = Math.abs(y);
        if (yMagnitude > largestMagnitude) {
            omittedComponent = 1;
            largestMagnitude = yMagnitude;
        }
        double zMagnitude = Math.abs(z);
        if (zMagnitude > largestMagnitude) {
            omittedComponent = 2;
            largestMagnitude = zMagnitude;
        }
        double wMagnitude = Math.abs(w);
        if (wMagnitude > largestMagnitude) {
            omittedComponent = 3;
        }

        double omittedValue = component(omittedComponent, x, y, z, w);
        if (omittedValue < 0.0) {
            x = -x;
            y = -y;
            z = -z;
            w = -w;
        }

        short a;
        short b;
        short c;
        switch (omittedComponent) {
            case 0 -> {
                a = quantizeRotationComponent(y);
                b = quantizeRotationComponent(z);
                c = quantizeRotationComponent(w);
            }
            case 1 -> {
                a = quantizeRotationComponent(x);
                b = quantizeRotationComponent(z);
                c = quantizeRotationComponent(w);
            }
            case 2 -> {
                a = quantizeRotationComponent(x);
                b = quantizeRotationComponent(y);
                c = quantizeRotationComponent(w);
            }
            case 3 -> {
                a = quantizeRotationComponent(x);
                b = quantizeRotationComponent(y);
                c = quantizeRotationComponent(z);
            }
            default -> throw new AssertionError("unexpected omitted quaternion component");
        }
        return new QuantizedRotation(omittedComponent, a, b, c);
    }

    /** Dequantizes a smallest-three rotation into the supplied caller-owned destination. */
    public static Quaternionf dequantizeRotation(
            QuantizedRotation quantized, Quaternionf destination) {
        Objects.requireNonNull(quantized, "quantized");
        Objects.requireNonNull(destination, "destination");

        long aCode = quantized.a();
        long bCode = quantized.b();
        long cCode = quantized.c();
        long storedSquaredCodeLength =
                aCode * aCode + bCode * bCode + cCode * cCode;
        if (storedSquaredCodeLength >= ROTATION_UNIT_SQUARED_CODE_LIMIT) {
            throw new IllegalArgumentException(
                    "quantized rotation components do not leave a valid omitted component");
        }

        double a = aCode * ROTATION_DECODE_SCALE;
        double b = bCode * ROTATION_DECODE_SCALE;
        double c = cCode * ROTATION_DECODE_SCALE;
        double storedLengthSquared = a * a + b * b + c * c;
        if (!(storedLengthSquared < 1.0)) {
            throw new IllegalArgumentException(
                    "quantized rotation components do not leave a valid omitted component");
        }

        double omitted = Math.sqrt(1.0 - storedLengthSquared);
        double x;
        double y;
        double z;
        double w;
        switch (quantized.omittedComponent()) {
            case 0 -> {
                x = omitted;
                y = a;
                z = b;
                w = c;
            }
            case 1 -> {
                x = a;
                y = omitted;
                z = b;
                w = c;
            }
            case 2 -> {
                x = a;
                y = b;
                z = omitted;
                w = c;
            }
            case 3 -> {
                x = a;
                y = b;
                z = c;
                w = omitted;
            }
            default -> throw new AssertionError("validated omitted quaternion component became invalid");
        }

        double lengthSquared = x * x + y * y + z * z + w * w;
        if (!(lengthSquared > 0.0) || !Double.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("reconstructed quaternion must have finite non-zero length");
        }
        double inverseLength = 1.0 / Math.sqrt(lengthSquared);
        float outputX = (float) (x * inverseLength);
        float outputY = (float) (y * inverseLength);
        float outputZ = (float) (z * inverseLength);
        float outputW = (float) (w * inverseLength);
        requireFinite(outputX, "decoded rotation.x");
        requireFinite(outputY, "decoded rotation.y");
        requireFinite(outputZ, "decoded rotation.z");
        requireFinite(outputW, "decoded rotation.w");
        return destination.set(outputX, outputY, outputZ, outputW);
    }

    private static short quantizePositionComponent(float value, String name) {
        requireFinite(value, name);
        if (value < POSITION_MIN_METERS || value > POSITION_MAX_METERS) {
            throw new IllegalArgumentException(
                    name
                            + " must be within ["
                            + POSITION_MIN_METERS
                            + ", "
                            + POSITION_MAX_METERS
                            + "] meters");
        }
        long encoded = Math.round((double) value / POSITION_STEP_METERS);
        if (encoded < Short.MIN_VALUE || encoded > Short.MAX_VALUE) {
            throw new IllegalArgumentException(
                    name + " cannot be represented by position quantization");
        }
        return (short) encoded;
    }

    private static short quantizeRotationComponent(double value) {
        long encoded = Math.round(value * ROTATION_ENCODE_SCALE);
        if (encoded < -Short.MAX_VALUE || encoded > Short.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "normalized rotation component exceeds smallest-three representable range");
        }
        return (short) encoded;
    }

    private static double component(int index, double x, double y, double z, double w) {
        return switch (index) {
            case 0 -> x;
            case 1 -> y;
            case 2 -> z;
            case 3 -> w;
            default -> throw new AssertionError("unexpected quaternion component index");
        };
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    /** Signed-short quantized canonical position values. */
    public record QuantizedPosition(short x, short y, short z) {}

    /** Smallest-three quaternion values with one omitted canonical non-negative component. */
    public record QuantizedRotation(int omittedComponent, short a, short b, short c) {
        public QuantizedRotation {
            if (omittedComponent < 0 || omittedComponent > 3) {
                throw new IllegalArgumentException("omittedComponent must be within [0, 3]");
            }
            if (a == Short.MIN_VALUE || b == Short.MIN_VALUE || c == Short.MIN_VALUE) {
                throw new IllegalArgumentException(
                        "quantized rotation component must not use reserved Short.MIN_VALUE");
            }
        }
    }
}
