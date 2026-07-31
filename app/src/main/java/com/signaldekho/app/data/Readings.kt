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
