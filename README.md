<p align="center">
  <img src="app/src/main/res/drawable/ic_swarm_net_launcher_foreground.png" alt="Swarm Net" width="200">
</p>

<h1 align="center">Swarm Net for Android</h1>

<p align="center">
  Decentralized mesh messaging and a lightweight content-addressed <strong>ledger</strong> for shared documents—built on Bluetooth LE, with optional internet transports where available.
</p>

> [!WARNING]
> This software has not received an independent security audit. It may contain vulnerabilities and may not meet its stated security goals. Do not use it for high-risk use cases until it has been reviewed. Work in progress.

---

## Swarm Net in this repo

**Swarm Net** is a fork and evolution of the [bitchat Android](https://github.com/permissionlesstech/bitchat-android) codebase, with a **Swarm Net** product identity, mesh **ledger** support, and related UX and networking improvements. The **binary mesh protocol** remains compatible with the original **bitchat** ecosystem (including [bitchat iOS](https://github.com/jackjackbits/bitchat)) for core chat, channels, and file transfer where protocol versions align.

---

## Network protocols

| Layer | Role |
|--------|------|
| **Bluetooth Low Energy (BLE)** | Primary transport: **GATT** peripheral + central roles, fixed **service UUID** and **characteristic** for mesh packets (see `AppConstants.Mesh.Gatt`). Peers scan, advertise, connect, and exchange framed binary payloads. |
| **Binary mesh protocol** | Compact `BitchatPacket` framing: type byte, TTL, routing, signatures; message types include announcements, chat, `FILE_TRANSFER`, fragment reassembly, sync, and **Swarm Net** `LEDGER_RECORD` (`0x23`) for ledger metadata. |
| **Noise Protocol** | Private messaging: **Noise**-style sessions with peers for encrypted DMs and encrypted file payloads. |
| **Ed25519 signatures** | Broadcast packets: mesh-layer signing for authenticity; identity announcements carry signing keys; ledger **inner** payloads are signed with the publisher’s signing key. |
| **Nostr** | Optional **Nostr** relay transport for geohash / location-adjacent features and relay-backed messaging when the network is available. |
| **Tor (Arti)** | Optional **Tor** integration for privacy-sensitive traffic when internet is used (see `net/` and `jniLibs`). |
| **HTTP / HTTPS** | Utilities like geocoding (e.g. OpenStreetMap) and relay connectivity use standard HTTP clients. |

---

## Features

### Mesh & chat

- **BLE mesh**: Peer discovery, multi-hop relay (TTL-limited), store-and-forward.
- **Public & channel chat**: IRC-style commands (`/join`, `/msg`, `/who`, channels, passworded channels).
- **Private messages**: Noise-encrypted direct messages when sessions exist.
- **Cross-platform**: Core mesh compatibility with **bitchat** on iOS/Android for mesh chat features.
- **Geohash** channels: Location-based Nostr-backed channels when online.
- **Foreground service**: Persistent mesh when the app is allowed to run in the background.

### Swarm Net ledger

- **Content-addressed documents**: SHA-256 hashes; metadata in **`LEDGER_RECORD`** packets; payloads distributed as **`FILE_TRANSFER`** with `ledger_<hash>_…` filenames.
- **Local library**: SQLite + on-disk blobs under app storage; browse ledger entries in the in-app **Ledger** sheet.
- **Verification**: Ledger records are signed; blob integrity is checked against the hash.

### Privacy & safety

- No accounts or phone numbers required for mesh use.
- **Emergency wipe** (triple-tap logo) clears sensitive local state.
- Battery-aware scanning and connection limits.

---

## Requirements

- **Android 8.0+** (API 26+)
- **Bluetooth LE** hardware
- Runtime permissions: Bluetooth (scan/connect/advertise), location (for BLE scanning on Android), notifications (where applicable), and others as prompted

---

## Build

### Prerequisites

- **Android Studio** (recent stable)
- **JDK 17** (recommended for AGP compatibility)
- **Android SDK** (set `sdk.dir` in `local.properties` if needed)

### Clone & build

```bash
git clone https://github.com/bharathparam/Swarm-net.git
cd Swarm-net
./gradlew assembleDebug
```

Debug APK output (example):

`app/build/outputs/apk/debug/app-universal-debug.apk`

### Tests

```bash
./gradlew test
```

---

## Project layout (high level)

| Area | Purpose |
|------|---------|
| `app/src/main/java/.../mesh/` | BLE mesh, GATT, routing, `BluetoothMeshService` |
| `app/src/main/java/.../protocol/` | Binary protocol & message types |
| `app/src/main/java/.../ledger/` | Ledger repository, record payloads, blob naming |
| `app/src/main/java/.../noise/` | Noise encryption |
| `app/src/main/java/.../nostr/` | Nostr integration |
| `app/src/main/java/.../service/` | `MeshForegroundService` |
| `app/src/main/java/.../ui/` | Jetpack Compose UI |

---

## Upstream & license

This project is derived from open-source work by the **bitchat** community. See [LICENSE](LICENSE.md) for license terms.

**Related projects**

- **Upstream Android**: [permissionlesstech/bitchat-android](https://github.com/permissionlesstech/bitchat-android/releases)
- **iOS**: [jackjackbits/bitchat](https://github.com/jackjackbits/bitchat)

---

## Contributing

Contributions are welcome: performance on BLE, UI/UX, security review, tests, and documentation. Open issues or pull requests on this repository.

---

## Support

- **Issues**: [GitHub Issues](https://github.com/bharathparam/Swarm-net/issues) for this repo.

For **upstream** bitchat-specific behavior, also see the [original Android](https://github.com/permissionlesstech/bitchat-android) and [iOS](https://github.com/jackjackbits/bitchat) repositories.
