package com.signaldekho.app.domain

data class RoomResult(val roomName: String, val wifiRssi: Int?, val cellDbm: Int?)

sealed interface Finding {
    data class BestWifiRoom(val room: String) : Finding
    data class WeakestWifiRoom(val room: String) : Finding
    data class WeakestCellRoom(val room: String) : Finding
    data class BestRoomForCalls(val room: String) : Finding
    data object WifiAllGood : Finding
    data object RouterReposition : Finding
    data object WifiNotMeasured : Finding
    data object AllRoomsSimilarWifi : Finding
    data object AllRoomsSimilarCell : Finding
}

object RecommendationEngine {
    /** Readings within this many dBm of each other are treated as the same. */
    private const val SIMILAR_DBM = 3

    fun analyze(rooms: List<RoomResult>): List<Finding> {
        if (rooms.isEmpty()) return emptyList()
        return buildList {
            addAll(wifiFindings(rooms))
            addAll(cellFindings(rooms))
        }
    }

    private fun wifiFindings(rooms: List<RoomResult>): List<Finding> {
        val readings = rooms.mapNotNull { r -> r.wifiRssi?.let { r.roomName to it } }
        if (readings.isEmpty()) return listOf(Finding.WifiNotMeasured)
        if (readings.all { SignalGrade.wifi(it.second) == Grade.EXCELLENT }) {
            return listOf(Finding.WifiAllGood)
        }

        // Ranking findings only with 2+ readings
        return buildList {
            if (readings.size >= 2) {
                if (isSimilar(readings.map { it.second })) {
                    add(Finding.AllRoomsSimilarWifi)
                } else {
                    val best = readings.maxBy { it.second }
                    val worst = readings.minBy { it.second }
                    add(Finding.BestWifiRoom(best.first))
                    add(Finding.WeakestWifiRoom(worst.first))
                }
            }
            // Advice always available (even single reading)
            if (readings.any { SignalGrade.wifi(it.second) == Grade.VERY_WEAK }) {
                add(Finding.RouterReposition)
            }
        }
    }

    private fun cellFindings(rooms: List<RoomResult>): List<Finding> {
        val readings = rooms.mapNotNull { r -> r.cellDbm?.let { r.roomName to it } }
        if (readings.isEmpty()) return emptyList()

        // Ranking findings only with 2+ readings
        if (readings.size >= 2) {
            if (isSimilar(readings.map { it.second })) return listOf(Finding.AllRoomsSimilarCell)

            val best = readings.maxBy { it.second }
            val worst = readings.minBy { it.second }
            return buildList {
                add(Finding.BestRoomForCalls(best.first))
                if (SignalGrade.cell(worst.second) != Grade.EXCELLENT) {
                    add(Finding.WeakestCellRoom(worst.first))
                }
            }
        }
        return emptyList()
    }

    private fun isSimilar(values: List<Int>): Boolean =
        values.size >= 2 && (values.max() - values.min()) <= SIMILAR_DBM
}
