# SignalDekho v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Free Android app showing cellular + WiFi + BLE signals with strength on one screen, plus a room-by-room "Ghar Scan" coverage survey producing a shareable, bilingual (EN/HI) report.

**Architecture:** Native Android, Kotlin + Jetpack Compose, MVVM with manual DI (an `AppContainer` on the `Application` class — no Hilt). Three thin Android repos (`CellularRepo`, `WifiRepo`, `BleRepo`) expose readings; pure-Kotlin domain (`SignalGrade`, `WifiThrottleScheduler`, `RecommendationEngine`) holds all testable logic; Room DB persists surveys. No backend, no analytics, no network calls.

**Tech Stack:** Kotlin 2.1, Jetpack Compose (BOM 2024.12.01), Navigation-Compose, Room 2.6.1 (KSP), JUnit4, AGP 8.7.3, JDK 17.

## Global Constraints

- Package: `com.signaldekho.app`. Project dir: `/Users/vaibhav/workspace/signaldekho` (git repo already initialized, spec committed).
- minSdk 26, targetSdk 35, compileSdk 35.
- v1 is completely free: NO ads SDK, NO analytics, NO network permission (`INTERNET` must NOT appear in the manifest).
- No background location, no foreground-service scanning. All scanning happens while the app is visible.
- Every user-visible string lives in `res/values/strings.xml` (English) — never hardcoded in composables. Hindi arrives in Task 13 via `res/values-hi/strings.xml`.
- WiFi scan throttle (Android 9+): 4 foreground scans per 2 minutes. UI must show countdown, never pretend to be live.
- Signal grading thresholds (locked): WiFi RSSI — GOOD ≥ −60 dBm, OK ≥ −75, WEAK below. Cellular dBm — GOOD ≥ −90, OK ≥ −105, WEAK below.
- Emulators return fake/empty signal data: repos are verified by device QA (Task 14), not unit tests. Unit tests cover only pure Kotlin (domain + mappers).
- Commit after every green test cycle. Commit messages end with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.
- All Gradle commands run from `/Users/vaibhav/workspace/signaldekho`. JDK 17 required (`export JAVA_HOME=$(/usr/libexec/java_home -v 17)` if needed; if no JDK 17, `brew install --cask temurin@17`).

---

### Task 1: Project scaffold

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/signaldekho/app/MainActivity.kt`, `app/src/main/java/com/signaldekho/app/ui/theme/Theme.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `.gitignore`
- Test: none (deliverable = `./gradlew :app:assembleDebug` succeeds)

**Interfaces:**
- Produces: buildable app skeleton; version catalog aliases used by all later tasks; manifest with all permissions.

- [ ] **Step 1: Gradle wrapper**

```bash
cd /Users/vaibhav/workspace/signaldekho
which gradle || brew install gradle
gradle wrapper --gradle-version 8.11.1
```

- [ ] **Step 2: Write root build files**

`.gitignore`:
```
.gradle/
build/
local.properties
.idea/
*.iml
.DS_Store
/captures
```

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "SignalDekho"
include(":app")
```

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

`gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
composeBom = "2024.12.01"
activityCompose = "1.9.3"
lifecycle = "2.8.7"
navigationCompose = "2.8.5"
room = "2.6.1"
coreKtx = "1.15.0"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 3: Write app module build file**

`app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.signaldekho.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.signaldekho.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
}
```

Also create empty `app/proguard-rules.pro` (comment line only: `# SignalDekho keep rules`).

- [ ] **Step 4: Manifest with all permissions**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        tools:targetApi="s" />

    <application
        android:name=".SignalDekhoApp"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:supportsRtl="true"
        android:theme="@style/Theme.SignalDekho">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Note: `BLUETOOTH_SCAN` has NO `neverForLocation` flag — readings are correlated with rooms, and pre-31 BLE scan already rides on fine location. `.SignalDekhoApp` doesn't exist until Task 7 — for THIS task only, omit the `android:name=".SignalDekhoApp"` line; Task 7 adds it.

For the launcher icon in this task, generate a placeholder adaptive icon:
`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_bg" />
    <foreground android:drawable="@drawable/ic_launcher_fg" />
</adaptive-icon>
```
`app/src/main/res/values/colors.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_bg">#0B5FFF</color>
</resources>
```
`app/src/main/res/drawable/ic_launcher_fg.xml` (simple white signal-bars glyph):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF"
        android:pathData="M30,72 h8 v-12 h-8 z M44,72 h8 v-20 h-8 z M58,72 h8 v-28 h-8 z M72,72 h8 v-36 h-8 z" />
</vector>
```

- [ ] **Step 5: Strings, theme, MainActivity**

`app/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SignalDekho</string>
</resources>
```

`app/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.SignalDekho" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`app/src/main/java/com/signaldekho/app/ui/theme/Theme.kt`:
```kotlin
package com.signaldekho.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GradeGood = Color(0xFF2E7D32)
val GradeOk = Color(0xFFF9A825)
val GradeWeak = Color(0xFFC62828)

@Composable
fun SignalDekhoTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = scheme, content = content)
}
```

`app/src/main/java/com/signaldekho/app/MainActivity.kt`:
```kotlin
package com.signaldekho.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.signaldekho.app.ui.theme.SignalDekhoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SignalDekhoTheme { Text("SignalDekho") } }
    }
}
```

- [ ] **Step 6: Build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: Android project scaffold — Compose app builds

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: SignalGrade (domain, TDD)

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/domain/SignalGrade.kt`
- Test: `app/src/test/java/com/signaldekho/app/domain/SignalGradeTest.kt`

**Interfaces:**
- Produces: `enum class Grade { GOOD, OK, WEAK }`; `object SignalGrade { fun wifi(rssi: Int): Grade; fun cell(dbm: Int): Grade }`. Used by Scanner UI, Report UI, RecommendationEngine.

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/signaldekho/app/domain/SignalGradeTest.kt`:
```kotlin
package com.signaldekho.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalGradeTest {
    @Test fun `wifi grades at locked thresholds`() {
        assertEquals(Grade.GOOD, SignalGrade.wifi(-60))
        assertEquals(Grade.GOOD, SignalGrade.wifi(-30))
        assertEquals(Grade.OK, SignalGrade.wifi(-61))
        assertEquals(Grade.OK, SignalGrade.wifi(-75))
        assertEquals(Grade.WEAK, SignalGrade.wifi(-76))
        assertEquals(Grade.WEAK, SignalGrade.wifi(-90))
    }

    @Test fun `cell grades at locked thresholds`() {
        assertEquals(Grade.GOOD, SignalGrade.cell(-90))
        assertEquals(Grade.GOOD, SignalGrade.cell(-70))
        assertEquals(Grade.OK, SignalGrade.cell(-91))
        assertEquals(Grade.OK, SignalGrade.cell(-105))
        assertEquals(Grade.WEAK, SignalGrade.cell(-106))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.signaldekho.app.domain.SignalGradeTest"`
Expected: FAIL — unresolved reference `Grade` / `SignalGrade`

- [ ] **Step 3: Implement**

