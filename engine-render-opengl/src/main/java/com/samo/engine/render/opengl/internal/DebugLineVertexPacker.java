package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.DebugAabb;
import com.samo.engine.core.api.DebugColor;
import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugPrimitive;
import com.samo.engine.core.api.DebugRay;
import com.samo.engine.core.api.DebugSphere;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.joml.Vector3f;

final class DebugLineVertexPacker {
    static final int FLOATS_PER_VERTEX = 6;
    static final int VERTEX_STRIDE_BYTES = FLOATS_PER_VERTEX * Float.BYTES;
    static final int SPHERE_SEGMENTS_PER_CIRCLE = 16;
    static final int MAX_VERTEX_COUNT = DebugFrame.MAX_PRIMITIVES * 3 * SPHERE_SEGMENTS_PER_CIRCLE * 2;
    static final int MAX_BYTES = MAX_VERTEX_COUNT * VERTEX_STRIDE_BYTES;

    private DebugLineVertexPacker() {
    }

    static int write(DebugFrame frame, ByteBuffer destination) {
        DebugFrame debugFrame = Objects.requireNonNull(frame, "frame");
        ByteBuffer target = Objects.requireNonNull(destination, "destination");
        int start = target.position();

        for (DebugPrimitive primitive : debugFrame.primitives()) {
            if (primitive instanceof DebugLine line) {
                putSegment(target, line.startX(), line.startY(), line.startZ(), line.endX(), line.endY(), line.endZ(), line.color());
            } else if (primitive instanceof DebugAabb box) {
                putAabb(target, box);
            } else if (primitive instanceof DebugSphere sphere) {
                putSphere(target, sphere);
            } else if (primitive instanceof DebugRay ray) {
                putRay(target, ray);
            } else {
                throw new IllegalArgumentException("Unsupported debug primitive: " + primitive.getClass().getName());
            }
        }

        int bytes = target.position() - start;
        return bytes / VERTEX_STRIDE_BYTES;
    }

    private static void putAabb(ByteBuffer target, DebugAabb debugAabb) {
        Vector3f min = debugAabb.bounds().minimum(new Vector3f());
        Vector3f max = debugAabb.bounds().maximum(new Vector3f());
        DebugColor color = debugAabb.color();

        putSegment(target, min.x, min.y, min.z, max.x, min.y, min.z, color);
        putSegment(target, max.x, min.y, min.z, max.x, max.y, min.z, color);
        putSegment(target, max.x, max.y, min.z, min.x, max.y, min.z, color);
        putSegment(target, min.x, max.y, min.z, min.x, min.y, min.z, color);

        putSegment(target, min.x, min.y, max.z, max.x, min.y, max.z, color);
        putSegment(target, max.x, min.y, max.z, max.x, max.y, max.z, color);
        putSegment(target, max.x, max.y, max.z, min.x, max.y, max.z, color);
        putSegment(target, min.x, max.y, max.z, min.x, min.y, max.z, color);

        putSegment(target, min.x, min.y, min.z, min.x, min.y, max.z, color);
        putSegment(target, max.x, min.y, min.z, max.x, min.y, max.z, color);
        putSegment(target, max.x, max.y, min.z, max.x, max.y, max.z, color);
        putSegment(target, min.x, max.y, min.z, min.x, max.y, max.z, color);
    }

    private static void putSphere(ByteBuffer target, DebugSphere debugSphere) {
        Vector3f center = debugSphere.sphere().center(new Vector3f());
        float radius = debugSphere.sphere().radius();
        DebugColor color = debugSphere.color();

        putCircle(target, center, radius, color, 0);
        putCircle(target, center, radius, color, 1);
        putCircle(target, center, radius, color, 2);
    }

    private static void putCircle(ByteBuffer target, Vector3f center, float radius, DebugColor color, int plane) {
        for (int segment = 0; segment < SPHERE_SEGMENTS_PER_CIRCLE; segment++) {
            double a0 = Math.PI * 2.0 * segment / SPHERE_SEGMENTS_PER_CIRCLE;
            double a1 = Math.PI * 2.0 * (segment + 1) / SPHERE_SEGMENTS_PER_CIRCLE;
            float c0 = (float) Math.cos(a0) * radius;
            float s0 = (float) Math.sin(a0) * radius;
            float c1 = (float) Math.cos(a1) * radius;
            float s1 = (float) Math.sin(a1) * radius;
            switch (plane) {
                case 0 -> putSegment(target, center.x + c0, center.y + s0, center.z, center.x + c1, center.y + s1, center.z, color);
                case 1 -> putSegment(target, center.x + c0, center.y, center.z + s0, center.x + c1, center.y, center.z + s1, color);
                case 2 -> putSegment(target, center.x, center.y + c0, center.z + s0, center.x, center.y + c1, center.z + s1, color);
                default -> throw new IllegalArgumentException("unsupported sphere plane");
            }
        }
    }

    private static void putRay(ByteBuffer target, DebugRay debugRay) {
        Vector3f origin = debugRay.ray().origin(new Vector3f());
        Vector3f end = debugRay.ray().pointAt(debugRay.lengthMeters(), new Vector3f());
        putSegment(target, origin.x, origin.y, origin.z, end.x, end.y, end.z, debugRay.color());
    }

    private static void putSegment(ByteBuffer target, float ax, float ay, float az, float bx, float by, float bz, DebugColor color) {
        putVertex(target, ax, ay, az, color);
        putVertex(target, bx, by, bz, color);
    }

    private static void putVertex(ByteBuffer target, float x, float y, float z, DebugColor color) {
        target.putFloat(x).putFloat(y).putFloat(z);
        target.putFloat(color.red()).putFloat(color.green()).putFloat(color.blue());
    }
}
