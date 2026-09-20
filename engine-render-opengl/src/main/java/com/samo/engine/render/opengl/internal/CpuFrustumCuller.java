package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.Aabb3f;
import com.samo.engine.core.api.Frustum3f;
import java.util.Objects;

final class CpuFrustumCuller {
    boolean isVisible(Frustum3f frustum, Aabb3f worldBounds) {
        return Objects.requireNonNull(frustum, "frustum").intersects(Objects.requireNonNull(worldBounds, "worldBounds"));
    }
}