`app/src/main/java/com/signaldekho/app/domain/SignalGrade.kt`:
```kotlin
package com.signaldekho.app.domain

enum class Grade { GOOD, OK, WEAK }

object SignalGrade {
    fun wifi(rssi: Int): Grade = when {
        rssi >= -60 -> Grade.GOOD
        rssi >= -75 -> Grade.OK
        else -> Grade.WEAK
    }

    fun cell(dbm: Int): Grade = when {
        dbm >= -90 -> Grade.GOOD
        dbm >= -105 -> Grade.OK
        else -> Grade.WEAK
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.signaldekho.app.domain.SignalGradeTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(domain): SignalGrade with locked wifi/cell thresholds

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: WifiThrottleScheduler (domain, TDD)

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/domain/WifiThrottleScheduler.kt`
- Test: `app/src/test/java/com/signaldekho/app/domain/WifiThrottleSchedulerTest.kt`

**Interfaces:**
- Consumes: nothing (clock injected as `() -> Long` millis).
- Produces: `class WifiThrottleScheduler(private val clock: () -> Long)` with `fun canScanNow(): Boolean`, `fun recordScan()`, `fun nextAllowedAtMillis(): Long` (returns `clock()` when allowed now). Used by `WifiRepo` (Task 5) and countdown UI.

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/signaldekho/app/domain/WifiThrottleSchedulerTest.kt`:
```kotlin
package com.signaldekho.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiThrottleSchedulerTest {
    private var now = 0L
    private val scheduler = WifiThrottleScheduler { now }

    @Test fun `allows 4 scans then blocks`() {
        repeat(4) {
            assertTrue(scheduler.canScanNow())
            scheduler.recordScan()
        }
        assertFalse(scheduler.canScanNow())
    }

    @Test fun `slot frees 120s after oldest scan`() {
        scheduler.recordScan()          // t=0
        now = 10_000; scheduler.recordScan()
        now = 20_000; scheduler.recordScan()
        now = 30_000; scheduler.recordScan()
        now = 119_999
        assertFalse(scheduler.canScanNow())
        assertEquals(120_000L, scheduler.nextAllowedAtMillis())
        now = 120_000
        assertTrue(scheduler.canScanNow())
    }

    @Test fun `nextAllowedAtMillis is now when quota free`() {
        now = 5_000
        assertEquals(5_000L, scheduler.nextAllowedAtMillis())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.signaldekho.app.domain.WifiThrottleSchedulerTest"`
Expected: FAIL — unresolved reference

- [ ] **Step 3: Implement**

`app/src/main/java/com/signaldekho/app/domain/WifiThrottleScheduler.kt`:
```kotlin
package com.signaldekho.app.domain

/** Android 9+ allows 4 foreground WiFi scans per 2 minutes. */
class WifiThrottleScheduler(private val clock: () -> Long) {
    private val windowMillis = 120_000L
    private val maxScans = 4
    private val scanTimes = ArrayDeque<Long>()

    fun canScanNow(): Boolean {
        prune()
        return scanTimes.size < maxScans
    }

    fun recordScan() {
        prune()
        scanTimes.addLast(clock())
    }

    fun nextAllowedAtMillis(): Long {
        prune()
        return if (scanTimes.size < maxScans) clock() else scanTimes.first() + windowMillis
    }

    private fun prune() {
        val cutoff = clock() - windowMillis
        while (scanTimes.isNotEmpty() && scanTimes.first() <= cutoff) scanTimes.removeFirst()
    }
}
```

Note the boundary: a scan at t=0 frees its slot at exactly t=120_000 (`<= cutoff` prune with `cutoff = now - window`).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.signaldekho.app.domain.WifiThrottleSchedulerTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(domain): throttle-aware WiFi scan scheduler

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: RecommendationEngine (domain, TDD)

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/domain/RecommendationEngine.kt`
- Test: `app/src/test/java/com/signaldekho/app/domain/RecommendationEngineTest.kt`

**Interfaces:**
- Consumes: `Grade`, `SignalGrade` (Task 2).
- Produces:
```kotlin
data class RoomResult(val roomName: String, val wifiRssi: Int?, val cellDbm: Int?)
sealed interface Finding {
    data class BestWifiRoom(val room: String) : Finding
    data class WeakestWifiRoom(val room: String) : Finding
    data class WeakestCellRoom(val room: String) : Finding
    data object WifiAllGood : Finding
    data object RouterReposition : Finding   // emitted when any room's wifi is WEAK
}
object RecommendationEngine { fun analyze(rooms: List<RoomResult>): List<Finding> }
```
Used by Report UI (Task 11), which maps each `Finding` to a localized string.

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/signaldekho/app/domain/RecommendationEngineTest.kt`:
```kotlin
package com.signaldekho.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    @Test fun `empty survey yields no findings`() {
        assertEquals(emptyList<Finding>(), RecommendationEngine.analyze(emptyList()))
    }

    @Test fun `identifies best and weakest wifi rooms`() {
        val rooms = listOf(
            RoomResult("Hall", wifiRssi = -50, cellDbm = -80),
            RoomResult("Kitchen", wifiRssi = -80, cellDbm = -85),
            RoomResult("Bedroom", wifiRssi = -65, cellDbm = -95),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.BestWifiRoom("Hall")))
        assertTrue(findings.contains(Finding.WeakestWifiRoom("Kitchen")))
        assertTrue(findings.contains(Finding.RouterReposition))
    }

    @Test fun `all-good wifi yields WifiAllGood and no reposition`() {
        val rooms = listOf(
            RoomResult("Hall", wifiRssi = -50, cellDbm = -80),
            RoomResult("Bedroom", wifiRssi = -55, cellDbm = -85),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.WifiAllGood))
        assertTrue(findings.none { it is Finding.RouterReposition })
        assertTrue(findings.none { it is Finding.WeakestWifiRoom })
    }

    @Test fun `weakest cell room reported only when some room is below GOOD`() {
        val rooms = listOf(
            RoomResult("Hall", wifiRssi = null, cellDbm = -80),
            RoomResult("Chhat", wifiRssi = null, cellDbm = -110),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.WeakestCellRoom("Chhat")))
    }

    @Test fun `rooms with null readings are skipped for that signal`() {
        val rooms = listOf(
            RoomResult("Hall", wifiRssi = -50, cellDbm = null),
            RoomResult("Store", wifiRssi = null, cellDbm = null),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.WifiAllGood))
        assertTrue(findings.none { it is Finding.WeakestCellRoom })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.signaldekho.app.domain.RecommendationEngineTest"`
Expected: FAIL — unresolved references

- [ ] **Step 3: Implement**

`app/src/main/java/com/signaldekho/app/domain/RecommendationEngine.kt`:
```kotlin
package com.signaldekho.app.domain

data class RoomResult(val roomName: String, val wifiRssi: Int?, val cellDbm: Int?)

sealed interface Finding {
    data class BestWifiRoom(val room: String) : Finding
    data class WeakestWifiRoom(val room: String) : Finding
    data class WeakestCellRoom(val room: String) : Finding
    data object WifiAllGood : Finding
    data object RouterReposition : Finding
}

object RecommendationEngine {
    fun analyze(rooms: List<RoomResult>): List<Finding> {
        val findings = mutableListOf<Finding>()

        val wifiRooms = rooms.filter { it.wifiRssi != null }
        if (wifiRooms.isNotEmpty()) {
            val best = wifiRooms.maxBy { it.wifiRssi!! }
            val worst = wifiRooms.minBy { it.wifiRssi!! }
            findings += Finding.BestWifiRoom(best.roomName)
            if (wifiRooms.all { SignalGrade.wifi(it.wifiRssi!!) == Grade.GOOD }) {
                findings += Finding.WifiAllGood
            } else {
                findings += Finding.WeakestWifiRoom(worst.roomName)
                if (wifiRooms.any { SignalGrade.wifi(it.wifiRssi!!) == Grade.WEAK }) {
                    findings += Finding.RouterReposition
                }
            }
        }

        val cellRooms = rooms.filter { it.cellDbm != null }
        if (cellRooms.isNotEmpty() && cellRooms.any { SignalGrade.cell(it.cellDbm!!) != Grade.GOOD }) {
            findings += Finding.WeakestCellRoom(cellRooms.minBy { it.cellDbm!! }.roomName)
        }
        return findings
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.signaldekho.app.domain.RecommendationEngineTest"`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(domain): rules-based RecommendationEngine

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: Reading models + WiFi channel mapper (TDD for mapper)

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/data/Readings.kt`, `app/src/main/java/com/signaldekho/app/data/WifiBand.kt`
- Test: `app/src/test/java/com/signaldekho/app/data/WifiBandTest.kt`

**Interfaces:**
- Produces:
```kotlin
data class CellReading(val simSlot: Int, val operatorName: String, val networkType: String, val dbm: Int?, val ageMillis: Long)
data class WifiReading(val ssid: String, val bssid: String, val rssi: Int, val frequencyMhz: Int) {
    val band: WifiBand; val channel: Int   // derived
}
data class BleReading(val name: String?, val address: String, val rssi: Int)
enum class WifiBand { GHZ_2_4, GHZ_5, GHZ_6, UNKNOWN }
object WifiChannels { fun band(freqMhz: Int): WifiBand; fun channel(freqMhz: Int): Int }
```

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/signaldekho/app/data/WifiBandTest.kt`:
```kotlin
package com.signaldekho.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiBandTest {
    @Test fun `band detection`() {
        assertEquals(WifiBand.GHZ_2_4, WifiChannels.band(2412))
        assertEquals(WifiBand.GHZ_2_4, WifiChannels.band(2484))
        assertEquals(WifiBand.GHZ_5, WifiChannels.band(5180))
        assertEquals(WifiBand.GHZ_6, WifiChannels.band(5955))
        assertEquals(WifiBand.UNKNOWN, WifiChannels.band(900))
    }

    @Test fun `channel from frequency`() {
        assertEquals(1, WifiChannels.channel(2412))
        assertEquals(6, WifiChannels.channel(2437))
        assertEquals(11, WifiChannels.channel(2462))
        assertEquals(14, WifiChannels.channel(2484))
        assertEquals(36, WifiChannels.channel(5180))
        assertEquals(149, WifiChannels.channel(5745))
        assertEquals(1, WifiChannels.channel(5955))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.signaldekho.app.data.WifiBandTest"`
Expected: FAIL — unresolved references

- [ ] **Step 3: Implement**

`app/src/main/java/com/signaldekho/app/data/WifiBand.kt`:
```kotlin
package com.signaldekho.app.data

enum class WifiBand { GHZ_2_4, GHZ_5, GHZ_6, UNKNOWN }

object WifiChannels {
    fun band(freqMhz: Int): WifiBand = when (freqMhz) {
        in 2400..2500 -> WifiBand.GHZ_2_4
        in 5150..5895 -> WifiBand.GHZ_5
        in 5925..7125 -> WifiBand.GHZ_6
        else -> WifiBand.UNKNOWN
    }

    fun channel(freqMhz: Int): Int = when {
        freqMhz == 2484 -> 14
        freqMhz in 2400..2500 -> (freqMhz - 2407) / 5
        freqMhz in 5150..5895 -> (freqMhz - 5000) / 5
        freqMhz in 5925..7125 -> (freqMhz - 5950) / 5
        else -> -1
    }
}
```

`app/src/main/java/com/signaldekho/app/data/Readings.kt`:
```kotlin
package com.signaldekho.app.data

data class CellReading(
    val simSlot: Int,
    val operatorName: String,
    val networkType: String,   // "2G" | "3G" | "4G" | "5G" | "?"
    val dbm: Int?,
    val ageMillis: Long,
)

data class WifiReading(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
) {
    val band: WifiBand get() = WifiChannels.band(frequencyMhz)
    val channel: Int get() = WifiChannels.channel(frequencyMhz)
}

data class BleReading(val name: String?, val address: String, val rssi: Int)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.signaldekho.app.data.WifiBandTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(data): reading models + wifi band/channel mapper

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: Android repos — CellularRepo, WifiRepo, BleRepo

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/data/CellularRepo.kt`, `app/src/main/java/com/signaldekho/app/data/WifiRepo.kt`, `app/src/main/java/com/signaldekho/app/data/BleRepo.kt`

**Interfaces:**
- Consumes: `WifiThrottleScheduler` (Task 3), reading models (Task 5).
- Produces (constructor takes `Context`; all methods assume permission already granted — callers gate on permission):
```kotlin
class CellularRepo(context: Context) { fun read(): List<CellReading> }
class WifiRepo(context: Context, val scheduler: WifiThrottleScheduler) {
    fun latestResults(): List<WifiReading>          // cached scan results
    fun requestScan(): Boolean                      // false if throttled or rejected
    fun connectedSsidAndRssi(): Pair<String, Int>?  // current connection
}
class BleRepo(context: Context) {
    fun startScan(onReading: (BleReading) -> Unit)
    fun stopScan()
}
```
- No unit tests (Android-typed APIs, fake on emulator) — verified in device QA (Task 14). Deliverable = compiles.

- [ ] **Step 1: CellularRepo**

`app/src/main/java/com/signaldekho/app/data/CellularRepo.kt`:
```kotlin
package com.signaldekho.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

@SuppressLint("MissingPermission") // callers gate on ACCESS_FINE_LOCATION + READ_PHONE_STATE
class CellularRepo(private val context: Context) {

    fun read(): List<CellReading> {
        val subMgr = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val telMgr = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val subs = subMgr.activeSubscriptionInfoList ?: return emptyList()
        return subs.map { sub ->
            val tm = telMgr.createForSubscriptionId(sub.subscriptionId)
            val registered = tm.allCellInfo?.filter { it.isRegistered } ?: emptyList()
            val primary = registered.firstOrNull()
            CellReading(
                simSlot = sub.simSlotIndex + 1,
                operatorName = sub.carrierName?.toString() ?: "?",
                networkType = primary?.let { networkTypeOf(it) } ?: "?",
                dbm = primary?.let { dbmOf(it) },
                ageMillis = primary?.let {
                    (SystemClock.elapsedRealtimeNanos() - it.timestampNanos) / 1_000_000
                } ?: 0L,
            )
        }
    }

    private fun networkTypeOf(info: CellInfo): String = when (info) {
        is CellInfoNr -> "5G"
        is CellInfoLte -> "4G"
        is CellInfoWcdma -> "3G"
        is CellInfoGsm -> "2G"
        else -> "?"
    }

    private fun dbmOf(info: CellInfo): Int? {
        val dbm = when (info) {
            is CellInfoNr -> info.cellSignalStrength.dbm
            is CellInfoLte -> info.cellSignalStrength.dbm
            is CellInfoWcdma -> info.cellSignalStrength.dbm
            is CellInfoGsm -> info.cellSignalStrength.dbm
            else -> return null
        }
        return if (dbm == CellInfo.UNAVAILABLE || dbm == Int.MAX_VALUE) null else dbm
    }
}
```

Note: `CellInfoNr` requires API 29 — it's only *instantiated* by the OS on API 29+ devices, so referencing the class is safe at minSdk 26 (class-load only happens inside `when` on devices that return it).

- [ ] **Step 2: WifiRepo**

`app/src/main/java/com/signaldekho/app/data/WifiRepo.kt`:
```kotlin
package com.signaldekho.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import com.signaldekho.app.domain.WifiThrottleScheduler

@SuppressLint("MissingPermission") // callers gate on ACCESS_FINE_LOCATION
class WifiRepo(context: Context, val scheduler: WifiThrottleScheduler) {
    private val wifi = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun latestResults(): List<WifiReading> =
        wifi.scanResults
            .filter { it.SSID.isNotBlank() }
            .map { WifiReading(it.SSID, it.BSSID, it.level, it.frequency) }
            .sortedByDescending { it.rssi }

    /** Returns true if a scan was actually kicked off. */
    @Suppress("DEPRECATION") // startScan deprecated but still the only way
    fun requestScan(): Boolean {
        if (!scheduler.canScanNow()) return false
        val accepted = wifi.startScan()
        if (accepted) scheduler.recordScan()
        return accepted
    }

    @Suppress("DEPRECATION") // connectionInfo fine for current-connection display
    fun connectedSsidAndRssi(): Pair<String, Int>? {
        val info = wifi.connectionInfo ?: return null
        val ssid = info.ssid?.trim('"') ?: return null
        if (ssid.isBlank() || ssid == "<unknown ssid>") return null
        return ssid to info.rssi
    }
}
```

- [ ] **Step 3: BleRepo**

`app/src/main/java/com/signaldekho/app/data/BleRepo.kt`:
```kotlin
package com.signaldekho.app.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context

@SuppressLint("MissingPermission") // callers gate on BLUETOOTH_SCAN / location
class BleRepo(context: Context) {
    private val adapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var callback: ScanCallback? = null

    fun startScan(onReading: (BleReading) -> Unit) {
        val scanner = adapter?.bluetoothLeScanner ?: return
        if (callback != null) return
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                onReading(BleReading(result.device?.name, result.device?.address ?: "?", result.rssi))
            }
        }
        scanner.startScan(callback)
    }

    fun stopScan() {
        callback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
        callback = null
    }
}
```

- [ ] **Step 4: Build + full test suite**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all prior tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(data): cellular/wifi/ble repos

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: Room DB (SurveyStore) + App shell with manual DI

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/data/survey/SurveyEntities.kt`, `app/src/main/java/com/signaldekho/app/data/survey/SurveyDao.kt`, `app/src/main/java/com/signaldekho/app/data/survey/SurveyDb.kt`, `app/src/main/java/com/signaldekho/app/SignalDekhoApp.kt`
- Modify: `app/src/main/AndroidManifest.xml` (add `android:name=".SignalDekhoApp"` to `<application>`)

**Interfaces:**
- Produces:
```kotlin
@Entity data class Survey(val id: Long = 0, val createdAt: Long)
@Entity data class RoomReadingEntity(val id: Long = 0, val surveyId: Long, val roomName: String,
    val wifiSsid: String?, val wifiRssi: Int?, val cellDbmSim1: Int?, val cellDbmSim2: Int?, val takenAt: Long)
interface SurveyDao {
    suspend fun insertSurvey(s: Survey): Long
    suspend fun insertReading(r: RoomReadingEntity)
    suspend fun latestSurveyId(): Long?
    fun readingsFor(surveyId: Long): Flow<List<RoomReadingEntity>>
    suspend fun deleteSurvey(surveyId: Long)
}
class SignalDekhoApp : Application { val container: AppContainer }
class AppContainer(context: Context) {
    val cellularRepo: CellularRepo; val wifiRepo: WifiRepo; val bleRepo: BleRepo; val surveyDao: SurveyDao
}
```

- [ ] **Step 1: Entities + DAO + DB**

`app/src/main/java/com/signaldekho/app/data/survey/SurveyEntities.kt`:
```kotlin
package com.signaldekho.app.data.survey

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class Survey(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
)

@Entity(tableName = "room_readings")
data class RoomReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surveyId: Long,
    val roomName: String,
    val wifiSsid: String?,
    val wifiRssi: Int?,
    val cellDbmSim1: Int?,
    val cellDbmSim2: Int?,
    val takenAt: Long,
)
```

`app/src/main/java/com/signaldekho/app/data/survey/SurveyDao.kt`:
```kotlin
package com.signaldekho.app.data.survey

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Insert suspend fun insertSurvey(s: Survey): Long
    @Insert suspend fun insertReading(r: RoomReadingEntity)
    @Query("SELECT id FROM surveys ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestSurveyId(): Long?
    @Query("SELECT * FROM room_readings WHERE surveyId = :surveyId ORDER BY takenAt")
    fun readingsFor(surveyId: Long): Flow<List<RoomReadingEntity>>
    @Query("DELETE FROM surveys WHERE id = :surveyId")
    suspend fun deleteSurvey(surveyId: Long)
    @Query("DELETE FROM room_readings WHERE surveyId = :surveyId")
    suspend fun deleteReadings(surveyId: Long)
}
```

`app/src/main/java/com/signaldekho/app/data/survey/SurveyDb.kt`:
```kotlin
package com.signaldekho.app.data.survey

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Survey::class, RoomReadingEntity::class], version = 1, exportSchema = false)
abstract class SurveyDb : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao

    companion object {
        fun build(context: Context): SurveyDb =
            Room.databaseBuilder(context, SurveyDb::class.java, "signaldekho.db").build()
    }
}
```

- [ ] **Step 2: Application + AppContainer, wire into manifest**

`app/src/main/java/com/signaldekho/app/SignalDekhoApp.kt`:
```kotlin
package com.signaldekho.app

