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
