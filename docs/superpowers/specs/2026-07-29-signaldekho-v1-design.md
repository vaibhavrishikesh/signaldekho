# SignalDekho v1 — Design Spec

**Date:** 2026-07-29
**Status:** Approved (brainstormed with Sunil)
**Working name:** SignalDekho (alt: Signal Sathi — final name TBD before Play Store listing, not blocking implementation)

## What it is

A free Android app that shows, on one screen, every signal present at the user's
location and its strength — cellular (per SIM), WiFi networks, and Bluetooth
devices — plus a "Ghar Scan" mode that builds a room-by-room coverage report
with plain-language recommendations. Bilingual UI (English + Hindi via string
resources; follows system language).

**Differentiator vs competition** (WiFi Analyzer, NetMonster, NetSpot, OpenSignal):
none of them combine all three signal types in one simple UI, none are
Hindi-friendly, and NetSpot charges for coverage mapping. We ship all-in-one +
free room-tagged coverage reports.

## Product decisions (locked)

- **Platform:** Native Android — Kotlin + Jetpack Compose, MVVM, single module, minSdk 26.
- **Monetization:** v1 is completely free, no ads. Revisit after traction.
- **Distribution:** Google Play Store.
- **Privacy:** No backend, no analytics, no data leaves the device. Surveys stored locally (Room DB). This is a selling point in the store listing.
- **Language:** English default + Hindi `values-hi` strings from day 1.

## Screens (3)

### 1. Scanner (home)
One scrollable screen, three sections:
- **SIM/Cellular:** per-SIM operator name, network type (2G/3G/4G/5G), dBm value + signal bars. Dual SIM shows both. Source: `TelephonyManager` / `getAllCellInfo()` (note: Android 10+ may return cached data; show timestamp-based freshness).
- **WiFi:** nearby networks — SSID, band (2.4/5/6 GHz), channel, RSSI color-coded (green ≥ -60 dBm, yellow -60 to -75, red < -75).
- **Bluetooth:** nearby BLE devices — name (or "Unknown device"), RSSI.

**WiFi throttle handling (critical UX constraint):** Android 9+ allows 4 foreground scans per 2 minutes. UI shows a refresh button with a countdown ("agla scan 18s mein") instead of pretending to be live. Cellular and BLE refresh on their own cadence (BLE is continuous while screen is on).

### 2. Ghar Scan (coverage survey)
Flow: add room by name (free text + quick chips: Bedroom, Kitchen, Hall, Chhat…) → stand in room → tap "Naap lo" → app records connected-WiFi RSSI, strongest nearby APs, and cellular dBm per SIM → repeat for next room → finish → Report.
Surveys are persisted on-device; v1 shows the report immediately after a survey (browsing past surveys: v1.1).

### 3. Report
- Room list, each color-coded green/yellow/red for WiFi and cellular separately.
- Best room / worst room callouts.
- Plain-language recommendations from a rules-based `RecommendationEngine` (no AI, no server). Examples: best/weakest-room callouts, "router ko ghar ke beech rakho" heuristic when any room is WEAK, all-good celebration. (Channel-congestion advice: v1.1.)
- **Share as image** button → renders report to a PNG → Android share sheet (WhatsApp-first). Image carries a small "SignalDekho" watermark — this is the marketing loop.

## Architecture

```
ui/ (Compose screens: Scanner, GharScan, Report, Onboarding)
  └── ViewModels (one per screen)
data/
  ├── CellularRepo   — TelephonyManager wrapper; per-SIM CellInfo → dBm, type, operator
  ├── WifiRepo       — WifiManager wrapper; throttle-aware scan scheduler (tracks quota 4/2min, exposes next-allowed-scan time)
  ├── BleRepo        — BluetoothLeScanner wrapper; scan while Scanner screen visible
  └── SurveyStore    — Room DB: Survey, RoomReading entities
domain/
  └── RecommendationEngine — pure Kotlin rules on a completed survey (unit-testable, no Android deps)
```

Each repo exposes a Kotlin Flow of readings; ViewModels combine them. Repos are
constructor-injected (manual DI or Hilt — implementer's choice, keep it simple).

## Permissions & onboarding

- Required: `ACCESS_FINE_LOCATION` (gates WiFi scan results, cell info, BLE scan results), `BLUETOOTH_SCAN` (API 31+, with `neverForLocation` NOT set since we correlate with location context — verify at implementation), `ACCESS_WIFI_STATE`/`CHANGE_WIFI_STATE`, `READ_PHONE_STATE` (for per-SIM info).
- No background location. Ever.
- First-run onboarding screen explains *why* location permission is needed ("Android ka rule hai — WiFi scan ke liye location permission chahiye; hum aapki location kahin nahi bhejte"). Request permission on first scan attempt, not at app open.
- Graceful degraded states: permission denied → explain + settings deeplink; location services off → prompt; airplane mode → show what's still available.

## Play Store compliance

- Location permission declaration in Play Console: core feature = showing nearby-network signal strength; approximate location insufficient because WiFi/BLE scan APIs require FINE.
- Data safety form: no data collected, no data shared.
- Store listing states the privacy stance explicitly.

## Explicitly NOT in v1 (YAGNI)

Speed test, background monitoring, history/graphs over time, floor-plan
heatmap (candidate paid feature later), GPS/outdoor coverage map, ads, accounts,
any network calls.

## Testing

- **Unit tests:** RecommendationEngine (pure Kotlin, main test surface), WifiRepo throttle scheduler (fake clock), repos with fakes.
- **Device QA:** signal APIs return fake/empty data on emulators — real testing happens on a physical Android phone via USB debugging. QA checklist: dual-SIM display, permission-denial flows, throttle countdown correctness, survey → report → share-image flow, Hindi locale pass.

## Success criteria (v1)

- Scanner shows real cellular dBm, WiFi list, and BLE devices on a physical device.
- A full Ghar Scan of 4+ rooms produces a correct, readable, shareable report image.
- All strings render correctly in both English and Hindi system locales.
- Play Store listing passes review (location declaration + data safety).
