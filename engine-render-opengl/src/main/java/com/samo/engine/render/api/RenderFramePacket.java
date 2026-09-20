package com.samo.engine.render.api;

import com.samo.engine.core.api.DebugFrame;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Immutable renderer-facing snapshot for one frame.
 *
 * <p>
 * The packet copies caller-owned camera matrices and the ordered local-light submission list,
 * and retains one immutable bounded debug-frame snapshot. It owns no native resources. Later
 * mutation of source matrices, copied-out matrices, or the source light list cannot change the
 * captured frame.
 */
public final class RenderFramePacket {
    private final Matrix4f view;
    private final Matrix4f projection;
    private final int framebufferWidth;
    private final int framebufferHeight;
    private final List<RenderLocalLight> localLights;
    private final DebugFrame debugFrame;

    public RenderFramePacket(Matrix4fc view, Matrix4fc projection, int framebufferWidth, int framebufferHeight) {

        this(view, projection, framebufferWidth, framebufferHeight, List.of(), DebugFrame.EMPTY);

    }

    public RenderFramePacket(Matrix4fc view, Matrix4fc projection, int framebufferWidth, int framebufferHeight, List<? extends RenderLocalLight> localLights) {

        this(view, projection, framebufferWidth, framebufferHeight, localLights, DebugFrame.EMPTY);

    }

    public RenderFramePacket(Matrix4fc view, Matrix4fc projection, int framebufferWidth, int framebufferHeight, List<? extends RenderLocalLight> localLights,
        DebugFrame debugFrame) {

        Matrix4fc viewMatrix = Objects.requireNonNull(view, "view");
        Matrix4fc projectionMatrix = Objects.requireNonNull(projection, "projection");
        List<? extends RenderLocalLight> submittedLights = Objects.requireNonNull(localLights, "localLights");
        DebugFrame submittedDebugFrame = Objects.requireNonNull(debugFrame, "debugFrame");
        if (framebufferWidth <= 0) {
            throw new IllegalArgumentException("framebufferWidth must be positive");
        }
        if (framebufferHeight <= 0) {
            throw new IllegalArgumentException("framebufferHeight must be positive");
        }
        requireFinite(viewMatrix, "view");
        requireFinite(projectionMatrix, "projection");

        this.view = new Matrix4f(viewMatrix);
        this.projection = new Matrix4f(projectionMatrix);
        this.framebufferWidth = framebufferWidth;
        this.framebufferHeight = framebufferHeight;
        for (RenderLocalLight light : submittedLights) {
            Objects.requireNonNull(light, "localLights must not contain null");
        }
        this.localLights = List.copyOf(submittedLights);
        this.debugFrame = submittedDebugFrame;

    }

    public int framebufferWidth() {

        return framebufferWidth;

    }

    public int framebufferHeight() {

        return framebufferHeight;

    }

    public List<RenderLocalLight> localLights() {

        return localLights;

    }

    public DebugFrame debugFrame() {

        return debugFrame;

    }

    public Matrix4f copyViewTo(Matrix4f destination) {

        return Objects.requireNonNull(destination, "destination").set(view);

    }

    public Matrix4f copyProjectionTo(Matrix4f destination) {

        return Objects.requireNonNull(destination, "destination").set(projection);

    }

    private static void requireFinite(Matrix4fc matrix, String name) {

        requireFinite(matrix.m00(), name + ".m00");
        requireFinite(matrix.m01(), name + ".m01");
        requireFinite(matrix.m02(), name + ".m02");
        requireFinite(matrix.m03(), name + ".m03");
        requireFinite(matrix.m10(), name + ".m10");
        requireFinite(matrix.m11(), name + ".m11");
        requireFinite(matrix.m12(), name + ".m12");
        requireFinite(matrix.m13(), name + ".m13");
        requireFinite(matrix.m20(), name + ".m20");
        requireFinite(matrix.m21(), name + ".m21");
        requireFinite(matrix.m22(), name + ".m22");
        requireFinite(matrix.m23(), name + ".m23");
        requireFinite(matrix.m30(), name + ".m30");
        requireFinite(matrix.m31(), name + ".m31");
        requireFinite(matrix.m32(), name + ".m32");
        requireFinite(matrix.m33(), name + ".m33");

    }

    private static void requireFinite(float value, String name) {

        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }

    }
}