import android.app.Application
import android.content.Context
import com.signaldekho.app.data.BleRepo
import com.signaldekho.app.data.CellularRepo
import com.signaldekho.app.data.WifiRepo
import com.signaldekho.app.data.survey.SurveyDb
import com.signaldekho.app.domain.WifiThrottleScheduler

class AppContainer(context: Context) {
    val cellularRepo = CellularRepo(context)
    val wifiRepo = WifiRepo(context, WifiThrottleScheduler { System.currentTimeMillis() })
    val bleRepo = BleRepo(context)
    private val db = SurveyDb.build(context)
    val surveyDao = db.surveyDao()
}

class SignalDekhoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

In `AndroidManifest.xml`, add `android:name=".SignalDekhoApp"` as the first attribute of `<application>`.

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (KSP generates Room impl), tests PASS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(data): Room survey store + app container DI

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 8: Navigation shell + permission gate + onboarding screen

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/ui/AppNav.kt`, `app/src/main/java/com/signaldekho/app/ui/PermissionGate.kt`, `app/src/main/java/com/signaldekho/app/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/signaldekho/app/MainActivity.kt`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `SignalDekhoApp.container`.
- Produces: routes `"scanner"`, `"gharscan"`, `"report/{surveyId}"`; composable `PermissionGate(content: @Composable () -> Unit)` that shows rationale/denied UI until required runtime permissions are granted; `LocalAppContainer` CompositionLocal for repo access from ViewModels via factory.

- [ ] **Step 1: Strings for onboarding/permissions**

Append inside `<resources>` of `app/src/main/res/values/strings.xml`:
```xml
    <string name="onboarding_title">Location permission kyun?</string>
    <string name="onboarding_body">Android ka rule hai — WiFi aur signal scan ke liye location permission zaroori hai. SignalDekho aapki location kahin nahi bhejta. Sab kuch aapke phone par hi rehta hai — no internet, no ads.</string>
    <string name="onboarding_cta">Theek hai, shuru karo</string>
    <string name="perm_denied_title">Permission chahiye</string>
    <string name="perm_denied_body">Bina location permission ke signals nahi dikh sakte. Settings mein jaakar allow karein.</string>
    <string name="perm_denied_cta">Settings kholo</string>
