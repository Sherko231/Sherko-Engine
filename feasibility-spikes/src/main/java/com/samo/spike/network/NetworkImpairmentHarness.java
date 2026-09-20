package com.samo.spike.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class NetworkImpairmentHarness {
    private static final String HOST = "127.0.0.1";
    private static final int DEFAULT_BASE_PORT = 42_100;
    private static final int DEFAULT_PACKET_COUNT = 8;
    private static final int DEFAULT_TIMEOUT_MILLIS = 10_000;
    private static final int PACKET_BYTES = Integer.BYTES + Long.BYTES;
    private static final long SEND_INTERVAL_MILLIS = 100L;

    private static final long FIXED_LATENCY_MILLIS = 80L;
    private static final long[] JITTER_DELAYS_MILLIS = {15L, 55L, 25L, 65L};

    private NetworkImpairmentHarness() {
    }

    public static void main(String[] args) throws Exception {
        String requestedMode = args.length == 0 ? "all" : args[0].toLowerCase(Locale.ROOT);
        int basePort = Integer.getInteger("spike.impairmentPort", DEFAULT_BASE_PORT);
        int packetCount = Integer.getInteger("spike.impairmentPacketCount", DEFAULT_PACKET_COUNT);
        int timeoutMillis = Integer.getInteger("spike.impairmentTimeoutMillis", DEFAULT_TIMEOUT_MILLIS);

        validateConfiguration(basePort, packetCount, timeoutMillis);

        if ("all".equals(requestedMode)) {
            int offset = 0;
            for (ImpairmentMode mode : ImpairmentMode.values()) {
                runScenario(mode, basePort + offset, packetCount, timeoutMillis);
                offset++;
            }
            System.out.println("P0-T11 suite passed: all five impairment modes were observed independently.");
            return;
        }

        ImpairmentMode mode = ImpairmentMode.parse(requestedMode);
        runScenario(mode, basePort, packetCount, timeoutMillis);
    }

    private static void runScenario(ImpairmentMode mode, int port, int packetCount, int timeoutMillis) throws Exception {
        System.out.println();
        System.out.printf("=== P0-T11 mode=%s port=%d packets=%d ===%n", mode.cliName, port, packetCount);

        CountDownLatch serverReady = new CountDownLatch(1);
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();

        Thread serverThread = Thread.ofPlatform().name("p0-t11-server-" + mode.cliName).start(() -> {
            try {
                runServer(mode, port, packetCount, timeoutMillis, serverReady);
            } catch (Throwable failure) {
                serverFailure.set(failure);
                serverReady.countDown();
            }
        });

        if (!serverReady.await(2, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Server did not become ready for mode=" + mode.cliName);
        }
        rethrow("Server failed before client start", serverFailure.get());

        ScenarioResult result;
        try {
            result = runClient(mode, port, packetCount, timeoutMillis);
        } finally {
            serverThread.join(timeoutMillis + 2_000L);
        }

        if (serverThread.isAlive()) {
            serverThread.interrupt();
            throw new IllegalStateException("Server did not terminate for mode=" + mode.cliName);
        }
        rethrow("Server failed", serverFailure.get());

        verify(mode, packetCount, result);
        System.out.printf("P0-T11 %s passed: %s%n", mode.cliName, result.summary(mode, packetCount));
    }

    private static void runServer(ImpairmentMode mode, int port, int packetCount, int timeoutMillis, CountDownLatch ready) throws Exception {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(HOST, port));
            channel.configureBlocking(false);
            ready.countDown();

            ByteBuffer packet = newPacketBuffer();
            HeldPacket heldForReordering = null;
            int receivedCount = 0;
            long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;

            while (receivedCount < packetCount) {
                packet.clear();
                SocketAddress sender = channel.receive(packet);
                if (sender == null) {
                    if (System.nanoTime() >= deadlineNanos) {
                        throw new IllegalStateException("Server timed out after receiving " + receivedCount + "/" + packetCount + " packets in mode=" + mode.cliName);
                    }
                    Thread.onSpinWait();
                    continue;
                }

                if (packet.position() != PACKET_BYTES) {
                    throw new IllegalStateException("Invalid UDP packet size: " + packet.position());
                }

                packet.flip();
                int sequence = packet.getInt();
                long sentNanos = packet.getLong();
                receivedCount++;
                deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;

                switch (mode) {
                    case LATENCY -> {
                        System.out.printf("Harness latency: seq=%d delay=%d ms%n", sequence, FIXED_LATENCY_MILLIS);
                        Thread.sleep(FIXED_LATENCY_MILLIS);
                        sendEcho(channel, sender, sequence, sentNanos);
                    }
                    case JITTER -> {
                        long delayMillis = JITTER_DELAYS_MILLIS[(sequence - 1) % JITTER_DELAYS_MILLIS.length];
                        System.out.printf("Harness jitter: seq=%d delay=%d ms%n", sequence, delayMillis);
                        Thread.sleep(delayMillis);
                        sendEcho(channel, sender, sequence, sentNanos);
                    }
                    case LOSS -> {
                        if (shouldDrop(sequence)) {
                            System.out.printf("Harness loss: dropped seq=%d%n", sequence);
                        } else {
                            sendEcho(channel, sender, sequence, sentNanos);
                        }
                    }
                    case DUPLICATION -> {
                        sendEcho(channel, sender, sequence, sentNanos);
                        if (shouldDuplicate(sequence)) {
                            sendEcho(channel, sender, sequence, sentNanos);
                            System.out.printf("Harness duplication: duplicated seq=%d%n", sequence);
                        }
                    }
                    case REORDERING -> {
                        if (sequence == 2) {
                            heldForReordering = new HeldPacket(sender, sequence, sentNanos);
                            System.out.println("Harness reordering: holding seq=2 until seq=3 arrives");
                        } else if (sequence == 3 && heldForReordering != null) {
                            sendEcho(channel, sender, sequence, sentNanos);
                            sendEcho(channel, heldForReordering.sender, heldForReordering.sequence, heldForReordering.sentNanos);
                            System.out.println("Harness reordering: emitted seq=3 before held seq=2");
                            heldForReordering = null;
                        } else {
                            sendEcho(channel, sender, sequence, sentNanos);
                        }
                    }
                }
            }

            if (heldForReordering != null) {
                sendEcho(channel, heldForReordering.sender, heldForReordering.sequence, heldForReordering.sentNanos);
            }
        }
    }

    private static ScenarioResult runClient(ImpairmentMode mode, int port, int packetCount, int timeoutMillis) throws Exception {
        InetSocketAddress serverAddress = new InetSocketAddress(HOST, port);
        int expectedReplies = expectedReplyCount(mode, packetCount);

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(HOST, 0));
            channel.connect(serverAddress);
            channel.configureBlocking(false);

            AtomicReference<Throwable> senderFailure = new AtomicReference<>();
            Thread senderThread = Thread.ofPlatform().name("p0-t11-client-sender-" + mode.cliName).start(() -> {
                try {
                    ByteBuffer outgoing = newPacketBuffer();
                    for (int sequence = 1; sequence <= packetCount; sequence++) {
                        long sentNanos = System.nanoTime();
                        outgoing.clear();
                        outgoing.putInt(sequence);
                        outgoing.putLong(sentNanos);
                        outgoing.flip();

                        while (outgoing.hasRemaining()) {
                            channel.write(outgoing);
                        }
                        System.out.printf("Client sent seq=%d%n", sequence);

                        if (sequence < packetCount) {
                            Thread.sleep(SEND_INTERVAL_MILLIS);
                        }
                    }
                } catch (Throwable failure) {
                    senderFailure.set(failure);
                }
            });

            ByteBuffer incoming = newPacketBuffer();
            List<Integer> receiveOrder = new ArrayList<>();
            Map<Integer, Integer> countsBySequence = new HashMap<>();
            Map<Integer, Double> rttBySequence = new HashMap<>();
            long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;

            while (receiveOrder.size() < expectedReplies) {
                incoming.clear();
                SocketAddress sender = channel.receive(incoming);
                if (sender == null) {
                    rethrow("Client sender failed", senderFailure.get());
                    if (System.nanoTime() >= deadlineNanos) {
                        throw new IllegalStateException("Client timed out after receiving " + receiveOrder.size() + "/" + expectedReplies + " replies in mode=" + mode.cliName);
                    }
                    Thread.onSpinWait();
                    continue;
                }

                if (incoming.position() != PACKET_BYTES) {
                    throw new IllegalStateException("Client received invalid packet size: " + incoming.position());
                }

                long receivedNanos = System.nanoTime();
                incoming.flip();
                int sequence = incoming.getInt();
                long sentNanos = incoming.getLong();
                double rttMillis = (receivedNanos - sentNanos) / 1_000_000.0;

                receiveOrder.add(sequence);
                countsBySequence.merge(sequence, 1, Integer::sum);
                rttBySequence.putIfAbsent(sequence, rttMillis);

                System.out.printf("Client received seq=%d RTT=%.3f ms%n", sequence, rttMillis);
                deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
            }

            senderThread.join(timeoutMillis);
            if (senderThread.isAlive()) {
                senderThread.interrupt();
                throw new IllegalStateException("Client sender did not terminate");
            }
            rethrow("Client sender failed", senderFailure.get());

            return new ScenarioResult(receiveOrder, countsBySequence, rttBySequence);
        }
    }

    private static void verify(ImpairmentMode mode, int packetCount, ScenarioResult result) {
        switch (mode) {
            case LATENCY -> {
                requireUniqueReplies(result, packetCount);
                double minimumRtt = result.minimumRttMillis();
                if (minimumRtt < FIXED_LATENCY_MILLIS - 20.0) {
                    throw new IllegalStateException("Latency was not observable enough; minimum RTT=" + minimumRtt + " ms");
                }
            }
            case JITTER -> {
                requireUniqueReplies(result, packetCount);
                double spread = result.maximumRttMillis() - result.minimumRttMillis();
                if (spread < 25.0) {
                    throw new IllegalStateException("Jitter was not observable enough; RTT spread=" + spread + " ms");
                }
            }
            case LOSS -> {
                int expectedUnique = packetCount - countMatching(packetCount, NetworkImpairmentHarness::shouldDrop);
                if (result.countsBySequence.size() != expectedUnique) {
                    throw new IllegalStateException("Expected " + expectedUnique + " unique replies after loss but received " + result.countsBySequence.size());
                }
                for (int sequence = 1; sequence <= packetCount; sequence++) {
                    boolean received = result.countsBySequence.containsKey(sequence);
                    if (shouldDrop(sequence) == received) {
                        throw new IllegalStateException("Unexpected loss result for seq=" + sequence + "; received=" + received);
                    }
                }
            }
            case DUPLICATION -> {
                requireUniqueReplies(result, packetCount);
                for (int sequence = 1; sequence <= packetCount; sequence++) {
                    int expected = shouldDuplicate(sequence) ? 2 : 1;
                    int actual = result.countsBySequence.getOrDefault(sequence, 0);
                    if (actual != expected) {
                        throw new IllegalStateException("Expected seq=" + sequence + " count=" + expected + " but got " + actual);
                    }
                }
            }
            case REORDERING -> {
                requireUniqueReplies(result, packetCount);
                int index2 = result.receiveOrder.indexOf(2);
                int index3 = result.receiveOrder.indexOf(3);
                if (index2 < 0 || index3 < 0 || index3 >= index2) {
                    throw new IllegalStateException("Expected seq=3 before seq=2; order=" + result.receiveOrder);
                }
            }
        }
    }

    private static void requireUniqueReplies(ScenarioResult result, int packetCount) {
        if (result.countsBySequence.size() != packetCount) {
            throw new IllegalStateException("Expected " + packetCount + " unique replies but received " + result.countsBySequence.size());
        }
    }

    private static int expectedReplyCount(ImpairmentMode mode, int packetCount) {
        return switch (mode) {
            case LOSS -> packetCount - countMatching(packetCount, NetworkImpairmentHarness::shouldDrop);
            case DUPLICATION -> packetCount + countMatching(packetCount, NetworkImpairmentHarness::shouldDuplicate);
            default -> packetCount;
        };
    }

    private static int countMatching(int packetCount, IntPredicate predicate) {
        int count = 0;
        for (int sequence = 1; sequence <= packetCount; sequence++) {
            if (predicate.test(sequence)) {
                count++;
            }
        }
        return count;
    }

    private static boolean shouldDrop(int sequence) {
        return sequence % 3 == 0;
    }

    private static boolean shouldDuplicate(int sequence) {
        return sequence % 4 == 0;
    }

    private static void sendEcho(DatagramChannel channel, SocketAddress receiver, int sequence, long sentNanos) throws IOException {
        ByteBuffer response = newPacketBuffer();
        response.putInt(sequence);
        response.putLong(sentNanos);
        response.flip();

        int sentBytes = channel.send(response, receiver);
        if (sentBytes != PACKET_BYTES) {
            throw new IllegalStateException("Expected to send " + PACKET_BYTES + " bytes but sent " + sentBytes);
        }
    }

    private static ByteBuffer newPacketBuffer() {
        return ByteBuffer.allocateDirect(PACKET_BYTES).order(ByteOrder.BIG_ENDIAN);
    }

    private static void validateConfiguration(int basePort, int packetCount, int timeoutMillis) {
        if (basePort < 1 || basePort + ImpairmentMode.values().length - 1 > 65_535) {
            throw new IllegalArgumentException("spike.impairmentPort does not leave room for all mode ports");
        }
        if (packetCount < 6) {
            throw new IllegalArgumentException("spike.impairmentPacketCount must be >= 6 so every deterministic impairment is exercised");
        }
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("spike.impairmentTimeoutMillis must be >= 1");
        }
    }

    private static void rethrow(String message, Throwable failure) {
        if (failure == null) {
            return;
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException(message, failure);
    }

    private enum ImpairmentMode {
        LATENCY("latency"), JITTER("jitter"), LOSS("loss"), DUPLICATION("duplication"), REORDERING("reordering");

        private final String cliName;

        ImpairmentMode(String cliName) {
            this.cliName = cliName;
        }

        private static ImpairmentMode parse(String value) {
            for (ImpairmentMode mode : values()) {
                if (mode.cliName.equals(value)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Unknown impairment mode: " + value + ". Expected latency, jitter, loss, duplication, reordering, or all.");
        }
    }

    private record HeldPacket(SocketAddress sender, int sequence, long sentNanos) {
    }

    private record ScenarioResult(List<Integer> receiveOrder, Map<Integer, Integer> countsBySequence, Map<Integer, Double> rttBySequence) {
        private double minimumRttMillis() {
            return rttBySequence.values().stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        }

        private double maximumRttMillis() {
            return rttBySequence.values().stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        }

        private String summary(ImpairmentMode mode, int packetCount) {
            return switch (mode) {
                case LATENCY -> String.format(Locale.ROOT, "fixed %d ms delay observed; min RTT=%.3f ms", FIXED_LATENCY_MILLIS, minimumRttMillis());
                case JITTER -> String.format(Locale.ROOT, "variable delay observed; RTT range=%.3f..%.3f ms", minimumRttMillis(), maximumRttMillis());
                case LOSS -> String.format(Locale.ROOT, "received %d/%d unique packets with deterministic drops", countsBySequence.size(), packetCount);
                case DUPLICATION -> String.format(Locale.ROOT, "received %d datagrams for %d unique sequences with deterministic duplicates", receiveOrder.size(), packetCount);
                case REORDERING -> "receive order=" + receiveOrder;
            };
        }
    }

    @FunctionalInterface
    private interface IntPredicate {
        boolean test(int value);
    }
}
