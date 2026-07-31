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
