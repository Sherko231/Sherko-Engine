package com.samo.spike.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.Locale;

public final class LocalhostUdpSpike {
    private static final String HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 42_060;
    private static final int DEFAULT_PACKET_COUNT = 8;
    private static final int DEFAULT_TIMEOUT_MILLIS = 10_000;
    private static final int PACKET_BYTES = Integer.BYTES + Long.BYTES;

    private LocalhostUdpSpike() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one argument: server or client");
        }

        String role = args[0].toLowerCase(Locale.ROOT);
        int port = Integer.getInteger("spike.udpPort", DEFAULT_PORT);
        int packetCount = Integer.getInteger("spike.packetCount", DEFAULT_PACKET_COUNT);
        int timeoutMillis = Integer.getInteger("spike.timeoutMillis", DEFAULT_TIMEOUT_MILLIS);

        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("spike.udpPort must be between 1 and 65535");
        }
        if (packetCount < 1) {
            throw new IllegalArgumentException("spike.packetCount must be >= 1");
        }
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("spike.timeoutMillis must be >= 1");
        }

        switch (role) {
            case "server" -> runServer(port, packetCount, timeoutMillis);
            case "client" -> runClient(port, packetCount, timeoutMillis);
            default -> throw new IllegalArgumentException("Unknown role: " + args[0]);
        }
    }

    private static void runServer(int port, int packetCount, int timeoutMillis) throws IOException {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(HOST, port));
            channel.configureBlocking(false);

            System.out.printf("P0-T06 server listening on %s:%d, expecting %d packets.%n", HOST, port, packetCount);

            ByteBuffer packet = newPacketBuffer();
            long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
            int receivedCount = 0;

            while (receivedCount < packetCount) {
                packet.clear();
                SocketAddress sender = channel.receive(packet);
                if (sender == null) {
                    if (System.nanoTime() >= deadlineNanos) {
                        throw new IllegalStateException("Server timed out after receiving " + receivedCount + "/" + packetCount + " packets");
                    }
                    Thread.onSpinWait();
                    continue;
                }

                if (packet.position() != PACKET_BYTES) {
                    throw new IllegalStateException("Server received invalid packet size: " + packet.position());
                }

                packet.flip();
                int sequence = packet.getInt();
                long sentNanos = packet.getLong();

                System.out.printf("Server received seq=%d from %s%n", sequence, sender);

                packet.clear();
                packet.putInt(sequence);
                packet.putLong(sentNanos);
                packet.flip();

                int bytesSent = channel.send(packet, sender);
                if (bytesSent != PACKET_BYTES) {
                    throw new IllegalStateException("Server sent unexpected byte count: " + bytesSent);
                }

                System.out.printf("Server echoed seq=%d%n", sequence);
                receivedCount++;
                deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
            }

            System.out.printf("P0-T06 server passed: received and echoed %d numbered UDP datagrams.%n", receivedCount);
        }
    }

    private static void runClient(int port, int packetCount, int timeoutMillis) throws IOException, InterruptedException {
        InetSocketAddress serverAddress = new InetSocketAddress(HOST, port);

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(HOST, 0));
            channel.connect(serverAddress);
            channel.configureBlocking(false);

            System.out.printf("P0-T06 client connected to %s:%d, sending %d packets.%n", HOST, port, packetCount);

            ByteBuffer packet = newPacketBuffer();
            double totalRttMillis = 0.0;

            for (int sequence = 1; sequence <= packetCount; sequence++) {
                long sentNanos = System.nanoTime();

                packet.clear();
                packet.putInt(sequence);
                packet.putLong(sentNanos);
                packet.flip();

                while (packet.hasRemaining()) {
                    channel.write(packet);
                }
                System.out.printf("Client sent seq=%d%n", sequence);

                long deadlineNanos = sentNanos + timeoutMillis * 1_000_000L;
                boolean replyReceived = false;

                while (!replyReceived) {
                    packet.clear();
                    SocketAddress sender = channel.receive(packet);
                    if (sender == null) {
                        if (System.nanoTime() >= deadlineNanos) {
                            throw new IllegalStateException("Client timed out waiting for seq=" + sequence);
                        }
                        Thread.onSpinWait();
                        continue;
                    }

                    if (packet.position() != PACKET_BYTES) {
                        throw new IllegalStateException("Client received invalid packet size: " + packet.position());
                    }

                    long receivedNanos = System.nanoTime();
                    packet.flip();
                    int echoedSequence = packet.getInt();
                    long echoedSentNanos = packet.getLong();

                    if (echoedSequence != sequence) {
                        throw new IllegalStateException("Client expected seq=" + sequence + " but received seq=" + echoedSequence);
                    }
                    if (echoedSentNanos != sentNanos) {
                        throw new IllegalStateException("Client received mismatched timestamp for seq=" + sequence);
                    }

                    double rttMillis = (receivedNanos - sentNanos) / 1_000_000.0;
                    totalRttMillis += rttMillis;

                    System.out.printf("Client received seq=%d RTT=%.3f ms%n", echoedSequence, rttMillis);
                    replyReceived = true;
                }

                Thread.sleep(50L);
            }

            double averageRttMillis = totalRttMillis / packetCount;
            System.out.printf("P0-T06 client passed: received %d/%d replies; average RTT=%.3f ms.%n", packetCount, packetCount, averageRttMillis);
        }
    }

    private static ByteBuffer newPacketBuffer() {
        return ByteBuffer.allocateDirect(PACKET_BYTES).order(ByteOrder.BIG_ENDIAN);
    }
}