```

- [ ] **Step 2: PermissionGate**

`app/src/main/java/com/signaldekho/app/ui/PermissionGate.kt`:
```kotlin
package com.signaldekho.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.signaldekho.app.R

val requiredPermissions: Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_SCAN)
}.toTypedArray()

@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        askedOnce = true
        granted = result.values.all { it }
    }

    if (granted) { content(); return }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!askedOnce) {
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { launcher.launch(requiredPermissions) }) {
                Text(stringResource(R.string.onboarding_cta))
            }
        } else {
            Text(stringResource(R.string.perm_denied_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.perm_denied_body), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)))
            }) { Text(stringResource(R.string.perm_denied_cta)) }
        }
    }
}
```

- [ ] **Step 3: AppNav + MainActivity**

`app/src/main/java/com/signaldekho/app/ui/AppNav.kt`:
```kotlin
package com.signaldekho.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.signaldekho.app.AppContainer
import com.signaldekho.app.ui.gharscan.GharScanScreen
import com.signaldekho.app.ui.report.ReportScreen
import com.signaldekho.app.ui.scanner.ScannerScreen

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "scanner") {
        composable("scanner") {
            ScannerScreen(onStartGharScan = { nav.navigate("gharscan") })
        }
        composable("gharscan") {
            GharScanScreen(onFinished = { surveyId ->
                nav.navigate("report/$surveyId") { popUpTo("scanner") }
            })
        }
        composable(
            "report/{surveyId}",
            arguments = listOf(navArgument("surveyId") { type = NavType.LongType }),
        ) { entry ->
            ReportScreen(surveyId = entry.arguments!!.getLong("surveyId"))
        }
    }
}
```

`MainActivity.kt` (replace whole file):
```kotlin
package com.signaldekho.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.signaldekho.app.ui.AppNav
import com.signaldekho.app.ui.LocalAppContainer
import com.signaldekho.app.ui.PermissionGate
import com.signaldekho.app.ui.theme.SignalDekhoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SignalDekhoApp).container
        setContent {
            SignalDekhoTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    PermissionGate { AppNav() }
                }
            }
        }
    }
}
```

`OnboardingScreen.kt` is not needed as a separate file — the rationale UI lives in `PermissionGate`. Do NOT create it (spec's "onboarding screen" == this gate).

NOTE: This task references `ScannerScreen`, `GharScanScreen`, `ReportScreen` which don't exist yet. To keep the build green, create the three files now with placeholder signatures that Tasks 9–11 will replace entirely:

`app/src/main/java/com/signaldekho/app/ui/scanner/ScannerScreen.kt`:
```kotlin
package com.signaldekho.app.ui.scanner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ScannerScreen(onStartGharScan: () -> Unit) { Text("scanner") }
```
`app/src/main/java/com/signaldekho/app/ui/gharscan/GharScanScreen.kt`:
```kotlin
package com.signaldekho.app.ui.gharscan

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GharScanScreen(onFinished: (Long) -> Unit) { Text("gharscan") }
```
`app/src/main/java/com/signaldekho/app/ui/report/ReportScreen.kt`:
```kotlin
package com.signaldekho.app.ui.report

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ReportScreen(surveyId: Long) { Text("report") }
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): nav shell + permission gate with onboarding rationale

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 9: Scanner screen (SIM + WiFi + BLE with countdown)

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/ui/scanner/ScannerViewModel.kt`
- Modify (replace): `app/src/main/java/com/signaldekho/app/ui/scanner/ScannerScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `CellularRepo.read()`, `WifiRepo` (all three methods + `scheduler.nextAllowedAtMillis()`), `BleRepo.startScan/stopScan`, `SignalGrade`, `Grade`, theme colors `GradeGood/GradeOk/GradeWeak`.
- Produces: `ScannerScreen(onStartGharScan: () -> Unit)` — final version.

