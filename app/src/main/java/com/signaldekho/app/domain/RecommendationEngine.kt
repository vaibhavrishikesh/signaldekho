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
