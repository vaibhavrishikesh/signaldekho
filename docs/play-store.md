# Play Store prep

## Decisions
- Final app name: **SignalDekho** (locked 2026-07-29; "Signal Range" was available
  but generic)

## Console tasks (manual, Sunil's account)
1. App listing: category Tools; title + short/full description (English)
2. Location permission declaration: core feature = showing signal strength of
   nearby networks/towers; WiFi scan + cell info + BLE scan APIs REQUIRE
   ACCESS_FINE_LOCATION — approximate is technically insufficient
3. Data safety form: no data collected, no data shared, no data encrypted-in-
   transit claim needed (no network calls at all — app has no INTERNET permission,
   verifiable in the manifest)
4. Content rating questionnaire: utility, no objectionable content
5. Release: signed AAB via `./gradlew :app:bundleRelease` (needs upload
   keystore — generate once, back it up: losing it = losing the app listing)

## Store copy angle
- "All your signals in one place — SIM, WiFi, and Bluetooth"
- "Free room-by-room coverage report (competitors charge for this)"
- "No internet permission at all — your data cannot leave the phone, and that is verifiable in the manifest"
