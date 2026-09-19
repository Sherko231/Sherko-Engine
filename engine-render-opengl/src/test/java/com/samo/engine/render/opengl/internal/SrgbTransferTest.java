package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SrgbTransferTest {
    private static final float TOLERANCE = 1.0e-6f;

    @Test
    void encodesPiecewiseIecSrgbReferenceValues() {
        assertEquals(0.0f, SrgbTransfer.encodeLinear(0.0f), TOLERANCE);
        assertEquals(0.040449936f, SrgbTransfer.encodeLinear(0.0031308f), TOLERANCE);
        assertEquals(0.7353569f, SrgbTransfer.encodeLinear(0.5f), TOLERANCE);
        assertEquals(1.0f, SrgbTransfer.encodeLinear(1.0f), TOLERANCE);
    }

    @Test
    void rejectsNonFiniteOrOutOfRangeLinearValues() {
        assertThrows(IllegalArgumentException.class, () -> SrgbTransfer.encodeLinear(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> SrgbTransfer.encodeLinear(-0.001f));
        assertThrows(IllegalArgumentException.class, () -> SrgbTransfer.encodeLinear(1.001f));
    }
}
