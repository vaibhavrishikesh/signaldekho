# SignalDekho

Free Android app: see which signals are present where you are — SIM/cellular, WiFi, and
Bluetooth — plus a room-by-room home coverage report.

No ads. No analytics. No internet permission. Nothing leaves your phone.

## Build

```bash
./gradlew :app:assembleDebug          # build
./gradlew :app:testDebugUnitTest      # unit tests
./gradlew :app:installDebug           # install on connected device
```

Requires JDK 17. Signal APIs return fake data on emulators — test on a real
device (see docs/device-qa.md).

## Docs

- Spec: docs/superpowers/specs/2026-07-29-signaldekho-v1-design.md
- Plan: docs/superpowers/plans/2026-07-29-signaldekho-v1.md
- Device QA: docs/device-qa.md
