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
