package com.samo.game.sandbox.demo;

import java.util.List;

final class EngineDemoTimeline {
    static final long SECOND_NANOS = 1_000_000_000L;
    static final long DEMO_DURATION_NANOS = 38L * SECOND_NANOS;

    private static final List<Step> STEPS = List.of(
            new Step(5L * SECOND_NANOS, Action.BORDERLESS_FULLSCREEN),
            new Step(10L * SECOND_NANOS, Action.WINDOWED),
            new Step(15L * SECOND_NANOS, Action.EXCLUSIVE_FULLSCREEN),
            new Step(20L * SECOND_NANOS, Action.WINDOWED),
            new Step(25L * SECOND_NANOS, Action.CAPTURE_CURSOR),
            new Step(34L * SECOND_NANOS, Action.RELEASE_CURSOR),
            new Step(DEMO_DURATION_NANOS, Action.SHUTDOWN));

    private EngineDemoTimeline() {
    }

    static List<Step> steps() {
        return STEPS;
    }

    static int firstPendingStepIndex(long elapsedNanos) {
        for (int index = 0; index < STEPS.size(); index++) {
            if (elapsedNanos < STEPS.get(index).atNanos()) {
                return index;
            }
        }
        return STEPS.size();
    }

    enum Action {
        BORDERLESS_FULLSCREEN,
        WINDOWED,
        EXCLUSIVE_FULLSCREEN,
        CAPTURE_CURSOR,
        RELEASE_CURSOR,
        SHUTDOWN
    }

    record Step(long atNanos, Action action) {
    }
}
