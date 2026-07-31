# Device QA checklist (physical Android phone, USB debugging on)

Install: `adb devices` shows device → `./gradlew :app:installDebug`

## Permissions
- [ ] Fresh install → rationale screen shows BEFORE system permission dialog
- [ ] Deny → denied screen with working "Settings kholo" deeplink
- [ ] Grant via settings → return to app → scanner loads

## Scanner
- [ ] SIM section: correct operator name(s); dual-SIM phones show both slots
- [ ] dBm value plausible (-50 to -120) and color dot matches grade
- [ ] WiFi list populates after Refresh; connected network tagged "Connected"
- [ ] Countdown appears after 4 rapid refreshes; hits 0 → Refresh returns
- [ ] BLE devices appear within ~10s (test near earbuds/watch)
- [ ] Airplane mode → SIM section shows no-SIM state, app doesn't crash
- [ ] WiFi off → empty WiFi state, no crash
- [ ] Bluetooth off → empty BLE state, no crash

## Ghar Scan → Report
- [ ] Measure 4 rooms (walk to genuinely different spots)
- [ ] Report shows 4 rows, colors differ where signal differed
- [ ] Findings make sense (best/weakest room match reality)
- [ ] Share → WhatsApp → image is readable, watermark present

## Locale
- [ ] Phone language → हिन्दी → all screens render Hindi strings
- [ ] Back to English → English strings

## Stability
- [ ] Rotate on every screen — no crash, state survives
- [ ] Background the app 2 min → resume → scanner recovers
