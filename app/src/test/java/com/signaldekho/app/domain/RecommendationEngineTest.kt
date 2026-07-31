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
