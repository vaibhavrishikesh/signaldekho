# SignalDekho v1.1 — Redesign + Report Rework

**Date:** 2026-07-31
**Status:** Approved (design mockup reviewed with Sunil after v1 device QA)
**Base:** v1 merged at `f678957`

## Why

Device QA on a Redmi Note 7 Pro proved v1 works but showed two real problems:

1. **It looks like a debug dump.** Flat unstyled lists on white, no hierarchy. The
   app's whole subject — signal strength — is carried by a 12dp dot, the smallest
   element on screen. Redundant text (`SIM1 JIO 4G — Jio · 4G`), useless precision
   (`17349 sec purana`), 20 identical WiFi rows.
2. **The report can be silently useless.** During QA the phone was not connected to
   WiFi while measuring, so every room recorded `wifiRssi = null` and the report showed
   `WiFi —` on every row with no explanation. It also ranked three rooms whose cell
   readings were identical (−111 each), producing a meaningless "weakest room" finding.
   This is the artifact meant to be shared on WhatsApp as the app's marketing loop.

The core v1 promise — a signal scanner an ordinary Hindi-speaking user understands —
is not met while dBm is the primary number and no plain-language verdict exists.

## Locked decisions

- **Plain-language grades replace raw dBm as the primary signal.** Four levels:
  `Badhiya` / `Theek` / `Kamzor` / `Bahut kamzor`. dBm still shown, small and muted.
  Level names live in `strings.xml` (both locales) so they can change without code.
- Thresholds extend v1's locked 3-level scale to 4 by splitting the top band:
  - WiFi RSSI: `Badhiya ≥ −55`, `Theek ≥ −67`, `Kamzor ≥ −80`, else `Bahut kamzor`
  - Cell dBm: `Badhiya ≥ −85`, `Theek ≥ −95`, `Kamzor ≥ −110`, else `Bahut kamzor`
  (v1's 3-level `Grade` is replaced, not kept alongside — one grading vocabulary.)
- No new permissions, no INTERNET, no ads/analytics. Still fully offline.
- No new screens. Same three destinations, same navigation graph.

## Scanner screen

**Connected WiFi hero card** (only when connected): tinted card with the network name,
the grade word at 26sp, dBm small, and a 4-segment strength bar. This answers "mera
WiFi kaisa hai" without reading a list.

**SIM rows**: carrier name cleaned (strip a trailing network-type suffix so
`JIO 4G — Jio` renders `Jio`), network type as a small outlined badge, grade word +
dBm right-aligned, 4-segment bar. Staleness shown only when a reading is genuinely old
(> 60s) and phrased in minutes, not seconds.

**Nearby networks**: deduplicated by SSID — strongest entry wins, with a `×N` count
badge when an SSID appears more than once. Compact rows: a small 3-bar meter, SSID,
band. Channel moves out of the primary row (it is engineer detail; keep it in the row
only as part of the existing detail line, band-first).

**Bluetooth**: unchanged in substance, kept last.

## Ghar Scan screen

**Pre-flight WiFi warning**: when the phone is not connected to WiFi, a warning banner
sits above the measure button — "WiFi se connect nahi ho — sirf SIM signal naapa
jayega." The user learns the limitation *before* spending effort walking the house.
Measuring is still allowed (cell-only surveys are legitimate).

Measured rooms render as chips rather than stacked text lines.

## Report screen

- Header: "Ghar ki coverage", room count + date.
- **Missing-data banner**: if no room captured a WiFi reading, an explicit banner says
  WiFi was not measured and why. Replaces silent `—` cells.
- **Room rows sorted best → worst**, each with a proportional bar and the grade word.
  Bar fill maps the reading within its scale (WiFi: −30 to −90; cell: −70 to −120),
  clamped to 0–100%.
- Both WiFi and cell values are colored by grade (v1 colored only on screen, and the
  shared image colored only WiFi).
- **Advice block** replaces bare findings: each finding renders with an icon and states
  an action ("router ko Hall ki taraf, oonchi jagah rakho"), not just an observation.

## RecommendationEngine changes

The v1 engine ranked rooms even when readings were identical and had no concept of
"this signal was never measured". New rules:

- If every room's reading for a signal is within 3 dBm, emit `AllRoomsSimilar` for
  that signal instead of best/weakest findings — no fake ranking.
- If no room has a WiFi reading, emit `WifiNotMeasured` instead of any WiFi finding.
- Keep: best room, weakest room, router-reposition (when any room is `Bahut kamzor`),
  all-good celebration.
- Add `BestRoomForCalls(room)` when cell readings differ meaningfully — the practical
  question users actually ask.

Engine stays pure Kotlin and remains the main unit-test surface. Every new rule gets
tests, including the two regressions QA exposed (all-identical readings; no WiFi data).

## Share image

`ReportImageRenderer` mirrors the new report: grade words, colored bars per room,
the missing-data banner line when applicable, advice lines, watermark. Its scaffolding
strings ("Coverage Report", "WiFi", "SIM") move to resources so the shared image is
Hindi when the phone is Hindi — currently they are hardcoded English.

## Out of scope (v1.2+)

Floor-plan heatmap, history of past surveys, BLE-specific thresholds, speed test,
background monitoring, moving repo IPC off the main thread.

## Success criteria

- No raw dBm appears as the largest element on any screen.
- A survey taken with WiFi disconnected produces a report that says so, in both languages.
- A survey where all rooms read identically produces no ranking finding.
- Shared PNG matches the on-screen report and renders Hindi under a Hindi locale.
- Unit tests cover every new engine rule; existing tests updated for the 4-level scale.
