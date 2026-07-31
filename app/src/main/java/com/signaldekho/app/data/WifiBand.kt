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