- [ ] **Step 1: Strings**

Append to `strings.xml`:
```xml
    <string name="scanner_sim_header">SIM Signal</string>
    <string name="scanner_wifi_header">WiFi Networks</string>
    <string name="scanner_ble_header">Bluetooth Devices</string>
    <string name="scanner_refresh">Refresh</string>
    <string name="scanner_next_scan_in">Agla scan %1$d sec mein</string>
    <string name="scanner_no_sim">SIM nahi mili</string>
    <string name="scanner_no_wifi">Koi WiFi network nahi dikha — Refresh dabao</string>
    <string name="scanner_no_ble">Koi Bluetooth device paas nahi</string>
    <string name="scanner_unknown_device">Unknown device</string>
    <string name="scanner_dbm">%1$d dBm</string>
    <string name="scanner_ghar_scan_cta">Ghar Scan karo</string>
    <string name="scanner_connected">Connected</string>
    <string name="scanner_reading_age">%1$d sec purana</string>
</resources>
```
(the `</resources>` shown is the file's existing closing tag — new strings go above it; same pattern in later string steps.)

- [ ] **Step 2: ViewModel**

`app/src/main/java/com/signaldekho/app/ui/scanner/ScannerViewModel.kt`:
```kotlin
package com.signaldekho.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldekho.app.data.BleReading
import com.signaldekho.app.data.BleRepo
import com.signaldekho.app.data.CellReading
import com.signaldekho.app.data.CellularRepo
import com.signaldekho.app.data.WifiReading
import com.signaldekho.app.data.WifiRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerState(
    val cells: List<CellReading> = emptyList(),
    val wifi: List<WifiReading> = emptyList(),
    val connectedSsid: String? = null,
    val ble: List<BleReading> = emptyList(),
    val secondsToNextScan: Int = 0,
)

class ScannerViewModel(
    private val cellularRepo: CellularRepo,
    private val wifiRepo: WifiRepo,
    private val bleRepo: BleRepo,
) : ViewModel() {
    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()
    private val bleSeen = LinkedHashMap<String, BleReading>() // keyed by address

    fun start() {
        refresh()
        bleRepo.startScan { reading ->
            bleSeen[reading.address] = reading
            _state.update { it.copy(ble = bleSeen.values.sortedByDescending { b -> b.rssi }) }
        }
        viewModelScope.launch {           // 1s tick: cellular + countdown
            while (true) {
                val next = wifiRepo.scheduler.nextAllowedAtMillis()
                val secs = ((next - System.currentTimeMillis()).coerceAtLeast(0) / 1000).toInt()
                _state.update {
                    it.copy(cells = cellularRepo.read(), secondsToNextScan = secs)
                }
                delay(1000)
            }
        }
    }

    fun stop() = bleRepo.stopScan()

    fun refresh() {
        wifiRepo.requestScan()
        _state.update {
            it.copy(
                wifi = wifiRepo.latestResults(),
                connectedSsid = wifiRepo.connectedSsidAndRssi()?.first,
                cells = cellularRepo.read(),
            )
        }
    }

    override fun onCleared() = stop()
}
```

- [ ] **Step 3: Screen (replace placeholder entirely)**

`app/src/main/java/com/signaldekho/app/ui/scanner/ScannerScreen.kt`:
```kotlin
package com.signaldekho.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signaldekho.app.R
import com.signaldekho.app.domain.Grade
import com.signaldekho.app.domain.SignalGrade
import com.signaldekho.app.ui.LocalAppContainer
import com.signaldekho.app.ui.theme.GradeGood
import com.signaldekho.app.ui.theme.GradeOk
import com.signaldekho.app.ui.theme.GradeWeak

fun gradeColor(g: Grade): Color = when (g) {
    Grade.GOOD -> GradeGood
    Grade.OK -> GradeOk
    Grade.WEAK -> GradeWeak
}

@Composable
private fun GradeDot(g: Grade) {
    Box(Modifier.size(12.dp).background(gradeColor(g), CircleShape))
}

@Composable
fun ScannerScreen(onStartGharScan: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ScannerViewModel = viewModel {
        ScannerViewModel(container.cellularRepo, container.wifiRepo, container.bleRepo)
    }
    val state by vm.state.collectAsState()
    DisposableEffect(Unit) {
        vm.start()
        onDispose { vm.stop() }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Button(onClick = onStartGharScan) { Text(stringResource(R.string.scanner_ghar_scan_cta)) }
            }
        }

        item { Text(stringResource(R.string.scanner_sim_header), style = MaterialTheme.typography.titleMedium) }
        if (state.cells.isEmpty()) {
            item { Text(stringResource(R.string.scanner_no_sim)) }
        } else {
            items(state.cells) { cell ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cell.dbm?.let { GradeDot(SignalGrade.cell(it)) }
                    Text("SIM${cell.simSlot} ${cell.operatorName} · ${cell.networkType}")
                    Text(cell.dbm?.let { stringResource(R.string.scanner_dbm, it) } ?: "—",
                        style = MaterialTheme.typography.bodyMedium)
                    if (cell.ageMillis > 10_000) {
                        Text(stringResource(R.string.scanner_reading_age, cell.ageMillis / 1000),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.scanner_wifi_header), style = MaterialTheme.typography.titleMedium)
                if (state.secondsToNextScan > 0) {
                    Text(stringResource(R.string.scanner_next_scan_in, state.secondsToNextScan),
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedButton(onClick = { vm.refresh() }) { Text(stringResource(R.string.scanner_refresh)) }
                }
            }
        }
        if (state.wifi.isEmpty()) {
            item { Text(stringResource(R.string.scanner_no_wifi)) }
        } else {
            items(state.wifi, key = { it.bssid }) { net ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradeDot(SignalGrade.wifi(net.rssi))
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(net.ssid, style = MaterialTheme.typography.bodyLarge)
                            if (net.ssid == state.connectedSsid) {
                                Text(stringResource(R.string.scanner_connected),
                                    style = MaterialTheme.typography.labelSmall, color = GradeGood)
                            }
                        }
                        Text("${net.rssi} dBm · ch ${net.channel} · ${net.band.name.removePrefix("GHZ_").replace('_', '.')} GHz",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text(stringResource(R.string.scanner_ble_header), style = MaterialTheme.typography.titleMedium) }
        if (state.ble.isEmpty()) {
            item { Text(stringResource(R.string.scanner_no_ble)) }
        } else {
            items(state.ble, key = { it.address }) { dev ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradeDot(SignalGrade.wifi(dev.rssi))
                    Text(dev.name ?: stringResource(R.string.scanner_unknown_device))
                    Text(stringResource(R.string.scanner_dbm, dev.rssi),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): scanner screen — SIM/WiFi/BLE with throttle countdown

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 10: Ghar Scan flow

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/ui/gharscan/GharScanViewModel.kt`
- Modify (replace): `app/src/main/java/com/signaldekho/app/ui/gharscan/GharScanScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `WifiRepo.connectedSsidAndRssi()`, `CellularRepo.read()`, `SurveyDao` (insertSurvey/insertReading), `RoomReadingEntity`, `Survey`.
- Produces: `GharScanScreen(onFinished: (Long) -> Unit)` — final version; calls `onFinished(surveyId)` when user taps done.

- [ ] **Step 1: Strings**

Append to `strings.xml`:
```xml
    <string name="ghar_title">Ghar Scan</string>
    <string name="ghar_room_hint">Room ka naam</string>
    <string name="ghar_measure">Naap lo</string>
    <string name="ghar_measured">%1$s ho gaya ✓</string>
    <string name="ghar_done">Report dekho</string>
    <string name="ghar_instructions">Har room mein jao, naam likho (ya chip dabao), phir \"Naap lo\" dabao. Kam se kam 2 rooms naapo.</string>
    <string name="ghar_chip_bedroom">Bedroom</string>
    <string name="ghar_chip_kitchen">Kitchen</string>
    <string name="ghar_chip_hall">Hall</string>
    <string name="ghar_chip_bathroom">Bathroom</string>
    <string name="ghar_chip_balcony">Balcony</string>
    <string name="ghar_chip_chhat">Chhat</string>
```

- [ ] **Step 2: ViewModel**

`app/src/main/java/com/signaldekho/app/ui/gharscan/GharScanViewModel.kt`:
```kotlin
package com.signaldekho.app.ui.gharscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldekho.app.data.CellularRepo
import com.signaldekho.app.data.WifiRepo
import com.signaldekho.app.data.survey.RoomReadingEntity
import com.signaldekho.app.data.survey.Survey
import com.signaldekho.app.data.survey.SurveyDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GharScanState(
    val measuredRooms: List<String> = emptyList(),
    val roomInput: String = "",
    val saving: Boolean = false,
)

class GharScanViewModel(
    private val wifiRepo: WifiRepo,
    private val cellularRepo: CellularRepo,
    private val dao: SurveyDao,
) : ViewModel() {
    private val _state = MutableStateFlow(GharScanState())
    val state: StateFlow<GharScanState> = _state.asStateFlow()
    private var surveyId: Long? = null

    fun setRoomInput(name: String) = _state.update { it.copy(roomInput = name) }

    fun measureCurrentRoom() {
        val room = _state.value.roomInput.trim()
        if (room.isEmpty() || _state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = surveyId ?: dao.insertSurvey(Survey(createdAt = System.currentTimeMillis()))
                .also { surveyId = it }
            val wifi = wifiRepo.connectedSsidAndRssi()
            val cells = cellularRepo.read()
            dao.insertReading(
                RoomReadingEntity(
                    surveyId = id,
                    roomName = room,
                    wifiSsid = wifi?.first,
                    wifiRssi = wifi?.second,
                    cellDbmSim1 = cells.getOrNull(0)?.dbm,
                    cellDbmSim2 = cells.getOrNull(1)?.dbm,
                    takenAt = System.currentTimeMillis(),
                )
            )
            _state.update {
                it.copy(measuredRooms = it.measuredRooms + room, roomInput = "", saving = false)
            }
        }
    }

    fun finish(onFinished: (Long) -> Unit) {
        surveyId?.let(onFinished)
    }
}
```

- [ ] **Step 3: Screen (replace placeholder entirely)**

`app/src/main/java/com/signaldekho/app/ui/gharscan/GharScanScreen.kt`:
```kotlin
package com.signaldekho.app.ui.gharscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signaldekho.app.R
import com.signaldekho.app.ui.LocalAppContainer

@Composable
fun GharScanScreen(onFinished: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: GharScanViewModel = viewModel {
        GharScanViewModel(container.wifiRepo, container.cellularRepo, container.surveyDao)
    }
    val state by vm.state.collectAsState()
    val chips = listOf(
        stringResource(R.string.ghar_chip_bedroom), stringResource(R.string.ghar_chip_kitchen),
        stringResource(R.string.ghar_chip_hall), stringResource(R.string.ghar_chip_bathroom),
        stringResource(R.string.ghar_chip_balcony), stringResource(R.string.ghar_chip_chhat),
    )

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.ghar_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.ghar_instructions), style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = state.roomInput,
            onValueChange = vm::setRoomInput,
            label = { Text(stringResource(R.string.ghar_room_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips) { chip ->
                AssistChip(onClick = { vm.setRoomInput(chip) }, label = { Text(chip) })
            }
        }
        Button(
            onClick = vm::measureCurrentRoom,
            enabled = state.roomInput.isNotBlank() && !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.ghar_measure)) }

        state.measuredRooms.forEach { room ->
            Text(stringResource(R.string.ghar_measured, room))
        }

        if (state.measuredRooms.size >= 2) {
            OutlinedButton(
                onClick = { vm.finish(onFinished) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.ghar_done)) }
        }
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): ghar scan room-tagging survey flow

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 11: Report screen with findings

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/ui/report/ReportViewModel.kt`
- Modify (replace): `app/src/main/java/com/signaldekho/app/ui/report/ReportScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `SurveyDao.readingsFor(surveyId)`, `RecommendationEngine.analyze`, `RoomResult`, `Finding`, `SignalGrade`, `gradeColor` (Task 9).
- Produces: `ReportScreen(surveyId: Long)` — final version. Also `data class ReportRow(val room: String, val wifiRssi: Int?, val cellDbm: Int?)` and `findingText(f: Finding): String` mapping used by share-image (Task 12).

