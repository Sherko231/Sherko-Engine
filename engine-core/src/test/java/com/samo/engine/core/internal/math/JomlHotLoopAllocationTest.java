package com.samo.engine.core.internal.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.management.ThreadMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** P4-T02 acceptance evidence for warmed, destination-reusing JOML hot-loop arithmetic. */
final class JomlHotLoopAllocationTest {
    private static final int WARMUP_ITERATIONS = 50_000;
    private static final int MEASURED_ITERATIONS = 20_000;
    private static final int MEASURED_PASSES = 5;
    private static final int ALLOCATING_CONTROL_ITERATIONS = 1_024;
    private static final int ALLOCATING_CONTROL_BYTES = 1_024;
    private static final float EPSILON = 1.0e-5f;
    private static final Path REPORT_PATH = Path.of("build", "reports", "allocation", "p4-t02-joml-hot-loop-allocation.txt");

    private static volatile Object allocationSink;
    private static volatile float mathSink;
    private static BenchmarkEvidence evidence;

    @BeforeAll
    static void runAllocationAcceptanceAndWriteReport() throws IOException {
        ThreadMXBean bean = requireThreadAllocationBean();
        boolean initiallyEnabled = bean.isThreadAllocatedMemoryEnabled();
        try {
            if (!initiallyEnabled) {
                bean.setThreadAllocatedMemoryEnabled(true);
            }
            if (!bean.isThreadAllocatedMemoryEnabled()) {
                throw new IllegalStateException("Thread allocation measurement could not be enabled");
            }

            long threadId = Thread.currentThread().threadId();
            MathWorkload workload = new MathWorkload();
            workload.run(WARMUP_ITERATIONS);
            warmAllocationCounter(bean, threadId);

            List<Long> passDeltas = new ArrayList<>(MEASURED_PASSES);
            for (int pass = 0; pass < MEASURED_PASSES; pass++) {
                long before = allocatedBytes(bean, threadId);
                workload.run(MEASURED_ITERATIONS);
                long after = allocatedBytes(bean, threadId);
                passDeltas.add(Math.subtractExact(after, before));
            }

            long controlBefore = allocatedBytes(bean, threadId);
            runAllocatingControl();
            long controlAfter = allocatedBytes(bean, threadId);
            long allocatingControlDelta = Math.subtractExact(controlAfter, controlBefore);

            evidence = new BenchmarkEvidence(List.copyOf(passDeltas), allocatingControlDelta, mathSink);
            writeReport(evidence);

            for (int pass = 0; pass < passDeltas.size(); pass++) {
                assertEquals(0L, passDeltas.get(pass).longValue(), "Warmed JOML hot-loop pass " + pass + " allocated heap bytes");
            }
            assertTrue(allocatingControlDelta > 0L, "Allocating control must report positive heap allocation through the same counter");
        } finally {
            if (!initiallyEnabled && bean.isThreadAllocatedMemoryEnabled()) {
                bean.setThreadAllocatedMemoryEnabled(false);
            }
        }
    }

    @Test
    void canonicalPositiveYRotationMapsForwardToLeft() {
        Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f);
        Vector3f result = new Vector3f();
        Quaternionf positiveQuarterTurnY = new Quaternionf().rotationY((float) (Math.PI / 2.0));

        positiveQuarterTurnY.transform(forward, result);

