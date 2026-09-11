package com.samo.engine.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AllocationMetricBenchmarkTest {
    private static final String EVENT_NAME = "jdk.ObjectAllocationSample";
    private static final String MEASUREMENT_SOURCE = "JFR jdk.ObjectAllocationSample weight";
    private static final int WARMUP_ITERATIONS = 512;
    private static final int MEASURED_ITERATIONS = 4_096;
    private static final Path REPORT_PATH =
            Path.of("build", "reports", "allocation", "p2-t11-allocation-metric.txt");

    private static volatile Object allocationSink;
    private static volatile long arithmeticSink;
    private static BenchmarkEvidence evidence;

    @BeforeAll
    static void runBenchmarkAndWriteReport() throws Exception {
        requireAllocationSampleEvent();

        Measurement simulationTick = measure(
                "simulation tick",
                "p2-t11-simulation-tick",
                WARMUP_ITERATIONS,
                MEASURED_ITERATIONS,
                iterations -> allocate(iterations, 2_048));
        Measurement renderFrame = measure(
                "render frame",
                "p2-t11-render-frame",
                WARMUP_ITERATIONS,
                MEASURED_ITERATIONS,
                iterations -> allocate(iterations, 4_096));
        Measurement allocatingControl = measure(
                "allocating control",
                "p2-t11-allocating-control",
                WARMUP_ITERATIONS,
                MEASURED_ITERATIONS,
                iterations -> allocate(iterations, 8_192));
        Measurement nonallocatingControl = measure(
                "nonallocating control",
                "p2-t11-nonallocating-control",
                WARMUP_ITERATIONS,
                MEASURED_ITERATIONS,
                AllocationMetricBenchmarkTest::runArithmetic);

        if (allocatingControl.sampleCount() == 0 || allocatingControl.sampledWeightBytes() <= 0) {
            throw new IllegalStateException(
                    "JFR allocation metric unusable: allocating control produced no usable allocation samples");
        }

        evidence = new BenchmarkEvidence(
                simulationTick,
                renderFrame,
                allocatingControl,
                nonallocatingControl);
        writeReport(evidence);
    }

    @Test
    void syntheticSampleArithmeticUsesIndependentExpectedSumAndThreadAttribution() {
        List<Sample> samples = List.of(
                new Sample("target", 100L),
                new Sample("other", 1_000L),
                new Sample("target", 300L));

        SampleSummary summary = summarizeSamples(samples, "target", 4);

        assertEquals(2L, summary.sampleCount());
        assertEquals(400L, summary.sampledWeightBytes());
        assertEquals(100.0, summary.estimatedBytesPerIteration());
    }

    @Test
    void reportArithmeticRejectsZeroIterations() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> summarizeSamples(List.of(new Sample("target", 10L)), "target", 0));

        assertTrue(failure.getMessage().contains("iteration count"));
    }

    @Test
    void channelLabelsRemainDistinctAndLiveEvidenceHasExpectedShape() {
        assertEquals("simulation tick", evidence.simulationTick().channel());
        assertEquals("render frame", evidence.renderFrame().channel());
        assertFalse(evidence.simulationTick().channel().equals(evidence.renderFrame().channel()));
        assertTrue(evidence.simulationTick().iterations() > 0);
        assertTrue(evidence.renderFrame().iterations() > 0);
        assertTrue(evidence.allocatingControl().sampleCount() > 0);
        assertTrue(evidence.allocatingControl().sampledWeightBytes() > 0);
        assertTrue(evidence.nonallocatingControl().iterations() > 0);
    }

    @Test
    void reportExistsWithStableFieldOrderAndRequiredLimitations() throws IOException {
        assertTrue(Files.isRegularFile(REPORT_PATH));
        List<String> lines = Files.readAllLines(REPORT_PATH, StandardCharsets.UTF_8);

        assertFieldOrder(lines, List.of(
                "java.version=",
                "measurement.source=" + MEASUREMENT_SOURCE,
                "estimate=true",
                "warmup.iterations=" + WARMUP_ITERATIONS,
                "simulation.tick.iterations=",
                "simulation.tick.sample.count=",
                "simulation.tick.sampled.weight.bytes=",
                "simulation.tick.duration.nanos=",
                "simulation.tick.estimated.bytes.per.tick=",
                "render.frame.iterations=",
                "render.frame.sample.count=",
                "render.frame.sampled.weight.bytes=",
                "render.frame.duration.nanos=",
                "render.frame.estimated.bytes.per.frame=",
                "allocating.control.sample.count=",
                "allocating.control.sampled.weight.bytes=",
                "allocating.control.estimated.bytes.per.iteration=",
                "nonallocating.control.sample.count=",
                "nonallocating.control.sampled.weight.bytes=",
                "nonallocating.control.estimated.bytes.per.iteration=",
                "limitations.sampled.estimate=true",
                "limitations.render.synthetic.headless=true",
                "limitations.heap.only=true",
                "limitations.native.gpu.attribution=false"));
    }

    private static Measurement measure(
            String channel,
            String threadName,
            int warmupIterations,
            int measuredIterations,
            Workload workload) throws Exception {
        if (warmupIterations < 0) {
            throw new IllegalArgumentException("warm-up iteration count must be non-negative");
        }
        if (measuredIterations <= 0) {
            throw new IllegalArgumentException("measured iteration count must be positive");
        }

        CountDownLatch warmupComplete = new CountDownLatch(1);
        CountDownLatch measurementStart = new CountDownLatch(1);
        AtomicLong elapsedNanos = new AtomicLong(-1L);
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();

        Thread worker = Thread.ofPlatform().name(threadName).unstarted(() -> {
            try {
                workload.run(warmupIterations);
                warmupComplete.countDown();
                measurementStart.await();
                long startNanos = System.nanoTime();
                workload.run(measuredIterations);
                elapsedNanos.set(System.nanoTime() - startNanos);
            } catch (Throwable failure) {
                workerFailure.set(failure);
                warmupComplete.countDown();
            }
        });

        Path recordingPath = Files.createTempFile("p2-t11-", ".jfr");
        try {
            worker.start();
            if (!warmupComplete.await(30, TimeUnit.SECONDS)) {
                worker.interrupt();
                throw new IllegalStateException("allocation benchmark warm-up timed out for " + channel);
            }
            rethrowWorkerFailure(workerFailure.get());

            try (Recording recording = new Recording()) {
                recording.enable(EVENT_NAME);
                recording.start();
                measurementStart.countDown();
                worker.join(Duration.ofSeconds(60));
                if (worker.isAlive()) {
                    worker.interrupt();
                    throw new IllegalStateException("allocation benchmark measurement timed out for " + channel);
                }
                rethrowWorkerFailure(workerFailure.get());
                recording.stop();
                recording.dump(recordingPath);
            }

            List<Sample> samples = readAllocationSamples(recordingPath);
            SampleSummary summary = summarizeSamples(samples, threadName, measuredIterations);
            return new Measurement(
                    channel,
                    measuredIterations,
                    summary.sampleCount(),
                    summary.sampledWeightBytes(),
                    elapsedNanos.get(),
                    summary.estimatedBytesPerIteration());
        } finally {
            Files.deleteIfExists(recordingPath);
        }
    }

    private static List<Sample> readAllocationSamples(Path recordingPath) throws IOException {
        List<Sample> samples = new ArrayList<>();
        for (RecordedEvent event : RecordingFile.readAllEvents(recordingPath)) {
            if (!EVENT_NAME.equals(event.getEventType().getName())) {
                continue;
            }
            if (!event.hasField("weight")) {
                throw new IllegalStateException("JFR allocation sample is missing weight field");
            }
            long weight = event.getLong("weight");
            if (weight <= 0) {
                throw new IllegalStateException("JFR allocation sample has unusable weight: " + weight);
            }
            RecordedThread thread = event.getThread();
            if (thread != null && thread.getJavaName() != null) {
                samples.add(new Sample(thread.getJavaName(), weight));
            }
        }
        return samples;
    }

    private static SampleSummary summarizeSamples(List<Sample> samples, String targetThread, int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("iteration count must be positive");
        }

        long count = 0L;
        long totalWeight = 0L;
        for (Sample sample : samples) {
            if (!targetThread.equals(sample.threadName())) {
                continue;
            }
            if (sample.weight() <= 0) {
                throw new IllegalArgumentException("sample weight must be positive");
            }
            count++;
            totalWeight = Math.addExact(totalWeight, sample.weight());
        }
        return new SampleSummary(count, totalWeight, (double) totalWeight / iterations);
    }

    private static void requireAllocationSampleEvent() {
        boolean available = FlightRecorder.getFlightRecorder().getEventTypes().stream()
                .anyMatch(eventType -> EVENT_NAME.equals(eventType.getName()));
        if (!available) {
            throw new IllegalStateException("JFR event unavailable: " + EVENT_NAME);
        }
    }

    private static void allocate(int iterations, int bytes) {
        for (int index = 0; index < iterations; index++) {
            byte[] allocation = new byte[bytes];
            allocation[0] = (byte) index;
            allocationSink = allocation;
        }
    }

    private static void runArithmetic(int iterations) {
        long value = arithmeticSink;
        for (int index = 0; index < iterations; index++) {
            value = value * 31L + index;
            value ^= value >>> 7;
        }
        arithmeticSink = value;
    }

    private static void writeReport(BenchmarkEvidence benchmark) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        List<String> lines = List.of(
                "java.version=" + System.getProperty("java.version"),
                "measurement.source=" + MEASUREMENT_SOURCE,
                "estimate=true",
                "warmup.iterations=" + WARMUP_ITERATIONS,
                "simulation.tick.iterations=" + benchmark.simulationTick().iterations(),
                "simulation.tick.sample.count=" + benchmark.simulationTick().sampleCount(),
                "simulation.tick.sampled.weight.bytes=" + benchmark.simulationTick().sampledWeightBytes(),
                "simulation.tick.duration.nanos=" + benchmark.simulationTick().durationNanos(),
                "simulation.tick.estimated.bytes.per.tick="
                        + benchmark.simulationTick().estimatedBytesPerIteration(),
                "render.frame.iterations=" + benchmark.renderFrame().iterations(),
                "render.frame.sample.count=" + benchmark.renderFrame().sampleCount(),
                "render.frame.sampled.weight.bytes=" + benchmark.renderFrame().sampledWeightBytes(),
                "render.frame.duration.nanos=" + benchmark.renderFrame().durationNanos(),
                "render.frame.estimated.bytes.per.frame="
                        + benchmark.renderFrame().estimatedBytesPerIteration(),
                "allocating.control.sample.count=" + benchmark.allocatingControl().sampleCount(),
                "allocating.control.sampled.weight.bytes=" + benchmark.allocatingControl().sampledWeightBytes(),
                "allocating.control.estimated.bytes.per.iteration="
                        + benchmark.allocatingControl().estimatedBytesPerIteration(),
                "nonallocating.control.sample.count=" + benchmark.nonallocatingControl().sampleCount(),
                "nonallocating.control.sampled.weight.bytes="
                        + benchmark.nonallocatingControl().sampledWeightBytes(),
                "nonallocating.control.estimated.bytes.per.iteration="
                        + benchmark.nonallocatingControl().estimatedBytesPerIteration(),
                "limitations.sampled.estimate=true",
                "limitations.render.synthetic.headless=true",
                "limitations.heap.only=true",
                "limitations.native.gpu.attribution=false");
        Files.write(REPORT_PATH, lines, StandardCharsets.UTF_8);
    }

    private static void assertFieldOrder(List<String> lines, List<String> prefixes) {
        assertEquals(prefixes.size(), lines.size());
        for (int index = 0; index < prefixes.size(); index++) {
            assertTrue(
                    lines.get(index).startsWith(prefixes.get(index)),
                    () -> "Expected report field " + prefixes.get(index) + " at line " + (index + 1));
        }
    }

    private static void rethrowWorkerFailure(Throwable failure) throws Exception {
        if (failure == null) {
            return;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof Exception exception) {
            throw exception;
        }
        throw new IllegalStateException("allocation benchmark worker failed", failure);
    }

    @FunctionalInterface
    private interface Workload {
        void run(int iterations);
    }

    private record Sample(String threadName, long weight) {
    }

    private record SampleSummary(long sampleCount, long sampledWeightBytes, double estimatedBytesPerIteration) {
    }

    private record Measurement(
            String channel,
            int iterations,
            long sampleCount,
            long sampledWeightBytes,
            long durationNanos,
            double estimatedBytesPerIteration) {
    }

    private record BenchmarkEvidence(
            Measurement simulationTick,
            Measurement renderFrame,
            Measurement allocatingControl,
            Measurement nonallocatingControl) {
    }
}
