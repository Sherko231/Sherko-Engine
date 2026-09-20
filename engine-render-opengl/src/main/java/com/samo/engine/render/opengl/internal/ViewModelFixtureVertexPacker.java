package com.samo.engine.render.opengl.internal;

import java.nio.ByteBuffer;
import java.util.Objects;

final class ViewModelFixtureVertexPacker {
    static final int VERTEX_COUNT = 6;
    static final int FLOATS_PER_VERTEX = 6;
    static final int VERTEX_BYTES = VERTEX_COUNT * FLOATS_PER_VERTEX * Float.BYTES;

    static final float COLOR_RED = 0.95f;
    static final float COLOR_GREEN = 0.55f;
    static final float COLOR_BLUE = 0.15f;

    private ViewModelFixtureVertexPacker() {

    }

    static ByteBuffer write(ByteBuffer destination) {

        ByteBuffer target = Objects.requireNonNull(destination, "destination");
        putVertex(target, -0.12f, -0.23f, -0.50f);
        putVertex(target, 0.36f, -0.23f, -0.50f);
        putVertex(target, 0.36f, -0.06f, -0.50f);
        putVertex(target, -0.12f, -0.23f, -0.50f);
        putVertex(target, 0.36f, -0.06f, -0.50f);
        putVertex(target, -0.12f, -0.06f, -0.50f);
        return target;

    }

    private static void putVertex(ByteBuffer target, float x, float y, float z) {

        target.putFloat(x).putFloat(y).putFloat(z);
        target.putFloat(COLOR_RED).putFloat(COLOR_GREEN).putFloat(COLOR_BLUE);

    }
}
