package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.Aabb3f;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.joml.Vector3f;

/** Fixed CPU-side geometry and texture data for the renderer-owned reference room. */
final class ReferenceRoomFixture {
    static final int VERTEX_COUNT = 24;
    static final int INDEX_COUNT = 36;
    static final int VERTEX_BYTES = VERTEX_COUNT * 8 * Float.BYTES;
    static final int INDEX_BYTES = INDEX_COUNT * Integer.BYTES;
    static final int TEXTURE_WIDTH = 4;
    static final int TEXTURE_HEIGHT = 4;
    static final Aabb3f WORLD_BOUNDS = new Aabb3f(new Vector3f(-2.0f, -1.5f, -3.0f), new Vector3f(2.0f, 1.5f, 0.5f));

    private ReferenceRoomFixture() {
    }

    static ByteBuffer vertices() {
        ByteBuffer data = ByteBuffer.allocateDirect(VERTEX_BYTES).order(ByteOrder.nativeOrder());

        putRoomQuad(data, -2.0f, -1.5f, -3.0f, 2.0f, -1.5f, -3.0f, 2.0f, 1.5f, -3.0f, -2.0f, 1.5f, -3.0f, 0.0f, 0.0f, 1.0f);
        putRoomQuad(data, -2.0f, -1.5f, 0.5f, 2.0f, -1.5f, 0.5f, 2.0f, -1.5f, -3.0f, -2.0f, -1.5f, -3.0f, 0.0f, 1.0f, 0.0f);
        putRoomQuad(data, -2.0f, 1.5f, -3.0f, 2.0f, 1.5f, -3.0f, 2.0f, 1.5f, 0.5f, -2.0f, 1.5f, 0.5f, 0.0f, -1.0f, 0.0f);
        putRoomQuad(data, -2.0f, -1.5f, 0.5f, -2.0f, -1.5f, -3.0f, -2.0f, 1.5f, -3.0f, -2.0f, 1.5f, 0.5f, 1.0f, 0.0f, 0.0f);
        putRoomQuad(data, 2.0f, -1.5f, -3.0f, 2.0f, -1.5f, 0.5f, 2.0f, 1.5f, 0.5f, 2.0f, 1.5f, -3.0f, -1.0f, 0.0f, 0.0f);
        putRoomQuadUv(data, -1.20f, -0.80f, -1.0f, 1.20f, -0.80f, -1.0f, 1.20f, 0.80f, -1.0f, -1.20f, 0.80f, -1.0f, 0.0f, 0.0f, 1.0f, 0.25f, 0.25f);
        return data.flip();
    }

    static ByteBuffer indices() {
        ByteBuffer data = ByteBuffer.allocateDirect(INDEX_BYTES).order(ByteOrder.nativeOrder());
        for (int quad = 0; quad < 6; quad++) {
            int base = quad * 4;
            data.putInt(base);
            data.putInt(base + 1);
            data.putInt(base + 2);
            data.putInt(base);
            data.putInt(base + 2);
            data.putInt(base + 3);
        }
        return data.flip();
    }

    static ByteBuffer textureRgba() {
        int[] rgba = {128, 128, 128, 255, 70, 150, 210, 255, 210, 110, 70, 255, 70, 150, 210, 255, 70, 150, 210, 255, 210, 110, 70, 255, 70, 150, 210, 255, 210, 110, 70, 255, 210,
            110, 70, 255, 70, 150, 210, 255, 210, 110, 70, 255, 70, 150, 210, 255, 70, 150, 210, 255, 210, 110, 70, 255, 70, 150, 210, 255, 210, 110, 70, 255};
        ByteBuffer data = ByteBuffer.allocateDirect(rgba.length).order(ByteOrder.nativeOrder());
        for (int component : rgba) {
            data.put((byte) component);
        }
        return data.flip();
    }

    private static void putRoomQuad(ByteBuffer data, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
        float nx, float ny, float nz) {
        putRoomQuadUv(data, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz, 1.0f, 1.0f);
    }

    private static void putRoomQuadUv(ByteBuffer data, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
        float nx, float ny, float nz, float maxU, float maxV) {
        putRoomVertex(data, x0, y0, z0, nx, ny, nz, 0.0f, 0.0f);
        putRoomVertex(data, x1, y1, z1, nx, ny, nz, maxU, 0.0f);
        putRoomVertex(data, x2, y2, z2, nx, ny, nz, maxU, maxV);
        putRoomVertex(data, x3, y3, z3, nx, ny, nz, 0.0f, maxV);
    }

    private static void putRoomVertex(ByteBuffer data, float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        data.putFloat(x).putFloat(y).putFloat(z);
        data.putFloat(nx).putFloat(ny).putFloat(nz);
        data.putFloat(u).putFloat(v);
    }
}
