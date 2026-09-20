package com.samo.engine.render.opengl.internal;

import com.samo.engine.core.api.EngineLogger;
import com.samo.engine.render.api.RenderLocalLight;
import java.util.List;
import java.util.Objects;

final class LocalLightSelector {
    static final int SHADER_CAPACITY = 8;
    private static final String SUBSYSTEM = "renderer";

    private final EngineLogger logger;
    private final int maximum;

    LocalLightSelector(EngineLogger logger, int maximum) {

        this.logger = Objects.requireNonNull(logger, "logger");
        if (maximum < 1 || maximum > SHADER_CAPACITY) {
            throw new IllegalArgumentException("maxLocalLights must be within [1," + SHADER_CAPACITY + "]");
        }
        this.maximum = maximum;

    }

    int maximum() {

        return maximum;

    }

    List<RenderLocalLight> select(List<RenderLocalLight> submitted) {

        List<RenderLocalLight> lights = Objects.requireNonNull(submitted, "submitted");
        int submittedCount = lights.size();
        int acceptedCount = Math.min(submittedCount, maximum);
        if (submittedCount > maximum) {
            int droppedCount = submittedCount - acceptedCount;
            logger.log(EngineLogger.Level.WARN,
                "Local light limit exceeded: submitted=%d accepted=%d dropped=%d configuredMax=%d".formatted(submittedCount, acceptedCount, droppedCount, maximum),
                new EngineLogger.Context(null, null, SUBSYSTEM, null, null));
        }
        return List.copyOf(lights.subList(0, acceptedCount));

    }
}