- [ ] **Step 1: Strings**

Append to `strings.xml`:
```xml
    <string name="report_title">Coverage Report</string>
    <string name="report_wifi_col">WiFi</string>
    <string name="report_cell_col">SIM</string>
    <string name="report_share">WhatsApp pe share karo</string>
    <string name="report_finding_best_wifi">Sabse accha WiFi: %1$s</string>
    <string name="report_finding_weakest_wifi">Sabse kamzor WiFi: %1$s</string>
    <string name="report_finding_weakest_cell">Sabse kamzor SIM signal: %1$s</string>
    <string name="report_finding_wifi_all_good">Har room mein WiFi badhiya hai 🎉</string>
    <string name="report_finding_router">Router ko ghar ke beech mein, oonchi jagah rakho — deewaron se door.</string>
    <string name="report_no_reading">—</string>
</resources>
```

- [ ] **Step 2: ViewModel**

`app/src/main/java/com/signaldekho/app/ui/report/ReportViewModel.kt`:
```kotlin
package com.signaldekho.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldekho.app.data.survey.SurveyDao
import com.signaldekho.app.domain.Finding
import com.signaldekho.app.domain.RecommendationEngine
import com.signaldekho.app.domain.RoomResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReportRow(val room: String, val wifiRssi: Int?, val cellDbm: Int?)
data class ReportState(val rows: List<ReportRow> = emptyList(), val findings: List<Finding> = emptyList())

class ReportViewModel(dao: SurveyDao, surveyId: Long) : ViewModel() {
    val state: StateFlow<ReportState> = dao.readingsFor(surveyId)
        .map { readings ->
            val rows = readings.map { r ->
                // strongest SIM reading represents the room
                val cell = listOfNotNull(r.cellDbmSim1, r.cellDbmSim2).maxOrNull()
                ReportRow(r.roomName, r.wifiRssi, cell)
            }
            val findings = RecommendationEngine.analyze(
                rows.map { RoomResult(it.room, it.wifiRssi, it.cellDbm) }
            )
            ReportState(rows, findings)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReportState())
}
```