        assertEquals(-1.0f, result.x, EPSILON);
        assertEquals(0.0f, result.y, EPSILON);
        assertEquals(0.0f, result.z, EPSILON);
    }

    @Test
    void evidenceReportContainsStableContractFields() throws IOException {
        assertTrue(Files.isRegularFile(REPORT_PATH));
        List<String> lines = Files.readAllLines(REPORT_PATH, StandardCharsets.UTF_8);

        assertEquals("task=P4-T02", lines.get(0));
        assertEquals("dependency=org.joml:joml:1.10.9", lines.get(1));
        assertEquals("measurement.source=com.sun.management.ThreadMXBean", lines.get(3));
        assertEquals("warmup.iterations=" + WARMUP_ITERATIONS, lines.get(5));
        assertEquals("measured.iterations.per.pass=" + MEASURED_ITERATIONS, lines.get(6));
        assertEquals("measured.passes=" + MEASURED_PASSES, lines.get(7));
        assertEquals("allocation.acceptance.zero.bytes=true", lines.get(14));
        assertEquals("limitations.heap.only=true", lines.get(15));
        assertEquals("limitations.jvm.accounting.approximation=true", lines.get(16));
        assertEquals("spatial.convention=right-handed,+Y-up,-Z-forward,radians", lines.get(17));
    }

    private static ThreadMXBean requireThreadAllocationBean() {
        java.lang.management.ThreadMXBean platformBean = ManagementFactory.getThreadMXBean();
        if (!(platformBean instanceof ThreadMXBean allocationBean)) {
            throw new IllegalStateException("Java runtime does not expose com.sun.management.ThreadMXBean allocation accounting");
        }
        if (!allocationBean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("Thread allocation measurement is unsupported by this Java runtime");
        }
        return allocationBean;
    }

    private static void warmAllocationCounter(ThreadMXBean bean, long threadId) {
        for (int index = 0; index < 128; index++) {
            allocatedBytes(bean, threadId);
        }
    }

    private static long allocatedBytes(ThreadMXBean bean, long threadId) {
        long allocated = bean.getThreadAllocatedBytes(threadId);
        if (allocated < 0L) {
            throw new IllegalStateException("Thread allocation counter returned unavailable value: " + allocated);
        }
        return allocated;
    }

    private static void runAllocatingControl() {
        for (int index = 0; index < ALLOCATING_CONTROL_ITERATIONS; index++) {
            byte[] allocation = new byte[ALLOCATING_CONTROL_BYTES];
            allocation[0] = (byte) index;
            allocationSink = allocation;
        }
    }

    private static void writeReport(BenchmarkEvidence benchmark) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("task=P4-T02");
        lines.add("dependency=org.joml:joml:1.10.9");
        lines.add("java.version=" + System.getProperty("java.version"));
        lines.add("measurement.source=com.sun.management.ThreadMXBean");
        lines.add("measurement.thread=current-platform-test-thread");
        lines.add("warmup.iterations=" + WARMUP_ITERATIONS);
        lines.add("measured.iterations.per.pass=" + MEASURED_ITERATIONS);
        lines.add("measured.passes=" + MEASURED_PASSES);
        for (int pass = 0; pass < MEASURED_PASSES; pass++) {
            lines.add("math.pass." + pass + ".allocated.bytes=" + benchmark.passDeltas().get(pass));
        }
        lines.add("allocating.control.allocated.bytes=" + benchmark.allocatingControlDelta());
        lines.add("allocation.acceptance.zero.bytes=" + benchmark.allPassesZero());
        lines.add("limitations.heap.only=true");
        lines.add("limitations.jvm.accounting.approximation=true");
        lines.add("spatial.convention=right-handed,+Y-up,-Z-forward,radians");
        lines.add("result.sink=" + benchmark.resultSink());
        Files.write(REPORT_PATH, lines, StandardCharsets.UTF_8);
    }

    private static final class MathWorkload {
        private final Vector3f source = new Vector3f();
        private final Vector3f rotated = new Vector3f();
        private final Vector3f transformed = new Vector3f();
        private final Quaternionf rotation = new Quaternionf().rotationY(0.625f);
        private final Matrix4f translation = new Matrix4f().translation(2.0f, -1.0f, -4.0f);

        void run(int iterations) {
            float sink = mathSink;
            for (int index = 0; index < iterations; index++) {
                float offset = (index & 7) * 0.001f;
                source.set(1.0f + offset, 2.0f - offset, -3.0f + offset);
                rotation.transform(source, rotated);
                translation.transformPosition(rotated, transformed);
                sink += transformed.x * 0.25f + transformed.y * 0.5f + transformed.z * 0.125f;
            }
            mathSink = sink;
        }
    }

    private record BenchmarkEvidence(List<Long> passDeltas, long allocatingControlDelta, float resultSink) {
        boolean allPassesZero() {
            for (long delta : passDeltas) {
                if (delta != 0L) {
                    return false;
                }
            }
            return true;
        }
    }
}
