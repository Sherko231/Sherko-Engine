# P0-T08 — SteamNetworkingSockets Java Coverage Audit

## Decision

**Result: FAILED for production transport coverage.**

The selected binding, **steamworks4j 1.10.0**, is sufficient for basic Steam client initialization and callbacks (proven by P0-T07), but it does **not** expose the `ISteamNetworkingSockets` API surface required by Sherko Engine's listen-server transport.

The binding exposes the older `SteamNetworking` / `ISteamNetworking` P2P packet-session API (`sendP2PPacket`, `readP2PPacket`, `acceptP2PSessionWithUser`, etc.). Those methods are not substitutes for the connection-oriented `ISteamNetworkingSockets` operations required by the engine.

Therefore the Phase 0 critical path proceeds to **P0-T09 — Java FFM access to the official Steam flat API**.

## Required API coverage

| Required engine operation | Official `ISteamNetworkingSockets` operation | steamworks4j 1.10.0 callable Java method | Coverage |
| --- | --- | --- | --- |
| Create/listen socket | `CreateListenSocketP2P` (or `CreateListenSocketIP` for an IP endpoint) | None | **Unsupported** |
| Outbound connect | `ConnectP2P` (or `ConnectByIPAddress`) | None | **Unsupported** |
| Accept incoming connection | `AcceptConnection` | None | **Unsupported** |
| Send messages on a connection | `SendMessageToConnection` | None | **Unsupported** |
| Receive messages on a connection | `ReceiveMessagesOnConnection` | None | **Unsupported** |
| Connection/status callbacks | `SteamNetConnectionStatusChangedCallback_t` | None | **Unsupported** |

## What steamworks4j 1.10.0 actually exposes

`com.codedisaster.steamworks.SteamNetworking` exposes the older Steam P2P packet/session API, including:

- `sendP2PPacket(...)`
- `isP2PPacketAvailable(...)`
- `readP2PPacket(...)`
- `acceptP2PSessionWithUser(...)`
- `closeP2PSessionWithUser(...)`
- `closeP2PChannelWithUser(...)`
- `getP2PSessionState(...)`
- `allowP2PPacketRelay(...)`

Its networking callback interface exposes only the older session callbacks:

- `onP2PSessionConnectFail(...)`
- `onP2PSessionRequest(...)`

There is no `SteamNetworkingSockets` Java wrapper class, no connection handle API matching `HSteamNetConnection`, no listen-socket handle API matching `HSteamListenSocket`, and no Java callback matching `SteamNetConnectionStatusChangedCallback_t` in version 1.10.0.

## Why the older P2P methods do not satisfy P0-T08

P0-T08 is specifically a feasibility gate for a production listen-server transport built on `ISteamNetworkingSockets`. The required model is explicit connection/listen-socket ownership with connection lifecycle callbacks and message send/receive APIs.

The old `ISteamNetworking` P2P packet API uses implicit peer sessions keyed by Steam ID and provides different lifecycle semantics. Treating `sendP2PPacket`/`readP2PPacket` as equivalent to `SendMessageToConnection`/`ReceiveMessagesOnConnection` would incorrectly pass the gate while leaving the required production API unavailable.

## Sources checked

- steamworks4j 1.10.0 `SteamNetworking.java`: https://github.com/code-disaster/steamworks4j/blob/1.10.0/java-wrapper/src/main/java/com/codedisaster/steamworks/SteamNetworking.java
- steamworks4j 1.10.0 `SteamNetworkingCallback.java`: https://github.com/code-disaster/steamworks4j/blob/1.10.0/java-wrapper/src/main/java/com/codedisaster/steamworks/SteamNetworkingCallback.java
- steamworks4j 1.10.0 changelog: https://github.com/code-disaster/steamworks4j/blob/1.10.0/CHANGES.md
- Valve `ISteamNetworkingSockets`: https://partner.steamgames.com/doc/api/ISteamNetworkingSockets

## Next action

Run **P0-T09**. The next spike must prove that Java 25's Foreign Function & Memory API can load the official Steam redistributable and invoke one harmless Steam flat-API networking function **without authored C/C++ glue**.
