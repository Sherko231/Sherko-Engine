package com.samo.engine.render.opengl.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.samo.engine.core.api.Aabb3f;
import com.samo.engine.core.api.DebugAabb;
import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugRay;
import com.samo.engine.core.api.DebugSphere;
import com.samo.engine.core.api.Ray3f;
import com.samo.engine.core.api.Sphere3f;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DebugLineVertexPackerTest {
    @Test
    void packsRepresentativePrimitiveFamiliesDeterministically() {
        DebugColor red = new DebugColor(1.0f, 0.0f, 0.0f);
        DebugColor green = new DebugColor(0.0f, 1.0f, 0.0f);
        DebugColor blue = new DebugColor(0.0f, 0.0f, 1.0f);
        DebugColor white = new DebugColor(1.0f, 1.0f, 1.0f);
        DebugFrame frame = new DebugFrame(
            List.of(new DebugLine(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, red), new DebugAabb(new Aabb3f(new Vector3f(-1.0f, -2.0f, -3.0f), new Vector3f(1.0f, 2.0f, 3.0f)), green),
                new DebugSphere(new Sphere3f(new Vector3f(2.0f, 3.0f, 4.0f), 1.0f), blue),
                new DebugRay(new Ray3f(new Vector3f(5.0f, 6.0f, 7.0f), new Vector3f(0.0f, 0.0f, -2.0f)), 2.0f, white)),
            List.of());
        ByteBuffer bytes = ByteBuffer.allocateDirect(DebugLineVertexPacker.MAX_BYTES).order(ByteOrder.nativeOrder());

        int vertices = DebugLineVertexPacker.write(frame, bytes);

        assertEquals(2 + 24 + 96 + 2, vertices);
        bytes.flip();
        assertVertex(bytes, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f);
        assertVertex(bytes, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f);
        assertVertex(bytes, -1.0f, -2.0f, -3.0f, 0.0f, 1.0f, 0.0f);
        assertVertex(bytes, 1.0f, -2.0f, -3.0f, 0.0f, 1.0f, 0.0f);

        int rayOffsetVertices = 2 + 24 + 96;
        bytes.position(rayOffsetVertices * DebugLineVertexPacker.VERTEX_STRIDE_BYTES);
        assertVertex(bytes, 5.0f, 6.0f, 7.0f, 1.0f, 1.0f, 1.0f);
        assertVertex(bytes, 5.0f, 6.0f, 5.0f, 1.0f, 1.0f, 1.0f);
    }

    @Test
    void worstCaseSphereCountFitsDeclaredCapacityExactly() {
        DebugSphere sphere = new DebugSphere(new Sphere3f(new Vector3f(), 1.0f), new DebugColor(1.0f, 1.0f, 1.0f));
        DebugFrame frame = new DebugFrame(java.util.Collections.nCopies(DebugFrame.MAX_PRIMITIVES, sphere), List.of());
        ByteBuffer bytes = ByteBuffer.allocateDirect(DebugLineVertexPacker.MAX_BYTES).order(ByteOrder.nativeOrder());

        assertEquals(DebugLineVertexPacker.MAX_VERTEX_COUNT, DebugLineVertexPacker.write(frame, bytes));
        assertEquals(DebugLineVertexPacker.MAX_BYTES, bytes.position());
    }

    private static void assertVertex(ByteBuffer bytes, float x, float y, float z, float red, float green, float blue) {
        assertEquals(x, bytes.getFloat());
        assertEquals(y, bytes.getFloat());
        assertEquals(z, bytes.getFloat());
        assertEquals(red, bytes.getFloat());
        assertEquals(green, bytes.getFloat());
        assertEquals(blue, bytes.getFloat());
    }
}
