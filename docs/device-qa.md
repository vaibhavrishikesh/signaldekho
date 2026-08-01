# Device QA checklist (physical Android phone, USB debugging on)

Install: `adb devices` shows device → `./gradlew :app:installDebug`

## Permissions
- [ ] Fresh install → rationale screen shows BEFORE system permission dialog
- [ ] Deny → denied screen with working "Settings kholo" deeplink
- [ ] Grant via settings → return to app → scanner loads

## Scanner
- [ ] A grade word (Excellent/Good/Weak/Very weak) is the largest text — not dBm
- [ ] Connected WiFi appears in its own hero card; disconnecting swaps it for the not-connected line
- [ ] Duplicate SSIDs collapse into one row with a ×N badge
- [ ] Carrier shows a clean name (e.g. "Jio"), not "JIO 4G — Jio · 4G"
- [ ] Staleness line appears only for readings older than a minute, in minutes
- [ ] Countdown appears after 4 rapid refreshes; Refresh returns when it hits 0
- [ ] Airplane mode / WiFi off / Bluetooth off each degrade gracefully, no crash

## Home scan
- [ ] With WiFi off, the pre-flight warning shows before measuring
- [ ] Measured rooms appear as chips
- [ ] Measure at least 4 rooms in genuinely different spots

## Report
- [ ] Rooms are sorted best signal first, each with a proportional bar and grade word
- [ ] A WiFi-less survey shows the "WiFi wasn't measured" banner and no WiFi rows
- [ ] A survey where all rooms read the same shows no best/weakest ranking
- [ ] Share → WhatsApp → the image matches the screen, nothing clipped

## Stability
- [ ] Rotate on every screen — no crash, state survives
- [ ] Background the app 2 min → resume → scanner recovers
- [ ] Background the app from Scanner → BLE scan + 1s tick actually stop (verify via logcat), resume → scanner recovers