- [ ] **Step 3: Screen (replace placeholder entirely)**

`app/src/main/java/com/signaldekho/app/ui/report/ReportScreen.kt`:
```kotlin
package com.signaldekho.app.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signaldekho.app.R
import com.signaldekho.app.domain.Finding
import com.signaldekho.app.domain.SignalGrade
import com.signaldekho.app.ui.LocalAppContainer
import com.signaldekho.app.ui.scanner.gradeColor

@Composable
fun findingText(f: Finding): String = when (f) {
    is Finding.BestWifiRoom -> stringResource(R.string.report_finding_best_wifi, f.room)
    is Finding.WeakestWifiRoom -> stringResource(R.string.report_finding_weakest_wifi, f.room)
    is Finding.WeakestCellRoom -> stringResource(R.string.report_finding_weakest_cell, f.room)
    Finding.WifiAllGood -> stringResource(R.string.report_finding_wifi_all_good)
    Finding.RouterReposition -> stringResource(R.string.report_finding_router)
}

@Composable
fun ReportScreen(surveyId: Long) {
    val container = LocalAppContainer.current
    val vm: ReportViewModel = viewModel { ReportViewModel(container.surveyDao, surveyId) }
    val state by vm.state.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(stringResource(R.string.report_title), style = MaterialTheme.typography.headlineMedium) }

        items(state.rows) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.room, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.report_wifi_col) + " " +
                            (row.wifiRssi?.let { "${it} dBm" } ?: stringResource(R.string.report_no_reading)),
                        color = row.wifiRssi?.let { gradeColor(SignalGrade.wifi(it)) }
                            ?: MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.report_cell_col) + " " +
                            (row.cellDbm?.let { "${it} dBm" } ?: stringResource(R.string.report_no_reading)),
                        color = row.cellDbm?.let { gradeColor(SignalGrade.cell(it)) }
                            ?: MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        item { HorizontalDivider() }
        items(state.findings) { f ->
            Text("• " + findingText(f), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
```

(Share button arrives in Task 12.)

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): coverage report with color-coded rooms + findings

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 12: Share report as image

**Files:**
- Create: `app/src/main/java/com/signaldekho/app/ui/report/ReportImageRenderer.kt`, `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml` (FileProvider), `app/src/main/java/com/signaldekho/app/ui/report/ReportScreen.kt` (share button)

**Interfaces:**
- Consumes: `ReportRow`, `Finding` (Task 11), `SignalGrade`, grade colors.
- Produces: `object ReportImageRenderer { fun render(context: Context, rows: List<ReportRow>, findingTexts: List<String>): File }` — draws a 1080-wide PNG into `context.cacheDir/reports/report.png`.

- [ ] **Step 1: FileProvider config**

`app/src/main/res/xml/file_paths.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="reports" path="reports/" />
</paths>
```

Inside `<application>` in `AndroidManifest.xml`, add:
```xml
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="com.signaldekho.app.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
```

- [ ] **Step 2: Renderer**

