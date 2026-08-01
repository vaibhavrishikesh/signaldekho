package com.signaldekho.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    @Test fun `empty survey yields no findings`() {
        assertEquals(emptyList<Finding>(), RecommendationEngine.analyze(emptyList()))
    }

    @Test fun `no wifi readings anywhere reports not measured and no wifi ranking`() {
        val rooms = listOf(
            RoomResult("Bedroom", wifiRssi = null, cellDbm = -111),
            RoomResult("Kitchen", wifiRssi = null, cellDbm = -111),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.WifiNotMeasured))
        assertTrue(findings.none { it is Finding.BestWifiRoom })
        assertTrue(findings.none { it is Finding.WeakestWifiRoom })
        assertTrue(findings.none { it is Finding.WifiAllGood })
    }

    @Test fun `identical cell readings yield similar instead of a fake ranking`() {
        val rooms = listOf(
            RoomResult("Bedroom", wifiRssi = null, cellDbm = -111),
            RoomResult("Kitchen", wifiRssi = null, cellDbm = -111),
            RoomResult("Hall", wifiRssi = null, cellDbm = -110),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.AllRoomsSimilarCell))
        assertTrue(findings.none { it is Finding.WeakestCellRoom })
        assertTrue(findings.none { it is Finding.BestRoomForCalls })
    }

    @Test fun `near-identical wifi readings yield similar instead of ranking`() {
        val rooms = listOf(
            RoomResult("Hall", wifiRssi = -60, cellDbm = null),
            RoomResult("Bedroom", wifiRssi = -62, cellDbm = null),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.AllRoomsSimilarWifi))
        assertTrue(findings.none { it is Finding.BestWifiRoom })
        assertTrue(findings.none { it is Finding.WeakestWifiRoom })
    }

    @Test fun `spread wifi readings rank rooms and advise repositioning`() {
        val rooms = listOf(
            RoomResult("Hall", wifiRssi = -50, cellDbm = -80),
            RoomResult("Kitchen", wifiRssi = -85, cellDbm = -84),
            RoomResult("Bedroom", wifiRssi = -65, cellDbm = -82),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.BestWifiRoom("Hall")))
        assertTrue(findings.contains(Finding.WeakestWifiRoom("Kitchen")))
        assertTrue(findings.contains(Finding.RouterReposition))
    }

    @Test fun `all-good wifi celebrates without weakest or reposition`() {
        val rooms = listOf(
            RoomResult("Hall", wifiRssi = -40, cellDbm = null),
            RoomResult("Bedroom", wifiRssi = -52, cellDbm = null),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.WifiAllGood))
        assertTrue(findings.none { it is Finding.RouterReposition })
        assertTrue(findings.none { it is Finding.WeakestWifiRoom })
    }

    @Test fun `spread cell readings name the best room for calls and the weakest`() {
        val rooms = listOf(
            RoomResult("Roof", wifiRssi = null, cellDbm = -80),
            RoomResult("Store", wifiRssi = null, cellDbm = -112),
        )
        val findings = RecommendationEngine.analyze(rooms)
        assertTrue(findings.contains(Finding.BestRoomForCalls("Roof")))
        assertTrue(findings.contains(Finding.WeakestCellRoom("Store")))
    }
}
