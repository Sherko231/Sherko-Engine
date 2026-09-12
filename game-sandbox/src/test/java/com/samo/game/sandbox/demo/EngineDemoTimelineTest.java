package com.samo.game.sandbox.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class EngineDemoTimelineTest {
    @Test
    void scriptedSequenceMatchesOwnerFacingDemoContract() {
        List<EngineDemoTimeline.Step> steps = EngineDemoTimeline.steps();

        assertThat(steps).containsExactly(
                new EngineDemoTimeline.Step(5L * EngineDemoTimeline.SECOND_NANOS,
                        EngineDemoTimeline.Action.BORDERLESS_FULLSCREEN),
                new EngineDemoTimeline.Step(10L * EngineDemoTimeline.SECOND_NANOS,
                        EngineDemoTimeline.Action.WINDOWED),
                new EngineDemoTimeline.Step(15L * EngineDemoTimeline.SECOND_NANOS,
                        EngineDemoTimeline.Action.EXCLUSIVE_FULLSCREEN),
                new EngineDemoTimeline.Step(20L * EngineDemoTimeline.SECOND_NANOS,
                        EngineDemoTimeline.Action.WINDOWED),
                new EngineDemoTimeline.Step(25L * EngineDemoTimeline.SECOND_NANOS,
                        EngineDemoTimeline.Action.CAPTURE_CURSOR),
                new EngineDemoTimeline.Step(34L * EngineDemoTimeline.SECOND_NANOS,
                        EngineDemoTimeline.Action.RELEASE_CURSOR),
                new EngineDemoTimeline.Step(38L * EngineDemoTimeline.SECOND_NANOS,
                        EngineDemoTimeline.Action.SHUTDOWN));
    }

    @Test
    void firstPendingStepIndexUsesStrictTimelineBoundaries() {
        assertThat(EngineDemoTimeline.firstPendingStepIndex(0L)).isZero();
        assertThat(EngineDemoTimeline.firstPendingStepIndex(5L * EngineDemoTimeline.SECOND_NANOS - 1L)).isZero();
        assertThat(EngineDemoTimeline.firstPendingStepIndex(5L * EngineDemoTimeline.SECOND_NANOS)).isEqualTo(1);
        assertThat(EngineDemoTimeline.firstPendingStepIndex(20L * EngineDemoTimeline.SECOND_NANOS)).isEqualTo(4);
        assertThat(EngineDemoTimeline.firstPendingStepIndex(EngineDemoTimeline.DEMO_DURATION_NANOS))
                .isEqualTo(EngineDemoTimeline.steps().size());
    }

    @Test
    void timelineIsOrderedAndEndsAtDeclaredDuration() {
        long previous = -1L;
        for (EngineDemoTimeline.Step step : EngineDemoTimeline.steps()) {
            assertThat(step.atNanos()).isGreaterThan(previous);
            previous = step.atNanos();
        }

        assertThat(previous).isEqualTo(EngineDemoTimeline.DEMO_DURATION_NANOS);
    }
}
