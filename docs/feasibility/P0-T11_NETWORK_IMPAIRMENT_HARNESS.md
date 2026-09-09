# P0-T11 — Network Impairment Harness

## Purpose

Development-only localhost UDP harness used to prove that the test environment can inject and observe network impairments independently before later gameplay networking work begins.

This is **not** a production transport abstraction, reliability layer, replication system, or Steam transport implementation.

## Implementation

Entry point:

`src/main/java/com/samo/spike/network/NetworkImpairmentHarness.java`

The harness starts a localhost UDP server and client inside one short-lived Java process and uses deterministic rules so results are reproducible.

Default configuration:

- packets: `8`
- timeout: `10,000 ms`
- base port: `42100`
- client send interval: `100 ms`

## Reproducible checks

Run each mode independently from the repository root.

### Latency

```powershell
.\gradlew.bat runNetworkImpairmentLatency
```

Expected observation:

- every packet is delayed by `80 ms` before echo;
- output contains `Harness latency: ... delay=80 ms`;
- measured RTT values visibly increase;
- task ends with `P0-T11 latency passed`.

### Jitter

```powershell
.\gradlew.bat runNetworkImpairmentJitter
```

Expected observation:

- deterministic delay pattern varies between `15`, `55`, `25`, and `65 ms`;
- output contains `Harness jitter:` lines with different delay values;
- measured RTT spread must be at least `25 ms`;
- task ends with `P0-T11 jitter passed`.

### Packet loss

```powershell
.\gradlew.bat runNetworkImpairmentLoss
```

Expected observation:

- every third packet is deliberately dropped;
- with the default 8 packets, sequence `3` and `6` are absent from replies;
- output contains `Harness loss: dropped seq=3` and `seq=6`;
- task ends with `P0-T11 loss passed`.

### Duplication

```powershell
.\gradlew.bat runNetworkImpairmentDuplication
```

Expected observation:

- every fourth packet is deliberately emitted twice;
- with the default 8 packets, sequence `4` and `8` are received twice;
- output contains `Harness duplication: duplicated seq=4` and `seq=8`;
- task ends with `P0-T11 duplication passed`.

### Reordering

```powershell
.\gradlew.bat runNetworkImpairmentReordering
```

Expected observation:

- sequence `2` is held until sequence `3` arrives;
- sequence `3` is emitted before sequence `2`;
- final receive order contains `3` before `2`;
- task ends with `P0-T11 reordering passed`.

## Full short suite

```powershell
.\gradlew.bat runNetworkImpairmentHarness
```

This runs all five modes sequentially. It should finish in seconds, not minutes, and end with:

```text
P0-T11 suite passed: all five impairment modes were observed independently.
```

## Tuning

Optional Gradle properties:

```powershell
.\gradlew.bat runNetworkImpairmentHarness -PimpairmentPacketCount=8 -PimpairmentTimeoutMillis=10000 -PimpairmentPort=42100
```

`impairmentPacketCount` must be at least `6` so the deterministic loss, duplication, and reordering cases are all exercised.

## Acceptance

P0-T11 is complete only after local runtime output proves all five modes independently and the full suite completes successfully.
