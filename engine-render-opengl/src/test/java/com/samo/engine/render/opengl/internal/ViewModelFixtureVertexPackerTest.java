package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class ViewModelFixtureVertexPackerTest {
    @Test
    void packsAcceptedSixVertexFixtureAndLinearColorExactly() {

        ByteBuffer bytes = ByteBuffer.allocateDirect(ViewModelFixtureVertexPacker.VERTEX_BYTES).order(ByteOrder.nativeOrder());

        ViewModelFixtureVertexPacker.write(bytes);

        assertEquals(ViewModelFixtureVertexPacker.VERTEX_BYTES, bytes.position());
        assertEquals(6, ViewModelFixtureVertexPacker.VERTEX_COUNT);
        bytes.flip();

        assertVertex(bytes, -0.12f, -0.23f, -0.50f);
        assertVertex(bytes, 0.36f, -0.23f, -0.50f);
        assertVertex(bytes, 0.36f, -0.06f, -0.50f);
        assertVertex(bytes, -0.12f, -0.23f, -0.50f);
        assertVertex(bytes, 0.36f, -0.06f, -0.50f);
        assertVertex(bytes, -0.12f, -0.06f, -0.50f);
        assertEquals(0, bytes.remaining());

    }

    private static void assertVertex(ByteBuffer bytes, float x, float y, float z) {

        assertEquals(x, bytes.getFloat());
        assertEquals(y, bytes.getFloat());
        assertEquals(z, bytes.getFloat());
        assertEquals(ViewModelFixtureVertexPacker.COLOR_RED, bytes.getFloat());
        assertEquals(ViewModelFixtureVertexPacker.COLOR_GREEN, bytes.getFloat());
        assertEquals(ViewModelFixtureVertexPacker.COLOR_BLUE, bytes.getFloat());

    }
}
