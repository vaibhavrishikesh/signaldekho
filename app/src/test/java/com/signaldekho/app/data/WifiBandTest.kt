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
