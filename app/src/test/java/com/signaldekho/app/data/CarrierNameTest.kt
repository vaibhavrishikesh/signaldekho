package com.signaldekho.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CarrierNameTest {
    @Test fun `strips redundant network-type suffix`() {
        assertEquals("Jio", CarrierName.clean("JIO 4G — Jio"))
        assertEquals("Jio", CarrierName.clean("Jio 4G"))
        assertEquals("airtel", CarrierName.clean("airtel 5G"))
    }

    @Test fun `keeps plain names untouched`() {
        assertEquals("Vi India", CarrierName.clean("Vi India"))
        assertEquals("BSNL", CarrierName.clean("BSNL"))
    }

    @Test fun `falls back for blank input`() {
        assertEquals("SIM", CarrierName.clean("   "))
    }
}