`app/src/main/java/com/signaldekho/app/ui/report/ReportImageRenderer.kt`:
```kotlin
package com.signaldekho.app.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.signaldekho.app.domain.Grade
import com.signaldekho.app.domain.SignalGrade
import java.io.File

object ReportImageRenderer {
    private const val W = 1080
    private const val PAD = 48f
    private const val ROW_H = 88f

    private fun gradeArgb(g: Grade): Int = when (g) {
        Grade.GOOD -> 0xFF2E7D32.toInt()
        Grade.OK -> 0xFFF9A825.toInt()
        Grade.WEAK -> 0xFFC62828.toInt()
    }

    fun render(context: Context, rows: List<ReportRow>, findingTexts: List<String>): File {
        val height = (PAD * 2 + 140 + rows.size * ROW_H + findingTexts.size * 72 + 120).toInt()
        val bmp = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 64f; isFakeBoldText = true
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 44f }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 34f }

        var y = PAD + 64
        c.drawText("SignalDekho — Coverage Report", PAD, y, title)
        y += 76

        rows.forEach { row ->
            row.wifiRssi?.let {
                body.color = gradeArgb(SignalGrade.wifi(it))
                c.drawCircle(PAD + 16, y - 14, 16f, body)
            }
            body.color = Color.BLACK
            c.drawText(row.room, PAD + 56, y, body)
            val wifiTxt = row.wifiRssi?.let { "WiFi ${it} dBm" } ?: "WiFi —"
            val cellTxt = row.cellDbm?.let { "SIM ${it} dBm" } ?: "SIM —"
            c.drawText("$wifiTxt   $cellTxt", W / 2f, y, body)
            y += ROW_H
        }

        y += 24
        findingTexts.forEach { txt ->
            c.drawText("• $txt", PAD, y, body)
            y += 72
        }

        c.drawText("Made with SignalDekho", PAD, height - PAD, small)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "report.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }
}
```

- [ ] **Step 3: Share button in ReportScreen**

In `ReportScreen.kt`, add imports:
```kotlin
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
```
Add inside the `LazyColumn`, after the findings items (findings need resolving to plain strings first — capture them before the button):
```kotlin
        item {
            val context = LocalContext.current
            val findingStrings = state.findings.map { findingText(it) }
            Button(onClick = {
                val file = ReportImageRenderer.render(context, state.rows, findingStrings)
                val uri = FileProvider.getUriForFile(context, "com.signaldekho.app.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, null))
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.report_share))
            }
        }
```
Note: `findingText` is `@Composable`, so `findingStrings` must be computed in composition (as shown), NOT inside the `onClick` lambda.

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(report): render report to PNG + share sheet

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 13: Hindi strings

**Files:**
- Create: `app/src/main/res/values-hi/strings.xml`

**Interfaces:**
- Consumes: every string key defined in Tasks 1, 8, 9, 10, 11. Key set must match `values/strings.xml` exactly (missing keys fall back to English — verify none are missing by diffing keys).

- [ ] **Step 1: Write full Hindi resource file**

`app/src/main/res/values-hi/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SignalDekho</string>
    <string name="onboarding_title">Location permission क्यों?</string>
    <string name="onboarding_body">Android का नियम है — WiFi और signal scan के लिए location permission ज़रूरी है। SignalDekho आपकी location कहीं नहीं भेजता। सब कुछ आपके phone पर ही रहता है — no internet, no ads.</string>
    <string name="onboarding_cta">ठीक है, शुरू करो</string>
    <string name="perm_denied_title">Permission चाहिए</string>
    <string name="perm_denied_body">बिना location permission के signals नहीं दिख सकते। Settings में जाकर allow करें।</string>
    <string name="perm_denied_cta">Settings खोलो</string>
    <string name="scanner_sim_header">SIM Signal</string>
    <string name="scanner_wifi_header">WiFi Networks</string>
    <string name="scanner_ble_header">Bluetooth Devices</string>
    <string name="scanner_refresh">Refresh</string>
    <string name="scanner_next_scan_in">अगला scan %1$d sec में</string>
    <string name="scanner_no_sim">SIM नहीं मिली</string>
    <string name="scanner_no_wifi">कोई WiFi network नहीं दिखा — Refresh दबाओ</string>
    <string name="scanner_no_ble">कोई Bluetooth device पास नहीं</string>
    <string name="scanner_unknown_device">अनजान device</string>
    <string name="scanner_dbm">%1$d dBm</string>
    <string name="scanner_ghar_scan_cta">घर Scan करो</string>
    <string name="scanner_connected">Connected</string>
    <string name="scanner_reading_age">%1$d sec पुराना</string>
    <string name="scanner_sim_line">SIM%1$d %2$s · %3$s</string>
    <string name="scanner_wifi_detail">%1$d dBm · ch %2$d · %3$s GHz</string>
    <string name="scanner_no_reading">—</string>
    <string name="ghar_title">घर Scan</string>
    <string name="ghar_room_hint">Room का नाम</string>
    <string name="ghar_measure">नाप लो</string>
    <string name="ghar_measured">%1$s हो गया ✓</string>
    <string name="ghar_done">Report देखो</string>
    <string name="ghar_instructions">हर room में जाओ, नाम लिखो (या chip दबाओ), फिर \"नाप लो\" दबाओ। कम से कम 2 rooms नापो।</string>
    <string name="ghar_chip_bedroom">Bedroom</string>
    <string name="ghar_chip_kitchen">Kitchen</string>
    <string name="ghar_chip_hall">Hall</string>
    <string name="ghar_chip_bathroom">Bathroom</string>
    <string name="ghar_chip_balcony">Balcony</string>
    <string name="ghar_chip_chhat">छत</string>
    <string name="report_title">Coverage Report</string>
    <string name="report_wifi_col">WiFi</string>
    <string name="report_cell_col">SIM</string>
    <string name="report_share">WhatsApp पे share करो</string>
    <string name="report_finding_best_wifi">सबसे अच्छा WiFi: %1$s</string>
    <string name="report_finding_weakest_wifi">सबसे कमज़ोर WiFi: %1$s</string>
    <string name="report_finding_weakest_cell">सबसे कमज़ोर SIM signal: %1$s</string>
    <string name="report_finding_wifi_all_good">हर room में WiFi बढ़िया है 🎉</string>
    <string name="report_finding_router">Router को घर के बीच में, ऊँची जगह रखो — दीवारों से दूर।</string>
    <string name="report_no_reading">—</string>
</resources>
```

- [ ] **Step 2: Verify key parity**

```bash
diff <(grep -o 'name="[^"]*"' app/src/main/res/values/strings.xml | sort) \
     <(grep -o 'name="[^"]*"' app/src/main/res/values-hi/strings.xml | sort)
```
Expected: no output (identical key sets). If keys differ, fix before committing.

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(i18n): Hindi strings (values-hi)

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 14: Device QA checklist + README

**Files:**
- Create: `README.md`, `docs/device-qa.md`

**Interfaces:** none — documentation deliverable. Device QA itself is performed with Sunil's physical Android phone connected over USB (`adb devices` must list it).

- [ ] **Step 1: README**

`README.md`:
```markdown
# SignalDekho

Free Android app: dekho kaun se signals hain aur kitne strong — SIM/cellular,
WiFi, Bluetooth — plus "Ghar Scan" room-by-room coverage report (EN/HI).

No ads. No analytics. No internet permission. Data phone se bahar nahi jata.

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
```

- [ ] **Step 2: QA checklist**

`docs/device-qa.md`:
```markdown
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
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "docs: README + device QA checklist

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 15: Play Store prep notes (docs only — actual listing needs Sunil's Play Console)

**Files:**
- Create: `docs/play-store.md`

- [ ] **Step 1: Write listing prep doc**

`docs/play-store.md`:
```markdown
# Play Store prep

## Decisions
- Final app name: **SignalDekho** (locked 2026-07-29; "Signal Range" available
  tha but generic)

## Console tasks (manual, Sunil's account)
1. App listing: category Tools; title + short/full description (EN + HI)
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
- "Sab signals ek jagah" — SIM + WiFi + Bluetooth
- Free Ghar Scan coverage report (competition charges for this)
- Privacy: no internet permission — data phone se bahar ja hi nahi sakta
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "docs: play store prep notes

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```
